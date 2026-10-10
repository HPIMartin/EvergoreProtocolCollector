import os
import signal
import subprocess
import sys
import threading
import time
import unittest

from process_group import KILL_WAIT, is_alive, stop_group
from temp_directories import temp_directory

STUBBORN = (
    "import os, signal, subprocess, sys, time\n"
    "signal.signal(signal.SIGTERM, signal.SIG_IGN)\n"
    "child = subprocess.Popen(['sleep', '300'])\n"
    "open(sys.argv[1], 'w').write(str(child.pid))\n"
    "time.sleep(300)\n"
)


def start_stubborn_group(test):
    marker = temp_directory(test) / "child.pid"
    leader = subprocess.Popen([sys.executable, "-B", "-c", STUBBORN, str(marker)], start_new_session=True)
    deadline = time.monotonic() + 10
    while not marker.exists() and time.monotonic() < deadline:
        time.sleep(0.05)
    return leader, int(marker.read_text())


def kill_group_if_left(group):
    try:
        os.killpg(group, signal.SIGKILL)
    except ProcessLookupError:
        pass


def wait_until_exited(process, seconds):
    deadline = time.monotonic() + seconds
    while process.poll() is None and time.monotonic() < deadline:
        time.sleep(0.05)


class FakeClock:
    def __init__(self):
        self.slept = 0.0
        self.sleeps = 0

    def now(self):
        return self.slept

    def sleep(self, seconds):
        self.slept += seconds
        self.sleeps += 1


class StoppingAGroup(unittest.TestCase):
    def test_a_leader_that_ignores_term_is_killed_and_its_child_is_gone_after_the_grace_period(self):
        leader, child = start_stubborn_group(self)
        self.addCleanup(kill_group_if_left, leader.pid)
        stopper = threading.Thread(target=stop_group, args=(leader.pid, 0.5, 0.05), daemon=True)

        stopper.start()
        wait_until_exited(leader, 5)
        stopper.join(timeout=5)

        self.assertEqual(-signal.SIGKILL, leader.returncode)
        self.assertFalse(stopper.is_alive())
        self.assertFalse(is_alive(child))

    def test_a_group_that_ends_on_term_is_not_waited_on_for_the_whole_grace_period(self):
        leader = subprocess.Popen(["sleep", "300"], start_new_session=True)
        self.addCleanup(kill_group_if_left, leader.pid)
        waited = []

        def sleep(seconds):
            waited.append(seconds)
            time.sleep(seconds)

        stop_group(leader.pid, grace=30, poll=0.05, sleep=sleep)

        leader.wait(timeout=5)
        self.assertLess(sum(waited), 30)
        self.assertEqual(-signal.SIGTERM, leader.returncode)

    def test_a_group_that_is_already_gone_is_no_error(self):
        leader = subprocess.Popen(["true"], start_new_session=True)
        leader.wait(timeout=5)

        stop_group(leader.pid, grace=0.5, poll=0.05)

        self.assertFalse(is_alive(leader.pid))

    def test_the_wait_after_kill_returns_once_the_members_are_gone(self):
        gone = subprocess.Popen(["true"], start_new_session=True)
        gone.wait(timeout=5)
        clock = FakeClock()

        tested = stop_group(
            gone.pid, grace=0, poll=0.5, clock=clock.now, sleep=clock.sleep, members=lambda group: [1] if clock.sleeps < 3 else []
        )

        self.assertTrue(tested)
        self.assertEqual(3, clock.sleeps)

    def test_the_wait_after_kill_ends_at_a_deadline_and_reports_the_group_it_could_not_clear(self):
        gone = subprocess.Popen(["true"], start_new_session=True)
        gone.wait(timeout=5)
        clock = FakeClock()

        tested = stop_group(gone.pid, grace=0, poll=0.5, clock=clock.now, sleep=clock.sleep, members=lambda group: [1])

        self.assertFalse(tested)
        self.assertLessEqual(clock.slept, KILL_WAIT + 0.5)

    def test_the_process_of_this_test_is_alive(self):
        tested = is_alive(os.getpid())

        self.assertTrue(tested)


if __name__ == "__main__":
    unittest.main()
