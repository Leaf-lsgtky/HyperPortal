package dev.dragshare.catcher;

/** Gesture eligibility, independent of the post-hold drag circle. */
final class LongPressGate {
    private float downX;
    private float downY;
    private float slopSquared;
    private boolean cancelled = true;

    // AES can dispatch from its interceptor thread; timers and web callbacks
    // run on main. Publish the coordinates and cancellation as one state.
    synchronized void reset(float x, float y, float slop) {
        downX = x;
        downY = y;
        slopSquared = slop * slop;
        cancelled = false;
    }

    synchronized void move(float x, float y) {
        float dx = x - downX;
        float dy = y - downY;
        // Latch cancellation: returning inside the circle cannot turn a scroll
        // back into a held touch. Only a fresh DOWN starts another candidate.
        if (dx * dx + dy * dy > slopSquared) cancelled = true;
    }

    synchronized void cancel() {
        cancelled = true;
    }

    synchronized boolean isCancelled() {
        return cancelled;
    }
}
