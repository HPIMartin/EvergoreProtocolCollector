import math
import unittest

from compare_sides import compare
from sample_sides import candidate_side_of, running_side_of

RUNNING = {
    "bank_placement": 500,
    "bank_withdrawl": 100,
    "storage_placement": 10.5,
    "storage_withdrawl": 2.5,
    "storage_donation": 0.5,
    "storage_craft_subsidy": 0.25,
}

LAST_BIT = "last-bit drift of an exact storage sum"


def drifted(value, ulps):
    for _ in range(abs(ulps)):
        value = math.nextafter(value, math.inf if ulps > 0 else -math.inf)
    return value


def sides(**candidate_changes):
    return (
        running_side_of({"Alice": RUNNING}),
        candidate_side_of({"Alice": {**RUNNING, **candidate_changes}}),
    )


class LastBitDrift(unittest.TestCase):
    def test_a_drift_of_a_few_ulp_in_a_storage_sum_is_accepted_even_where_it_flips_a_shown_figure(self):
        for key in ("storage_placement", "storage_withdrawl", "storage_donation", "storage_craft_subsidy"):
            for ulps in (1, -1, 4, -4):
                with self.subTest(key=key, ulps=ulps):
                    running, candidate = sides(**{key: drifted(RUNNING[key], ulps)})

                    tested = compare(running, candidate)

                    self.assertEqual([], tested.findings)

    def test_the_accepted_drift_is_named_by_its_rule(self):
        running, candidate = sides(storage_placement=drifted(RUNNING["storage_placement"], -1))

        tested = compare(running, candidate)

        self.assertIn(LAST_BIT, {a.rule.name for a in tested.accepted})
        self.assertEqual({"storageDeposited", "balance"}, {a.figure for a in tested.accepted if a.scope == "Alice"})

    def test_a_drift_beyond_four_ulp_is_a_finding(self):
        running, candidate = sides(storage_placement=drifted(RUNNING["storage_placement"], 5))

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Alice" in f and "exact storage_placement" in f]
        self.assertEqual(1, len(found))

    def test_the_bound_is_measured_at_the_larger_magnitude(self):
        large = {**RUNNING, "storage_placement": 2733625.2}
        running = running_side_of({"Alice": large})
        candidate = candidate_side_of({"Alice": {**large, "storage_placement": drifted(2733625.2, 4)}})

        tested = compare(running, candidate)

        self.assertEqual([], tested.findings)

    def test_a_bank_sum_never_drifts(self):
        running, candidate = sides(bank_withdrawl=101)

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Alice" in f and "exact bank_withdrawl" in f]
        self.assertEqual(1, len(found))


if __name__ == "__main__":
    unittest.main()
