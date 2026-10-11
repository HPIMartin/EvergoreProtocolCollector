import shutil
import subprocess
import unittest
from pathlib import Path

from temp_directories import temp_directory

SELF_TEST = Path(__file__).resolve().parent / "self-test"


class SelfTest(unittest.TestCase):
    def test_a_self_test_that_finds_no_test_fails(self):
        empty = temp_directory(self)
        shutil.copy2(SELF_TEST, empty / "self-test")

        tested = subprocess.run(["sh", str(empty / "self-test")], capture_output=True, text=True, timeout=60)

        self.assertNotEqual(0, tested.returncode, tested.stdout + tested.stderr)


if __name__ == "__main__":
    unittest.main()
