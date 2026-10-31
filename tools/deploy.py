#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Deployment & Live Debugging Tool for ohmyAES.

Usage:
  python tools/deploy.py --install all
  python tools/deploy.py --install bridge
  python tools/deploy.py --install aes
  python tools/deploy.py --refresh-lsp
  python tools/deploy.py --soft-reboot
  python tools/deploy.py --start
  python tools/deploy.py --logs
  python tools/deploy.py --all
"""

import argparse
import subprocess
import sys
import time
from pathlib import Path

# Add tools to sys.path
sys.path.insert(0, str(Path(__file__).resolve().parent))
import config as cfg
import build


def adb(cmd, shell=True, check=True):
    full_cmd = f"adb {cmd}"
    res = subprocess.run(full_cmd, shell=shell, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    if res.returncode != 0 and check:
        print(f"[ERROR] adb command failed: {full_cmd}\n{res.stdout}", file=sys.stderr)
        raise RuntimeError(f"ADB failed: {full_cmd}")
    return res


def install_apk(apk_path):
    p = Path(apk_path)
    if not p.is_file():
        raise RuntimeError(f"APK not found: {p}")
    print(f"[INSTALL] Installing {p.name}...")
    res = adb(f'install -r -d "{p}"')
    print(res.stdout.strip())


def refresh_lsposed():
    print("[LSP] Refreshing LSPosed module scope and path for com.dragshare.portal...")
    adb("shell \"su -c 'lspctl module disable --user=0 com.dragshare.portal && lspctl module enable --user=0 com.dragshare.portal'\"")
    res = adb("shell \"su -c 'lspctl module show com.dragshare.portal'\"")
    print(res.stdout.strip())


def soft_reboot():
    print("[REBOOT] Requesting soft reboot (system_server restart)...")
    adb("shell \"su -c killall system_server\"", check=False)
    print("[REBOOT] system_server restart signal sent! Waiting 6 seconds for framework restart...")
    time.sleep(6)


def start_portal_service():
    print("[PORTAL] Starting DragShareBridge service...")
    adb("shell am start -n com.dragshare.portal/.MainActivity")


def monitor_logs():
    print("[LOGS] Starting live logcat monitor (Ctrl+C to exit)...")
    tags = [
        "DragShareBridge:V",
        "DragBridge:V",
        "ContentCatcher:V",
        "PortalService:V",
        "LSPosedFramework:I",
        "*:S"
    ]
    log_cmd = f"adb logcat -v time {' '.join(tags)}"
    try:
        subprocess.run(log_cmd, shell=True)
    except KeyboardInterrupt:
        print("\n[LOGS] Stopped monitor.")


def main():
    parser = argparse.ArgumentParser(description="Deployment & Debugging Tool for ohmyAES")
    parser.add_argument("--install", choices=["bridge", "aes", "all"], help="Install built APK(s)")
    parser.add_argument("--refresh-lsp", action="store_true", help="Sync LSPosed module path via lspctl")
    parser.add_argument("--soft-reboot", action="store_true", help="Perform quick soft reboot via root killall system_server")
    parser.add_argument("--start", action="store_true", help="Start DragShareBridge MainActivity/Service")
    parser.add_argument("--logs", action="store_true", help="Attach live logcat listener")
    parser.add_argument("--all", action="store_true", help="Build, install all, refresh LSPosed, and start service")

    args = parser.parse_args()

    if args.all:
        print("=== Running full workflow: build -> install -> sync lsp -> start ===")
        build.build_bridge()
        build.build_aes()
        install_apk(cfg.DIST_DIR / "DragShareBridge.apk")
        install_apk(cfg.DIST_DIR / "ContentCatcher-patched.apk")
        refresh_lsposed()
        start_portal_service()
        print("\n[SUCCESS] All components built, installed and synced! Run `python tools/deploy.py --soft-reboot` if system_server needs reload.")
        return

    if args.install:
        if args.install in ("bridge", "all"):
            install_apk(cfg.DIST_DIR / "DragShareBridge.apk")
        if args.install in ("aes", "all"):
            install_apk(cfg.DIST_DIR / "ContentCatcher-patched.apk")

    if args.refresh_lsp:
        refresh_lsposed()

    if args.soft_reboot:
        soft_reboot()

    if args.start:
        start_portal_service()

    if args.logs:
        monitor_logs()


if __name__ == "__main__":
    main()
