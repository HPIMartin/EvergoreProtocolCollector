import tempfile
from pathlib import Path


def temp_directory(test):
    directory = tempfile.TemporaryDirectory()
    test.addCleanup(directory.cleanup)
    return Path(directory.name)
