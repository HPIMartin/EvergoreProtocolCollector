import hashlib
import json
import os
import shutil
import signal
import socket
import sqlite3
import subprocess
import sys
import time
import unittest
from pathlib import Path

from process_group import is_alive, members_of, stop_group
from temp_directories import temp_directory

RELEASE_DIR = Path(__file__).resolve().parent

MEMBERS = {
    "Älf Beispiel": {
        "bank_placement": 500,
        "bank_withdrawl": 100,
        "storage_placement": 10.5,
        "storage_withdrawl": 2.5,
        "storage_donation": 0.5,
        "storage_craft_subsidy": 0.25,
    },
    "Bob": {
        "bank_placement": 70,
        "bank_withdrawl": 0,
        "storage_placement": 1.4,
        "storage_withdrawl": 0.6,
        "storage_donation": 0.0,
        "storage_craft_subsidy": 0.0,
    },
}


def free_port():
    with socket.socket() as probe:
        probe.bind(("127.0.0.1", 0))
        return probe.getsockname()[1]


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def stored_rows(path):
    connection = sqlite3.connect(path)
    try:
        return sorted(
            connection.execute("SELECT key, value FROM metaInformation WHERE key NOT LIKE 'sums_recomputed_at_%'")
        )
    finally:
        connection.close()


def helper_commands(group):
    commands = []
    for pid in members_of(group):
        try:
            arguments = Path(f"/proc/{pid}/cmdline").read_text(encoding="utf-8").split("\0")
        except OSError:
            continue
        if len(arguments) > 3 and arguments[2].endswith("compare_sides.py"):
            commands.append(arguments[3])
    return commands


def files_under(root, skipping):
    return sorted(
        str(path.relative_to(root))
        for path in Path(root).rglob("*")
        if skipping not in path.relative_to(root).parts
    )


class Layout:
    def __init__(self, test):
        self._test = test
        self.root = temp_directory(test)
        shutil.copytree(RELEASE_DIR, self.root / "release", ignore=shutil.ignore_patterns("__pycache__"))
        (self.root / ".git").mkdir()
        self.work = self.root / "build" / "release-comparison"
        self.snapshot = self._synthetic_snapshot()
        test.addCleanup(self._stop_recorded_sides)
        self.running_dist = self._stub_distribution("running-dist")
        self.candidate_dist = self._stub_distribution("candidate-dist")

    def _synthetic_snapshot(self):
        path = temp_directory(self._test) / "snapshot.sqlite"
        connection = sqlite3.connect(path)
        connection.execute("CREATE TABLE metaInformation (key VARCHAR, value VARCHAR)")
        for member, stored in MEMBERS.items():
            connection.executemany(
                "INSERT INTO metaInformation VALUES (?, ?)", [(f"{key}_{member}", repr(value)) for key, value in stored.items()]
            )
        connection.commit()
        connection.close()
        return path

    def _stub_distribution(self, name):
        binary = self.root / name / "bin" / "protocolParser"
        binary.parent.mkdir(parents=True)
        binary.write_text(f'#!/bin/sh\nexec python3 -B "{self.root}/release/stub_app.py" "$@"\n', encoding="utf-8")
        binary.chmod(0o755)
        return binary.parent.parent

    def start(self, snapshot=None, running_dist=None, candidate_dist=None, driver=None, cwd=None, **environment):
        variables = dict(os.environ)
        variables.update(
            RELEASE_COMPARE_RUNNING_PORT=str(free_port()),
            RELEASE_COMPARE_CANDIDATE_PORT=str(free_port()),
            RELEASE_COMPARE_TIMEOUT="20",
            RELEASE_COMPARE_POLL="0.1",
            RELEASE_COMPARE_GRACE="2",
        )
        variables.update(environment)
        return subprocess.Popen(
            [
                str(driver or self.root / "release" / "compare"),
                str(running_dist or self.running_dist),
                str(candidate_dist or self.candidate_dist),
                str(snapshot or self.snapshot),
            ],
            env=variables,
            cwd=cwd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            start_new_session=True,
        )

    def run(self, timeout=120, **arguments):
        driver = self.start(**arguments)
        try:
            stdout, stderr = driver.communicate(timeout=timeout)
        except subprocess.TimeoutExpired:
            self._end(driver)
            raise
        return subprocess.CompletedProcess(driver.args, driver.returncode, stdout, stderr)

    def _end(self, driver):
        os.killpg(driver.pid, signal.SIGTERM)
        try:
            driver.communicate(timeout=15)
        except subprocess.TimeoutExpired:
            os.killpg(driver.pid, signal.SIGKILL)
            driver.communicate()
        self._stop_recorded_sides()

    def _stop_recorded_sides(self):
        for side in ("running", "candidate"):
            record_path = self.work / side / "stub-record.json"
            if record_path.exists():
                pid = json.loads(record_path.read_text(encoding="utf-8"))["pid"]
                if is_alive(pid):
                    stop_group(pid, grace=1, poll=0.1)

    def record(self, side):
        return json.loads((self.work / side / "stub-record.json").read_text(encoding="utf-8"))

    def requests(self, side):
        lines = (self.work / side / "stub-requests.log").read_text(encoding="utf-8").splitlines()
        return [(float(line.split()[0]), line.split()[1]) for line in lines]

    def assert_no_process_left(self, test):
        for side in ("running", "candidate"):
            record_path = self.work / side / "stub-record.json"
            if record_path.exists():
                record = json.loads(record_path.read_text(encoding="utf-8"))
                test.assertFalse(is_alive(record["pid"]), f"{side} stub is still alive")
                if record["child"]:
                    test.assertFalse(is_alive(record["child"]), f"{side} stub child is still alive")


