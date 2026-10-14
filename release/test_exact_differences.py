import math
import unittest

from compare_sides import compare
from sample_sides import ledgers_holding, side_of, storage_entry

STORED = {
    "bank_placement": 100,
    "bank_withdrawl": 30,
    "storage_placement": 30.0,
    "storage_withdrawl": 2.0,
    "storage_donation": 1.0,
    "storage_craft_subsidy": 6.0,
}


class ExactSums(unittest.TestCase):
    def test_an_ammunition_credit_the_candidate_adds_a_second_time_is_a_finding(self):
        ledgers = ledgers_holding(storage_entry("Pfeile", quantity=10, quality=100))
        credited_twice = dict(STORED, storage_placement=42.0, storage_craft_subsidy=18.0)
        running = side_of({"Alice": STORED}, ledgers=ledgers)
        candidate = side_of({"Alice": credited_twice}, ledgers=ledgers)

        tested = compare(running, candidate)

        self.assertIn("Alice: exact storage_placement differs, running 30.0, candidate 42.0", tested.findings)
        self.assertIn("Alice: exact storage_craft_subsidy differs, running 6.0, candidate 18.0", tested.findings)

    def test_a_drift_in_the_last_bit_of_an_exact_storage_sum_is_a_finding(self):
        drifted = dict(STORED, storage_placement=math.nextafter(30.0, math.inf))
        running, candidate = side_of({"Alice": STORED}), side_of({"Alice": drifted})

        tested = compare(running, candidate)

        self.assertEqual(
            [f"Alice: exact storage_placement differs, running 30.0, candidate {drifted['storage_placement']!r}"],
            tested.findings,
        )


if __name__ == "__main__":
    unittest.main()
