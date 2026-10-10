from collections import namedtuple

Rule = namedtuple("Rule", "name decisions")

ROUNDING = Rule("rounded once, halves away from zero", ("2026-09-27", "2026-09-30"))

AMMUNITION = Rule("full ammunition credit", ("2026-09-27", "2026-10-01"))

LAST_BIT = Rule("last-bit drift of an exact storage sum", ("2026-09-27",))

LISTED_ADDITIONS = {
    "row": ("balance",),
    "totals": ("balance", "storageValue"),
    "admin": ("roundTrips", "roundTripAbstentions"),
}
