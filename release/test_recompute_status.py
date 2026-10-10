import unittest

from compare_sides import compare
from sample_sides import admin, candidate_side_of, running_side_of, side

INSTANT = "2026-10-10T08:00:00Z"


class RecomputeStatus(unittest.TestCase):
    def test_two_sides_without_a_successful_recompute_and_with_a_failure_are_a_finding_each(self):
        failed = admin(lastSuccessfulRecompute=None, lastRecomputeFailure=INSTANT)
        running = side(admin=failed)
        candidate = side(admin=failed)

        tested = compare(running, candidate)

        self.assertEqual(1, tested.exit_code)
        self.assertIn("admin: the running side shows no successful recompute", tested.findings)
        self.assertIn("admin: the candidate side shows no successful recompute", tested.findings)

    def test_a_failure_next_to_a_successful_recompute_is_a_finding_for_that_side_only(self):
        running = side(admin=admin(lastSuccessfulRecompute=INSTANT))
        candidate = side(admin=admin(lastSuccessfulRecompute=INSTANT, lastRecomputeFailure=INSTANT))

        tested = compare(running, candidate)

        self.assertEqual(["admin: the candidate side shows a recompute failure"], tested.findings)

    def test_a_side_with_a_successful_recompute_and_no_failure_is_no_finding(self):
        running = side(admin=admin(lastSuccessfulRecompute=INSTANT))
        candidate = side(admin=admin(lastSuccessfulRecompute=INSTANT))

        tested = compare(running, candidate)

        self.assertEqual([], tested.findings)


STARTED = 1760000000000

STORED = {
    "bank_placement": 500,
    "bank_withdrawl": 100,
    "storage_placement": 10.0,
    "storage_withdrawl": 2.0,
    "storage_donation": 1.0,
    "storage_craft_subsidy": 0.0,
}


def running_recomputed(**instants):
    return running_side_of({"Alice": STORED}, recomputed=instants)


def candidate_recomputed(**instants):
    return candidate_side_of({"Alice": STORED}, recomputed=instants)


def without_stored_sums():
    return side(admin=admin(), recomputed={}, exact={"Alice": {}})


class SumsRecomputedInThisRun(unittest.TestCase):
    def test_a_member_whose_sums_were_recomputed_after_the_start_on_both_sides_is_no_finding(self):
        running = running_recomputed(Alice=STARTED)
        candidate = candidate_recomputed(Alice=STARTED + 1)

        tested = compare(running, candidate, STARTED)

        self.assertEqual([], tested.findings)

    def test_a_member_with_no_recompute_instant_is_a_finding_for_that_side(self):
        running = running_recomputed(Alice=STARTED)
        candidate = candidate_recomputed()

        tested = compare(running, candidate, STARTED)

        self.assertEqual(["Alice: the candidate side did not recompute the sums in this run"], tested.findings)

    def test_a_member_whose_recompute_instant_is_before_the_start_is_a_finding_for_that_side(self):
        running = running_recomputed(Alice=STARTED - 1)
        candidate = candidate_recomputed(Alice=STARTED)

        tested = compare(running, candidate, STARTED)

        self.assertEqual(["Alice: the running side did not recompute the sums in this run"], tested.findings)

    def test_a_member_with_sums_on_one_side_only_and_no_recompute_instant_is_a_finding_for_each_side(self):
        running = without_stored_sums()
        candidate = candidate_recomputed()

        tested = compare(running, candidate, STARTED)

        self.assertIn("Alice: the running side did not recompute the sums in this run", tested.findings)
        self.assertIn("Alice: the candidate side did not recompute the sums in this run", tested.findings)

    def test_a_member_with_no_stored_sums_on_either_side_needs_no_recompute_instant(self):
        running = without_stored_sums()
        candidate = without_stored_sums()

        tested = compare(running, candidate, STARTED)

        self.assertEqual([], [f for f in tested.findings if "did not recompute" in f])

    def test_without_a_start_instant_no_recompute_instant_is_required(self):
        running = running_recomputed()
        candidate = candidate_recomputed()

        tested = compare(running, candidate)

        self.assertEqual([], tested.findings)


if __name__ == "__main__":
    unittest.main()
