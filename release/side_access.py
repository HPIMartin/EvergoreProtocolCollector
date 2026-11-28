import re
import time
import urllib.error

from side_fetch import DIRECT, http_get_json, http_get_json_logged_in, log_in

ANNOUNCEMENT = re.compile(r"Console admin key, valid until the next restart: ([A-Za-z0-9_-]+)")
UNAUTHORIZED = 401


def admin_key_in(log_path):
    if log_path is None:
        return None
    try:
        with open(log_path, encoding="utf-8", errors="replace") as log:
            keys = ANNOUNCEMENT.findall(log.read())
    except OSError:
        return None
    return keys[-1] if keys else None


class SideAccess:
    def __init__(self, base_url, token, log_path, pacer, opener=DIRECT, sleep=time.sleep):
        self._base_url = base_url
        self._log_path = log_path
        self._pacer = pacer
        self._opener = opener
        self._sleep = sleep
        self._by_token = http_get_json(base_url, token, pacer, opener, sleep)
        self._by_login = None

    def get_json(self, path, query):
        if self._by_login is None:
            self._log_in_once_announced()
        if self._by_login is not None:
            return self._by_login(path, query)
        try:
            return self._by_token(path, query)
        except urllib.error.HTTPError as refusal:
            if refusal.code == UNAUTHORIZED and self._log_in_once_announced():
                return self._by_login(path, query)
            raise

    def _log_in_once_announced(self):
        key = admin_key_in(self._log_path)
        if key is None:
            return False
        cookie = log_in(self._base_url, key, self._pacer, self._opener, self._sleep)
        self._by_login = http_get_json_logged_in(self._base_url, cookie, self._pacer, self._opener, self._sleep)
        return True
