import unittest

from compare_sides import compare
from sample_sides import candidate_side_of, running_side_of

STORED = {
    "bank_placement": 500,
    "bank_withdrawl": 100,
    "storage_placement": 10.5,
    "storage_withdrawl": 2.5,
    "storage_donation": 0.5,
    "storage_craft_subsidy": 0.25,
}


class NonFiniteSums(unittest.TestCase):
    def test_a_stored_sum_that_is_not_finite_is_a_finding_naming_the_member_and_the_key(self):
        for label, value in (("nan", float("nan")), ("inf", float("inf")), ("-inf", float("-inf"))):
            for side_name in ("running", "candidate"):
                with self.subTest(value=label, side=side_name):
                    running = running_side_of({"Alice": STORED})
                    candidate = candidate_side_of({"Alice": STORED})
                    {"running": running, "candidate": candidate}[side_name]["exact"]["Alice"] = {
                        **STORED,
                        "storage_withdrawl": value,
                    }

                    tested = compare(running, candidate)

                    self.assertEqual(
                        [f"Alice: exact storage_withdrawl is {label} on the {side_name} side"], tested.findings
                    )


if __name__ == "__main__":
    unittest.main()
