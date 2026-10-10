import hashlib
import sqlite3
import unittest
from pathlib import Path

from exact_sums import read_exact_sums, read_recompute_instants
from temp_directories import temp_directory


def meta_store(test, rows):
    directory = temp_directory(test)
    path = directory / "temp.sqlite"
    connection = sqlite3.connect(path)
    connection.execute("CREATE TABLE metaInformation (key VARCHAR, value VARCHAR)")
    connection.executemany("INSERT INTO metaInformation VALUES (?, ?)", rows)
    connection.commit()
    connection.close()
    return path


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


class ExactSums(unittest.TestCase):
    def test_every_stored_sum_of_a_member_is_read_with_its_type(self):
        path = meta_store(
            self,
            [
                ("bank_placement_Älf Beispiel", "10485363"),
                ("bank_withdrawl_Älf Beispiel", "120"),
                ("storage_placement_Älf Beispiel", "2733625.2"),
                ("storage_withdrawl_Älf Beispiel", "9889929.600000001"),
                ("storage_donation_Älf Beispiel", "6898416.0"),
                ("storage_craft_subsidy_Älf Beispiel", "6720.0"),
                ("sums_recomputed_at_Älf Beispiel", "1760000000000"),
                ("last_updated", "10.10.2026 08:00"),
            ]
        )

        tested = read_exact_sums(path)

        self.assertEqual(
            {
                "Älf Beispiel": {
                    "bank_placement": 10485363,
                    "bank_withdrawl": 120,
                    "storage_placement": 2733625.2,
                    "storage_withdrawl": 9889929.600000001,
                    "storage_donation": 6898416.0,
                    "storage_craft_subsidy": 6720.0,
                }
            },
            tested,
        )
        self.assertIsInstance(tested["Älf Beispiel"]["bank_placement"], int)

    def test_a_sum_that_is_not_stored_is_absent(self):
        path = meta_store(self, [("bank_placement_Bob", "5")])

        tested = read_exact_sums(path)

        self.assertEqual({"Bob": {"bank_placement": 5}}, tested)

    def test_a_member_with_only_a_recompute_instant_has_no_sums(self):
        path = meta_store(self, [("sums_recomputed_at_Bob", "1")])

        tested = read_exact_sums(path)

        self.assertEqual({}, tested)

    def test_the_recompute_instant_of_every_member_is_read_in_epoch_millis(self):
        path = meta_store(
            self,
            [
                ("sums_recomputed_at_Älf Beispiel", "1760000000000"),
                ("sums_recomputed_at_Bob", "5"),
                ("bank_placement_Bob", "5"),
            ],
        )

        tested = read_recompute_instants(path)

        self.assertEqual({"Älf Beispiel": 1760000000000, "Bob": 5}, tested)

    def test_the_store_is_left_as_it_was(self):
        path = meta_store(self, [("bank_placement_Bob", "5")])
        before = digest(path)

        read_exact_sums(path)

        self.assertEqual(before, digest(path))


if __name__ == "__main__":
    unittest.main()
