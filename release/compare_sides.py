import argparse
import json
import os
import sys
from collections import Counter

from candidate_rules import GUILD_FIGURES, guild_figures, member_figures
from derived_figures import with_derived_figures
from exact_explanations import explain
from expected_deviations import ADDITIONS, LISTED_ADDITIONS, NOT_REACHED
from exact_sums import read_exact_sums, read_recompute_instants
from process_group import is_alive, members_of, stop_group
from report import Report
from side_fetch import PAGE_SIZE, Pacer, fetch_side, http_get_json, wait_for_recompute
from shown_figures import Accepted, Context, differences, exact_differences, explanation, running_rule_differences, text_of

TOKEN_VARIABLE = "EVERGORE_SECURITY_API_TOKEN"
LEDGERS = ("bank", "storage")
CRASH_EXIT_CODE = 4
ADMIN_NAME_LISTS = ("unknownItemNames", "failedAvatarNames")


def compare(running, candidate, started=None):
    findings = []
    accepted = []
    field_findings, added_fields = _field_set_differences(running, candidate)
    findings.extend(field_findings)
    findings.extend(_admin_differences(running["admin"], candidate["admin"]))
    findings.extend(_recompute_status_findings("running", running["admin"]))
    findings.extend(_recompute_status_findings("candidate", candidate["admin"]))
    if started is not None:
        unstored = _members_without_stored_sums(running, candidate)
        findings.extend(_stale_sums_findings("running", running, started, unstored))
        findings.extend(_stale_sums_findings("candidate", candidate, started, unstored))
    findings.extend(_total_count_differences("overview", running["overview"], candidate["overview"]))
    findings.extend(_member_differences(running["overview"], candidate["overview"]))
    shown = differences(with_derived_figures(running["overview"]), with_derived_figures(candidate["overview"]))
    if "exact" in running and "exact" in candidate:
        differing = exact_differences(running, candidate)
        explained = explain(differing, running, candidate)
        findings.extend(_exact_differences(differing, explained))
        findings.extend(running_rule_differences(running))
        findings.extend(_candidate_rounding_differences(candidate))
    else:
        differing = None
        explained = None
    for difference in shown:
        rules = [] if differing is None else explanation(difference, Context(differing, explained, running, candidate))
        if not rules:
            findings.append(text_of(difference))
        accepted.extend(Accepted(rule, *difference) for rule in rules)
    findings.extend(_ledger_differences(running["ledgers"], candidate["ledgers"]))
    uses = Counter(a.rule for a in accepted)
    uses[ADDITIONS] += len(added_fields)
    for keys in (explained or {}).values():
        uses.update(keys.values())
    return Report(findings, accepted, _uncompared_guild_figures(accepted, candidate), uses)


def _uncompared_guild_figures(accepted, candidate):
    if not any(a.scope == "totals" and a.rule == NOT_REACHED for a in accepted):
        return []
    totals = candidate["overview"]["totals"]
    return [figure for figure in GUILD_FIGURES if totals.get(figure) is None]


def _exact_differences(differing, explained):
    for member, keys in differing.items():
        for key, (running, candidate) in keys.items():
            if key in explained[member]:
                continue
            yield f"{member}: exact {key} differs, running {running!r}, candidate {candidate!r}"


def _ledger_differences(running, candidate):
    for member in sorted(running.keys() & candidate.keys()):
        for ledger in LEDGERS:
            scope = f"{member} {ledger} ledger"
            yield from _total_count_differences(scope, running[member][ledger], candidate[member][ledger])
            yield from _entry_differences(scope, running[member][ledger], candidate[member][ledger])


def _entry_differences(scope, running, candidate):
    running_entries = _entry_multiset(running)
    candidate_entries = _entry_multiset(candidate)
    for entry, count in sorted((running_entries - candidate_entries).items()):
        yield f"{scope}: {count} more of {entry} in the running side only"
    for entry, count in sorted((candidate_entries - running_entries).items()):
        yield f"{scope}: {count} more of {entry} in the candidate side only"