class FullRun(unittest.TestCase):
    def setUp(self):
        self.layout = Layout(self)

    def test_two_sides_that_agree_pass_with_exit_zero_and_the_report_on_stdout(self):
        tested = self.layout.run()

        self.assertEqual(0, tested.returncode, tested.stdout + tested.stderr)
        self.assertIn("PASS", tested.stdout)
        self.layout.assert_no_process_left(self)

    def test_the_snapshot_is_copied_once_per_side_and_the_original_stays_as_it_was(self):
        before = digest(self.layout.snapshot)

        self.layout.run()

        self.assertEqual(before, digest(self.layout.snapshot))
        for side in ("running", "candidate"):
            copy = self.layout.work / side / "database" / "temp.sqlite"
            self.assertEqual(stored_rows(self.layout.snapshot), stored_rows(copy))
            self.assertEqual(str(self.layout.work / side), self.layout.record(side)["cwd"])

    def test_nothing_is_written_outside_the_work_directory(self):
        before = files_under(self.layout.root, "build")
        snapshot_directory = files_under(self.layout.snapshot.parent, "")

        self.layout.run()

        self.assertEqual(before, files_under(self.layout.root, "build"))
        self.assertEqual(snapshot_directory, files_under(self.layout.snapshot.parent, ""))

    def test_the_two_sides_run_on_the_two_ports_from_the_environment(self):
        running_port, candidate_port = free_port(), free_port()

        self.layout.run(
            RELEASE_COMPARE_RUNNING_PORT=str(running_port), RELEASE_COMPARE_CANDIDATE_PORT=str(candidate_port)
        )

        self.assertEqual(running_port, self.layout.record("running")["port"])
        self.assertEqual(candidate_port, self.layout.record("candidate")["port"])
        self.assertNotEqual(running_port, candidate_port)

    def test_the_throwaway_credentials_are_present_and_differ_from_run_to_run(self):
        self.layout.run()
        first = self.layout.record("running")["env"]
        self.layout.run()
        second = self.layout.record("running")["env"]

        for name in ("EVERGORE_SECURITY_API_TOKEN", "EVERGORE_CREDENTIALS_USERNAME", "EVERGORE_CREDENTIALS_PASSWORD"):
            self.assertTrue(first[name], name)
            self.assertNotEqual(first[name], second[name], name)
        self.assertEqual("UTC", first["TZ"])

    def test_both_wire_sides_are_fetched_into_the_work_directory(self):
        self.layout.run()

        for side in ("running", "candidate"):
            wire = json.loads((self.layout.work / f"{side}.json").read_text(encoding="utf-8"))
            self.assertEqual(["Älf Beispiel", "Bob"], [row["avatar"] for row in wire["overview"]["items"]])

    def test_the_work_directory_is_replaced_at_the_start(self):
        stale = self.layout.work / "stale.txt"
        stale.parent.mkdir(parents=True)
        stale.write_text("old", encoding="utf-8")

        self.layout.run()

        self.assertFalse(stale.exists())


