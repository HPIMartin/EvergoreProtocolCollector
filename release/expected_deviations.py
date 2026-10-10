from collections import namedtuple

Rule = namedtuple("Rule", "name decisions")

ROUNDING = Rule("rounded once, halves away from zero", ("2026-09-27", "2026-09-30"))

LISTED_ADDITIONS = {
    "row": ("balance",),
    "totals": ("balance", "storageValue"),
    "admin": ("roundTrips", "roundTripAbstentions"),
}