def _entry_multiset(ledger):
    return Counter(json.dumps(entry, sort_keys=True, ensure_ascii=False) for entry in ledger["items"])


def _total_count_differences(scope, running, candidate):
    if running["totalCount"] != candidate["totalCount"]:
        yield (
            f"{scope}: totalCount differs, running {running['totalCount']!r}, "
            f"candidate {candidate['totalCount']!r}"
        )


def _member_differences(running, candidate):
    running_names = {row["avatar"] for row in running["items"]}
    candidate_names = {row["avatar"] for row in candidate["items"]}
    for name in sorted(running_names - candidate_names):
        yield f"{name}: listed by the running side only"
    for name in sorted(candidate_names - running_names):
        yield f"{name}: listed by the candidate side only"


def _field_set_differences(running, candidate):
    shapes = (
        ("row", lambda side: _fields_of(side["overview"]["items"])),
        ("totals", lambda side: set(side["overview"]["totals"])),
        ("admin", lambda side: set(side["admin"])),
        ("ledger entry", lambda side: _fields_of(_all_entries(side))),
    )
    findings = []
    added = []
    for scope, fields_of in shapes:
        running_fields = fields_of(running)
        candidate_fields = fields_of(candidate)
        for field in sorted(running_fields - candidate_fields):
            findings.append(f"{scope}: field {field} is served by the running side only")
        listed = set(LISTED_ADDITIONS.get(scope, ()))
        added.extend((scope, field) for field in sorted(candidate_fields - running_fields & listed))
        for field in sorted(candidate_fields - running_fields - listed):
            findings.append(f"{scope}: field {field} is served by the candidate side only")
    return findings, added


def _fields_of(objects):
    return {field for obj in objects for field in obj}


def _all_entries(side):
    return [
        entry
        for ledgers in side["ledgers"].values()
        for ledger in ledgers.values()
        for entry in ledger["items"]
    ]


def _admin_differences(running, candidate):
    for field in ADMIN_NAME_LISTS:
        if field in running and field in candidate:
            only_running = Counter(running[field]) - Counter(candidate[field])
            only_candidate = Counter(candidate[field]) - Counter(running[field])
            for name in sorted(only_running.elements()):
                yield f"admin: {field} lists {name} in the running side only"
            for name in sorted(only_candidate.elements()):
                yield f"admin: {field} lists {name} in the candidate side only"


def _recompute_status_findings(side, admin):
    if not admin.get("lastSuccessfulRecompute"):
        yield f"admin: the {side} side shows no successful recompute"
    if admin.get("lastRecomputeFailure"):
        yield f"admin: the {side} side shows a recompute failure"


def _members_without_stored_sums(running, candidate):
    return {
        row["avatar"]
        for row in running["overview"]["items"]
        if not running.get("exact", {}).get(row["avatar"]) and not candidate.get("exact", {}).get(row["avatar"])
    }


def _stale_sums_findings(label, side, started, unstored):
    instants = side.get("recomputed", {})
    for row in side["overview"]["items"]:
        if row["avatar"] in unstored:
            continue
        instant = instants.get(row["avatar"])
        if instant is None or instant < started:
            yield f"{row['avatar']}: the {label} side did not recompute the sums in this run"


def _candidate_rounding_differences(candidate):
    overview = candidate["overview"]
    exact = candidate["exact"]
    stored_in_served_order = [exact.get(row["avatar"], {}) for row in overview["items"]]
    for row, stored in zip(overview["items"], stored_in_served_order):
        expected = member_figures(stored) or {figure: None for figure in GUILD_FIGURES}
        yield from _rounding_differences(row["avatar"], row, expected)
    yield from _rounding_differences("totals", overview["totals"], guild_figures(stored_in_served_order))


def _rounding_differences(scope, shown, expected):
    for figure, value in expected.items():
        if figure in shown and shown[figure] != value:
            yield f"{scope}: candidate serves {figure} {shown[figure]!r}, its exact sums yield {value!r}"