class Waiting(unittest.TestCase):
    def setUp(self):
        self.layout = Layout(self)

    def test_nothing_is_fetched_before_the_recompute_shows(self):
        self.layout.run(STUB_RECOMPUTE_AFTER="1.5")

        for side in ("running", "candidate"):
            started = self.layout.record(side)["started"]
            fetched = [at for at, path in self.layout.requests(side) if path == "/api/v1/avatars"]
            self.assertTrue(fetched)
            self.assertGreaterEqual(min(fetched), started + 1.5)

    def test_a_recompute_that_never_shows_fails_at_the_timeout_and_leaves_no_process(self):
        tested = self.layout.run(STUB_RECOMPUTE_AFTER="never", RELEASE_COMPARE_TIMEOUT="2")

        self.assertEqual(3, tested.returncode, tested.stdout + tested.stderr)
        self.assertNotIn("PASS", tested.stdout)
        self.layout.assert_no_process_left(self)

    def test_a_recompute_that_failed_ends_the_run_at_once_with_exit_three(self):
        tested = self.layout.run(STUB_RECOMPUTE_FAILS="1", RELEASE_COMPARE_TIMEOUT="60")

        self.assertEqual(3, tested.returncode, tested.stdout + tested.stderr)
        self.assertNotIn("PASS", tested.stdout)
        self.layout.assert_no_process_left(self)

    def test_a_driver_that_is_terminated_while_waiting_stops_both_sides(self):
        driver = self.layout.start(RELEASE_COMPARE_TIMEOUT="60", STUB_RECOMPUTE_AFTER="never")
        deadline = time.monotonic() + 20
        while not (self.layout.work / "candidate" / "stub-record.json").exists() and time.monotonic() < deadline:
            time.sleep(0.1)

        driver.send_signal(signal.SIGTERM)
        driver.communicate(timeout=30)

        self.assertEqual(143, driver.returncode)
        self.layout.assert_no_process_left(self)


class TimingOut(unittest.TestCase):
    def setUp(self):
        self.layout = Layout(self)

    def test_a_run_that_times_out_in_the_test_leaves_no_side_behind(self):
        with self.assertRaises(subprocess.TimeoutExpired):
            self.layout.run(timeout=3, STUB_RECOMPUTE_AFTER="never", RELEASE_COMPARE_TIMEOUT="60")

        self.layout.assert_no_process_left(self)


class Stopping(unittest.TestCase):
    def setUp(self):
        self.layout = Layout(self)

    def wait_for_both_sides(self, since):
        deadline = time.monotonic() + 20
        while time.monotonic() < deadline:
            records = [self.layout.work / side / "stub-record.json" for side in ("running", "candidate")]
            if all(path.exists() for path in records):
                try:
                    if all(json.loads(path.read_text(encoding="utf-8"))["started"] >= since for path in records):
                        return
                except ValueError:
                    pass
            time.sleep(0.1)

    def test_no_helper_outlives_a_terminated_driver(self):
        since = time.time()
        driver = self.layout.start(RELEASE_COMPARE_TIMEOUT="60", RELEASE_COMPARE_POLL="3", STUB_RECOMPUTE_AFTER="never")
        self.wait_for_both_sides(since)
        deadline = time.monotonic() + 10
        while "wait" not in helper_commands(driver.pid) and time.monotonic() < deadline:
            time.sleep(0.05)
        self.assertIn("wait", helper_commands(driver.pid))

        driver.send_signal(signal.SIGTERM)
        driver.wait(timeout=30)

        self.assertEqual([], members_of(driver.pid))

    def test_hangup_and_quit_stop_both_sides_like_terminate_does(self):
        for signum, status in ((signal.SIGHUP, 129), (signal.SIGQUIT, 131)):
            with self.subTest(signal=signum.name):
                since = time.time()
                driver = self.layout.start(RELEASE_COMPARE_TIMEOUT="60", STUB_RECOMPUTE_AFTER="never")
                self.wait_for_both_sides(since)

                driver.send_signal(signum)
                driver.communicate(timeout=30)

                self.assertEqual(status, driver.returncode)
                self.layout.assert_no_process_left(self)

    def test_a_second_interrupt_while_the_sides_are_being_stopped_does_not_abort_the_stop(self):
        since = time.time()
        driver = self.layout.start(
            RELEASE_COMPARE_TIMEOUT="60", RELEASE_COMPARE_GRACE="5", STUB_RECOMPUTE_AFTER="never", STUB_IGNORE_TERM="1"
        )
        self.wait_for_both_sides(since)

        os.killpg(driver.pid, signal.SIGINT)
        deadline = time.monotonic() + 10
        while "stop" not in helper_commands(driver.pid) and time.monotonic() < deadline:
            time.sleep(0.02)
        self.assertIn("stop", helper_commands(driver.pid))
        os.killpg(driver.pid, signal.SIGINT)
        driver.communicate(timeout=30)

        self.assertEqual(130, driver.returncode)
        self.layout.assert_no_process_left(self)

    def test_a_side_that_ignores_term_and_spawns_a_child_is_gone_after_the_run(self):
        tested = self.layout.run(STUB_IGNORE_TERM="1", STUB_SPAWN_CHILD="1", RELEASE_COMPARE_GRACE="1")

        self.assertEqual(0, tested.returncode, tested.stdout + tested.stderr)
        self.assertTrue(self.layout.record("running")["child"])
        self.layout.assert_no_process_left(self)


