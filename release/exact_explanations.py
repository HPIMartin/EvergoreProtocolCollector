import math

from expected_deviations import AMMUNITION, LAST_BIT

STORAGE_SUMS = ("storage_placement", "storage_withdrawl", "storage_donation", "storage_craft_subsidy")
ULP_BOUND = 4
ACCUMULATION_SLACK_ULPS = 4
AMMUNITION_MARKET_VALUES = {"Pfeile": 3, "Bolzen": 12, "Magieessenz": 4}
CREDITED_PLACEMENT = 1.0
GOODS_PLACEMENT = 0.6


def explain(differing, running, candidate):
    explained = {}
    for member, keys in differing.items():
        reasons = {}
        if _is_full_ammunition_credit(member, keys, running):
            reasons["storage_placement"] = AMMUNITION
            reasons["storage_craft_subsidy"] = AMMUNITION
        for key in STORAGE_SUMS:
            if key in keys and key not in reasons and _within_bound(_delta(keys[key]), keys[key]):
                reasons[key] = LAST_BIT
        explained[member] = {key: rule for key, rule in reasons.items() if key in keys}
    return explained


def _is_full_ammunition_credit(member, keys, running):
    entries = running["ledgers"].get(member, {}).get("storage", {"items": []})["items"]
    expected = _expected_ammunition_credit(entries)
    if expected <= 0:
        return False
    if "bank_placement" in keys or "bank_withdrawl" in keys:
        return False
    if "storage_placement" not in keys or "storage_craft_subsidy" not in keys:
        return False
    for key in ("storage_placement", "storage_craft_subsidy"):
        if not _equals_within_accumulation(_delta(keys[key]), expected, keys[key], len(entries)):
            return False
    return all(_within_bound(_delta(keys[key]), keys[key]) for key in ("storage_withdrawl", "storage_donation") if key in keys)


def _expected_ammunition_credit(entries):
    total = 0.0
    for entry in entries:
        market_value = AMMUNITION_MARKET_VALUES.get(entry["name"])
        if entry["transferType"] == "DEPOSIT" and market_value is not None:
            quality = entry["quality"] / 100
            quantity = entry["quantity"]
            total += market_value * CREDITED_PLACEMENT * quantity * quality - market_value * GOODS_PLACEMENT * quantity * quality
    return total


def _equals_within_accumulation(difference, expected, pair, entry_count):
    magnitude = max(abs(value or 0.0) for value in (*pair, expected))
    return abs(difference - expected) <= (entry_count + ACCUMULATION_SLACK_ULPS) * math.ulp(magnitude)


def _delta(pair):
    running, candidate = pair
    return (candidate or 0.0) - (running or 0.0)


def _within_bound(difference, values):
    magnitude = max(abs(value or 0.0) for value in values)
    return abs(difference) <= ULP_BOUND * math.ulp(magnitude)
