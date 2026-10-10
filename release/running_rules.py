import math

from candidate_rules import is_computed

CHECKED_SUMS = ("bankWithdrawn", "bankDeposited", "storageWithdrawn", "storageDeposited", "net")
CHECKED_FLOWS = ("donation", "craftSubsidy")


def round_half_up(exact):
    floor = math.floor(exact)
    return floor + (1 if exact - floor >= 0.5 else 0)


def member_figures(stored):
    if not is_computed(stored):
        return None
    storage_deposited = round_half_up(stored["storage_placement"])
    storage_withdrawn = round_half_up(stored["storage_withdrawl"])
    figures = {
        "bankWithdrawn": stored["bank_withdrawl"],
        "bankDeposited": stored["bank_placement"],
        "storageWithdrawn": storage_withdrawn,
        "storageDeposited": storage_deposited,
        "net": stored["bank_placement"] - stored["bank_withdrawl"] + storage_deposited - storage_withdrawn,
    }
    figures["donation"] = None
    figures["craftSubsidy"] = None
    if "storage_donation" in stored and "storage_craft_subsidy" in stored:
        figures["donation"] = round_half_up(stored["storage_donation"])
        figures["craftSubsidy"] = round_half_up(stored["storage_craft_subsidy"])
    return figures


def totals_of_rows(rows):
    sums = {}
    for figure in CHECKED_SUMS + CHECKED_FLOWS:
        if all(row[figure] is not None for row in rows):
            sums[figure] = sum(row[figure] for row in rows)
    return sums
