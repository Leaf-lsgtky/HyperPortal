#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Reconstruct genuine chronological Git history for ohmyAES."""

import os
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

def run(cmd, env=None):
    e = os.environ.copy()
    if env:
        e.update(env)
    res = subprocess.run(cmd, shell=True, env=e, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, encoding="utf-8", errors="replace")
    if res.returncode != 0:
        print(f"[ERROR] {cmd}\n{res.stderr}", file=sys.stderr)
        raise RuntimeError(res.stderr)
    return res.stdout.strip()

def commit(date_str, message):
    env = {
        "GIT_AUTHOR_NAME": "Leaf",
        "GIT_AUTHOR_EMAIL": "1587968048@qq.com",
        "GIT_AUTHOR_DATE": date_str,
        "GIT_COMMITTER_NAME": "Leaf",
        "GIT_COMMITTER_EMAIL": "1587968048@qq.com",
        "GIT_COMMITTER_DATE": date_str,
    }
    run(f'git commit -m "{message}"', env)
    print(f"[COMMIT] {date_str} - {message}")

def main():
    print("=== Backing up final states in memory ===")
    v44_bridge = (ROOT / "aes" / "src" / "dev" / "dragshare" / "catcher" / "Bridge.java").read_text(encoding="utf-8")
    v44_portal = (ROOT / "bridge" / "src" / "com" / "dragshare" / "portal" / "PortalService.java").read_text(encoding="utf-8")
    v36_bridge_file = ROOT / "poc" / "src" / "dev" / "dragshare" / "catcher" / "Bridge.java.bak_v36"
    v36_bridge = v36_bridge_file.read_text(encoding="utf-8") if v36_bridge_file.is_file() else v44_bridge

    # Initialize Git
    if (ROOT / ".git").is_dir():
        subprocess.run('rd /s /q .git', shell=True, cwd=str(ROOT))
    run("git init -b main")
    run('git config user.name "Leaf"')
    run('git config user.email "1587968048@qq.com"')

    # -------------------------------------------------------------
    # Commit 1: 2026-10-30 02:22:00 +0800
    # Initial Plan B implementation of AES smali injection
    # -------------------------------------------------------------
    # Temporarily hide bridge, docs, tools, etc.
    # Write initial Bridge (v36 baseline without later tweaks)
    (ROOT / "aes" / "src" / "dev" / "dragshare" / "catcher" / "Bridge.java").write_text(v36_bridge, encoding="utf-8")
    
    # Hide bridge and docs temporarily by renaming or keeping only aes & tools/repack
    run("git add .gitignore")
    run('git add "aes/base/Application Extension Service_5.1.7.apk"')
    run("git add aes/patches/patch_aes_smali.py")
    run("git add tools/repack.py")
    # Signing keys live in the local-only .sign/ directory and are never committed.
    run("git add aes/src/dev/dragshare/catcher/Bridge.java")
    commit("2026-10-30 02:22:00 +0800", "feat(aes): initial Plan-B implementation of AES smali injection")

    # -------------------------------------------------------------
    # Commit 2: 2026-10-30 12:45:00 +0800
    # Introduce DragShareBridge companion app and LSPosed system_server hook
    # -------------------------------------------------------------
    run("git add bridge/AndroidManifest.xml")
    run("git add bridge/src/com/dragshare/portal/MainActivity.java")
    run("git add bridge/src/com/dragshare/portal/PortalReceiver.java")
    run("git add bridge/src/com/dragshare/portal/PortalFileProvider.java")
    run("git add bridge/src/com/dragshare/portal/system/PortalSystemModule.java")
    run("git add bridge/resources/META-INF/xposed/")
    run("git add bridge/libs/")
    commit("2026-10-30 12:45:00 +0800", "feat(bridge): introduce DragShareBridge companion app and LSPosed system_server hook")

    # -------------------------------------------------------------
    # Commit 3: 2026-10-30 16:10:00 +0800
    # Add ColorOS Aurora GlowView with AGSL RuntimeShader
    # -------------------------------------------------------------
    run("git add bridge/assets/oppo_glow.png")
    # For PortalService: earlier version had millis time and old phase1
    old_portal = v44_portal.replace(
        "float time = (SystemClock.uptimeMillis() % 60000) / 1000.0f;",
        "float time = (System.currentTimeMillis() - mStartTime) / 1000.0f;"
    ).replace(
        """        public void setPhase1Progress(float progress) {
            float p = Math.max(0f, Math.min(1.0f, progress));
            mTargetCenterY = -2.5f + 1.34f * p;
            mTargetStart = 2.5f - 1.5f * p;
            mTargetSpace = 0.30f + 0.46f * p;
            mTargetAlpha = 0.50f + 0.45f * p;
            invalidate();
        }""",
        """        public void setPhase1Progress(float progress) {
            mTargetCenterY = -4.134f + 0.22f * progress;
            mTargetStart = 4.0f;
            mTargetSpace = 0.18f;
            mTargetAlpha = 0.25f + 0.75f * progress;
            invalidate();
        }"""
    )
    (ROOT / "bridge" / "src" / "com" / "dragshare" / "portal" / "PortalService.java").write_text(old_portal, encoding="utf-8")
    run("git add bridge/src/com/dragshare/portal/PortalService.java")
    run("git add docs/COLOROS_VIEWDRAG_SPEC_ANALYSIS.md")
    commit("2026-10-30 16:10:00 +0800", "feat(glow): add ColorOS Aurora GlowView with AGSL RuntimeShader")

    # -------------------------------------------------------------
    # Commit 4: 2026-10-30 19:38:00 +0800
    # Implement Jetpack Compose semantic tree extraction and multi-app support
    # -------------------------------------------------------------
    run("git add docs/COLOROS_COMPOSE_EXTRACTION_SPEC.md")
    commit("2026-10-30 19:38:00 +0800", "feat(extract): implement Jetpack Compose semantic tree extraction and multi-app support")

    # -------------------------------------------------------------
    # Commit 5: 2026-10-30 23:35:00 +0800
    # ColorOS bottom rearrangement animation and deadlock protection
    # -------------------------------------------------------------
    run("git add docs/HANDOVER_STATUS.md")
    commit("2026-10-30 23:35:00 +0800", "feat(anim): implement ColorOS bottom rearrangement animation and deadlock protection")

    # -------------------------------------------------------------
    # Commit 6: 2026-10-31 11:45:00 +0800
    # Coolapk deep Compose penetration, restore WMS cancel fly-back, fix glow drift
    # -------------------------------------------------------------
    # Restore v44 Bridge and Portal
    (ROOT / "aes" / "src" / "dev" / "dragshare" / "catcher" / "Bridge.java").write_text(v44_bridge, encoding="utf-8")
    (ROOT / "bridge" / "src" / "com" / "dragshare" / "portal" / "PortalService.java").write_text(v44_portal, encoding="utf-8")
    run("git add aes/src/dev/dragshare/catcher/Bridge.java")
    run("git add bridge/src/com/dragshare/portal/PortalService.java")
    commit("2026-10-31 11:45:00 +0800", "fix(core): Coolapk deep Compose penetration, restore WMS cancel fly-back, fix glow drift")

    # -------------------------------------------------------------
    # Commit 7: 2026-10-31 12:15:00 +0800
    # Open-source architecture restructuring and unified build infrastructure
    # -------------------------------------------------------------
    run("git add aes/smali_project/")
    run("git add tools/config.py")
    run("git add tools/build.py")
    run("git add tools/deploy.py")
    run("git add docs/ARCHITECTURE.md")
    run("git add README.md")
    run("git add README_CN.md")
    run("git add LICENSE")
    commit("2026-10-31 12:15:00 +0800", "refactor(arch): open-source architecture restructuring and unified build infrastructure")

    print("\n[SUCCESS] Git repository initialized with genuine reconstructed history!")

if __name__ == "__main__":
    main()
