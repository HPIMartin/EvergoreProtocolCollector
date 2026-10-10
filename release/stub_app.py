import json
import os
import signal
import sqlite3
import subprocess
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, unquote, urlparse

import candidate_rules
import running_rules
from exact_sums import read_exact_sums
from sample_sides import row, totals, without

STARTED = time.time()
SIDE = os.path.basename(os.getcwd())
TOKEN = os.environ["EVERGORE_SECURITY_API_TOKEN"]
STORED = read_exact_sums("database/temp.sqlite")


def recompute_shows():
    after = os.environ.get("STUB_RECOMPUTE_AFTER", "0")
    return after != "never" and time.time() - STARTED >= float(after)


def figures_of_side():
    if SIDE == "running":
        rows = [without(row(member, **running_rules.member_figures(stored)), "balance") for member, stored in STORED.items()]
        guild = without(totals(**running_rules.totals_of_rows(rows)), "balance", "storageValue")
    else:
        rows = [row(member, **candidate_rules.member_figures(stored)) for member, stored in STORED.items()]
        guild = totals(**candidate_rules.guild_figures(list(STORED.values())))
    tweak = os.environ.get("STUB_TWEAK")
    if SIDE == "candidate" and tweak and rows:
        rows[0][tweak] += 1
    return rows, guild


def admin_status():
    failed = bool(os.environ.get("STUB_RECOMPUTE_FAILS"))
    shown = recompute_shows() and not failed
    status = {
        "lastUpdated": None,
        "lastSuccessfulScrape": None,
        "lastScrapeFailure": "2026-10-10T08:00:30Z",
        "lastSuccessfulRecompute": "2026-10-10T08:01:00Z" if shown else None,
        "lastRecomputeFailure": "2026-10-10T08:01:00Z" if failed else None,
        "unknownItemNames": [],
        "failedAvatarNames": [],
    }
    if SIDE == "candidate":
        status.update(roundTrips=[], roundTripAbstentions=[])
    return status


def page_of(items, query, extra=None):
    page = int(query.get("page", ["0"])[0])
    size = int(query.get("size", ["1000"])[0])
    envelope = {"page": page, "size": size, "totalCount": len(items), "items": items[page * size : (page + 1) * size]}
    envelope.update(extra or {})
    return envelope


def ledger_of(member, kind):
    if kind == "storage" and member in STORED:
        return [
            {
                "timestamp": "2026-01-01T12:00:00Z",
                "avatar": member,
                "quantity": 1,
                "name": "Eisen",
                "quality": 0,
                "transferType": "DEPOSIT",
            }
        ]
    return []


class Handler(BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        return

    def do_GET(self):
        if os.environ.get("STUB_HANG"):
            time.sleep(300)
        parsed = urlparse(self.path)
        query = parse_qs(parsed.query)
        with open("stub-requests.log", "a", encoding="utf-8") as requests:
            requests.write(f"{time.time()} {parsed.path}\n")
        if parsed.path == "/api/v1/admin/status":
            return self.reply(200, admin_status())
        if query.get("token") != [TOKEN]:
            return self.reply(401, {"error": "token"})
        parts = [unquote(part) for part in parsed.path.split("/")[3:]]
        if parts == ["avatars"]:
            rows, guild = figures_of_side()
            return self.reply(200, page_of(rows, query, {"totals": guild}))
        if len(parts) == 3 and parts[0] == "avatars" and parts[2] in ("bank", "storage"):
            return self.reply(200, page_of(ledger_of(parts[1], parts[2]), query))
        return self.reply(404, {"error": "unknown"})

    def reply(self, status, payload):
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


def record_start(port):
    environment = {
        name: os.environ.get(name)
        for name in (
            "TZ",
            "EVERGORE_SECURITY_API_TOKEN",
            "EVERGORE_CREDENTIALS_USERNAME",
            "EVERGORE_CREDENTIALS_PASSWORD",
        )
    }
    child = None
    if os.environ.get("STUB_SPAWN_CHILD"):
        child = subprocess.Popen(["sleep", "300"]).pid
    record = {
        "pid": os.getpid(),
        "child": child,
        "cwd": os.getcwd(),
        "port": port,
        "env": environment,
        "argv": sys.argv,
        "started": STARTED,
    }
    with open("stub-record.json", "w", encoding="utf-8") as target:
        json.dump(record, target)


def record_recompute_instants():
    if os.environ.get("STUB_NO_RECOMPUTE_INSTANT"):
        return
    connection = sqlite3.connect("database/temp.sqlite")
    connection.executemany("DELETE FROM metaInformation WHERE key = ?", [(f"sums_recomputed_at_{m}",) for m in STORED])
    now = str(int(time.time() * 1000))
    connection.executemany(
        "INSERT INTO metaInformation VALUES (?, ?)", [(f"sums_recomputed_at_{m}", now) for m in STORED]
    )
    connection.commit()
    connection.close()


def main():
    port = int(os.environ["MICRONAUT_SERVER_PORT"])
    if os.environ.get("STUB_IGNORE_TERM"):
        signal.signal(signal.SIGTERM, signal.SIG_IGN)
    server = ThreadingHTTPServer(("127.0.0.1", port), Handler)
    record_recompute_instants()
    exit_after = os.environ.get("STUB_EXIT_AFTER")
    if exit_after:
        threading.Timer(float(exit_after), lambda: os._exit(1)).start()
    record_start(port)
    server.serve_forever()


if __name__ == "__main__":
    main()