def main(argv, environ, out, err=sys.stderr):
    try:
        arguments = _parser().parse_args(argv)
    except SystemExit:
        return 2
    try:
        return arguments.command(arguments, environ, out)
    except Exception as crash:
        err.write(f"compare_sides.py: {type(crash).__name__}: {crash}\n")
        return CRASH_EXIT_CODE


def _parser():
    parser = argparse.ArgumentParser(prog="compare_sides.py")
    commands = parser.add_subparsers(dest="name", required=True)

    wait = commands.add_parser("wait")
    wait.add_argument("--base-url", required=True)
    wait.add_argument("--timeout", type=float, required=True)
    wait.add_argument("--poll", type=float, required=True)
    wait.add_argument("--pid", type=int, required=True)
    wait.set_defaults(command=_wait)

    alive = commands.add_parser("alive")
    alive.add_argument("--pid", type=int, required=True)
    alive.set_defaults(command=_alive)

    fetch = commands.add_parser("fetch")
    fetch.add_argument("--base-url", required=True)
    fetch.add_argument("--out", required=True)
    fetch.set_defaults(command=_fetch)

    occupied = commands.add_parser("occupied")
    occupied.add_argument("--group", type=int, required=True)
    occupied.set_defaults(command=_occupied)

    stop = commands.add_parser("stop")
    stop.add_argument("--group", type=int, required=True)
    stop.add_argument("--grace", type=float, required=True)
    stop.add_argument("--poll", type=float, required=True)
    stop.set_defaults(command=_stop)

    compare_command = commands.add_parser("compare")
    for name in ("running-wire", "running-db", "candidate-wire", "candidate-db"):
        compare_command.add_argument(f"--{name}", required=True)
    compare_command.add_argument("--started-at-millis", type=int)
    compare_command.set_defaults(command=_compare)
    return parser


def _wait(arguments, environ, out):
    get_json = http_get_json(arguments.base_url, environ.get(TOKEN_VARIABLE, ""), Pacer())
    recomputed = wait_for_recompute(
        lambda: get_json("/api/v1/admin/status", {}),
        arguments.timeout,
        arguments.poll,
        alive=lambda: is_alive(arguments.pid),
    )
    if not recomputed and not is_alive(arguments.pid):
        out.write(f"The side at {arguments.base_url} is not running.\n")
    elif not recomputed:
        out.write(
            f"No successful recompute showed at {arguments.base_url}, "
            f"within {arguments.timeout} seconds or before a failure.\n"
        )
    return 0 if recomputed else 1


def _alive(arguments, environ, out):
    return 0 if is_alive(arguments.pid) else 1


def _occupied(arguments, environ, out):
    return 0 if arguments.group >= 2 and members_of(arguments.group) else 1


def _fetch(arguments, environ, out):
    token = environ.get(TOKEN_VARIABLE)
    if not token:
        out.write(f"{TOKEN_VARIABLE} is not set in the environment.\n")
        return 2
    fetched = fetch_side(http_get_json(arguments.base_url, token, Pacer()), PAGE_SIZE)
    with open(arguments.out, "w", encoding="utf-8") as target:
        json.dump(fetched, target, ensure_ascii=False)
    return 0


def _stop(arguments, environ, out):
    if arguments.group < 2:
        out.write("Refusing to signal a process group below 2.\n")
        return 2
    if not stop_group(arguments.group, arguments.grace, arguments.poll):
        out.write(f"The process group {arguments.group} is still alive after the kill.\n")
        return 3
    return 0


def _compare(arguments, environ, out):
    running = _side_from(arguments.running_wire, arguments.running_db)
    candidate = _side_from(arguments.candidate_wire, arguments.candidate_db)
    report = compare(running, candidate, arguments.started_at_millis)
    out.write(report.render())
    return report.exit_code


def _side_from(wire_path, database_path):
    with open(wire_path, encoding="utf-8") as wire:
        side = json.load(wire)
    side["exact"] = read_exact_sums(database_path)
    side["recomputed"] = read_recompute_instants(database_path)
    return side


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:], os.environ, sys.stdout, sys.stderr))
