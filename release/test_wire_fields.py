import unittest

from compare_sides import compare
from sample_sides import admin, row, side, totals


def without(figures, *names):
    return {key: value for key, value in figures.items() if key not in names}


def entry(**extra):
    return {"item": "Eisen", "quantity": 1, **extra}


def with_entries(*entries):
    ledger = {"totalCount": len(entries), "items": list(entries)}
    return side(ledgers={"Alice": {"bank": {"totalCount": 0, "items": []}, "storage": ledger}})


class ListedAdditions(unittest.TestCase):
    def test_the_candidate_only_fields_listed_as_additions_are_no_finding(self):
        running = side(
            [without(row("Alice"), "balance")],
            without(totals(), "balance", "storageValue"),
            admin=without(admin(), "roundTrips", "roundTripAbstentions"),
        )
        candidate = side()

        tested = compare(running, candidate)

        self.assertEqual([], tested.findings)


class UnlistedFields(unittest.TestCase):
    def test_an_extra_row_field_on_the_candidate_is_a_finding_naming_field_and_side(self):
        running = side([row("Alice")])
        candidate = side([row("Alice", rank=3)])

        tested = compare(running, candidate)

        self.assertEqual(1, len(tested.findings))
        for expected in ("row", "rank", "candidate"):
            self.assertIn(expected, tested.findings[0])

    def test_a_row_field_only_the_running_side_serves_is_a_finding(self):
        running = side([row("Alice")])
        candidate = side([without(row("Alice"), "donation")])

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "donation" in f and "running" in f]
        self.assertEqual(1, len(found))

    def test_a_listed_addition_on_the_wrong_side_is_a_finding(self):
        running = side()
        candidate = side([without(row("Alice"), "balance")])

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "balance" in f and "running" in f]
        self.assertEqual(1, len(found))

    def test_a_totals_field_difference_is_a_finding(self):
        running = side(overview_totals=totals())
        candidate = side(overview_totals=totals(extra=1))

        tested = compare(running, candidate)

        self.assertEqual(1, len(tested.findings))
        for expected in ("totals", "extra", "candidate"):
            self.assertIn(expected, tested.findings[0])

    def test_an_admin_status_field_difference_is_a_finding(self):
        running = side(admin=admin())
        candidate = side(admin=admin(surprise=1))

        tested = compare(running, candidate)

        self.assertEqual(1, len(tested.findings))
        for expected in ("admin", "surprise", "candidate"):
            self.assertIn(expected, tested.findings[0])

    def test_a_ledger_entry_field_difference_is_a_finding(self):
        running = with_entries(entry())
        candidate = with_entries(entry(source="x"))

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "ledger entry" in f and "source" in f]
        self.assertEqual(1, len(found))
        self.assertIn("candidate", found[0])


class AdminValues(unittest.TestCase):
    def test_run_instants_are_not_compared(self):
        running = side(admin=admin(lastSuccessfulRecompute="2026-10-10T08:00:00Z"))
        candidate = side(admin=admin(lastSuccessfulRecompute="2026-10-10T09:00:00Z"))

        tested = compare(running, candidate)

        self.assertEqual([], tested.findings)

    def test_name_lists_are_compared_as_multisets(self):
        running = side(admin=admin(unknownItemNames=["Zwerg", "Älf"]))
        candidate = side(admin=admin(unknownItemNames=["Älf", "Zwerg"]))

        tested = compare(running, candidate)

        self.assertEqual([], tested.findings)

    def test_a_name_only_one_side_lists_is_a_finding(self):
        running = side(admin=admin(failedAvatarNames=["Bob"]))
        candidate = side(admin=admin(failedAvatarNames=[]))

        tested = compare(running, candidate)

        self.assertEqual(1, len(tested.findings))
        for expected in ("failedAvatarNames", "Bob", "running"):
            self.assertIn(expected, tested.findings[0])


if __name__ == "__main__":
    unittest.main()
