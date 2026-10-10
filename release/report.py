from expected_deviations import ALL_RULES, label_of

NOT_COMPARED = "The five run instants of the admin status are not compared: they differ by construction."


class Report:
    def __init__(self, findings, accepted, uncompared, uses):
        self.findings = findings
        self.accepted = accepted
        self.uncompared = uncompared
        self.uses = uses

    @property
    def exit_code(self):
        return 1 if self.findings else 0

    def render(self):
        lines = [f"Findings ({len(self.findings)}):"]
        lines.extend(f"  - {finding}" for finding in self.findings or ["none"])
        lines.append("Accepted deviations:")
        for rule in ALL_RULES:
            if self.uses[rule]:
                lines.append(f"  - {label_of(rule)}: {self.uses[rule]} accepted")
                lines.extend(_figure_line(accepted) for accepted in self.accepted if accepted.rule == rule)
        if not any(self.uses[rule] for rule in ALL_RULES):
            lines.append("  none")
        lines.append("Listed rules that did not occur:")
        unused = [rule for rule in ALL_RULES if not self.uses[rule]]
        lines.extend(f"  - {label_of(rule)}" for rule in unused)
        if not unused:
            lines.append("  none")
        if self.uncompared:
            lines.append(f"Guild figures not compared: {', '.join(self.uncompared)}")
        lines.append(NOT_COMPARED)
        lines.append("FAIL: the findings above need a decision" if self.findings else "PASS: no findings")
        return "\n".join(lines) + "\n"


def _figure_line(accepted):
    return f"    - {accepted.scope}: {accepted.figure}, running {accepted.running!r}, candidate {accepted.candidate!r}"
