import email.message
import io
import json
import unittest
import urllib.error
import urllib.request
from urllib.parse import parse_qs, urlparse

from side_access import SideAccess, admin_key_in
from side_fetch import LoginRefused, Pacer, http_get_json_logged_in, log_in
from temp_directories import temp_directory

KEY = "k3y-Of_43-characters-AAAAAAAAAAAAAAAAAAAAAA"
ANNOUNCED = (
    "\x1b[36m08:00:01.123\x1b[0m \x1b[90m[main]\x1b[0m \x1b[34mINFO \x1b[0m \x1b[35md.s.e.p.Logger\x1b[0m - "
    f"Console admin key, valid until the next restart: {KEY}\n"
)


class FakeClock:
    def __init__(self):
        self.slept = []

    def clock(self):
        return 0.0

    def sleep(self, seconds):
        self.slept.append(seconds)


class Response(io.BytesIO):
    def __init__(self, payload, cookies=(), status=200):
        super().__init__(json.dumps(payload).encode("utf-8"))
        self.status = status
        self.headers = email.message.Message()
        for cookie in cookies:
            self.headers["Set-Cookie"] = cookie

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        return False


def refusal(url, code):
    return urllib.error.HTTPError(url, code, "refused", {}, io.BytesIO(b""))


def pacer(fake):
    return Pacer(25, 10.0, fake.clock, fake.sleep)


def url_of(request):
    return request.full_url if isinstance(request, urllib.request.Request) else request


class LoggingIn(unittest.TestCase):
    def test_the_login_posts_the_name_admin_and_the_key_as_json_to_the_login_path(self):
        seen = []

        def opener(request, timeout=None):
            seen.append(request)
            return Response({"step": "logged-in", "next": "/"}, ["epc_session=signed; Max-Age=604800; Path=/; HttpOnly"])

        fake = FakeClock()

        tested = log_in("http://127.0.0.1:1", KEY, pacer(fake), opener, fake.sleep)

        self.assertEqual("epc_session=signed", tested)
        self.assertEqual("POST", seen[0].get_method())
        self.assertEqual("http://127.0.0.1:1/api/v1/login", seen[0].full_url)
        self.assertEqual("application/json", seen[0].get_header("Content-type"))
        self.assertEqual({"name": "admin", "password": KEY}, json.loads(seen[0].data))

    def test_the_login_answers_the_session_cookie_without_its_attributes(self):
        def opener(request, timeout=None):
            return Response({"step": "logged-in", "next": "/"}, ["epc_session=signed; Max-Age=604800; Path=/; HttpOnly"])

        fake = FakeClock()

        tested = log_in("http://127.0.0.1:1", KEY, pacer(fake), opener, fake.sleep)

        self.assertEqual("epc_session=signed", tested)

    def test_a_refused_login_raises_a_refused_login_carrying_its_status(self):
        def opener(request, timeout=None):
            raise refusal(request.full_url, 401)

        fake = FakeClock()

        with self.assertRaises(LoginRefused) as raised:
            log_in("http://127.0.0.1:1", KEY, pacer(fake), opener, fake.sleep)

        self.assertEqual(401, raised.exception.code)

    def test_a_login_answered_without_a_session_cookie_is_a_refused_login_naming_its_status_and_the_missing_cookie(self):
        def opener(request, timeout=None):
            return Response({"step": "choose-new-password"}, status=200)

        fake = FakeClock()

        with self.assertRaises(LoginRefused) as raised:
            log_in("http://127.0.0.1:1", KEY, pacer(fake), opener, fake.sleep)

        tested = str(raised.exception)

        self.assertEqual("HTTP 200 without a session cookie", tested)

    def test_a_login_turned_away_for_too_many_requests_is_waited_out_and_repeated(self):
        outcomes = [refusal("u", 429), Response({"step": "logged-in"}, ["epc_session=signed; Path=/"])]

        def opener(request, timeout=None):
            outcome = outcomes.pop(0)
            if isinstance(outcome, Exception):
                raise outcome
            return outcome

        fake = FakeClock()

        tested = log_in("http://127.0.0.1:1", KEY, pacer(fake), opener, fake.sleep)

        self.assertEqual("epc_session=signed", tested)
        self.assertEqual([65], fake.slept)

    def test_the_refused_login_names_its_status(self):
        tested = str(LoginRefused(401))

        self.assertIn("HTTP 401", tested)


class LoggedInRequests(unittest.TestCase):
    def test_a_request_with_the_login_carries_its_cookie_and_no_token(self):
        seen = []

        def opener(request, timeout=None):
            seen.append(request)
            return Response({"ok": True})

        fake = FakeClock()
        tested = http_get_json_logged_in("http://127.0.0.1:1", "epc_session=signed", pacer(fake), opener, fake.sleep)

        result = tested("/api/v1/avatars", {"page": 0, "size": 1000})

        self.assertEqual({"ok": True}, result)
        self.assertEqual("epc_session=signed", seen[0].get_header("Cookie"))
        parsed = urlparse(seen[0].full_url)
        self.assertEqual("/api/v1/avatars", parsed.path)
        self.assertEqual({"page": ["0"], "size": ["1000"]}, parse_qs(parsed.query))


