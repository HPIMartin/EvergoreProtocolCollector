import unittest

from compare_sides import compare
from sample_sides import row, side
from side_fetch import fetch_all_pages


def entry(item, quantity=1, when="2026-01-01T10:00:00Z", kind="DEPOSIT"):
    return {"item": item, "quantity": quantity, "timestamp": when, "kind": kind}


def ledger(*entries):
    return {"totalCount": len(entries), "items": list(entries)}


def with_ledgers(bank=(), storage=()):
    return side(ledgers={"Alice": {"bank": ledger(*bank), "storage": ledger(*storage)}})


class LedgerEntries(unittest.TestCase):
    def test_equal_ledgers_in_a_different_order_are_no_finding(self):
        first, second = entry("Eisen"), entry("Holz", 3)
        running = with_ledgers(storage=[first, second])
        candidate = with_ledgers(storage=[second, first])

        tested = compare(running, candidate)

        self.assertEqual([], tested.findings)

    def test_an_entry_only_the_candidate_holds_is_a_finding_naming_member_ledger_and_entry(self):
        running = with_ledgers(storage=[entry("Eisen")])
        candidate = with_ledgers(storage=[entry("Eisen"), entry("Holz", 3)])

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Holz" in f]
        self.assertEqual(1, len(found))
        for expected in ("Alice", "storage", "candidate"):
            self.assertIn(expected, found[0])

    def test_an_entry_only_the_running_side_holds_in_the_bank_ledger_is_a_finding(self):
        running = with_ledgers(bank=[entry("Gold", 50)])
        candidate = with_ledgers()

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Gold" in f]
        self.assertEqual(1, len(found))
        for expected in ("Alice", "bank", "running"):
            self.assertIn(expected, found[0])

    def test_a_duplicate_on_one_side_only_is_a_finding(self):
        running = with_ledgers(storage=[entry("Eisen")])
        candidate = with_ledgers(storage=[entry("Eisen"), entry("Eisen")])

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Eisen" in f]
        self.assertEqual(1, len(found))
        self.assertIn("candidate", found[0])

    def test_differing_ledger_total_counts_are_a_finding(self):
        running = with_ledgers(storage=[entry("Eisen")])
        candidate = with_ledgers(storage=[entry("Eisen")])
        candidate["ledgers"]["Alice"]["storage"]["totalCount"] = 9

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "totalCount" in f]
        self.assertEqual(1, len(found))
        for expected in ("Alice", "storage", "1", "9"):
            self.assertIn(expected, found[0])


class AllPages(unittest.TestCase):
    def test_every_page_is_read_until_the_total_count_is_collected(self):
        pages = {
            0: {"page": 0, "size": 2, "totalCount": 3, "items": [1, 2]},
            1: {"page": 1, "size": 2, "totalCount": 3, "items": [3]},
        }
        requested = []

        def get_json(path, query):
            requested.append((path, dict(query)))
            return pages[query["page"]]

        tested = fetch_all_pages(get_json, "/api/v1/avatars/Alice/storage", 2)

        self.assertEqual([1, 2, 3], tested["items"])
        self.assertEqual(3, tested["totalCount"])
        self.assertEqual([0, 1], [query["page"] for _, query in requested])
        self.assertEqual({2}, {query["size"] for _, query in requested})

    def test_the_envelope_beyond_the_items_is_kept_from_the_first_page(self):
        pages = {
            0: {"page": 0, "size": 1, "totalCount": 2, "items": [1], "totals": {"net": 7}},
            1: {"page": 1, "size": 1, "totalCount": 2, "items": [2], "totals": {"net": 7}},
        }

        tested = fetch_all_pages(lambda path, query: pages[query["page"]], "/p", 1)

        self.assertEqual({"net": 7}, tested["totals"])


if __name__ == "__main__":
    unittest.main()
