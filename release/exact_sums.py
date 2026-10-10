import sqlite3
from pathlib import Path

LONG_PREFIXES = ("bank_placement", "bank_withdrawl")
DOUBLE_PREFIXES = ("storage_placement", "storage_withdrawl", "storage_donation", "storage_craft_subsidy")


def read_exact_sums(database):
    connection = sqlite3.connect(Path(database).resolve().as_uri() + "?mode=ro", uri=True)
    try:
        rows = connection.execute("SELECT key, value FROM metaInformation").fetchall()
    finally:
        connection.close()
    sums = {}
    for key, value in rows:
        for prefix in LONG_PREFIXES:
            if key.startswith(prefix + "_"):
                sums.setdefault(key[len(prefix) + 1 :], {})[prefix] = int(value)
        for prefix in DOUBLE_PREFIXES:
            if key.startswith(prefix + "_"):
                sums.setdefault(key[len(prefix) + 1 :], {})[prefix] = float(value)
    return sums


def read_recompute_instants(database):
    prefix = "sums_recomputed_at_"
    connection = sqlite3.connect(Path(database).resolve().as_uri() + "?mode=ro", uri=True)
    try:
        rows = connection.execute("SELECT key, value FROM metaInformation").fetchall()
    finally:
        connection.close()
    return {key[len(prefix) :]: int(value) for key, value in rows if key.startswith(prefix)}
