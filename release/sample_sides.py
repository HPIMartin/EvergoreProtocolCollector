import copy

import candidate_rules
import running_rules

ROW_FIGURES = (
    "bankWithdrawn",
    "bankDeposited",
    "storageWithdrawn",
    "storageDeposited",
    "net",
    "donation",
    "craftSubsidy",
)


def without(figures, *names):
    return {key: value for key, value in figures.items() if key not in names}


def row(avatar, **figures):
    base = {
        "avatar": avatar,
        "bankWithdrawn": 0,
        "bankDeposited": 0,
        "storageWithdrawn": 0,
        "storageDeposited": 0,
        "net": 0,
        "donation": 0,
        "craftSubsidy": 0,
        "balance": 0,
        "lastBankActivity": None,
        "lastStorageActivity": None,
        "staleSumsFrom": None,
    }
    base.update(figures)
    return base


def totals(**figures):
    base = {
        "bankWithdrawn": 0,
        "bankDeposited": 0,
        "storageWithdrawn": 0,
        "storageDeposited": 0,
        "net": 0,
        "donation": 0,
        "craftSubsidy": 0,
        "balance": 0,
        "storageValue": 0,
        "containsStaleSums": False,
    }
    base.update(figures)
    return base


def side(rows=None, overview_totals=None, **parts):
    rows = [row("Alice")] if rows is None else rows
    built = {
        "overview": {
            "totalCount": len(rows),
            "items": copy.deepcopy(rows),
            "totals": totals() if overview_totals is None else overview_totals,
        },
        "ledgers": {},
        "admin": admin(),
    }
    built.update(parts)
    return built


def admin(**fields):
    base = {
        "lastUpdated": None,
        "lastSuccessfulScrape": None,
        "lastScrapeFailure": None,
        "lastSuccessfulRecompute": "2026-10-10T08:01:00Z",
        "lastRecomputeFailure": None,
        "unknownItemNames": [],
        "failedAvatarNames": [],
        "roundTrips": 0,
        "roundTripAbstentions": 0,
    }
    base.update(fields)
    return base


def storage_entry(name, transfer_type="DEPOSIT", quantity=1, quality=0):
    return {
        "timestamp": "2026-01-01T12:00:00Z",
        "avatar": "Alice",
        "quantity": quantity,
        "name": name,
        "quality": quality,
        "transferType": transfer_type,
    }


def ledgers_holding(*storage_entries, member="Alice"):
    storage = {"totalCount": len(storage_entries), "items": list(storage_entries)}
    return {member: {"bank": {"totalCount": 0, "items": []}, "storage": storage}}


def running_side_of(stored_by_member, **parts):
    rows = []
    for member, stored in stored_by_member.items():
        figures = running_rules.member_figures(stored)
        rows.append(without(row(member, **figures), "balance"))
    guild = without(totals(**running_rules.totals_of_rows(rows)), "balance", "storageValue")
    return side(rows, guild, exact=stored_by_member, **parts)


def candidate_side_of(stored_by_member, **parts):
    rows = [row(member, **candidate_rules.member_figures(stored)) for member, stored in stored_by_member.items()]
    guild = totals(**candidate_rules.guild_figures(list(stored_by_member.values())))
    return side(rows, guild, exact=stored_by_member, **parts)