class ExitCode(unittest.TestCase):
    def setUp(self):
        self.layout = Layout(self)

    def test_the_comparators_exit_code_and_report_are_passed_through(self):
        tested = self.layout.run(STUB_TWEAK="bankDeposited")

        self.assertEqual(1, tested.returncode, tested.stdout + tested.stderr)
        self.assertIn("Älf Beispiel: bankDeposited differs", tested.stdout)
        self.assertIn("FAIL", tested.stdout)
        self.layout.assert_no_process_left(self)


class Secrets(unittest.TestCase):
    def setUp(self):
        self.layout = Layout(self)

    def test_the_token_and_the_credentials_never_travel_in_the_arguments_of_a_command(self):
        tools = temp_directory(self)
        log = tools / "env-arguments.log"
        fake = tools / "env"
        fake.write_text(f'#!/bin/sh\nprintf "%s\\n" "$@" >> "{log}"\nexec /usr/bin/env "$@"\n', encoding="utf-8")
        fake.chmod(0o755)

        tested = self.layout.run(PATH=f"{tools}:{os.environ['PATH']}")

        self.assertEqual(0, tested.returncode, tested.stdout + tested.stderr)
        secrets = self.layout.record("running")["env"]
        logged = log.read_text(encoding="utf-8") if log.exists() else ""
        for name in ("EVERGORE_SECURITY_API_TOKEN", "EVERGORE_CREDENTIALS_USERNAME", "EVERGORE_CREDENTIALS_PASSWORD"):
            self.assertNotIn(secrets[name], logged, name)


class StopFailures(unittest.TestCase):
    def setUp(self):
        self.layout = Layout(self)

    def test_a_side_whose_group_cannot_be_stopped_is_named_on_stderr_and_ends_the_run_with_exit_three(self):
        tools = temp_directory(self)
        fake = tools / "python3"
        fake.write_text(f'#!/bin/sh\ncase "$3" in stop) exit 1 ;; esac\nexec {sys.executable} "$@"\n', encoding="utf-8")
        fake.chmod(0o755)

        tested = self.layout.run(PATH=f"{tools}:{os.environ['PATH']}")

        self.assertEqual(3, tested.returncode, tested.stdout + tested.stderr)
        for side in ("running", "candidate"):
            self.assertIn(str(self.layout.record(side)["pid"]), tested.stderr)


SECRET_VARIABLES = ("EVERGORE_SECURITY_API_TOKEN", "EVERGORE_CREDENTIALS_USERNAME", "EVERGORE_CREDENTIALS_PASSWORD")


def command_lines_of(groups):
    lines = []
    for group in groups:
        for pid in members_of(group):
            try:
                lines.append(Path(f"/proc/{pid}/cmdline").read_bytes().decode("utf-8", "replace"))
            except OSError:
                continue
    return lines


class SecretsInFlight(unittest.TestCase):
    def setUp(self):
        self.layout = Layout(self)

    def side_groups(self):
        groups = []
        for side in ("running", "candidate"):
            try:
                groups.append(self.layout.record(side)["pid"])
            except (OSError, ValueError):
                continue
        return groups

    def test_no_process_of_a_run_carries_the_token_or_the_credentials_in_its_command_line(self):
        driver = self.layout.start(STUB_RECOMPUTE_AFTER="1.5")
        observed = []
        while driver.poll() is None:
            observed.extend(command_lines_of([driver.pid, *self.side_groups()]))
            time.sleep(0.02)
        driver.communicate()

        self.assertEqual(0, driver.returncode)
        self.assertTrue(observed)
        for side in ("running", "candidate"):
            record = self.layout.record(side)
            for name in SECRET_VARIABLES:
                self.assertNotIn(record["env"][name], "\0".join(record["argv"]), f"{side} {name} in its own arguments")
                for line in observed:
                    self.assertNotIn(record["env"][name], line, f"{side} {name} in a command line")


