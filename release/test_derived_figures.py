import unittest

from compare_sides import compare
from sample_sides import row, side, totals, without


def running_side(rows, overview_totals):
    return side(
        [without(r, "balance") for r in rows],
        without(overview_totals, "balance", "storageValue"),
    )


class DerivedFigures(unittest.TestCase):
    def test_the_running_balance_is_net_plus_donation_minus_craft_subsidy(self):
        running = running_side([row("Alice", net=100, donation=30, craftSubsidy=10)], totals())
        candidate = side([row("Alice", net=100, donation=30, craftSubsidy=10, balance=120)])

        tested = compare(running, candidate)

        self.assertEqual([], tested.findings)

    def test_a_candidate_balance_other_than_the_derived_one_is_a_finding(self):
        running = running_side([row("Alice", net=100, donation=30, craftSubsidy=10)], totals())
        candidate = side([row("Alice", net=100, donation=30, craftSubsidy=10, balance=121)])

        tested = compare(running, candidate)

        self.assertEqual(1, len(tested.findings))
        for expected in ("Alice", "balance", "120", "121"):
            self.assertIn(expected, tested.findings[0])

    def test_a_null_donation_leaves_the_derived_balance_null(self):
        running = running_side([row("Alice", net=100, donation=None, craftSubsidy=10)], totals())
        candidate = side([row("Alice", net=100, donation=None, craftSubsidy=10, balance=None)])

        tested = compare(running, candidate)

        self.assertEqual([], tested.findings)

    def test_the_running_guild_storage_value_and_balance_come_from_the_totals(self):
        figures = dict(storageDeposited=900, storageWithdrawn=200, donation=50, craftSubsidy=20, net=300)
        running = running_side([], totals(**figures))
        candidate = side([], totals(**figures, storageValue=730, balance=330))

        tested = compare(running, candidate)

        self.assertEqual([], tested.findings)

    def test_a_candidate_guild_storage_value_other_than_the_derived_one_is_a_finding(self):
        figures = dict(storageDeposited=900, storageWithdrawn=200, donation=50, craftSubsidy=20, net=300)
        running = running_side([], totals(**figures))
        candidate = side([], totals(**figures, storageValue=731, balance=330))

        tested = compare(running, candidate)

        self.assertEqual(1, len(tested.findings))
        for expected in ("totals", "storageValue", "730", "731"):
            self.assertIn(expected, tested.findings[0])

    def test_a_null_craft_subsidy_in_the_totals_leaves_both_guild_figures_null(self):
        figures = dict(storageDeposited=900, storageWithdrawn=200, donation=50, craftSubsidy=None, net=300)
        running = running_side([], totals(**figures))
        candidate = side([], totals(**figures, storageValue=None, balance=None))

        tested = compare(running, candidate)

        self.assertEqual([], tested.findings)


if __name__ == "__main__":
    unittest.main()
