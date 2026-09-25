package cinnamon.events;

import java.util.ArrayList;
import java.util.List;

public class Event<T> {

    private final List<T> listeners = new ArrayList<>();
    private final InvokerFactory<T> factory;
    private T invoker;

    public Event(InvokerFactory<T> factory) {
        this.factory = factory;
        this.invoker = factory.build(listeners);
    }

    public void register(T listener) {
        listeners.add(listener);
        this.invoker = factory.build(listeners);
    }

    public T invoker() {
        return invoker;
    }

    public interface InvokerFactory<T> {
        T build(List<T> listeners);
    }
}
