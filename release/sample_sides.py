import copy

ROW_FIGURES = (
    "bankWithdrawn",
    "bankDeposited",
    "storageWithdrawn",
    "storageDeposited",
    "net",
    "donation",
    "craftSubsidy",
)


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
        "lastSuccessfulRecompute": None,
        "lastRecomputeFailure": None,
        "unknownItemNames": [],
        "failedAvatarNames": [],
        "roundTrips": 0,
        "roundTripAbstentions": 0,
    }
    base.update(fields)
    return base
