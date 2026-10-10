import unittest

from compare_sides import compare
from sample_sides import row, side, totals

STORED = {
    "bank_placement": 100,
    "bank_withdrawl": 30,
    "storage_placement": 10.5,
    "storage_withdrawl": 2.5,
    "storage_donation": 0.5,
    "storage_craft_subsidy": 0.25,
}

ALICE = dict(
    bankWithdrawn=30,
    bankDeposited=100,
    storageWithdrawn=3,
    storageDeposited=11,
    net=78,
    donation=1,
    craftSubsidy=0,
    balance=78,
)

GUILD = dict(ALICE, storageValue=8)


def both_sides(candidate_row, candidate_totals=None):
    exact = {"Alice": STORED}
    guild = totals(**GUILD) if candidate_totals is None else candidate_totals
    return (
        side([candidate_row], guild, exact=exact),
        side([candidate_row], guild, exact=exact),
    )


class CandidateRounding(unittest.TestCase):
    def test_figures_that_are_the_roundings_of_the_exact_values_are_no_finding(self):
        running, candidate = both_sides(row("Alice", **ALICE))

        tested = compare(running, candidate)

        self.assertEqual([], tested.findings)

    def test_a_row_figure_that_is_not_the_rounding_of_its_exact_value_is_a_finding(self):
        running, candidate = both_sides(row("Alice", **dict(ALICE, storageDeposited=10)))

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "storageDeposited" in f]
        self.assertEqual(1, len(found))
        for expected in ("Alice", "candidate", "10", "11"):
            self.assertIn(expected, found[0])

    def test_a_totals_figure_that_is_not_the_rounding_of_its_exact_sum_is_a_finding(self):
        running, candidate = both_sides(row("Alice", **ALICE), totals(**dict(GUILD, net=79)))

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "totals" in f and "net" in f]
        self.assertEqual(1, len(found))
        for expected in ("candidate", "79", "78"):
            self.assertIn(expected, found[0])

    def test_a_computed_row_shown_as_not_yet_computed_is_a_finding(self):
        nulls = {figure: None for figure in ALICE}
        running, candidate = both_sides(row("Alice", **nulls))

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Alice" in f and "candidate" in f]
        self.assertEqual(len(ALICE), len(found))

    def test_a_row_shown_with_figures_but_stored_as_not_yet_computed_is_a_finding(self):
        running, candidate = both_sides(row("Alice", **ALICE))
        candidate["exact"] = {"Alice": {}}
        running["exact"] = {"Alice": {}}

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Alice" in f and "candidate" in f and "net" in f]
        self.assertEqual(1, len(found))


if __name__ == "__main__":
    unittest.main()
