def with_derived_figures(overview):
    return {
        **overview,
        "items": [_with_balance(row) for row in overview["items"]],
        "totals": _with_guild_figures(overview["totals"]),
    }


def _with_balance(figures):
    if "balance" in figures:
        return figures
    return {**figures, "balance": _balance_of(figures)}


def _with_guild_figures(totals):
    derived = _with_balance(totals)
    if "storageValue" in derived:
        return derived
    return {**derived, "storageValue": _storage_value_of(totals)}


def _balance_of(figures):
    if figures["donation"] is None or figures["craftSubsidy"] is None:
        return None
    return figures["net"] + figures["donation"] - figures["craftSubsidy"]


def _storage_value_of(figures):
    if figures["donation"] is None or figures["craftSubsidy"] is None:
        return None
    return figures["storageDeposited"] + figures["donation"] - figures["craftSubsidy"] - figures["storageWithdrawn"]
