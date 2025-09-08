package cinnamon.utils;

public class Timer {

    private final float tickMs;
    private long lastMs;
    public float deltaTime;
    public float tickDelta;
    public float partialTick;

    public Timer(int tps) {
        this.tickMs = 1000.0f / tps;
        this.lastMs = getTime();
    }

    public int update() {
        long CurrMs = getTime();
        long diffMs = CurrMs - lastMs;

        deltaTime = diffMs / 1000.0f;
        tickDelta = diffMs / tickMs;
        lastMs = CurrMs;

        partialTick += tickDelta;
        int i = (int) partialTick;
        partialTick -= i;

        return i;
    }

    private static long getTime() {
        return System.nanoTime() / 1_000_000L;
    }
}
