package cinnamon.utils;

import cinnamon.Client;

import java.util.ArrayList;
import java.util.List;

public class Await {

    private static final List<Await> AWAIT_LIST = new ArrayList<>();

    private final Runnable runnable;
    private final long time;

    public Await(long time, Runnable runnable) {
        this.runnable = runnable;
        this.time = Client.getInstance().ticks + time;
        AWAIT_LIST.add(this);
    }

    public boolean isDone() {
        return Client.getInstance().ticks >= time;
    }

    public static void tick() {
        for (int i = AWAIT_LIST.size() - 1; i >= 0; i--) {
            Await await = AWAIT_LIST.get(i);
            if (await.isDone()) {
                await.runnable.run();
                AWAIT_LIST.remove(i);
            }
        }
    }
}
