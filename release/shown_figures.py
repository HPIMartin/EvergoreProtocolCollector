from collections import namedtuple

import running_rules
from expected_deviations import ROUNDING

Difference = namedtuple("Difference", "scope figure running candidate")
Accepted = namedtuple("Accepted", "rule scope figure running candidate")

SUM_KEYS = ("bank_placement", "bank_withdrawl", "storage_placement", "storage_withdrawl")
SHARE_KEYS = ("storage_donation", "storage_craft_subsidy")
EXACT_KEYS = SUM_KEYS + SHARE_KEYS

ROUNDED_FIGURE_INPUTS = {
    "storageWithdrawn": ("storage_withdrawl",),
    "storageDeposited": ("storage_placement",),
    "donation": ("storage_donation",),
    "craftSubsidy": ("storage_craft_subsidy",),
    "net": SUM_KEYS,
    "balance": SUM_KEYS + SHARE_KEYS,
    "storageValue": ("storage_placement", "storage_withdrawl") + SHARE_KEYS,
}


def text_of(difference):
    return (
        f"{difference.scope}: {difference.figure} differs, "
        f"running {difference.running!r}, candidate {difference.candidate!r}"
    )


def differences(running, candidate):
    candidate_rows = {row["avatar"]: row for row in candidate["items"]}
    for running_row in running["items"]:
        candidate_row = candidate_rows.get(running_row["avatar"])
        if candidate_row is not None:
            yield from _differences_of(running_row["avatar"], running_row, candidate_row)
    yield from _differences_of("totals", running["totals"], candidate["totals"])


def _differences_of(scope, running, candidate):
    for figure in running:
        if figure in candidate and figure != "avatar" and running[figure] != candidate[figure]:
            yield Difference(scope, figure, running[figure], candidate[figure])


def exact_differences(running, candidate):
    members = {row["avatar"] for row in running["overview"]["items"]} & {
        row["avatar"] for row in candidate["overview"]["items"]
    }
    found = {}
    for member in sorted(members):
        stored_running = running["exact"].get(member, {})
        stored_candidate = candidate["exact"].get(member, {})
        for key in EXACT_KEYS:
            if stored_running.get(key) != stored_candidate.get(key):
                found.setdefault(member, {})[key] = (stored_running.get(key), stored_candidate.get(key))
    return found


def explanation(difference, exact_difference_by_member):
    inputs = ROUNDED_FIGURE_INPUTS.get(difference.figure)
    if inputs is None:
        return None
    if difference.scope == "totals":
        differing = {key for keys in exact_difference_by_member.values() for key in keys}
    else:
        differing = set(exact_difference_by_member.get(difference.scope, {}))
    if differing & set(inputs):
        return None
    return ROUNDING


def running_rule_differences(running):
    overview = running["overview"]
    for row in overview["items"]:
        expected = running_rules.member_figures(running["exact"].get(row["avatar"], {}))
        for figure, value in (expected or {}).items():
            if row[figure] != value:
                yield f"{row['avatar']}: running serves {figure} {row[figure]!r}, its exact sums yield {value!r}"
    totals = overview["totals"]
    for figure, value in running_rules.totals_of_rows(overview["items"]).items():
        if totals[figure] != value:
            yield f"totals: running serves {figure} {totals[figure]!r}, the sum of its rows is {value!r}"
