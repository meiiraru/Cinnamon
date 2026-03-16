package cinnamon.physics;

import cinnamon.Client;
import cinnamon.render.MatrixStack;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class World {

    private final List<WorldObject> worldObjects = new ArrayList<>();
    private final List<TickableObject> tickableObjects = new ArrayList<>();
    private final Queue<RenderableObject> renderableObjects = new PriorityQueue<>((r1, r2) -> Integer.compare(r2.getRenderPriority(), r1.getRenderPriority()));

    private final Client client;
    private final PhysicsSystem physics = new PhysicsSystem();
    private int nextId = 0;

    public World() {
        this.client = Client.getInstance();
    }

    public void tick() {
        for (TickableObject obj : tickableObjects)
            obj.preTick();

        //step physics using a fixed timestep
        physics.tick(1f / Client.TPS);

        for (TickableObject obj : tickableObjects)
            obj.tick();
    }

    public int render(MatrixStack matrices, float delta) {
        int i = 0;
        for (RenderableObject renderableObject : renderableObjects)
            if (renderableObject.render(matrices, client.camera, delta))
                i++;

        return i;
    }

    public void addWorldObject(WorldObject object) {
        worldObjects.add(object);
        if (object instanceof TickableObject tickable)
            tickableObjects.add(tickable);
        if (object instanceof RenderableObject renderable)
            renderableObjects.add(renderable);
        object.setWorld(this);
    }

    public PhysicsSystem getPhysics() {
        return physics;
    }

    public int getNextId() {
        return nextId++;
    }

    /** Dispose ODE resources and clear objects. */
    public void close() {
        physics.free();
        worldObjects.clear();
        tickableObjects.clear();
        renderableObjects.clear();
    }
}
