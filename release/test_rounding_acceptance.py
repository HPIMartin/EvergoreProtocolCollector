import unittest

from compare_sides import compare
from sample_sides import row, side, totals, without

SAME_EXACT = {
    "bank_placement": 0,
    "bank_withdrawl": 0,
    "storage_placement": 1.4,
    "storage_withdrawl": 0.6,
    "storage_donation": 0.5,
    "storage_craft_subsidy": 0.0,
}


def figures_of(net, **overrides):
    shown = dict(
        bankWithdrawn=0,
        bankDeposited=0,
        storageWithdrawn=1,
        storageDeposited=1,
        net=net,
        donation=1,
        craftSubsidy=0,
    )
    shown.update(overrides)
    return shown


def running_of(stored, net, **overrides):
    shown = figures_of(net, **overrides)
    return side(
        [without(row("Alice", **shown), "balance")],
        without(totals(**shown), "balance", "storageValue"),
        exact={"Alice": stored},
    )


def candidate_of(stored, net, balance, storage_value, **overrides):
    shown = figures_of(net, balance=balance, **overrides)
    return side(
        [row("Alice", **shown)],
        totals(**shown, storageValue=storage_value),
        exact={"Alice": stored},
    )


class RoundingDifferences(unittest.TestCase):
    def test_a_shown_difference_with_equal_exact_values_is_accepted_under_the_rounding_rule(self):
        running = running_of(SAME_EXACT, net=0)
        candidate = candidate_of(SAME_EXACT, net=1, balance=1, storage_value=1)

        tested = compare(running, candidate)

        self.assertEqual([], tested.findings)
        accepted = {(a.rule.name, a.scope, a.figure) for a in tested.accepted}
        self.assertEqual(
            {
                ("rounded once, halves away from zero", "Alice", "net"),
                ("rounded once, halves away from zero", "totals", "net"),
            },
            accepted,
        )
        self.assertEqual(0, tested.exit_code)

    def test_differing_exact_values_are_a_finding_even_when_the_shown_figure_is_one_off(self):
        other = dict(SAME_EXACT, storage_placement=1.5)
        running = running_of(SAME_EXACT, net=0)
        candidate = candidate_of(other, net=1, balance=1, storage_value=1, storageDeposited=2)

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "storageDeposited" in f and "Alice" in f]
        self.assertEqual(1, len(found))
        exact = [f for f in tested.findings if "storage_placement" in f]
        self.assertEqual(1, len(exact))
        for expected in ("Alice", "1.4", "1.5"):
            self.assertIn(expected, exact[0])
        self.assertEqual(1, tested.exit_code)

    def test_a_differing_bank_figure_is_never_a_rounding_difference(self):
        running = running_of(SAME_EXACT, net=0)
        candidate = candidate_of(SAME_EXACT, net=1, balance=1, storage_value=1, bankDeposited=3)

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Alice" in f and "bankDeposited" in f and "differs" in f]
        self.assertEqual(1, len(found))

    def test_a_running_row_figure_its_exact_sums_do_not_yield_is_a_finding(self):
        running = running_of(SAME_EXACT, net=0, storageDeposited=5)
        candidate = candidate_of(SAME_EXACT, net=1, balance=1, storage_value=1, storageDeposited=5)

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "running serves storageDeposited" in f]
        self.assertEqual(1, len(found))
        for expected in ("Alice", "5", "1"):
            self.assertIn(expected, found[0])

    def test_running_totals_that_are_not_the_sum_of_its_rows_are_a_finding(self):
        running = running_of(SAME_EXACT, net=0)
        running["overview"]["totals"]["storageDeposited"] = 7
        candidate = candidate_of(SAME_EXACT, net=1, balance=1, storage_value=1, storageDeposited=7)

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "totals: running serves storageDeposited" in f]
        self.assertEqual(1, len(found))


if __name__ == "__main__":
    unittest.main()
