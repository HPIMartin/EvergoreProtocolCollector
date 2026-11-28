import http.client
import json
import time
import urllib.error
import urllib.request
from collections import deque
from urllib.parse import quote, urlencode

PAGE_SIZE = 1000
REQUESTS_PER_WINDOW = 25
WINDOW_SECONDS = 10.0
BLOCK_SECONDS = 65
REFUSALS_RETRIED = 3
SOCKET_TIMEOUT_SECONDS = 5


class Pacer:
    def __init__(self, limit=REQUESTS_PER_WINDOW, window=WINDOW_SECONDS, clock=time.monotonic, sleep=time.sleep):
        self._limit = limit
        self._window = window
        self._clock = clock
        self._sleep = sleep
        self._started = deque()

    def wait(self):
        if len(self._started) >= self._limit:
            until = self._started.popleft() + self._window
            pause = until - self._clock()
            if pause > 0:
                self._sleep(pause)
        self._started.append(self._clock())


DIRECT = urllib.request.build_opener(urllib.request.ProxyHandler({})).open


LOGIN_PATH = "/api/v1/login"
ADMIN_NAME = "admin"
SESSION_COOKIE = "epc_session"


class LoginRefused(Exception):
    def __init__(self, code, detail=""):
        super().__init__(f"HTTP {code}{detail}")
        self.code = code


def log_in(base_url, key, pacer, opener=DIRECT, sleep=time.sleep):
    body = json.dumps({"name": ADMIN_NAME, "password": key}).encode("utf-8")
    request = urllib.request.Request(
        f"{base_url}{LOGIN_PATH}", data=body, headers={"Content-Type": "application/json"}, method="POST"
    )
    try:
        with _paced(lambda: opener(request, timeout=SOCKET_TIMEOUT_SECONDS), pacer, sleep) as response:
            cookies = response.headers.get_all("Set-Cookie") or []
            status = response.status
    except urllib.error.HTTPError as refusal:
        raise LoginRefused(refusal.code) from None
    for cookie in cookies:
        pair = cookie.split(";", 1)[0].strip()
        if pair.startswith(f"{SESSION_COOKIE}="):
            return pair
    raise LoginRefused(status, " without a session cookie")


def http_get_json_logged_in(base_url, cookie, pacer, opener=DIRECT, sleep=time.sleep):
    def get_json(path, query):
        address = f"{base_url}{path}?{urlencode(query)}" if query else f"{base_url}{path}"
        request = urllib.request.Request(address, headers={"Cookie": cookie})
        return _json_of(lambda: opener(request, timeout=SOCKET_TIMEOUT_SECONDS), pacer, sleep)

    return get_json


def http_get_json(base_url, token, pacer, opener=DIRECT, sleep=time.sleep):
    def get_json(path, query):
        url = f"{base_url}{path}?{urlencode({**query, 'token': token})}"
        return _json_of(lambda: opener(url, timeout=SOCKET_TIMEOUT_SECONDS), pacer, sleep)

    return get_json


def _json_of(open_once, pacer, sleep):
    with _paced(open_once, pacer, sleep) as response:
        return json.loads(response.read().decode("utf-8"))


def _paced(open_once, pacer, sleep):
    refusals = 0
    while True:
        pacer.wait()
        try:
            return open_once()
        except urllib.error.HTTPError as error:
            if error.code != 429 or refusals == REFUSALS_RETRIED:
                raise
            refusals += 1
            sleep(BLOCK_SECONDS)


def fetch_side(get_json, size=PAGE_SIZE):
    overview = fetch_all_pages(get_json, "/api/v1/avatars", size)
    ledgers = {}
    for row in overview["items"]:
        path = f"/api/v1/avatars/{quote(row['avatar'], safe='')}"
        ledgers[row["avatar"]] = {
            "bank": fetch_all_pages(get_json, f"{path}/bank", size),
            "storage": fetch_all_pages(get_json, f"{path}/storage", size),
        }
    return {"overview": overview, "ledgers": ledgers, "admin": get_json("/api/v1/admin/status", {})}


def fetch_all_pages(get_json, path, size):
    first = get_json(path, {"page": 0, "size": size})
    collected = dict(first)
    collected.pop("page", None)
    collected.pop("size", None)
    items = list(first["items"])
    page = 0
    while first["items"] and len(items) < first["totalCount"]:
        page += 1
        following = get_json(path, {"page": page, "size": size})
        if not following["items"]:
            break
        items.extend(following["items"])
    collected["items"] = items
    return collected


def wait_for_recompute(get_status, timeout, poll, clock=time.monotonic, sleep=time.sleep, alive=lambda: True):
    deadline = clock() + timeout
    while True:
        if not alive():
            return False
        try:
            status = get_status()
        except urllib.error.HTTPError:
            raise
        except (OSError, ValueError, http.client.HTTPException):
            status = {}
        if not isinstance(status, dict):
            status = {}
        if status.get("lastSuccessfulRecompute"):
            return True
        if status.get("lastRecomputeFailure"):
            return False
        if clock() >= deadline:
            return False
        sleep(poll)
