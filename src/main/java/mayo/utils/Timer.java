package mayo.utils;

public class Timer {

    private final int ups;
    private long lastTime = System.nanoTime();
    private float delta;

    public Timer(int ups) {
        this.ups = 1_000_000_000 / ups;
    }

    public int update() {
        long currTime = System.nanoTime();
        long diff = currTime - lastTime;
        lastTime = currTime;
        this.delta += (float) diff / ups;
        int i = (int) delta;
        this.delta -= i;
        return i;
    }

    public float delta() {
        return delta;
    }
}
