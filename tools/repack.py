#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Repack the original APK with patched classes.dex and classes2.dex, and optionally update versionCode."""

import sys
import zipfile
from pathlib import Path

RES_XML_TYPE = 0x0003
RES_STRING_POOL_TYPE = 0x0001
RES_XML_START_ELEMENT_TYPE = 0x0102
TYPE_INT_DEC = 0x10


class AxmlVersionCode:
    def __init__(self, data):
        self.data = bytearray(data)

    def u16(self, off):
        return int.from_bytes(self.data[off:off + 2], "little")

    def u32(self, off):
        return int.from_bytes(self.data[off:off + 4], "little")

    def strings(self):
        off = 8
        if self.u16(off) != RES_STRING_POOL_TYPE:
            raise RuntimeError("no string pool")
        header_size = self.u16(off + 2)
        chunk_size = self.u32(off + 4)
        count = self.u32(off + 8)
        flags = self.u32(off + 16)
        strings_start = self.u32(off + 20)
        utf8 = bool(flags & (1 << 8))
        base = off + strings_start
        offsets = [self.u32(off + header_size + i * 4) for i in range(count)]
        result = []
        for item in offsets:
            cur = base + item
            if utf8:
                cur += 1
                length = self.data[cur]
                cur += 1
                result.append(self.data[cur:cur + length].decode("utf-8", "replace"))
            else:
                length = self.u16(cur)
                cur += 2
                if length & 0x8000:
                    length = ((length & 0x7fff) << 16) | self.u16(cur)
                    cur += 2
                result.append(self.data[cur:cur + length * 2].decode("utf-16-le", "replace"))
        return result, off + chunk_size

    def apply(self, new_code):
        strings, cursor = self.strings()
        total = self.u32(4)
        while cursor < total:
            chunk_type = self.u16(cursor)
            chunk_size = self.u32(cursor + 4)
            if chunk_size <= 0:
                break
            if chunk_type == RES_XML_START_ELEMENT_TYPE:
                name_idx = self.u32(cursor + 20)
                attr_start = self.u16(cursor + 24)
                attr_size = self.u16(cursor + 26)
                attr_count = self.u16(cursor + 28)
                if 0 <= name_idx < len(strings) and strings[name_idx] == "manifest":
                    for i in range(attr_count):
                        attr = cursor + 16 + attr_start + i * attr_size
                        attr_name = self.u32(attr + 4)
                        if 0 <= attr_name < len(strings) and strings[attr_name] == "versionCode":
                            old = self.u32(attr + 16)
                            self.data[attr + 15] = TYPE_INT_DEC
                            self.data[attr + 16:attr + 20] = int(new_code).to_bytes(4, "little")
                            print(f"[REPACK] versionCode: {old} -> {new_code}")
                            return bytes(self.data)
            cursor += chunk_size
        raise RuntimeError("versionCode not found")


def repack_apk(original_apk, patched_dex, classes2_dex, out_apk, version_code=None):
    with open(patched_dex, "rb") as f:
        patched = f.read()
    with open(classes2_dex, "rb") as f:
        second = f.read()

    with zipfile.ZipFile(original_apk, "r") as src, zipfile.ZipFile(out_apk, "w") as dst:
        for info in src.infolist():
            data = src.read(info.filename)
            if info.filename == "classes.dex":
                data = patched
            elif info.filename == "AndroidManifest.xml" and version_code is not None:
                data = AxmlVersionCode(data).apply(int(version_code))
            elif info.filename.startswith("META-INF/") and (info.filename.endswith(".SF") or info.filename.endswith(".RSA") or info.filename.endswith(".MF")):
                # Strip old signatures
                continue
            dst.writestr(info, data)
        # Add classes2.dex
        dst.writestr("classes2.dex", second)
    print(f"[REPACK] Packaged {out_apk}")


if __name__ == "__main__":
    if len(sys.argv) < 5:
        print("Usage: python repack.py <orig.apk> <classes.dex> <classes2.dex> <out.apk> [versionCode]")
        sys.exit(1)
    orig = sys.argv[1]
    cdex = sys.argv[2]
    c2dex = sys.argv[3]
    out = sys.argv[4]
    vcode = sys.argv[5] if len(sys.argv) > 5 else None
    repack_apk(orig, cdex, c2dex, out, vcode)
