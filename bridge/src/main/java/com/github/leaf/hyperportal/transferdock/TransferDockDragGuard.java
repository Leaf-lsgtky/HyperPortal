// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock;

/**
 * Process-local guard between the transfer dock drag and the Content Portal
 * overlay.  The SystemUI drag listener checks this before accepting a global
 * drag, so dragging an item out of the transfer dock never starts the
 * ColorOS glow / app-target shelf.
 */
public final class TransferDockDragGuard {

    public static final String ACTION_STATE =
            "com.github.leaf.hyperportal.TRANSFER_DOCK_DRAG_STATE";

    private static volatile boolean sDragging;

    private TransferDockDragGuard() {
    }

    public static void setDragging(boolean dragging) {
        sDragging = dragging;
    }

    public static boolean isDragging() {
        return sDragging;
    }
}
