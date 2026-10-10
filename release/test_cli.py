import io
import json
import sqlite3
import unittest
from unittest import mock

from compare_sides import main
from sample_sides import candidate_side_of, running_side_of
from temp_directories import temp_directory

STORED = {
    "bank_placement": 500,
    "bank_withdrawl": 100,
    "storage_placement": 10.5,
    "storage_withdrawl": 2.5,
    "storage_donation": 0.5,
    "storage_craft_subsidy": 0.25,
}


def write_side(directory, name, side, stored_by_member):
    wire = {key: side[key] for key in ("overview", "ledgers", "admin")}
    wire_path = directory / f"{name}.json"
    wire_path.write_text(json.dumps(wire), encoding="utf-8")
    database = directory / f"{name}.sqlite"
    connection = sqlite3.connect(database)
    connection.execute("CREATE TABLE metaInformation (key VARCHAR, value VARCHAR)")
    for member, stored in stored_by_member.items():
        connection.executemany(
            "INSERT INTO metaInformation VALUES (?, ?)", [(f"{key}_{member}", repr(value)) for key, value in stored.items()]
        )
    connection.commit()
    connection.close()
    return wire_path, database


def run_compare(test, running_stored, candidate_stored):
    directory = temp_directory(test)
    running_wire, running_db = write_side(directory, "running", running_side_of({"Älf B": running_stored}), {"Älf B": running_stored})
    candidate_wire, candidate_db = write_side(
        directory, "candidate", candidate_side_of({"Älf B": candidate_stored}), {"Älf B": candidate_stored}
    )
    out = io.StringIO()
    code = main(
        [
            "compare",
            "--running-wire", str(running_wire),
            "--running-db", str(running_db),
            "--candidate-wire", str(candidate_wire),
            "--candidate-db", str(candidate_db),
        ],
        {},
        out,
    )
    return code, out.getvalue()


class CompareCommand(unittest.TestCase):
    def test_equal_sums_in_both_stores_pass_with_exit_zero(self):
        code, report = run_compare(self, STORED, STORED)

        self.assertEqual(0, code)
        self.assertIn("PASS", report)

    def test_a_differing_bank_sum_fails_with_exit_one_and_a_finding_naming_the_member(self):
        code, report = run_compare(self, STORED, {**STORED, "bank_placement": 501})

        self.assertEqual(1, code)
        self.assertIn("Älf B: exact bank_placement differs, running 500, candidate 501", report)


class ComparatorCrash(unittest.TestCase):
    def arguments(self, directory):
        return [
            "compare",
            "--running-wire", str(directory / "running.json"),
            "--running-db", str(directory / "running.sqlite"),
            "--candidate-wire", str(directory / "candidate.json"),
            "--candidate-db", str(directory / "candidate.sqlite"),
        ]

    def test_malformed_json_is_exit_four_with_the_error_on_stderr_and_no_verdict_line(self):
        directory = temp_directory(self)
        write_side(directory, "running", running_side_of({"Älf B": STORED}), {"Älf B": STORED})
        write_side(directory, "candidate", candidate_side_of({"Älf B": STORED}), {"Älf B": STORED})
        (directory / "candidate.json").write_text("{not json", encoding="utf-8")
        out, err = io.StringIO(), io.StringIO()

        tested = main(self.arguments(directory), {}, out, err)

        self.assertEqual(4, tested)
        self.assertIn("JSONDecodeError", err.getvalue())
        self.assertNotIn("PASS", out.getvalue())
        self.assertNotIn("FAIL", out.getvalue())

    def test_a_database_without_the_meta_information_table_is_exit_four_with_the_error_on_stderr(self):
        directory = temp_directory(self)
        write_side(directory, "running", running_side_of({"Älf B": STORED}), {"Älf B": STORED})
        write_side(directory, "candidate", candidate_side_of({"Älf B": STORED}), {"Älf B": STORED})
        connection = sqlite3.connect(directory / "candidate.sqlite")
        connection.execute("DROP TABLE metaInformation")
        connection.commit()
        connection.close()
        out, err = io.StringIO(), io.StringIO()

        tested = main(self.arguments(directory), {}, out, err)

        self.assertEqual(4, tested)
        self.assertIn("metaInformation", err.getvalue())

    def test_findings_and_a_pass_keep_their_own_codes_and_print_their_verdict(self):
        for candidate_stored, code, verdict in ((STORED, 0, "PASS"), ({**STORED, "bank_placement": 501}, 1, "FAIL")):
            with self.subTest(code=code):
                tested_code, report = run_compare(self, STORED, candidate_stored)

                self.assertEqual(code, tested_code)
                self.assertIn(verdict, report)


class OtherCommands(unittest.TestCase):
    def test_a_fetch_without_the_token_in_the_environment_is_refused(self):
        out = io.StringIO()

        tested = main(["fetch", "--base-url", "http://127.0.0.1:1", "--out", "/nonexistent/x.json"], {}, out)

        self.assertEqual(2, tested)
        self.assertIn("EVERGORE_SECURITY_API_TOKEN", out.getvalue())

    def test_stopping_a_group_below_two_is_refused(self):
        out = io.StringIO()

        tested = main(["stop", "--group", "1", "--grace", "1", "--poll", "0.1"], {}, out)

        self.assertEqual(2, tested)

    def test_a_group_that_stays_alive_after_the_kill_is_exit_three_and_named(self):
        out = io.StringIO()

        with mock.patch("compare_sides.stop_group", return_value=False):
            tested = main(["stop", "--group", "4242", "--grace", "1", "--poll", "0.1"], {}, out)

        self.assertEqual(3, tested)
        self.assertIn("4242", out.getvalue())

    def test_an_unknown_command_is_refused(self):
        out = io.StringIO()

        tested = main(["frobnicate"], {}, out)

        self.assertEqual(2, tested)


if __name__ == "__main__":
    unittest.main()
