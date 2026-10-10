import unittest

from compare_sides import compare
from sample_sides import candidate_side_of, ledgers_holding, running_side_of, storage_entry

RUNNING = {
    "bank_placement": 500,
    "bank_withdrawl": 100,
    "storage_placement": 1000.0,
    "storage_withdrawl": 200.0,
    "storage_donation": 50.0,
    "storage_craft_subsidy": 100.0,
}


def with_credit(**changes):
    return {**RUNNING, **changes}


AMMUNITION = "full ammunition credit"


def sides(candidate_stored, *entries):
    ledgers = ledgers_holding(*entries)
    return (
        running_side_of({"Alice": RUNNING}, ledgers=ledgers),
        candidate_side_of({"Alice": candidate_stored}, ledgers=ledgers),
    )


class AmmunitionCredit(unittest.TestCase):
    def test_the_full_credit_of_a_deposited_ammunition_sort_is_accepted_in_its_shape(self):
        for sort in ("Pfeile", "Bolzen", "Magieessenz"):
            with self.subTest(sort=sort):
                running, candidate = sides(
                    with_credit(storage_placement=1060.0, storage_craft_subsidy=160.0), storage_entry(sort)
                )

                tested = compare(running, candidate)

                self.assertEqual([], tested.findings)
                rules = {a.rule.name for a in tested.accepted}
                self.assertIn(AMMUNITION, rules)
                moved = {a.figure for a in tested.accepted if a.scope == "Alice" and a.rule.name == AMMUNITION}
                self.assertEqual({"storageDeposited", "craftSubsidy", "net"}, moved)

    def test_the_same_shape_of_difference_without_an_ammunition_deposit_is_a_finding(self):
        running, candidate = sides(
            with_credit(storage_placement=1060.0, storage_craft_subsidy=160.0), storage_entry("Eisen")
        )

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Alice" in f and "exact storage_placement" in f]
        self.assertEqual(1, len(found))
        self.assertEqual([], [a for a in tested.accepted if a.rule.name == AMMUNITION])

    def test_a_withdrawal_of_ammunition_is_no_deposit(self):
        running, candidate = sides(
            with_credit(storage_placement=1060.0, storage_craft_subsidy=160.0), storage_entry("Pfeile", "WITHDRAWAL")
        )

        tested = compare(running, candidate)

        self.assertNotEqual(0, tested.exit_code)

    def test_a_credit_the_subsidy_does_not_match_is_a_finding(self):
        running, candidate = sides(
            with_credit(storage_placement=1060.0, storage_craft_subsidy=150.0), storage_entry("Pfeile")
        )

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Alice" in f and "exact storage_placement" in f]
        self.assertEqual(1, len(found))

    def test_a_differing_withdrawal_sum_is_a_finding_even_for_an_ammunition_member(self):
        running, candidate = sides(
            with_credit(storage_placement=1060.0, storage_craft_subsidy=160.0, storage_withdrawl=205.0),
            storage_entry("Pfeile"),
        )

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Alice" in f and "exact storage_withdrawl" in f]
        self.assertEqual(1, len(found))
        self.assertEqual([], [a for a in tested.accepted if a.rule.name == AMMUNITION])

    def test_a_differing_donation_is_a_finding_even_for_an_ammunition_member(self):
        running, candidate = sides(
            with_credit(storage_placement=1060.0, storage_craft_subsidy=160.0, storage_donation=55.0),
            storage_entry("Pfeile"),
        )

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Alice" in f and "exact storage_donation" in f]
        self.assertEqual(1, len(found))

    def test_a_differing_bank_sum_is_a_finding_even_for_an_ammunition_member(self):
        running, candidate = sides(
            with_credit(storage_placement=1060.0, storage_craft_subsidy=160.0, bank_placement=501),
            storage_entry("Pfeile"),
        )

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Alice" in f and "exact bank_placement" in f]
        self.assertEqual(1, len(found))

    def test_a_credit_that_lowers_the_candidate_is_a_finding(self):
        running, candidate = sides(
            with_credit(storage_placement=940.0, storage_craft_subsidy=40.0), storage_entry("Pfeile")
        )

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Alice" in f and "exact storage_placement" in f]
        self.assertEqual(1, len(found))


if __name__ == "__main__":
    unittest.main()
