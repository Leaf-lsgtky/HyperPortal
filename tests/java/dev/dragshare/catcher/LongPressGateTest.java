package dev.dragshare.catcher;

/** Host-JVM regression cases; runs without an Android device or injected input. */
public final class LongPressGateTest {
    private static void check(boolean condition, String scenario) {
        if (!condition) throw new AssertionError(scenario);
    }

    public static void main(String[] args) {
        LongPressGate gate = new LongPressGate();
        check(gate.isCancelled(), "no long press before DOWN");

        gate.reset(100, 200, 24);
        gate.move(103, 204);
        check(!gate.isCancelled(), "finger jitter must still allow a stationary hold");
        gate.move(124, 200);
        check(!gate.isCancelled(), "the slop boundary itself is allowed");

        // At 3x density the former 24dp portal circle was 72px. A feed can
        // already scroll at 25px, while the old fallback kept extracting it.
        gate.move(125, 200);
        check(gate.isCancelled(), "scroll inside the portal circle cancels long press");
        gate.move(100, 200);
        check(gate.isCancelled(), "out-and-back historical samples stay cancelled");

        gate.reset(100, 200, 24);
        gate.move(118, 218);
        check(gate.isCancelled(), "diagonal movement uses radial scroll slop");

        gate.reset(300, 400, 24);
        gate.move(301, 401);
        check(!gate.isCancelled(), "a new DOWN recovers after a scroll");
        gate.cancel();
        gate.move(300, 400);
        check(gate.isCancelled(), "UP/CANCEL/multi-touch cannot revive a pending hold");

        gate.reset(300, 400, 12);
        gate.move(313, 400);
        check(gate.isCancelled(), "a new gesture uses its current density/slop");
        System.out.println("LongPressGate: 9 regression checks passed");
    }
}
