import unittest

from candidate_rules import GUILD_FIGURES
from compare_sides import compare
from sample_sides import row, side, totals, without

RULE = "member no recompute has reached"
ROW_FIGURES = (
    "bankWithdrawn",
    "bankDeposited",
    "storageWithdrawn",
    "storageDeposited",
    "net",
    "donation",
    "craftSubsidy",
    "balance",
)

COMPUTED = {
    "bank_placement": 500,
    "bank_withdrawl": 100,
    "storage_placement": 10.0,
    "storage_withdrawl": 2.0,
    "storage_donation": 1.0,
    "storage_craft_subsidy": 0.0,
}


def nothing(**flows):
    return {figure: None for figure in ROW_FIGURES} | flows


def running_zero_side(flows=None, running_stored=None):
    zeros = dict(
        bankWithdrawn=0,
        bankDeposited=0,
        storageWithdrawn=0,
        storageDeposited=0,
        net=0,
        donation=None,
        craftSubsidy=None,
    )
    zeros.update(flows or {})
    return side(
        [without(row("Alice", **zeros), "balance")],
        without(totals(**zeros), "balance", "storageValue"),
        exact={"Alice": running_stored or {}},
    )


def candidate_null_side(**row_changes):
    guild = totals(**{figure: None for figure in GUILD_FIGURES})
    return side([row("Alice", **nothing(**row_changes))], guild, exact={"Alice": {}})


class NotYetComputed(unittest.TestCase):
    def test_a_null_candidate_row_against_a_zero_running_row_is_accepted_under_its_rule(self):
        running = running_zero_side()
        candidate = candidate_null_side()

        tested = compare(running, candidate)

        self.assertEqual([], tested.findings)
        self.assertIn(RULE, {a.rule.name for a in tested.accepted})
        self.assertEqual({"Alice", "totals"}, {a.scope for a in tested.accepted})

    def test_zero_running_flows_are_accepted_as_well_as_null_ones(self):
        running = running_zero_side({"donation": 0, "craftSubsidy": 0})
        candidate = candidate_null_side()

        tested = compare(running, candidate)

        self.assertEqual([], tested.findings)

    def test_the_stale_sums_instant_of_a_member_no_recompute_reached_is_accepted(self):
        running = running_zero_side()
        running["overview"]["items"][0]["staleSumsFrom"] = "2026-10-01T05:00:00Z"
        candidate = candidate_null_side()

        tested = compare(running, candidate)

        self.assertEqual([], tested.findings)

    def test_the_report_names_the_guild_figures_that_stayed_uncompared(self):
        running = running_zero_side()
        candidate = candidate_null_side()

        tested = compare(running, candidate)

        self.assertEqual(set(GUILD_FIGURES), set(tested.uncompared))

    def test_a_null_candidate_row_against_real_running_values_is_a_finding(self):
        running = running_zero_side(
            dict(bankWithdrawn=100, bankDeposited=500, storageWithdrawn=2, storageDeposited=10, net=408),
            running_stored=COMPUTED,
        )
        candidate = candidate_null_side()

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Alice" in f and "bankDeposited" in f and "differs" in f]
        self.assertEqual(1, len(found))
        self.assertEqual([], [a for a in tested.accepted if a.rule.name == RULE])

    def test_a_running_row_with_a_nonzero_sum_is_no_member_without_a_recompute(self):
        running = running_zero_side(dict(storageDeposited=3))
        candidate = candidate_null_side()

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Alice" in f and "storageDeposited" in f and "differs" in f]
        self.assertEqual(1, len(found))

    def test_a_member_the_running_side_computed_but_the_candidate_did_not_reach_is_a_finding(self):
        running = running_zero_side(
            dict(bankWithdrawn=100, bankDeposited=500, storageWithdrawn=2, storageDeposited=10, net=408),
            running_stored=COMPUTED,
        )
        candidate = candidate_null_side()

        tested = compare(running, candidate)

        self.assertNotEqual([], [f for f in tested.findings if "Alice" in f and "exact" in f])

    def test_no_guild_figure_is_reported_uncompared_where_the_rule_was_not_used(self):
        running = running_zero_side()
        running["overview"]["items"][0].update(nothing())
        candidate = candidate_null_side()

        tested = compare(running, candidate)

        self.assertEqual([], tested.uncompared)


if __name__ == "__main__":
    unittest.main()