class RecomputeInThisRun(unittest.TestCase):
    def setUp(self):
        self.layout = Layout(self)

    def test_sides_whose_stores_show_no_recompute_in_this_run_fail_with_a_finding_per_member_and_side(self):
        tested = self.layout.run(STUB_NO_RECOMPUTE_INSTANT="1")

        self.assertEqual(1, tested.returncode, tested.stdout + tested.stderr)
        for side in ("running", "candidate"):
            for member in MEMBERS:
                self.assertIn(f"{member}: the {side} side did not recompute the sums in this run", tested.stdout)


class UnresponsiveSides(unittest.TestCase):
    def setUp(self):
        self.layout = Layout(self)

    def test_a_side_that_accepts_but_never_answers_ends_the_run_within_the_timeout_with_exit_three(self):
        tested = self.layout.run(timeout=40, STUB_HANG="1", RELEASE_COMPARE_TIMEOUT="2")

        self.assertEqual(3, tested.returncode, tested.stdout + tested.stderr)
        self.assertNotIn("PASS", tested.stdout)
        self.layout.assert_no_process_left(self)


class DeadSides(unittest.TestCase):
    def setUp(self):
        self.layout = Layout(self)

    def test_a_side_that_exits_at_once_is_exit_three_with_the_side_named_not_running(self):
        tested = self.layout.run(STUB_EXIT_AFTER="0", STUB_RECOMPUTE_AFTER="never", RELEASE_COMPARE_TIMEOUT="60")

        self.assertEqual(3, tested.returncode, tested.stdout + tested.stderr)
        self.assertIn("not running", tested.stdout + tested.stderr)
        self.layout.assert_no_process_left(self)

    def test_a_side_that_exits_while_the_driver_waits_is_exit_three_with_the_side_named_not_running(self):
        tested = self.layout.run(STUB_EXIT_AFTER="1.5", STUB_RECOMPUTE_AFTER="never", RELEASE_COMPARE_TIMEOUT="60")

        self.assertEqual(3, tested.returncode, tested.stdout + tested.stderr)
        self.assertIn("not running", tested.stdout + tested.stderr)
        self.layout.assert_no_process_left(self)


