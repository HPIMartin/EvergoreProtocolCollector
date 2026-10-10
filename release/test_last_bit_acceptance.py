import math
import random
import unittest

from compare_sides import compare
from sample_sides import candidate_side_of, ledgers_holding, running_side_of, storage_entry

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


def sides(entry_count=0, **candidate_changes):
    ledgers = ledgers_holding(*[storage_entry("Eisen")] * entry_count)
    return (
        running_side_of({"Alice": RUNNING}, ledgers=ledgers),
        candidate_side_of({"Alice": {**RUNNING, **candidate_changes}}, ledgers=ledgers),
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

    def test_a_drift_that_moves_no_shown_figure_leaves_a_guild_rounding_difference_to_the_rounding_rule(self):
        tenths = {**RUNNING, "storage_placement": 0.4}
        running = running_side_of({"Alice": tenths, "Bob": tenths})
        candidate = candidate_side_of({"Alice": {**tenths, "storage_placement": drifted(0.4, 1)}, "Bob": tenths})

        tested = compare(running, candidate)

        self.assertEqual([], tested.findings)
        self.assertEqual(
            ["rounded once, halves away from zero"],
            [a.rule.name for a in tested.accepted if a.scope == "totals" and a.figure == "storageDeposited"],
        )

    def test_a_drift_beyond_four_ulp_is_a_finding(self):
        running, candidate = sides(storage_placement=drifted(RUNNING["storage_placement"], 5))

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Alice" in f and "exact storage_placement" in f]
        self.assertEqual(1, len(found))

    def test_the_bound_grows_with_the_storage_entries_of_the_member_by_one_ulp_each(self):
        for entries in (1, 10, 50):
            for ulps, accepted in ((entries + 4, True), (entries + 5, False)):
                with self.subTest(entries=entries, ulps=ulps):
                    running, candidate = sides(entries, storage_placement=drifted(RUNNING["storage_placement"], ulps))

                    tested = compare(running, candidate)

                    found = [f for f in tested.findings if "Alice" in f and "exact storage_placement" in f]
                    self.assertEqual(0 if accepted else 1, len(found))

    def test_a_reordered_sum_over_many_entries_is_accepted(self):
        generator = random.Random(5)
        amounts = [generator.uniform(0.01, 50) * 0.6 for _ in range(200)]
        in_order = 0.0
        for amount in amounts:
            in_order += amount
        reordered = 0.0
        for amount in sorted(amounts):
            reordered += amount
        ledgers = ledgers_holding(*[storage_entry("Eisen")] * 200)
        running = running_side_of({"Alice": {**RUNNING, "storage_placement": in_order}}, ledgers=ledgers)
        candidate = candidate_side_of({"Alice": {**RUNNING, "storage_placement": reordered}}, ledgers=ledgers)

        tested = compare(running, candidate)

        self.assertLess(4, abs(in_order - reordered) / math.ulp(in_order))
        self.assertEqual([], tested.findings)

    def test_the_bound_is_measured_at_the_larger_magnitude(self):
        large = {**RUNNING, "storage_placement": 2733625.2}
        running = running_side_of({"Alice": large})
        candidate = candidate_side_of({"Alice": {**large, "storage_placement": drifted(2733625.2, 4)}})

        tested = compare(running, candidate)

        self.assertEqual([], tested.findings)

    def test_a_sum_stored_on_one_side_only_is_never_within_bound(self):
        without_share = {key: value for key, value in RUNNING.items() if key not in ("storage_donation", "storage_craft_subsidy")}
        running = running_side_of({"Alice": without_share})
        candidate = candidate_side_of({"Alice": {**without_share, "storage_donation": 0.0, "storage_craft_subsidy": 0.0}})

        tested = compare(running, candidate)

        self.assertIn("Alice: exact storage_donation differs, running None, candidate 0.0", tested.findings)
        self.assertNotIn(LAST_BIT, {a.rule.name for a in tested.accepted})

    def test_a_bank_sum_never_drifts(self):
        running, candidate = sides(bank_withdrawl=101)

        tested = compare(running, candidate)

        found = [f for f in tested.findings if "Alice" in f and "exact bank_withdrawl" in f]
        self.assertEqual(1, len(found))


if __name__ == "__main__":
    unittest.main()
