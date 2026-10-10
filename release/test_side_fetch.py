import http.client
import io
import json
import unittest
import urllib.error
from urllib.parse import parse_qs, urlparse

from side_fetch import Pacer, fetch_side, http_get_json, wait_for_recompute


class FakeClock:
    def __init__(self):
        self.now = 0.0
        self.slept = []

    def clock(self):
        return self.now

    def sleep(self, seconds):
        self.slept.append(seconds)
        self.now += seconds


class Response(io.BytesIO):
    def __enter__(self):
        return self

    def __exit__(self, *exc):
        return False


def json_response(payload):
    return Response(json.dumps(payload).encode("utf-8"))


def too_many(url):
    return urllib.error.HTTPError(url, 429, "Too Many Requests", {}, io.BytesIO(b""))


class Pacing(unittest.TestCase):
    def test_requests_within_the_limit_do_not_wait(self):
        fake = FakeClock()
        tested = Pacer(25, 10.0, fake.clock, fake.sleep)

        for _ in range(25):
            tested.wait()

        self.assertEqual([], fake.slept)

    def test_the_request_beyond_the_limit_waits_until_the_window_has_passed(self):
        fake = FakeClock()
        tested = Pacer(25, 10.0, fake.clock, fake.sleep)
        for _ in range(25):
            tested.wait()

        tested.wait()

        self.assertEqual([10.0], fake.slept)


class Requests(unittest.TestCase):
    def test_every_request_carries_a_socket_timeout_of_a_few_seconds(self):
        seen = []

        def opener(url, timeout=None):
            seen.append(timeout)
            return json_response({})

        fake = FakeClock()
        tested = http_get_json("http://127.0.0.1:1", "t", Pacer(25, 10.0, fake.clock, fake.sleep), opener, fake.sleep)

        tested("/p", {})

        self.assertEqual(1, len(seen))
        self.assertGreater(seen[0], 0)
        self.assertLessEqual(seen[0], 10)

    def test_the_token_travels_as_a_query_value_with_the_page_window(self):
        seen = []

        def opener(url, timeout=None):
            seen.append(url)
            return json_response({"ok": True})

        fake = FakeClock()
        tested = http_get_json("http://127.0.0.1:1", "secret", Pacer(25, 10.0, fake.clock, fake.sleep), opener, fake.sleep)

        result = tested("/api/v1/avatars", {"page": 0, "size": 1000})

        self.assertEqual({"ok": True}, result)
        parsed = urlparse(seen[0])
        self.assertEqual("/api/v1/avatars", parsed.path)
        self.assertEqual({"page": ["0"], "size": ["1000"], "token": ["secret"]}, parse_qs(parsed.query))

    def test_a_refused_request_is_waited_out_and_repeated(self):
        outcomes = [too_many("u"), too_many("u"), json_response({"ok": 1})]

        def opener(url, timeout=None):
            outcome = outcomes.pop(0)
            if isinstance(outcome, Exception):
                raise outcome
            return outcome

        fake = FakeClock()
        tested = http_get_json("http://127.0.0.1:1", "t", Pacer(25, 10.0, fake.clock, fake.sleep), opener, fake.sleep)

        result = tested("/p", {})

        self.assertEqual({"ok": 1}, result)
        self.assertEqual([65, 65], fake.slept)

    def test_a_fourth_refusal_is_raised(self):
        def opener(url, timeout=None):
            raise too_many(url)

        fake = FakeClock()
        tested = http_get_json("http://127.0.0.1:1", "t", Pacer(25, 10.0, fake.clock, fake.sleep), opener, fake.sleep)

        with self.assertRaises(urllib.error.HTTPError):
            tested("/p", {})

        self.assertEqual([65, 65, 65], fake.slept)

    def test_another_http_error_is_raised_at_once(self):
        def opener(url, timeout=None):
            raise urllib.error.HTTPError(url, 500, "boom", {}, io.BytesIO(b""))

        fake = FakeClock()
        tested = http_get_json("http://127.0.0.1:1", "t", Pacer(25, 10.0, fake.clock, fake.sleep), opener, fake.sleep)

        with self.assertRaises(urllib.error.HTTPError):
            tested("/p", {})

        self.assertEqual([], fake.slept)


