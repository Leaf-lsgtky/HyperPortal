package dev.dragshare.catcher;

/** Main-thread gesture eligibility, independent of the post-hold drag circle. */
final class LongPressGate {
    private float downX;
    private float downY;
    private float slopSquared;
    private boolean cancelled = true;

    void reset(float x, float y, float slop) {
        downX = x;
        downY = y;
        slopSquared = slop * slop;
        cancelled = false;
    }

    void move(float x, float y) {
        float dx = x - downX;
        float dy = y - downY;
        // Latch cancellation: returning inside the circle cannot turn a scroll
        // back into a held touch. Only a fresh DOWN starts another candidate.
        if (dx * dx + dy * dy > slopSquared) cancelled = true;
    }

    void cancel() {
        cancelled = true;
    }

    boolean isCancelled() {
        return cancelled;
    }
}
