package cinnamon.utils;

import java.util.LinkedList;

public class CircularQueue<E> extends LinkedList<E> {

    private final int capacity;

    public CircularQueue(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public boolean add(E e) {
        if (size() >= capacity)
            removeFirst();
        return super.add(e);
    }

    public int getCapacity() {
        return capacity;
    }
}
