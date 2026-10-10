import unittest

import running_rules

STORED = {
    "bank_placement": 500,
    "bank_withdrawl": 100,
    "storage_placement": 10.5,
    "storage_withdrawl": 2.5,
}


class MemberFigures(unittest.TestCase):
    def test_donation_and_craft_subsidy_are_formed_together_when_both_sums_exist(self):
        stored = {**STORED, "storage_donation": 0.5, "storage_craft_subsidy": 0.25}

        tested = running_rules.member_figures(stored)

        self.assertEqual(1, tested["donation"])
        self.assertEqual(0, tested["craftSubsidy"])

    def test_with_only_one_of_the_two_sums_both_figures_are_none(self):
        for present in ("storage_donation", "storage_craft_subsidy"):
            with self.subTest(present=present):
                stored = {**STORED, present: 5.0}

                tested = running_rules.member_figures(stored)

                self.assertIsNone(tested["donation"])
                self.assertIsNone(tested["craftSubsidy"])

    def test_without_either_sum_both_figures_are_none(self):
        tested = running_rules.member_figures(STORED)

        self.assertIsNone(tested["donation"])
        self.assertIsNone(tested["craftSubsidy"])


if __name__ == "__main__":
    unittest.main()
