import math

LEDGER_SUMS = ("bank_placement", "bank_withdrawl", "storage_placement", "storage_withdrawl")


def whole_gold_of(exact):
    if exact < 0:
        return -_nearest_half_up(-exact)
    return _nearest_half_up(exact)


def _nearest_half_up(value):
    floor = math.floor(value)
    return floor + (1 if value - floor >= 0.5 else 0)


def is_computed(stored):
    return all(key in stored for key in LEDGER_SUMS)


def member_figures(stored):
    if not is_computed(stored):
        return None
    net = _net_of(stored["bank_placement"], stored["bank_withdrawl"], stored["storage_placement"], stored["storage_withdrawl"])
    figures = {
        "bankWithdrawn": stored["bank_withdrawl"],
        "bankDeposited": stored["bank_placement"],
        "storageWithdrawn": whole_gold_of(stored["storage_withdrawl"]),
        "storageDeposited": whole_gold_of(stored["storage_placement"]),
        "net": whole_gold_of(net),
        "donation": None,
        "craftSubsidy": None,
        "balance": None,
    }
    if _has_guild_share(stored):
        donation = stored["storage_donation"]
        craft_subsidy = stored["storage_craft_subsidy"]
        figures["donation"] = whole_gold_of(donation)
        figures["craftSubsidy"] = whole_gold_of(craft_subsidy)
        figures["balance"] = whole_gold_of(net + donation - craft_subsidy)
    return figures


def guild_figures(stored_in_served_order):
    if not all(is_computed(stored) for stored in stored_in_served_order):
        return {figure: None for figure in GUILD_FIGURES}
    bank_placement = sum(stored["bank_placement"] for stored in stored_in_served_order)
    bank_withdrawl = sum(stored["bank_withdrawl"] for stored in stored_in_served_order)
    storage_placement = _running_sum(stored["storage_placement"] for stored in stored_in_served_order)
    storage_withdrawl = _running_sum(stored["storage_withdrawl"] for stored in stored_in_served_order)
    net = _net_of(bank_placement, bank_withdrawl, storage_placement, storage_withdrawl)
    figures = {
        "bankWithdrawn": bank_withdrawl,
        "bankDeposited": bank_placement,
        "storageWithdrawn": whole_gold_of(storage_withdrawl),
        "storageDeposited": whole_gold_of(storage_placement),
        "net": whole_gold_of(net),
        "donation": None,
        "craftSubsidy": None,
        "balance": None,
        "storageValue": None,
    }
    if all(_has_guild_share(stored) for stored in stored_in_served_order):
        donation = _running_sum(stored["storage_donation"] for stored in stored_in_served_order)
        craft_subsidy = _running_sum(stored["storage_craft_subsidy"] for stored in stored_in_served_order)
        figures["donation"] = whole_gold_of(donation)
        figures["craftSubsidy"] = whole_gold_of(craft_subsidy)
        figures["balance"] = whole_gold_of(net + donation - craft_subsidy)
        figures["storageValue"] = whole_gold_of(storage_placement + donation - craft_subsidy - storage_withdrawl)
    return figures


GUILD_FIGURES = (
    "bankWithdrawn",
    "bankDeposited",
    "storageWithdrawn",
    "storageDeposited",
    "net",
    "donation",
    "craftSubsidy",
    "balance",
    "storageValue",
)


def _has_guild_share(stored):
    return "storage_donation" in stored and "storage_craft_subsidy" in stored


def _net_of(bank_placement, bank_withdrawl, storage_placement, storage_withdrawl):
    return float(bank_placement - bank_withdrawl) + storage_placement - storage_withdrawl


def _running_sum(values):
    total = 0.0
    for value in values:
        total += value
    return total