class Refusals(unittest.TestCase):
    def setUp(self):
        self.layout = Layout(self)

    def test_a_driver_reached_through_a_symlink_to_the_script_is_refused_and_writes_nothing_beside_the_link(self):
        beside = temp_directory(self)
        link = beside / "bin" / "compare"
        link.parent.mkdir()
        link.symlink_to(self.layout.root / "release" / "compare")

        tested = self.layout.run(driver=link)

        self.assertEqual(2, tested.returncode, tested.stdout + tested.stderr)
        self.assertEqual(["bin"], sorted(path.name for path in beside.iterdir()))
        self.assertFalse(self.layout.work.exists())

    def test_a_checkout_root_that_is_no_git_worktree_is_refused(self):
        (self.layout.root / ".git").rmdir()

        tested = self.layout.run()

        self.assertEqual(2, tested.returncode, tested.stdout + tested.stderr)
        self.assertFalse(self.layout.work.exists())

    def test_a_driver_reached_through_a_symlinked_checkout_works_in_the_physical_root(self):
        link = temp_directory(self) / "checkout"
        link.symlink_to(self.layout.root)

        tested = self.layout.run(driver=link / "release" / "compare")

        self.assertEqual(0, tested.returncode, tested.stdout + tested.stderr)
        self.assertEqual(str(self.layout.work / "running"), self.layout.record("running")["cwd"])

    def test_relative_paths_to_the_distributions_and_the_snapshot_work_from_another_directory(self):
        elsewhere = temp_directory(self)

        def relative(path):
            return os.path.relpath(path, elsewhere)

        tested = self.layout.run(
            cwd=elsewhere,
            running_dist=relative(self.layout.running_dist),
            candidate_dist=relative(self.layout.candidate_dist),
            snapshot=relative(self.layout.snapshot),
        )

        self.assertEqual(0, tested.returncode, tested.stdout + tested.stderr)
        self.assertIn("PASS", tested.stdout)

    def test_a_snapshot_with_a_wal_or_journal_sidecar_is_refused_before_anything_starts(self):
        for suffix in ("-wal", "-journal"):
            with self.subTest(suffix=suffix):
                sidecar = Path(f"{self.layout.snapshot}{suffix}")
                sidecar.write_bytes(b"")
                self.addCleanup(sidecar.unlink, missing_ok=True)

                tested = self.layout.run()

                self.assertEqual(2, tested.returncode, tested.stdout + tested.stderr)
                self.assertIn(sidecar.name, tested.stderr)
                self.assertFalse(self.layout.work.exists())
                sidecar.unlink()

    def test_a_group_of_a_previous_run_that_is_still_alive_is_refused_and_the_work_directory_stays(self):
        leftover = subprocess.Popen(["sleep", "300"], start_new_session=True)
        self.addCleanup(leftover.kill)
        stale = self.layout.work / "stale.txt"
        stale.parent.mkdir(parents=True)
        stale.write_text("old", encoding="utf-8")
        (self.layout.work / "groups").write_text(f"{leftover.pid}\n", encoding="utf-8")

        tested = self.layout.run()

        self.assertEqual(2, tested.returncode, tested.stdout + tested.stderr)
        self.assertIn(str(leftover.pid), tested.stderr)
        self.assertTrue(stale.exists())
        self.assertIsNone(leftover.poll())

    def test_a_group_of_a_previous_run_that_is_gone_does_not_stop_the_run(self):
        gone = subprocess.Popen(["true"], start_new_session=True)
        gone.wait(timeout=5)
        self.layout.work.mkdir(parents=True)
        (self.layout.work / "groups").write_text(f"{gone.pid}\n", encoding="utf-8")

        tested = self.layout.run()

        self.assertEqual(0, tested.returncode, tested.stdout + tested.stderr)

    def test_the_groups_of_both_sides_are_recorded_while_they_run(self):
        driver = self.layout.start(STUB_RECOMPUTE_AFTER="never", RELEASE_COMPARE_TIMEOUT="60")
        deadline = time.monotonic() + 20
        records = [self.layout.work / side / "stub-record.json" for side in ("running", "candidate")]
        groups = self.layout.work / "groups"
        while time.monotonic() < deadline:
            recorded = groups.read_text(encoding="utf-8").split() if groups.exists() else []
            if len(recorded) == 2 and all(path.exists() for path in records):
                break
            time.sleep(0.05)
        sides = [str(self.layout.record(side)["pid"]) for side in ("running", "candidate")]
        driver.send_signal(signal.SIGTERM)
        driver.communicate(timeout=30)

        self.assertEqual(sides, recorded)
        self.assertFalse((self.layout.work / "groups").exists())

    def test_equal_ports_are_refused_before_anything_starts(self):
        port = str(free_port())

        tested = self.layout.run(RELEASE_COMPARE_RUNNING_PORT=port, RELEASE_COMPARE_CANDIDATE_PORT=port)

        self.assertEqual(2, tested.returncode)
        self.assertFalse(self.layout.work.exists())

    def test_a_setting_that_is_no_number_is_refused_before_anything_starts(self):
        settings = (
            ("RELEASE_COMPARE_GRACE", "10s"),
            ("RELEASE_COMPARE_GRACE", ""),
            ("RELEASE_COMPARE_TIMEOUT", "soon"),
            ("RELEASE_COMPARE_POLL", "1.2.3"),
            ("RELEASE_COMPARE_RUNNING_PORT", "80x"),
            ("RELEASE_COMPARE_CANDIDATE_PORT", "0"),
            ("RELEASE_COMPARE_CANDIDATE_PORT", "65536"),
            ("RELEASE_COMPARE_RUNNING_PORT", "1.5"),
        )
        for name, value in settings:
            with self.subTest(name=name, value=value):
                tested = self.layout.run(**{name: value})

                self.assertEqual(2, tested.returncode, tested.stdout + tested.stderr)
                self.assertIn(name, tested.stderr)
                self.assertFalse(self.layout.work.exists())

    def test_a_missing_snapshot_is_refused_before_anything_starts(self):
        tested = self.layout.run(snapshot=self.layout.root / "no-such.sqlite")

        self.assertEqual(2, tested.returncode)
        self.assertFalse(self.layout.work.exists())

    def test_a_distribution_without_its_start_script_is_refused(self):
        tested = self.layout.run(running_dist=self.layout.root)

        self.assertEqual(2, tested.returncode)
        self.assertFalse(self.layout.work.exists())


if __name__ == "__main__":
    unittest.main()
