import unittest

from compare_sides import compare
from sample_sides import row, side, totals, without


class OverviewFigures(unittest.TestCase):
    def test_identical_sides_pass(self):
        running = side([row("Alice", net=100), row("Bob", net=5)])
        candidate = side([row("Alice", net=100), row("Bob", net=5)])

        tested = compare(running, candidate)

        self.assertEqual([], tested.findings)
        self.assertEqual(0, tested.exit_code)

    def test_a_differing_row_figure_is_one_finding_naming_member_figure_and_both_values(self):
        running = side([row("Alice", net=100), row("Bob", net=5)])
        candidate = side([row("Alice", net=101), row("Bob", net=5)])

        tested = compare(running, candidate)

        self.assertEqual(1, len(tested.findings))
        finding = tested.findings[0]
        for expected in ("Alice", "net", "100", "101"):
            self.assertIn(expected, finding)
        self.assertEqual(1, tested.exit_code)

    def test_a_differing_totals_figure_is_one_finding_naming_totals_figure_and_both_values(self):
        running = side(overview_totals=totals(donation=70))
        candidate = side(overview_totals=totals(donation=71))

        tested = compare(running, candidate)

        self.assertEqual(1, len(tested.findings))
        finding = tested.findings[0]
        for expected in ("totals", "donation", "70", "71"):
            self.assertIn(expected, finding)
        self.assertEqual(1, tested.exit_code)

    def test_a_differing_activity_instant_is_a_finding(self):
        running = side([row("Alice", lastBankActivity="2026-01-01T00:00:00Z")])
        candidate = side([row("Alice", lastBankActivity="2026-01-02T00:00:00Z")])

        tested = compare(running, candidate)

        self.assertEqual(1, len(tested.findings))
        self.assertIn("lastBankActivity", tested.findings[0])


    def test_a_whole_gold_figure_served_as_a_fraction_is_a_finding_naming_both_types(self):
        running = side([row("Alice", net=860)], overview_totals=totals(storageValue=860))
        candidate = side([row("Alice", net=860.0)], overview_totals=totals(storageValue=860.0))

        tested = compare(running, candidate)

        self.assertEqual(
            [
                "Alice: net is served as int by the running side and as float by the candidate side",
                "totals: storageValue is served as int by the running side and as float by the candidate side",
            ],
            tested.findings,
        )


    def test_a_listed_addition_served_as_a_fraction_is_a_finding(self):
        running = side([without(row("Alice"), "balance")], without(totals(), "balance", "storageValue"))
        candidate = side([row("Alice", balance=0.0)], totals(storageValue=0.0))

        tested = compare(running, candidate)

        self.assertEqual(
            [
                "Alice: balance is served as float by the candidate side, a whole number is expected",
                "totals: storageValue is served as float by the candidate side, a whole number is expected",
            ],
            tested.findings,
        )

if __name__ == "__main__":
    unittest.main()
