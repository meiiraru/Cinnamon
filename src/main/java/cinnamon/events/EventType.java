package cinnamon.events;

public enum EventType {
    TICK_BEFORE_WORLD,
    TICK_BEFORE_GUI,
    TICK_END,

    RENDER_BEFORE_WORLD,
    RENDER_BEFORE_GUI,
    RENDER_END,

    RESOURCE_INIT,
    RESOURCE_FREE
}
