package cinnamon.math;

/**
 * A timer that tracks the elapsed system time and updates based on a specified ticks per second (TPS) value
 */
public class Timer {

    private final float tickMs;
    private long lastMs;
    private float deltaTime;
    private float tickDelta;
    private float partialTick;

    public Timer(int tps) {
        this.tickMs = 1000.0f / tps;
        this.lastMs = getTime();
    }

    /**
     * Updates the timer and calculates the time since the last update in seconds and ticks
     * @return The number of whole ticks that have passed since the last update
     */
    public int update() {
        long currMs = getTime();
        long diffMs = currMs - lastMs;

        deltaTime = diffMs / 1000.0f;
        tickDelta = diffMs / tickMs;
        lastMs = currMs;

        partialTick += tickDelta;
        int i = (int) partialTick;
        partialTick -= i;

        return i;
    }

    private static long getTime() {
        return System.nanoTime() / 1_000_000L;
    }

    /**
     * @return The time since the last update in seconds
     */
    public float deltaTime() {
        return deltaTime;
    }

    /**
     * @return The time since the last update in ticks
     */
    public float tickDelta() {
        return tickDelta;
    }

    /**
     * @return The total fraction of a tick since the last update
     */
    public float partialTick() {
        return partialTick;
    }
}
