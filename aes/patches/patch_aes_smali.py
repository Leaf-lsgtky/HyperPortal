#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Minimal Plan-B smali patches for AES 5.1.7.

Usage:
  python patch_aes_smali.py <apktool-decoded-dir>

The script is intentionally small: it only inserts static forwards to
dev.dragshare.catcher.Bridge and adds a fetchViewContent wrapper.
"""

import os
import re
import sys

BRIDGE = "Ldev/dragshare/catcher/Bridge;"


def read(path):
    with open(path, "r", encoding="utf-8", newline="") as handle:
        return handle.read()


def write(path, text):
    with open(path, "w", encoding="utf-8", newline="") as handle:
        handle.write(text)


def replace_once(text, pattern, replacement, label):
    new_text, count = re.subn(pattern, replacement, text, count=1, flags=re.S)
    if count != 1:
        raise SystemExit("patch failed: %s (matches=%d)" % (label, count))
    print("patched: " + label)
    return new_text


def patch_interceptor(decoded):
    path = os.path.join(decoded, "smali", "com", "miui", "contentcatcher", "Interceptor.smali")
    text = read(path)

    # A0. Let ComposeView / WebView through as leaf candidates instead of
    # recursing into their empty child lists.
    method_start = text.find(
        ".method public final d(Landroid/view/View;Landroid/graphics/PointF;"
        "Landroid/graphics/PointF;Ljava/util/ArrayList;Ljava/util/HashMap;)V")
    if method_start < 0:
        raise SystemExit("patch failed: Interceptor.d not found")
    method_end = text.find(".end method", method_start)
    body = text[method_start:method_end]
    match = re.search(
        r"instance-of\s+([vp]\d+),\s*([vp]\d+),\s*Landroid/view/ViewGroup;",
        body)
    if match is None:
        print("skip: Interceptor.d ViewGroup check already patched")
    else:
        dest, source = match.group(1), match.group(2)
        replacement = (
            "invoke-static {%s}, %s->shouldHandleAsViewGroup(Landroid/view/View;)Z\n\n"
            "    move-result %s" % (source, BRIDGE, dest)
        )
        body = body[:match.start()] + replacement + body[match.end():]
        text = text[:method_start] + body + text[method_end:]
        print("patched: Interceptor.d ViewGroup -> Bridge.shouldHandleAsViewGroup")

    # A. every touch event: prove classes2.dex is loaded in the target process
    text = replace_once(
        text,
        r"(\.method public final dispatchTouchEvent\(Landroid/view/MotionEvent;Landroid/view/View;Landroid/app/Activity;\)Z\s*\n\s*\.locals 4\s*\n)",
        r"\1\n    invoke-static {p1, p2, p3}, " + BRIDGE +
        "->ping(Landroid/view/MotionEvent;Landroid/view/View;Landroid/app/Activity;)V\n",
        "Interceptor.dispatchTouchEvent -> Bridge.ping",
    )

    # B. long-press entry: record the two touch points
    text = replace_once(
        text,
        r"(\.method public final l\(Landroid/graphics/PointF;Landroid/graphics/PointF;Landroid/view/View;\)Z\s*\n\s*\.locals 17\s*\n)",
        r"\1\n    invoke-static/range {p1 .. p3}, " + BRIDGE +
        "->recordTouch(Landroid/graphics/PointF;Landroid/graphics/PointF;Landroid/view/View;)V\n",
        "Interceptor.l -> Bridge.recordTouch",
    )

    # C. replace the AES service PageConfig lookup with the local injector
    text = replace_once(
        text,
        r"invoke-static \{\}, Lmiui/contentcatcher/sdk/ContentCatcherManager;->getInstance\(\)"
        r"Lmiui/contentcatcher/sdk/ContentCatcherManager;\s*"
        r"move-result-object v2\s*"
        r"iget-object v3, v0, Lcom/miui/contentcatcher/Interceptor;->c:Lmiui/contentcatcher/sdk/Token;\s*"
        r"invoke-virtual \{v2, v3\}, Lmiui/contentcatcher/sdk/ContentCatcherManager;->getPageConfig"
        r"\(Lmiui/contentcatcher/sdk/Token;\)Lmiui/contentcatcher/sdk/data/PageConfig;\s*"
        r"move-result-object v2",
        "iget-object v3, v0, Lcom/miui/contentcatcher/Interceptor;->c:Lmiui/contentcatcher/sdk/Token;\n\n"
        "    invoke-static {v3}, " + BRIDGE +
        "->getPageConfig(Ljava/lang/Object;)Ljava/lang/Object;\n\n"
        "    move-result-object v2\n\n"
        "    check-cast v2, Lmiui/contentcatcher/sdk/data/PageConfig;",
        "Interceptor.n -> Bridge.getPageConfig",
    )

    # D. direct dispatch: Interceptor.l's own onContentCatched call
    text = replace_once(
        text,
        r"invoke-static \{\}, Lmiui/contentcatcher/sdk/ContentCatcherManager;->getInstance\(\)"
        r"Lmiui/contentcatcher/sdk/ContentCatcherManager;\s*"
        r"move-result-object v1\s*"
        r"invoke-virtual \{v1, v0\}, Lmiui/contentcatcher/sdk/ContentCatcherManager;->onContentCatched"
        r"\(Lmiui/contentcatcher/sdk/Content;\)V",
        "invoke-static {v0}, " + BRIDGE + "->deliverContent(Ljava/lang/Object;)V",
        "Interceptor.l -> Bridge.deliverContent",
    )

    write(path, text)


def patch_fetcher(decoded):
    path = os.path.join(decoded, "smali", "com", "miui", "catcherpatch", "ViewContentFetcher.smali")
    text = read(path)
    old_decl = ".method public fetchViewContent(Landroid/view/View;IZ)Landroid/os/Bundle;"
    if old_decl not in text:
        raise SystemExit("patch failed: fetchViewContent declaration not found")
    if "fetchViewContentImpl" not in text:
        text = text.replace(old_decl,
                            ".method public fetchViewContentImpl(Landroid/view/View;IZ)Landroid/os/Bundle;",
                            1)
        wrapper = (
            "\n\n.method public fetchViewContent(Landroid/view/View;IZ)Landroid/os/Bundle;\n"
            "    .locals 2\n\n"
            "    invoke-static {p1, p2, p3}, " + BRIDGE +
            "->preExtract(Landroid/view/View;IZ)Landroid/os/Bundle;\n\n"
            "    move-result-object v0\n\n"
            "    if-eqz v0, :orig\n\n"
            "    return-object v0\n\n"
            "    :orig\n"
            "    invoke-virtual {p0, p1, p2, p3}, Lcom/miui/catcherpatch/ViewContentFetcher;"
            "->fetchViewContentImpl(Landroid/view/View;IZ)Landroid/os/Bundle;\n\n"
            "    move-result-object v0\n\n"
            "    invoke-static {p1, p2, p3, v0}, " + BRIDGE +
            "->enhanceFetchViewContent(Landroid/view/View;IZLandroid/os/Bundle;)Landroid/os/Bundle;\n\n"
            "    move-result-object v1\n\n"
            "    return-object v1\n"
            ".end method\n"
        )
        text = text.rstrip() + wrapper
        print("patched: ViewContentFetcher.fetchViewContent wrapper")
    else:
        print("skip: ViewContentFetcher already wrapped")
    write(path, text)


def patch_datachub(decoded):
    path = os.path.join(decoded, "smali", "d", "c.smali")
    text = read(path)
    text = replace_once(
        text,
        r"invoke-static \{\}, Lmiui/contentcatcher/sdk/ContentCatcherManager;->getInstance\(\)"
        r"Lmiui/contentcatcher/sdk/ContentCatcherManager;\s*"
        r"move-result-object v1\s*"
        r"invoke-virtual \{v1, v2\}, Lmiui/contentcatcher/sdk/ContentCatcherManager;->onContentCatched"
        r"\(Lmiui/contentcatcher/sdk/Content;\)V",
        "invoke-static {v2}, " + BRIDGE + "->deliverContent(Ljava/lang/Object;)V",
        "d/c.b -> Bridge.deliverContent",
    )
    write(path, text)


def main():
    if len(sys.argv) != 2:
        raise SystemExit("usage: patch_aes_smali.py <decoded-dir>")
    decoded = sys.argv[1]
    patch_interceptor(decoded)
    patch_fetcher(decoded)
    patch_datachub(decoded)
    print("done")


if __name__ == "__main__":
    main()
