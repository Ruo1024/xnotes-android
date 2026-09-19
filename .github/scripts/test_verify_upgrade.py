import tempfile
import unittest
from pathlib import Path

from verify_upgrade import check_upgrade, metadata


class UpgradeTests(unittest.TestCase):
    def test_increasing_version(self):
        check_upgrade(("com.xnotes.debug", 55, {"a"}), ("com.xnotes.debug", 56, {"a"}))

    def test_rejects_equal_and_lower_versions(self):
        for code in (54, 55):
            with self.subTest(code=code), self.assertRaises(ValueError):
                check_upgrade(("app", 55, {"a"}), ("app", code, {"a"}))

    def test_rejects_package_and_certificate_changes(self):
        for new in (("other", 56, {"a"}), ("app", 56, {"b"}), ("app", 56, {"a", "b"})):
            with self.subTest(new=new), self.assertRaises(ValueError):
                check_upgrade(("app", 55, {"a"}), new)

    def test_reads_real_tool_output_shape(self):
        with tempfile.TemporaryDirectory() as tmp:
            folder = Path(tmp)
            (folder / "package.txt").write_text(
                "package: name='com.xnotes.debug' versionCode='56' versionName='0.8.18-zh.1'\n")
            (folder / "signing.txt").write_text(
                "Verified using v2 scheme: true\nSigner #1 certificate SHA-256 digest: " + "a" * 64 + "\n")
            self.assertEqual(("com.xnotes.debug", 56, {"a" * 64}), metadata(folder))

    def test_missing_certificate_fails_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            folder = Path(tmp)
            (folder / "package.txt").write_text("package: name='app' versionCode='56'\n")
            (folder / "signing.txt").write_text("unrecognized output")
            with self.assertRaises(ValueError):
                metadata(folder)
