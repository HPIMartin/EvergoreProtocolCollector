import unittest

from candidate_rules import guild_figures, member_figures, whole_gold_of

STORED = {
    "bank_placement": 100,
    "bank_withdrawl": 30,
    "storage_placement": 10.5,
    "storage_withdrawl": 2.5,
    "storage_donation": 0.5,
    "storage_craft_subsidy": 0.25,
}


class WholeGold(unittest.TestCase):
    def test_a_figure_is_rounded_to_the_nearest_whole_gold_with_halves_away_from_zero(self):
        for exact, expected in ((-0.5, -1), (0.5, 1), (2.4999, 2), (0.49999999999999994, 0), (-2.5, -3), (2.5, 3)):
            with self.subTest(exact=exact):
                tested = whole_gold_of(exact)

                self.assertEqual(expected, tested)


class MemberFigures(unittest.TestCase):
    def test_every_shown_figure_is_the_rounding_of_its_exact_value(self):
        tested = member_figures(STORED)

        self.assertEqual(
            {
                "bankWithdrawn": 30,
                "bankDeposited": 100,
                "storageWithdrawn": 3,
                "storageDeposited": 11,
                "net": 78,
                "donation": 1,
                "craftSubsidy": 0,
                "balance": 78,
            },
            tested,
        )

    def test_a_member_without_a_stored_donation_has_null_flows_and_balance(self):
        stored = {k: v for k, v in STORED.items() if k != "storage_donation"}

        tested = member_figures(stored)

        self.assertEqual((78, None, None, None), (tested["net"], tested["donation"], tested["craftSubsidy"], tested["balance"]))

    def test_a_member_whose_four_ledger_sums_are_not_all_stored_is_not_yet_computed(self):
        stored = {k: v for k, v in STORED.items() if k != "storage_withdrawl"}

        tested = member_figures(stored)

        self.assertIsNone(tested)


class GuildFigures(unittest.TestCase):
    def test_the_exact_contributions_are_summed_before_the_total_is_rounded_once(self):
        first = {**STORED, "storage_placement": 0.4, "storage_withdrawl": 0.0, "bank_placement": 0, "bank_withdrawl": 0}
        second = {**first, "storage_placement": 0.4}

        tested = guild_figures([first, second])

        self.assertEqual(1, tested["storageDeposited"])
        self.assertEqual(1, tested["net"])

    def test_the_guild_storage_value_is_the_deposits_plus_donation_less_subsidy_and_withdrawals(self):
        tested = guild_figures([STORED, STORED])

        self.assertEqual(17, tested["storageValue"])

    def test_one_member_not_yet_computed_leaves_every_total_null(self):
        tested = guild_figures([STORED, {}])

        self.assertEqual({None}, set(tested.values()))

    def test_one_member_without_a_guild_share_leaves_the_guild_share_figures_null(self):
        without_share = {k: v for k, v in STORED.items() if k != "storage_donation"}

        tested = guild_figures([STORED, without_share])

        self.assertEqual(
            (None, None, None, None, 156),
            (tested["donation"], tested["craftSubsidy"], tested["balance"], tested["storageValue"], tested["net"]),
        )


if __name__ == "__main__":
    unittest.main()
