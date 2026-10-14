from collections import namedtuple

Difference = namedtuple("Difference", "scope figure running candidate")

SUM_KEYS = ("bank_placement", "bank_withdrawl", "storage_placement", "storage_withdrawl")
SHARE_KEYS = ("storage_donation", "storage_craft_subsidy")
EXACT_KEYS = SUM_KEYS + SHARE_KEYS


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
