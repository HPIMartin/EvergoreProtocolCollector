import math
import re
import unittest

from compare_sides import compare
from expected_deviations import ALL_RULES
from sample_sides import candidate_side_of, running_side_of, side

STORED = {
    "bank_placement": 500,
    "bank_withdrawl": 100,
    "storage_placement": 10.5,
    "storage_withdrawl": 2.5,
    "storage_donation": 0.5,
    "storage_craft_subsidy": 0.25,
}


def drifted_sides():
    drifted = {**STORED, "storage_placement": math.nextafter(10.5, 0.0)}
    return running_side_of({"Alice": STORED}), candidate_side_of({"Alice": drifted})


def identical_sides():
    return running_side_of({"Alice": STORED}), candidate_side_of({"Alice": STORED})


def plain_sides():
    return side(), side()


class Rendering(unittest.TestCase):
    def test_every_rule_used_is_named_with_its_decision_dates_and_how_often(self):
        running, candidate = drifted_sides()

        tested = compare(running, candidate).render()

        self.assertIn("last-bit drift of an exact storage sum (decision 2026-09-27)", tested)
        self.assertRegex(tested, r"last-bit drift of an exact storage sum \(decision 2026-09-27\): \d+ ")

    def test_every_accepted_figure_is_listed_under_its_rule_with_both_values(self):
        running, candidate = drifted_sides()

        tested = compare(running, candidate).render()

        under_rule = tested.split("last-bit drift of an exact storage sum (decision 2026-09-27)")[1]
        self.assertIn("    - Alice: storageDeposited, running 11, candidate 10", under_rule.split("Listed rules")[0])

    def test_a_rule_with_two_decisions_names_both_dates(self):
        running, candidate = drifted_sides()

        tested = compare(running, candidate).render()

        self.assertIn("rounded once, halves away from zero (decisions 2026-09-27, 2026-09-30)", tested)

    def test_a_listed_rule_that_did_not_occur_is_listed_as_unused_and_the_exit_stays_zero(self):
        running, candidate = plain_sides()

        report = compare(running, candidate)

        tested = report.render()
        self.assertEqual(0, report.exit_code)
        unused = tested.split("Listed rules that did not occur:")[1]
        for rule in ALL_RULES:
            self.assertIn(rule.name, unused)

    def test_a_rule_that_occurred_is_not_listed_as_unused(self):
        running, candidate = drifted_sides()

        tested = compare(running, candidate).render()

        unused = tested.split("Listed rules that did not occur:")[1]
        self.assertNotIn("last-bit drift of an exact storage sum", unused)

    def test_an_exact_sum_a_rule_explained_without_a_shown_difference_is_listed_with_both_values(self):
        drifted = {**STORED, "storage_withdrawl": math.nextafter(2.5, 3.0)}
        running, candidate = running_side_of({"Alice": STORED}), candidate_side_of({"Alice": drifted})

        tested = compare(running, candidate).render()

        under_rule = tested.split("last-bit drift of an exact storage sum (decision 2026-09-27)")[1].split("Listed rules")[0]
        self.assertIn(f"    - Alice: exact storage_withdrawl, running 2.5, candidate {drifted['storage_withdrawl']!r}", under_rule)

    def test_the_count_of_every_rule_equals_the_number_of_lines_listed_under_it(self):
        for name, sides in (("drifted", drifted_sides()), ("identical", identical_sides())):
            with self.subTest(sides=name):
                tested = compare(*sides).render()

                accepted = tested.split("Accepted deviations:")[1].split("Listed rules that did not occur:")[0]
                counted = 0
                lines = 0
                for line in accepted.splitlines():
                    if re.match(r"  - .*: \d+ accepted$", line):
                        counted += int(line.rsplit(": ", 1)[1].split()[0])
                    elif line.startswith("    - "):
                        lines += 1
                self.assertGreater(counted, 0)
                self.assertEqual(counted, lines)

    def test_a_rule_that_explained_an_exact_sum_without_a_shown_difference_counts_as_used(self):
        drifted = {**STORED, "storage_donation": math.nextafter(0.5, 1.0)}
        running, candidate = running_side_of({"Alice": STORED}), candidate_side_of({"Alice": drifted})

        tested = compare(running, candidate).render()

        unused = tested.split("Listed rules that did not occur:")[1]
        self.assertNotIn("last-bit drift of an exact storage sum", unused)

    def test_the_listed_additions_count_as_used_when_the_candidate_serves_them(self):
        running, candidate = identical_sides()

        tested = compare(running, candidate).render()

        unused = tested.split("Listed rules that did not occur:")[1]
        self.assertNotIn("listed wire fields", unused)

    def test_findings_are_listed_and_the_verdict_names_the_exit_status(self):
        running, candidate = identical_sides()
        candidate["overview"]["items"][0]["bankDeposited"] = 7

        report = compare(running, candidate)

        tested = report.render()
        self.assertIn("Alice: bankDeposited differs, running 500, candidate 7", tested)
        self.assertIn("FAIL", tested)
        self.assertEqual(1, report.exit_code)

    def test_a_passing_report_says_pass(self):
        running, candidate = plain_sides()

        tested = compare(running, candidate).render()

        self.assertIn("PASS", tested)
        self.assertNotIn("FAIL", tested)

    def test_the_admin_run_instants_are_said_to_be_not_compared(self):
        running, candidate = identical_sides()

        tested = compare(running, candidate).render()

        self.assertIn("The five run instants of the admin status are not compared", tested)

    def test_uncompared_guild_figures_are_named(self):
        running, candidate = identical_sides()
        candidate_report = compare(running, candidate)
        candidate_report.uncompared = ["net", "balance"]

        tested = candidate_report.render()

        self.assertIn("Guild figures not compared: net, balance", tested)


if __name__ == "__main__":
    unittest.main()
