"""Run with: python -m unittest discover -s tests -v."""
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from build import smali_fingerprint


class LongPressTests(unittest.TestCase):
    def test_scroll_cancels_hold_until_next_down(self):
        with tempfile.TemporaryDirectory(prefix="hyperportal-gesture-") as directory:
            subprocess.run([
                "javac", "-encoding", "utf-8", "--release", "11", "-d", directory,
                str(ROOT / "aes/src/dev/dragshare/catcher/LongPressGate.java"),
                str(ROOT / "tests/java/dev/dragshare/catcher/LongPressGateTest.java"),
            ], check=True, capture_output=True, text=True)
            result = subprocess.run([
                "java", "-cp", directory, "dev.dragshare.catcher.LongPressGateTest",
            ], check=True, capture_output=True, text=True)
            self.assertIn("9 regression checks passed", result.stdout)


class SmaliCacheTests(unittest.TestCase):
    def test_edits_additions_and_deletions_invalidate_dex(self):
        with tempfile.TemporaryDirectory(prefix="hyperportal-smali-") as directory:
            project = Path(directory)
            smali = project / "smali" / "Interceptor.smali"
            smali.parent.mkdir()
            smali.write_text("old", encoding="utf-8")
            before = smali_fingerprint(project)
            smali.write_text("new", encoding="utf-8")
            edited = smali_fingerprint(project)
            self.assertNotEqual(before, edited)
            added = project / "smali_classes2" / "Hook.smali"
            added.parent.mkdir()
            added.write_text("new hook", encoding="utf-8")
            self.assertNotEqual(edited, smali_fingerprint(project))
            added.unlink()
            self.assertEqual(edited, smali_fingerprint(project))
            smali.unlink()
            self.assertNotEqual(edited, smali_fingerprint(project))

    def test_build_outputs_do_not_invalidate_sources(self):
        with tempfile.TemporaryDirectory(prefix="hyperportal-smali-") as directory:
            project = Path(directory)
            before = smali_fingerprint(project)
            output = project / "build" / "classes.dex"
            output.parent.mkdir()
            output.write_bytes(b"generated")
            self.assertEqual(before, smali_fingerprint(project))
            (project / "apktool.yml").write_text("version: 2", encoding="utf-8")
            self.assertNotEqual(before, smali_fingerprint(project))


if __name__ == "__main__":
    unittest.main()