class WholeSide(unittest.TestCase):
    def test_the_overview_the_ledgers_per_member_and_the_admin_status_are_collected(self):
        requested = []

        def get_json(path, query):
            requested.append(path)
            if path == "/api/v1/avatars":
                return {"totalCount": 1, "items": [{"avatar": "Älf Beispiel"}], "totals": {"net": 1}}
            if path == "/api/v1/admin/status":
                return {"unknownItemNames": []}
            return {"totalCount": 0, "items": []}

        tested = fetch_side(get_json, 1000)

        self.assertEqual({"net": 1}, tested["overview"]["totals"])
        self.assertEqual({"unknownItemNames": []}, tested["admin"])
        self.assertEqual({"bank", "storage"}, set(tested["ledgers"]["Älf Beispiel"]))
        self.assertIn("/api/v1/avatars/%C3%84lf%20Beispiel/bank", requested)
        self.assertIn("/api/v1/avatars/%C3%84lf%20Beispiel/storage", requested)


class WaitingForTheRecompute(unittest.TestCase):
    def test_the_wait_ends_when_a_recompute_succeeded(self):
        statuses = [{"lastSuccessfulRecompute": None}, {"lastSuccessfulRecompute": "2026-10-10T08:00:00Z"}]
        fake = FakeClock()

        tested = wait_for_recompute(lambda: statuses.pop(0), 100, 5, fake.clock, fake.sleep)

        self.assertTrue(tested)
        self.assertEqual([5], fake.slept)

    def test_a_recompute_that_failed_without_ever_succeeding_is_a_failed_wait_at_once(self):
        fake = FakeClock()

        tested = wait_for_recompute(
            lambda: {"lastSuccessfulRecompute": None, "lastRecomputeFailure": "2026-10-10T08:00:00Z"},
            100,
            5,
            fake.clock,
            fake.sleep,
        )

        self.assertFalse(tested)
        self.assertEqual([], fake.slept)

    def test_a_failure_next_to_a_successful_recompute_ends_the_wait_successfully(self):
        fake = FakeClock()

        tested = wait_for_recompute(
            lambda: {"lastSuccessfulRecompute": "2026-10-10T08:01:00Z", "lastRecomputeFailure": "2026-10-10T08:00:00Z"},
            100,
            5,
            fake.clock,
            fake.sleep,
        )

        self.assertTrue(tested)

    def test_an_app_that_does_not_answer_yet_is_asked_again(self):
        outcomes = [ConnectionRefusedError(), {"lastSuccessfulRecompute": "x"}]

        def status():
            outcome = outcomes.pop(0)
            if isinstance(outcome, Exception):
                raise outcome
            return outcome

        fake = FakeClock()

        tested = wait_for_recompute(status, 100, 5, fake.clock, fake.sleep)

        self.assertTrue(tested)

    def test_a_side_that_is_no_longer_alive_ends_the_wait_at_once_as_failed(self):
        fake = FakeClock()

        tested = wait_for_recompute(lambda: {}, 100, 5, fake.clock, fake.sleep, alive=lambda: False)

        self.assertFalse(tested)
        self.assertEqual([], fake.slept)

    def test_a_malformed_or_partial_body_is_asked_again(self):
        for failure in (json.JSONDecodeError("bad", "{", 1), http.client.IncompleteRead(b"{"), ValueError("partial")):
            with self.subTest(failure=type(failure).__name__):
                outcomes = [failure, {"lastSuccessfulRecompute": "x"}]

                def status():
                    outcome = outcomes.pop(0)
                    if isinstance(outcome, Exception):
                        raise outcome
                    return outcome

                fake = FakeClock()

                tested = wait_for_recompute(status, 100, 5, fake.clock, fake.sleep)

                self.assertTrue(tested)
                self.assertEqual([5], fake.slept)

    def test_a_body_that_is_not_an_object_is_asked_again(self):
        outcomes = [[], {"lastSuccessfulRecompute": "x"}]
        fake = FakeClock()

        tested = wait_for_recompute(lambda: outcomes.pop(0), 100, 5, fake.clock, fake.sleep)

        self.assertTrue(tested)
        self.assertEqual([5], fake.slept)

    def test_the_wait_gives_up_at_the_timeout(self):
        fake = FakeClock()

        tested = wait_for_recompute(lambda: {"lastSuccessfulRecompute": None}, 20, 5, fake.clock, fake.sleep)

        self.assertFalse(tested)
        self.assertEqual([5, 5, 5, 5], fake.slept)


if __name__ == "__main__":
    unittest.main()
