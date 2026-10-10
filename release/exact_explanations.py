import math

from expected_deviations import AMMUNITION

AMMUNITION_SORTS = ("Pfeile", "Bolzen", "Magieessenz")
ULP_BOUND = 4


def explain(differing, running, candidate):
    explained = {}
    for member, keys in differing.items():
        reasons = {}
        if _is_full_ammunition_credit(member, keys, running, candidate):
            reasons["storage_placement"] = AMMUNITION
            reasons["storage_craft_subsidy"] = AMMUNITION
        explained[member] = {key: rule for key, rule in reasons.items() if key in keys}
    return explained


def _is_full_ammunition_credit(member, keys, running, candidate):
    if not _deposited_ammunition(member, running, candidate):
        return False
    if "bank_placement" in keys or "bank_withdrawl" in keys:
        return False
    if "storage_placement" not in keys or "storage_craft_subsidy" not in keys:
        return False
    placement = _delta(keys["storage_placement"])
    craft_subsidy = _delta(keys["storage_craft_subsidy"])
    if placement <= 0:
        return False
    if not _within_bound(placement - craft_subsidy, keys["storage_placement"] + keys["storage_craft_subsidy"]):
        return False
    return all(_within_bound(_delta(keys[key]), keys[key]) for key in ("storage_withdrawl", "storage_donation") if key in keys)


def _deposited_ammunition(member, running, candidate):
    return any(
        entry["transferType"] == "DEPOSIT" and entry["name"] in AMMUNITION_SORTS
        for side in (running, candidate)
        for entry in side["ledgers"].get(member, {}).get("storage", {"items": []})["items"]
    )


def _delta(pair):
    running, candidate = pair
    return (candidate or 0.0) - (running or 0.0)


def _within_bound(difference, values):
    magnitude = max(abs(value or 0.0) for value in values)
    return abs(difference) <= ULP_BOUND * math.ulp(magnitude)
