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

MARKET_VALUES = {"Pfeile": 3, "Bolzen": 12, "Magieessenz": 4}

AMMUNITION = "full ammunition credit"


def credit_of(sort, quantity, quality):
    market = MARKET_VALUES[sort]
    return market * 1.0 * quantity * (quality / 100) - market * 0.6 * quantity * (quality / 100)


def true_delta(*entries):
    total = 0.0
    for entry in entries:
        if entry["transferType"] == "DEPOSIT" and entry["name"] in MARKET_VALUES:
            total += credit_of(entry["name"], entry["quantity"], entry["quality"])
    return total


def with_credit(**changes):
    return {**RUNNING, **changes}


def credited(placement_delta, craft_subsidy_delta, **changes):
    return with_credit(
        storage_placement=RUNNING["storage_placement"] + placement_delta,
        storage_craft_subsidy=RUNNING["storage_craft_subsidy"] + craft_subsidy_delta,
        **changes,
    )


def sides(candidate_stored, *entries):
    ledgers = ledgers_holding(*entries)
    return (
        running_side_of({"Alice": RUNNING}, ledgers=ledgers),
        candidate_side_of({"Alice": candidate_stored}, ledgers=ledgers),
    )


def arrows():
    return storage_entry("Pfeile", quantity=50, quality=100)


class AmmunitionCredit(unittest.TestCase):
    def test_the_full_credit_of_a_deposited_ammunition_sort_is_accepted_in_its_shape(self):
        for entry in (
            storage_entry("Pfeile", quantity=50, quality=100),
            storage_entry("Bolzen", quantity=25, quality=50),
            storage_entry("Magieessenz", quantity=75, quality=50),
        ):
            with self.subTest(sort=entry["name"]):
                delta = true_delta(entry)
                running, candidate = sides(credited(delta, delta), entry)

                tested = compare(running, candidate)

                self.assertEqual([], tested.findings)
                rules = {a.rule.name for a in tested.accepted}
                self.assertIn(AMMUNITION, rules)
                moved = {a.figure for a in tested.accepted if a.scope == "Alice" and a.rule.name == AMMUNITION}
                self.assertEqual({"storageDeposited", "craftSubsidy", "net"}, moved)

    def test_the_true_delta_of_a_mixed_ledger_is_accepted(self):
        entries = [
            storage_entry("Pfeile", quantity=50, quality=100),
            storage_entry("Bolzen", quantity=25, quality=50),
            storage_entry("Magieessenz", quantity=75, quality=50),
            storage_entry("Eisenbarren", quantity=100, quality=100),
            storage_entry("Pfeile", "WITHDRAWAL", quantity=10, quality=100),
            storage_entry("Pfeile", quantity=7, quality=33),
        ]
        delta = true_delta(*entries)
        running, candidate = sides(credited(delta, delta), *entries)

        tested = compare(running, candidate)

        self.assertEqual([], tested.findings)

    def test_the_same_shape_of_difference_without_an_ammunition_deposit_is_a_finding(self):
        running, candidate = sides(credited(60.0, 60.0), storage_entry("Eisen", quantity=50, quality=100))

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Alice" in f and "exact storage_placement" in f]
        self.assertEqual(1, len(found))
        self.assertEqual([], [a for a in tested.accepted if a.rule.name == AMMUNITION])

    def test_a_withdrawal_of_ammunition_is_no_deposit(self):
        running, candidate = sides(credited(60.0, 60.0), storage_entry("Pfeile", "WITHDRAWAL", quantity=50, quality=100))

        tested = compare(running, candidate)

        self.assertNotEqual(0, tested.exit_code)

    def test_a_credit_the_subsidy_does_not_match_is_a_finding(self):
        delta = true_delta(arrows())
        running, candidate = sides(credited(delta, delta - 10), arrows())

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Alice" in f and "exact storage_placement" in f]
        self.assertEqual(1, len(found))

    def test_a_credit_that_exceeds_what_the_ledger_explains_is_a_finding(self):
        running, candidate = sides(credited(1000000.0, 1000000.0), arrows())

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Alice" in f and "exact storage_placement" in f]
        self.assertEqual(1, len(found))
        self.assertEqual([], [a for a in tested.accepted if a.rule.name == AMMUNITION])

    def test_a_credit_hidden_behind_one_arrow_next_to_other_deposits_is_a_finding(self):
        entries = [arrows()] + [storage_entry("Eisenbarren", quantity=1, quality=100)] * 100
        running, candidate = sides(credited(4800.0, 4800.0), *entries)

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Alice" in f and "exact storage_placement" in f]
        self.assertEqual(1, len(found))

    def test_a_differing_withdrawal_sum_is_a_finding_even_for_an_ammunition_member(self):
        delta = true_delta(arrows())
        running, candidate = sides(credited(delta, delta, storage_withdrawl=205.0), arrows())

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Alice" in f and "exact storage_withdrawl" in f]
        self.assertEqual(1, len(found))
        self.assertEqual([], [a for a in tested.accepted if a.rule.name == AMMUNITION])

    def test_a_differing_donation_is_a_finding_even_for_an_ammunition_member(self):
        delta = true_delta(arrows())
        running, candidate = sides(credited(delta, delta, storage_donation=55.0), arrows())

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Alice" in f and "exact storage_donation" in f]
        self.assertEqual(1, len(found))

    def test_a_differing_bank_sum_is_a_finding_even_for_an_ammunition_member(self):
        delta = true_delta(arrows())
        running, candidate = sides(credited(delta, delta, bank_placement=501), arrows())

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Alice" in f and "exact bank_placement" in f]
        self.assertEqual(1, len(found))

    def test_a_credit_that_lowers_the_candidate_is_a_finding(self):
        running, candidate = sides(credited(-60.0, -60.0), arrows())

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Alice" in f and "exact storage_placement" in f]
        self.assertEqual(1, len(found))


if __name__ == "__main__":
    unittest.main()
