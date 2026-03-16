package cinnamon.physics;

import cinnamon.physics.component.Component;
import cinnamon.physics.component.ComponentType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public abstract class WorldObject {

    private final Map<ComponentType, List<Component>> components = new EnumMap<>(ComponentType.class);

    private World world;
    private final int id;

    public WorldObject(int id) {
        this.id = id;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> List<T> getComponents(ComponentType type) {
        List<Component> comps = components.get(type);
        if (comps == null) return List.of();
        return (List<T>) comps;
    }

    public void addComponent(Component component) {
        components.computeIfAbsent(component.getType(), k -> new ArrayList<>()).add(component);
    }

    public int getId() {
        return id;
    }
}