class TheKeyInTheLog(unittest.TestCase):
    def log(self, text):
        path = temp_directory(self) / "app.log"
        path.write_text(text, encoding="utf-8")
        return path

    def test_the_key_is_read_off_the_announcement_in_a_colored_log_line(self):
        tested = admin_key_in(self.log("starting\n" + ANNOUNCED + "Startup completed\n"))

        self.assertEqual(KEY, tested)

    def test_a_log_ending_in_a_half_written_announcement_holds_no_key(self):
        half_written = ANNOUNCED[: ANNOUNCED.index(KEY) + len(KEY) // 2]

        tested = admin_key_in(self.log("starting\n" + half_written))

        self.assertIsNone(tested)

    def test_a_log_without_the_announcement_holds_no_key(self):
        tested = admin_key_in(self.log("Startup completed in 2000ms\n"))

        self.assertIsNone(tested)

    def test_a_log_not_written_yet_holds_no_key(self):
        tested = admin_key_in(temp_directory(self) / "app.log")

        self.assertIsNone(tested)

    def test_no_log_named_holds_no_key(self):
        tested = admin_key_in(None)

        self.assertIsNone(tested)


class Side:
    def __init__(self, cookie_code=200, token_code=200):
        self.requests = []
        self.cookie_code = cookie_code
        self.token_code = token_code

    def open(self, request, timeout=None):
        self.requests.append(request)
        url = url_of(request)
        if urlparse(url).path == "/api/v1/login":
            return Response({"step": "logged-in"}, ["epc_session=signed; Path=/"])
        if isinstance(request, urllib.request.Request) and request.get_header("Cookie"):
            if self.cookie_code != 200:
                raise refusal(url, self.cookie_code)
            return Response({"by": "login"})
        if self.token_code != 200:
            raise refusal(url, self.token_code)
        return Response({"by": "token"})

    def logins(self):
        return [request for request in self.requests if urlparse(url_of(request)).path == "/api/v1/login"]


class SideAnnouncingTheKeyAsTheTokenIsAsked(Side):
    def __init__(self):
        super().__init__(token_code=401)
        self.log = None

    def open(self, request, timeout=None):
        if not isinstance(request, urllib.request.Request):
            self.log.write_text(ANNOUNCED, encoding="utf-8")
        return super().open(request, timeout)


class ChoosingTheAccess(unittest.TestCase):
    def access(self, side, log_text):
        log = temp_directory(self) / "app.log"
        log.write_text(log_text, encoding="utf-8")
        fake = FakeClock()
        return SideAccess("http://127.0.0.1:1", "the-token", log, pacer(fake), side.open, fake.sleep), log

    def test_a_side_whose_log_announces_no_key_is_asked_with_the_token(self):
        side = Side()
        tested, _ = self.access(side, "Startup completed\n")

        result = tested.get_json("/api/v1/admin/status", {})

        self.assertEqual({"by": "token"}, result)
        self.assertEqual({"token": ["the-token"]}, parse_qs(urlparse(url_of(side.requests[0])).query))

    def test_a_side_whose_log_announces_the_key_is_logged_in_once_and_asked_with_the_login(self):
        side = Side()
        tested, _ = self.access(side, ANNOUNCED)

        results = [tested.get_json("/api/v1/admin/status", {}), tested.get_json("/api/v1/avatars", {"page": 0})]

        self.assertEqual([{"by": "login"}, {"by": "login"}], results)
        self.assertEqual(1, len(side.logins()))
        for request in side.requests:
            self.assertNotIn("token", parse_qs(urlparse(url_of(request)).query))

    def test_a_token_refused_once_the_key_is_announced_logs_in_and_asks_again_with_the_login(self):
        side = SideAnnouncingTheKeyAsTheTokenIsAsked()
        tested, side.log = self.access(side, "starting\n")

        result = tested.get_json("/api/v1/admin/status", {})

        self.assertEqual({"by": "login"}, result)
        self.assertEqual(1, len(side.logins()))

    def test_a_token_refused_while_the_log_announces_no_key_raises_that_refusal(self):
        side = Side(token_code=401)
        tested, _ = self.access(side, "Startup completed\n")

        with self.assertRaises(urllib.error.HTTPError) as raised:
            tested.get_json("/api/v1/admin/status", {})

        self.assertEqual(401, raised.exception.code)
        self.assertEqual([], side.logins())

    def test_a_request_refused_under_the_login_raises_that_refusal(self):
        side = Side(cookie_code=403)
        tested, _ = self.access(side, ANNOUNCED)

        with self.assertRaises(urllib.error.HTTPError) as raised:
            tested.get_json("/api/v1/admin/status", {})

        self.assertEqual(403, raised.exception.code)


if __name__ == "__main__":
    unittest.main()
