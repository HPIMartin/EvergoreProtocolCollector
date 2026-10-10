from expected_deviations import ALL_RULES, label_of

NOT_COMPARED = "The five run instants of the admin status are not compared: they differ by construction."


class Report:
    def __init__(self, findings, accepted, uncompared, unshown=()):
        self.findings = findings
        self.accepted = accepted
        self.uncompared = uncompared
        self.unshown = list(unshown)

    @property
    def exit_code(self):
        return 1 if self.findings else 0

    def render(self):
        listed = {rule: [a for a in self.accepted + self.unshown if a.rule == rule] for rule in ALL_RULES}
        lines = [f"Findings ({len(self.findings)}):"]
        lines.extend(f"  - {finding}" for finding in self.findings or ["none"])
        lines.append("Accepted deviations:")
        for rule in ALL_RULES:
            if listed[rule]:
                lines.append(f"  - {label_of(rule)}: {len(listed[rule])} accepted")
                lines.extend(_figure_line(accepted) for accepted in listed[rule])
        if not any(listed.values()):
            lines.append("  none")
        lines.append("Listed rules that did not occur:")
        unused = [rule for rule in ALL_RULES if not listed[rule]]
        lines.extend(f"  - {label_of(rule)}" for rule in unused)
        if not unused:
            lines.append("  none")
        if self.uncompared:
            lines.append(f"Guild figures not compared: {', '.join(self.uncompared)}")
        lines.append(NOT_COMPARED)
        lines.append("FAIL: the findings above need a decision" if self.findings else "PASS: no findings")
        return "\n".join(lines) + "\n"


def _figure_line(accepted):
    if accepted.running is None and accepted.candidate is None:
        return f"    - {accepted.scope}: {accepted.figure}"
    return f"    - {accepted.scope}: {accepted.figure}, running {accepted.running!r}, candidate {accepted.candidate!r}"
