class Report:
    def __init__(self, findings):
        self.findings = findings

    @property
    def exit_code(self):
        return 1 if self.findings else 0


def compare(running, candidate):
    findings = []
    findings.extend(_overview_differences(running["overview"], candidate["overview"]))
    return Report(findings)


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
