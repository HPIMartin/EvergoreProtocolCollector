import unittest

from compare_sides import compare
from sample_sides import admin, row, side, totals, without


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


    def test_a_listed_admin_addition_served_as_anything_but_a_list_is_a_finding(self):
        for served, kind in ((None, "NoneType"), ("x", "str"), (3.5, "float"), (0, "int")):
            with self.subTest(served=served):
                running = side(admin=without(admin(), "roundTrips", "roundTripAbstentions"))
                candidate = side(admin=admin(roundTrips=served))

                tested = compare(running, candidate)

                self.assertEqual([f"admin: roundTrips is served as {kind} by the candidate side, a list is expected"], tested.findings)

    def test_a_listed_addition_neither_side_serves_is_a_finding(self):
        running = side(
            [without(row("Alice"), "balance")],
            without(totals(), "balance", "storageValue"),
            admin=without(admin(), "roundTrips", "roundTripAbstentions"),
        )
        candidate = side(
            [without(row("Alice"), "balance")],
            without(totals(), "balance", "storageValue"),
            admin=without(admin(), "roundTrips"),
        )

        tested = compare(running, candidate)

        self.assertEqual(
            [
                "row: field balance is a listed addition the candidate side does not serve",
                "totals: field balance is a listed addition the candidate side does not serve",
                "totals: field storageValue is a listed addition the candidate side does not serve",
                "admin: field roundTrips is a listed addition the candidate side does not serve",
            ],
            tested.findings,
        )

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

    def test_a_figure_one_candidate_row_leaves_out_is_a_finding_for_that_member(self):
        running = side([row("Alice"), row("Bob")])
        candidate = side([row("Alice"), without(row("Bob"), "net", "bankDeposited")])

        tested = compare(running, candidate)

        self.assertEqual(
            [
                "Bob: field bankDeposited is left out of this row on the candidate side",
                "Bob: field net is left out of this row on the candidate side",
            ],
            tested.findings,
        )

    def test_a_candidate_only_figure_one_row_leaves_out_is_a_finding_for_that_member(self):
        running = side([without(row("Alice"), "balance"), without(row("Bob"), "balance")], without(totals(), "balance", "storageValue"))
        candidate = side([without(row("Alice"), "balance"), row("Bob")])

        tested = compare(running, candidate)

        self.assertEqual(["Alice: field balance is left out of this row on the candidate side"], tested.findings)

    def test_a_figure_one_running_row_leaves_out_is_a_finding_and_no_crash(self):
        running = side(
            [without(row("Alice"), "balance"), without(row("Bob"), "balance", "net")],
            without(totals(), "balance", "storageValue"),
            admin=without(admin(), "roundTrips", "roundTripAbstentions"),
        )
        candidate = side([row("Alice"), row("Bob")])

        tested = compare(running, candidate)

        self.assertEqual(["Bob: field net is left out of this row on the running side"], tested.findings)

    def test_a_field_no_candidate_row_serves_is_one_finding_not_one_per_member(self):
        running = side([row("Alice"), row("Bob")])
        candidate = side([without(row("Alice"), "donation"), without(row("Bob"), "donation")])

        tested = compare(running, candidate)

        self.assertEqual(["row: field donation is served by the running side only"], tested.findings)

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
        running = side(admin=admin(unknownItemNames=["Zwerg"]))
        candidate = side(admin=admin(unknownItemNames=[]))

        tested = compare(running, candidate)

        self.assertEqual(1, len(tested.findings))
        for expected in ("unknownItemNames", "Zwerg", "running"):
            self.assertIn(expected, tested.findings[0])


if __name__ == "__main__":
    unittest.main()
