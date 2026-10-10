import json
from collections import Counter

LEDGERS = ("bank", "storage")


class Report:
    def __init__(self, findings):
        self.findings = findings

    @property
    def exit_code(self):
        return 1 if self.findings else 0


def compare(running, candidate):
    findings = []
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
