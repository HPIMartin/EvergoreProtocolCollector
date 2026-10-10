import json
from collections import Counter

from expected_deviations import LISTED_ADDITIONS

LEDGERS = ("bank", "storage")
ADMIN_NAME_LISTS = ("unknownItemNames", "failedAvatarNames")


class Report:
    def __init__(self, findings):
        self.findings = findings

    @property
    def exit_code(self):
        return 1 if self.findings else 0


def compare(running, candidate):
    findings = []
    findings.extend(_field_set_differences(running, candidate))
    findings.extend(_admin_differences(running["admin"], candidate["admin"]))
    findings.extend(_total_count_differences("overview", running["overview"], candidate["overview"]))
    findings.extend(_member_differences(running["overview"], candidate["overview"]))
    findings.extend(_overview_differences(running["overview"], candidate["overview"]))
    findings.extend(_ledger_differences(running["ledgers"], candidate["ledgers"]))
    return Report(findings)


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


def _overview_differences(running, candidate):
    candidate_rows = {row["avatar"]: row for row in candidate["items"]}
    for running_row in running["items"]:
        candidate_row = candidate_rows.get(running_row["avatar"])
        if candidate_row is not None:
            yield from _figure_differences(running_row["avatar"], running_row, candidate_row)
    yield from _figure_differences("totals", running["totals"], candidate["totals"])


def _figure_differences(scope, running, candidate):
    for figure in running:
        if figure in candidate and figure != "avatar" and running[figure] != candidate[figure]:
            yield f"{scope}: {figure} differs, running {running[figure]!r}, candidate {candidate[figure]!r}"


def _field_set_differences(running, candidate):
    shapes = (
        ("row", lambda side: _fields_of(side["overview"]["items"])),
        ("totals", lambda side: set(side["overview"]["totals"])),
        ("admin", lambda side: set(side["admin"])),
        ("ledger entry", lambda side: _fields_of(_all_entries(side))),
    )
    for scope, fields_of in shapes:
        running_fields = fields_of(running)
        candidate_fields = fields_of(candidate)
        for field in sorted(running_fields - candidate_fields):
            yield f"{scope}: field {field} is served by the running side only"
        listed = LISTED_ADDITIONS.get(scope, ())
        for field in sorted(candidate_fields - running_fields - set(listed)):
            yield f"{scope}: field {field} is served by the candidate side only"


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
