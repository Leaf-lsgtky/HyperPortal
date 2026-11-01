#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unified Build System for ohmyAES.

Builds both:
  1. AES (ContentCatcher-patched.apk) - Hooked privileged plugin
  2. DragShareBridge (HyperPortal.apk) - Companion Service & LSPosed 3D tilt module

Usage:
  python tools/build.py bridge [--clean]           # normal test build, R8 OFF
  python tools/build.py bridge --r8                # pre-release R8 build -> dist/HyperPortal-r8.apk
  python tools/build.py aes [--clean] [--version-code <N>]   # never R8-minified
  python tools/build.py all [--clean] [--r8]
"""

import argparse
import os
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

# Add tools to sys.path
sys.path.insert(0, str(Path(__file__).resolve().parent))
import config as cfg
import repack


def run_cmd(cmd, shell=True, check=True):
    if isinstance(cmd, list):
        cmd_str = subprocess.list2cmdline([str(c) for c in cmd])
    else:
        cmd_str = str(cmd)
    res = subprocess.run(cmd_str, shell=shell, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace")
    if res.returncode != 0 and check:
        print(f"[ERROR] Command failed with code {res.returncode}:\n{cmd_str}\n\nOutput:\n{res.stdout}", file=sys.stderr)
        raise RuntimeError(f"Command failed: {cmd_str}")
    return res


def sign_and_verify(aligned_apk, output_apk):
    if output_apk.exists():
        output_apk.unlink()
    
    alias, key_pass, keystore_pass = cfg.require_signing()

    # 1. Sign
    common = (
        f'sign --ks "{cfg.KEYSTORE}" --ks-key-alias {alias} '
        f'--ks-pass pass:{keystore_pass} --key-pass pass:{key_pass} '
        f'--out "{output_apk}" "{aligned_apk}"'
    )
    if cfg.APKSIGNER_CMD.endswith(".jar\"") or "java -jar" in cfg.APKSIGNER_CMD:
        sign_cmd = f'{cfg.APKSIGNER_CMD} {common}'
    else:
        sign_cmd = f'"{cfg.APKSIGNER_CMD}" {common}'
    run_cmd(sign_cmd)

    # 2. Verify
    if cfg.APKSIGNER_CMD.endswith(".jar\"") or "java -jar" in cfg.APKSIGNER_CMD:
        verify_cmd = f'{cfg.APKSIGNER_CMD} verify --verbose "{output_apk}"'
    else:
        verify_cmd = f'"{cfg.APKSIGNER_CMD}" verify --verbose "{output_apk}"'
    run_cmd(verify_cmd)
    print(f"[OK] Built & Verified: {output_apk.name} ({output_apk.stat().st_size:,} bytes)")


def build_bridge(clean=False, r8=False):
    print("=" * 60)
    print("Building HyperPortal (Miuix Compose App & LSPosed Module)...")
    print("=" * 60)
    cfg.ensure_dirs()
    gradle_cmd = "gradlew.bat" if os.name == "nt" else "./gradlew"
    task = ":bridge:assembleRelease"
    if clean:
        task = ":bridge:clean " + task
    if r8:
        task += " -Phyperportal.minify=true"
        print("[R8] Building a minified release APK (pre-release verification only)")
    cmd = f"{gradle_cmd} {task}"
    print(f"[1/2] Running Gradle: {cmd}...")
    run_cmd(cmd, shell=True)

    built_apk = cfg.ROOT_DIR / "bridge" / "build" / "outputs" / "apk" / "release" / "bridge-release.apk"
    if not built_apk.is_file():
        raise RuntimeError(f"Gradle failed to produce release APK at {built_apk}")

    out_apk = cfg.DIST_DIR / ("HyperPortal-r8.apk" if r8 else "HyperPortal.apk")
    shutil.copy2(built_apk, out_apk)
    print(f"[2/2] [OK] Built & Verified: {out_apk.name} ({out_apk.stat().st_size:,} bytes)")
    return out_apk

def build_aes(clean=False, version_code=1000000045):
    print("=" * 60)
    print("Building AES (ContentCatcher Privileged Hook)...")
    print("=" * 60)
    if not cfg.AES_BASE_APK.is_file():
        raise RuntimeError(f"Base APK not found: {cfg.AES_BASE_APK}")
    
    cfg.ensure_dirs()
    a_cache = cfg.BUILD_CACHE_DIR / "aes"
    classes_dir = a_cache / "classes"
    dex_dir = a_cache / "dex"

    if clean and a_cache.exists():
        shutil.rmtree(a_cache, ignore_errors=True)
    classes_dir.mkdir(parents=True, exist_ok=True)
    dex_dir.mkdir(parents=True, exist_ok=True)

    # 1. Compile Bridge.java
    print("[1/5] Compiling Bridge.java...")
    bridge_src_files = list(cfg.AES_SRC.rglob("*.java"))
    if not bridge_src_files:
        raise RuntimeError("No Java files found in aes/src!")
    
    javac_cmd = [
        "javac", "-encoding", "utf-8", "--release", "11",
        "-cp", str(cfg.ANDROID_JAR),
        "-d", str(classes_dir)
    ] + [str(p) for p in bridge_src_files]
    run_cmd(javac_cmd)

    # 2. Dex to classes2.dex
    print("[2/5] Dexing Bridge to classes2.dex...")
    for old_dex in dex_dir.glob("*.dex"):
        old_dex.unlink()

    class_files = list(classes_dir.rglob("*.class"))
    d8_cmd = f'{cfg.D8_CMD} --min-api 29 --lib "{cfg.ANDROID_JAR}" --output "{dex_dir}" ' + " ".join(f'"{str(cf)}"' for cf in class_files)
    run_cmd(d8_cmd)

    classes2_dex = dex_dir / "classes.dex"
    if not classes2_dex.is_file():
        raise RuntimeError("D8 failed to generate classes2.dex!")

    # 3. Ensure base classes.dex exists (from smali)
    cached_classes_dex = a_cache / "classes.dex"
    if not cached_classes_dex.is_file() or clean:
        print("[3/5] Assembling patched smali into classes.dex...")
        if not cfg.APKTOOL_JAR:
            raise RuntimeError("apktool.jar not found to compile smali!")
        rebuilt_apk = a_cache / "smali_rebuilt.apk"
        if rebuilt_apk.exists():
            rebuilt_apk.unlink()
        run_cmd(f'java -jar "{cfg.APKTOOL_JAR}" b "{cfg.AES_SMALI_PROJECT}" -o "{rebuilt_apk}"')
        with zipfile.ZipFile(rebuilt_apk, "r") as z:
            with open(cached_classes_dex, "wb") as out:
                out.write(z.read("classes.dex"))
        if rebuilt_apk.exists():
            rebuilt_apk.unlink()
    else:
        print("[3/5] Using cached base classes.dex...")

    # 4. Repack APK
    print("[4/5] Repacking APK with classes.dex & classes2.dex...")
    unsigned_apk = a_cache / "aes-unsigned.apk"
    aligned_apk = a_cache / "aes-aligned.apk"
    repack.repack_apk(str(cfg.AES_BASE_APK), str(cached_classes_dex), str(classes2_dex), str(unsigned_apk), version_code)

    # 5. Zipalign & Sign
    print("[5/5] Zipalign and signing...")
    if aligned_apk.exists():
        aligned_apk.unlink()
    run_cmd(f'"{cfg.ZIPALIGN_EXE}" -f -p 4 "{unsigned_apk}" "{aligned_apk}"')

    final_apk = cfg.DIST_DIR / "ContentCatcher-patched.apk"
    sign_and_verify(aligned_apk, final_apk)
    return final_apk


def main():
    parser = argparse.ArgumentParser(description="Unified Build Tool for ohmyAES")
    parser.add_argument("target", choices=["bridge", "aes", "all", "clean"], help="Target component to build")
    parser.add_argument("--clean", action="store_true", help="Force clean build cache before building")
    parser.add_argument("--version-code", type=int, default=1000000045, help="versionCode for ContentCatcher APK")
    parser.add_argument(
        "--r8",
        action="store_true",
        help="Build the bridge with R8/minification (pre-release only; AES stays unminified)",
    )
    args = parser.parse_args()

    if args.target == "clean":
        if cfg.BUILD_CACHE_DIR.exists():
            shutil.rmtree(cfg.BUILD_CACHE_DIR, ignore_errors=True)
            print("[OK] Cleaned .build_cache")
        if cfg.DIST_DIR.exists():
            shutil.rmtree(cfg.DIST_DIR, ignore_errors=True)
            print("[OK] Cleaned dist")
        return

    if args.target in ("bridge", "all"):
        build_bridge(clean=args.clean, r8=args.r8)
    if args.target in ("aes", "all"):
        build_aes(clean=args.clean, version_code=args.version_code)


if __name__ == "__main__":
    main()
