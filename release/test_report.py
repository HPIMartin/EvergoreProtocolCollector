import unittest

from compare_sides import compare
from sample_sides import side, side_of

STORED = {
    "bank_placement": 500,
    "bank_withdrawl": 100,
    "storage_placement": 10.5,
    "storage_withdrawl": 2.5,
    "storage_donation": 0.5,
    "storage_craft_subsidy": 0.25,
}


def identical_sides():
    return side_of({"Alice": STORED}), side_of({"Alice": STORED})


def plain_sides():
    return side(), side()


class Rendering(unittest.TestCase):
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

    def test_the_report_says_that_no_deviation_is_expected(self):
        running, candidate = identical_sides()

        rendered = compare(running, candidate).render()

        self.assertIn("Expected deviations: none, every difference between the two sides is a finding.", rendered.splitlines())


if __name__ == "__main__":
    unittest.main()
