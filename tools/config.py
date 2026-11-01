#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Toolchain configuration and auto-discovery for ohmyAES."""

import os
import shutil
import sys
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parent.parent

# 1. Discover Android SDK
SDK_CANDIDATES = [
    os.environ.get("ANDROID_HOME"),
    os.environ.get("ANDROID_SDK_ROOT"),
    r"E:\Android\Sdk",
    Path.home() / "AppData" / "Local" / "Android" / "Sdk",
    r"C:\Android\Sdk",
]
ANDROID_SDK = None
for candidate in SDK_CANDIDATES:
    if candidate and Path(candidate).is_dir():
        ANDROID_SDK = Path(candidate)
        break

if not ANDROID_SDK:
    print("[WARN] Android SDK not found in standard paths. Please set ANDROID_HOME.", file=sys.stderr)

# 2. Find android.jar (prefer highest API version)
ANDROID_JAR = None
def _parse_api(name):
    clean = name.replace("android-", "")
    try:
        parts = [int(x) for x in clean.split(".")]
        return tuple(parts)
    except Exception:
        return (0,)

if ANDROID_SDK:
    platforms_dir = ANDROID_SDK / "platforms"
    if platforms_dir.is_dir():
        versions = sorted(platforms_dir.glob("android-*"), key=lambda p: _parse_api(p.name), reverse=True)
        for v in versions:
            jar = v / "android.jar"
            if jar.is_file():
                ANDROID_JAR = jar
                break

# 3. Find build tools (d8, zipalign, apksigner)
BUILD_TOOLS_DIR = None
D8_CMD = None
ZIPALIGN_EXE = None
APKSIGNER_CMD = None

if ANDROID_SDK:
    bt_root = ANDROID_SDK / "build-tools"
    if bt_root.is_dir():
        bt_versions = sorted(bt_root.iterdir(), key=lambda p: p.name, reverse=True)
        for bt in bt_versions:
            if bt.is_dir():
                d8_bat = bt / "d8.bat"
                d8_jar = bt / "lib" / "d8.jar"
                za = bt / "zipalign.exe" if os.name == "nt" else bt / "zipalign"
                signer_bat = bt / "apksigner.bat"
                signer_jar = bt / "lib" / "apksigner.jar"

                if (d8_bat.is_file() or d8_jar.is_file()) and za.is_file() and (signer_bat.is_file() or signer_jar.is_file()):
                    BUILD_TOOLS_DIR = bt
                    D8_CMD = str(d8_bat) if d8_bat.is_file() else f'java -cp "{d8_jar}" com.android.tools.r8.D8'
                    ZIPALIGN_EXE = str(za)
                    APKSIGNER_CMD = str(signer_bat) if signer_bat.is_file() else f'java -jar "{signer_jar}"'
                    break

# 4. Find Apktool
APKTOOL_CANDIDATES = [
    Path(r"e:\workspace\apktool.jar"),
    ROOT_DIR / "tools" / "bin" / "apktool.jar",
    ROOT_DIR / "tools" / "apktool.jar",
]
APKTOOL_JAR = None
for cand in APKTOOL_CANDIDATES:
    if cand.is_file():
        APKTOOL_JAR = cand
        break
if not APKTOOL_JAR:
    which_apktool = shutil.which("apktool")
    if which_apktool:
        APKTOOL_JAR = Path(which_apktool)

# 5. Signing key (local-only; .sign/ is git-ignored, never committed)
SIGNING_DIR = ROOT_DIR / ".sign"
KEYSTORE = SIGNING_DIR / "my-release-key.jks"
SIGNING_PROPERTIES = SIGNING_DIR / "qianming.txt"


def _read_signing_value(key):
    if not SIGNING_PROPERTIES.is_file():
        raise RuntimeError(
            f"Signing credentials not found: {SIGNING_PROPERTIES}. "
            "Place my-release-key.jks and qianming.txt in .sign/ (never commit them)."
        )
    lines = [line.strip() for line in SIGNING_PROPERTIES.read_text(encoding="utf-8").splitlines()]
    try:
        index = lines.index(key)
    except ValueError:
        raise RuntimeError(f"Missing {key} in {SIGNING_PROPERTIES}") from None
    if index + 1 >= len(lines) or not lines[index + 1]:
        raise RuntimeError(f"Empty {key} in {SIGNING_PROPERTIES}")
    return lines[index + 1]


def require_signing():
    """Resolve the local release signing material, failing loudly when absent."""
    if not KEYSTORE.is_file():
        raise RuntimeError(f"Keystore not found: {KEYSTORE}")
    return _read_signing_value("ALIAS"), _read_signing_value("KEY_PASSWORD"), _read_signing_value("KEYSTORE_PASSWORD")


# Populated lazily by require_signing(); kept as module attributes for the build scripts.
KEY_ALIAS = None
KEY_PASS = None
KEYSTORE_PASS = None

# 6. Paths
AES_DIR = ROOT_DIR / "aes"
AES_SRC = AES_DIR / "src"
AES_BASE_APK = AES_DIR / "base" / "Application Extension Service_5.1.7.apk"
AES_SMALI_PROJECT = AES_DIR / "smali_project"

BRIDGE_DIR = ROOT_DIR / "bridge"
BRIDGE_SRC = BRIDGE_DIR / "src"
BRIDGE_MANIFEST = BRIDGE_DIR / "AndroidManifest.xml"
BRIDGE_ASSETS = BRIDGE_DIR / "assets"
BRIDGE_RESOURCES = BRIDGE_DIR / "resources"
BRIDGE_LIBS = BRIDGE_DIR / "libs"

DIST_DIR = ROOT_DIR / "dist"
BUILD_CACHE_DIR = ROOT_DIR / ".build_cache"

def ensure_dirs():
    DIST_DIR.mkdir(parents=True, exist_ok=True)
    BUILD_CACHE_DIR.mkdir(parents=True, exist_ok=True)
