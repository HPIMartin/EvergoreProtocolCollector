NO_DEVIATION = "Expected deviations: none, every difference between the two sides is a finding."
NOT_COMPARED = "The five run instants of the admin status are not compared: they differ by construction."


class Report:
    def __init__(self, findings):
        self.findings = findings

    @property
    def exit_code(self):
        return 1 if self.findings else 0

    def render(self):
        lines = [f"Findings ({len(self.findings)}):"]
        lines.extend(f"  - {finding}" for finding in self.findings or ["none"])
        lines.append(NO_DEVIATION)
        lines.append(NOT_COMPARED)
        lines.append("FAIL: the findings above need a decision" if self.findings else "PASS: no findings")
        return "\n".join(lines) + "\n"
