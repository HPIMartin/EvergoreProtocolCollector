import os
import signal
import time

KILL_WAIT = 5.0


def _stat_fields(pid):
    try:
        with open(f"/proc/{pid}/stat", encoding="utf-8") as stat:
            text = stat.read()
    except OSError:
        return None
    fields = text[text.rindex(")") + 2 :].split()
    return fields[0], int(fields[2])


def is_alive(pid):
    fields = _stat_fields(pid)
    return fields is not None and fields[0] != "Z"


def members_of(group):
    alive = []
    for entry in os.listdir("/proc"):
        if entry.isdigit():
            fields = _stat_fields(int(entry))
            if fields is not None and fields[1] == group and fields[0] != "Z":
                alive.append(int(entry))
    return alive


def stop_group(group, grace, poll, clock=time.monotonic, sleep=time.sleep, members=members_of):
    _signal_group(group, signal.SIGTERM)
    deadline = clock() + grace
    while members(group) and clock() < deadline:
        sleep(poll)
    _signal_group(group, signal.SIGKILL)
    deadline = clock() + KILL_WAIT
    while members(group) and clock() < deadline:
        sleep(poll)
    return not members(group)


def _signal_group(group, signum):
    try:
        os.killpg(group, signum)
    except ProcessLookupError:
        pass
