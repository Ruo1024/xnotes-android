"""Compare metadata produced by aapt and apksigner after APK verification."""
import re
import sys
from pathlib import Path


def metadata(directory):
    package_text = (directory / "package.txt").read_text()
    package = re.search(r"^package: name='([^']+)' versionCode='(\d+)'", package_text)
    certificates = frozenset(re.findall(
        r"^Signer #\d+ certificate SHA-256 digest: ([0-9a-f]{64})$",
        (directory / "signing.txt").read_text(), re.MULTILINE))
    if not package or not certificates:
        raise ValueError(f"Missing package/version/signing metadata: {directory}")
    return package[1], int(package[2]), certificates


def check_upgrade(old, new):
    if old[0] != new[0]:
        raise ValueError("Application ID changed")
    if old[2] != new[2]:
        raise ValueError("Signing certificates changed")
    if new[1] <= old[1]:
        raise ValueError(f"versionCode must increase: {old[1]} -> {new[1]}")


if __name__ == "__main__":
    old, new = map(Path, sys.argv[1:])
    check_upgrade(metadata(old), metadata(new))
    print(f"Upgrade verified: {old} -> {new}")
