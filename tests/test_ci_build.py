"""Check that CI intermediates cannot silently replace signed build outputs."""
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
import build


class BridgeSigningModeTests(unittest.TestCase):
    def check_build(self, *, unsigned, r8=False):
        with tempfile.TemporaryDirectory(prefix="hyperportal-ci-") as directory:
            root = Path(directory)
            release = root / "bridge/build/outputs/apk/release"
            release.mkdir(parents=True)
            # Both may exist in a reused workspace; select the correct one.
            (release / "bridge-release.apk").write_bytes(b"signed")
            (release / "bridge-release-unsigned.apk").write_bytes(b"unsigned")
            dist = root / "dist"
            dist.mkdir()
            with (patch.object(build.cfg, "ROOT_DIR", root),
                  patch.object(build.cfg, "DIST_DIR", dist),
                  patch.object(build.cfg, "ensure_dirs"),
                  patch.object(build, "run_cmd") as run):
                result = build.build_bridge(unsigned=unsigned, r8=r8)
            self.assertEqual(result.read_bytes(), b"unsigned" if unsigned else b"signed")
            self.assertEqual("-Phyperportal.unsigned=true" in run.call_args.args[0], unsigned)
            name = "HyperPortal-r8" if r8 else "HyperPortal"
            self.assertEqual(result.name, name + ("-unsigned" if unsigned else "") + ".apk")

    def test_signed_build_keeps_release_output(self):
        self.check_build(unsigned=False)

    def test_unsigned_build_cannot_pick_up_stale_signed_apk(self):
        self.check_build(unsigned=True)

    def test_r8_unsigned_build_has_a_distinct_output(self):
        self.check_build(unsigned=True, r8=True)


if __name__ == "__main__":
    unittest.main()
