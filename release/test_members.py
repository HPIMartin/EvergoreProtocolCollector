import copy
import unittest

from compare_sides import compare
from sample_sides import row, side


class Members(unittest.TestCase):
    def test_a_member_only_the_running_side_lists_is_a_finding(self):
        running = side([row("Alice"), row("Bob")])
        candidate = side([row("Alice")])

        tested = compare(running, candidate)

        missing = [f for f in tested.findings if "Bob" in f]
        self.assertEqual(1, len(missing))
        self.assertIn("running", missing[0])
        self.assertIn("only", missing[0])

    def test_a_member_only_the_candidate_lists_is_a_finding(self):
        running = side([row("Alice")])
        candidate = side([row("Alice"), row("Zoë")])

        tested = compare(running, candidate)

        missing = [f for f in tested.findings if "Zoë" in f]
        self.assertEqual(1, len(missing))
        self.assertIn("candidate", missing[0])
        self.assertIn("only", missing[0])

    def test_differing_total_counts_are_a_finding_naming_both_counts(self):
        running = side([row("Alice")])
        candidate = copy.deepcopy(running)
        candidate["overview"]["totalCount"] = 7

        tested = compare(running, candidate)

        counted = [f for f in tested.findings if "totalCount" in f]
        self.assertEqual(1, len(counted))
        self.assertIn("1", counted[0])
        self.assertIn("7", counted[0])


if __name__ == "__main__":
    unittest.main()
