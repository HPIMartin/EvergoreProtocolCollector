from collections import namedtuple

Rule = namedtuple("Rule", "name decisions")

ROUNDING = Rule("rounded once, halves away from zero", ("2026-09-27", "2026-09-30"))

AMMUNITION = Rule("full ammunition credit", ("2026-09-27", "2026-10-01"))

LAST_BIT = Rule("last-bit drift of an exact storage sum", ("2026-09-27",))

NOT_REACHED = Rule("member no recompute has reached", ("2026-09-23", "2026-10-06"))

ADDITIONS = Rule("listed wire fields only the candidate serves", ("2026-09-10", "2026-09-22"))

ALL_RULES = (ROUNDING, AMMUNITION, LAST_BIT, NOT_REACHED, ADDITIONS)

LISTED_ADDITIONS = {
    "row": ("balance",),
    "totals": ("balance", "storageValue"),
    "admin": ("roundTrips", "roundTripAbstentions"),
}


def label_of(rule):
    noun = "decision" if len(rule.decisions) == 1 else "decisions"
    return f"{rule.name} ({noun} {', '.join(rule.decisions)})"
