package cinnamon.events;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Event<T> {

    private final Map<String, T> listeners = new HashMap<>();
    private final InvokerFactory<T> factory;
    private T invoker;

    public Event(InvokerFactory<T> factory) {
        this.factory = factory;
        this.invoker = factory.build(listeners.values());
    }

    public void register(String id, T listener) {
        if (listeners.containsKey(id))
            Events.LOGGER.warn("Overwriting event listener with id: " + id);
        listeners.put(id, listener);
        this.invoker = factory.build(listeners.values());
    }

    public void removeEvent(String id) {
        if (!listeners.containsKey(id))
            return;
        listeners.remove(id);
        this.invoker = factory.build(listeners.values());
    }

    public T invoker() {
        return invoker;
    }

    public interface InvokerFactory<T> {
        T build(Collection<T> listeners);
    }
}
