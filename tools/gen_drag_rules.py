"""Generate ``aes/src/dev/dragshare/catcher/DragRules.java`` from ColorOS's
``sys_view_drag_list.xml`` (extracted from a ColorOS 17 super image).

The generated class is a plain ``String[][]`` of rows so the AES hook needs no
asset loading at runtime (the AES APK is repacked by swapping dex files only).

Row layout (pipe separated, trailing fields optional):
    pkg | activity | type | name | parent | brother | id | uncle | nephews | flags

flags: ``t`` = touch-view (only the innermost view under the finger),
       ``b`` = bottom-view (keep looking for a deeper match).

usage:
    python tools/gen_drag_rules.py [--src PATH] [--out PATH]
"""
import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_SRC = ROOT / ".analysis" / "coloros" / "sys_view_drag_list.xml"
DEFAULT_OUT = ROOT / "aes" / "src" / "dev" / "dragshare" / "catcher" / "DragRules.java"

KEEP = ("name", "type", "parent", "brother", "id", "uncle", "nephews")
FLAGS = ("touch-view", "bottom-view")
ROWS_PER_CHUNK = 300


def attr(tag, name):
    m = re.search(r'\b%s\s*=\s*"([^"]*)"' % re.escape(name), tag)
    return m.group(1).strip() if m else ""


def parse(xml_text):
    start = xml_text.find("<whiteList>")
    end = xml_text.find("<blackList>")
    if start < 0:
        raise SystemExit("no <whiteList> in source")
    body = xml_text[start:end if end > 0 else len(xml_text)]

    rows = []
    for pm in re.finditer(r'<package name="([^"]+)">(.*?)</package>', body, re.S):
        pkg, pkg_body = pm.group(1), pm.group(2)
        for am in re.finditer(r'<activity name="([^"]+)"[^>]*>(.*?)</activity>', pkg_body, re.S):
            act, act_body = am.group(1), am.group(2)
            for vm in re.finditer(r"<view\b([^>]*?)/?>", act_body, re.S):
                tag = vm.group(1)
                fields = {k: attr(tag, k) for k in KEEP}
                if not fields["type"] or not fields["name"]:
                    continue
                flags = ""
                if attr(tag, "touch-view") in ("1", "true"):
                    flags += "t"
                if attr(tag, "bottom-view") in ("1", "true"):
                    flags += "b"
                cols = [pkg, act, fields["type"], fields["name"], fields["parent"],
                        fields["brother"], fields["id"], fields["uncle"], fields["nephews"], flags]
                if any("|" in c or "\n" in c for c in cols):
                    continue
                row = "|".join(cols).rstrip("|")
                rows.append(row)

    seen, uniq = set(), []
    for r in rows:
        if r in seen:
            continue
        seen.add(r)
        uniq.append(r)
    return uniq


def java_literal(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


def emit(rows, out_path):
    chunks = [rows[i:i + ROWS_PER_CHUNK] for i in range(0, len(rows), ROWS_PER_CHUNK)]
    lines = [
        "package dev.dragshare.catcher;",
        "",
        "/**",
        " * GENERATED FILE - do not edit by hand.",
        " *",
        " * Regenerate with: python tools/gen_drag_rules.py",
        " * Source: ColorOS 17 /system_ext/oplus/sys_view_drag_list.xml (whiteList),",
        " * reduced to the matchers HyperPortal implements:",
        " *   pkg|activity|type|name|parent|brother|id|uncle|nephews|flags",
        " *",
        " * The rule pins a view class to one content modality, which is what keeps",
        " * mixed text+image cards unambiguous. Apps that are not listed simply fall",
        " * back to the heuristic extraction path in Bridge.",
        " */",
        "final class DragRules {",
        "    private DragRules() {",
        "    }",
        "",
        "    static final int ROW_COUNT = %d;" % len(rows),
        "",
    ]
    for i, chunk in enumerate(chunks):
        lines.append("    static final String[] C%d = {" % i)
        for row in chunk:
            lines.append("            %s," % java_literal(row))
        lines.append("    };")
        lines.append("")
    lines.append("    static final String[][] CHUNKS = {")
    lines.append("            " + ", ".join("C%d" % i for i in range(len(chunks))))
    lines.append("    };")
    lines.append("}")
    out_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return len(chunks)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--src", default=str(DEFAULT_SRC))
    ap.add_argument("--out", default=str(DEFAULT_OUT))
    args = ap.parse_args()

    src = Path(args.src)
    if not src.is_file():
        raise SystemExit("source not found: %s" % src)
    rows = parse(src.read_text(encoding="utf-8"))
    if not rows:
        raise SystemExit("no rules parsed")
    out = Path(args.out)
    nchunks = emit(rows, out)
    pkgs = len({r.split("|")[0] for r in rows})
    print("rules: %d rows, %d packages, %d chunks -> %s" % (len(rows), pkgs, nchunks, out))


if __name__ == "__main__":
    sys.exit(main())
