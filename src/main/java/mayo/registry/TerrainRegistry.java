package mayo.registry;

import com.esotericsoftware.kryo.Kryo;
import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Resource;
import mayo.world.terrain.*;

import java.util.function.Supplier;

public enum TerrainRegistry implements Registry<Terrain> {
    GRASS(Grass.class, Grass::new),
    PILLAR(Pillar.class, Pillar::new),
    TEAPOT(Teapot.class, Teapot::new),
    LIGHT_POLE(LightPole.class, LightPole::new),
    TORII_GATE(ToriiGate.class, ToriiGate::new),
    FENCE(Fence.class, Fence::new),
    TREE(Tree.class, Tree::new),
    BOX(Box.class, Box::new),
    SPHERE(Sphere.class, Sphere::new);

    private static final String MODELS_PATH = "models/terrain/";

    public final Resource resource;
    public final Model model;

    private final Class<? extends Terrain> clazz;
    private final Supplier<Terrain> factory;

    TerrainRegistry(Class<? extends Terrain> clazz, Supplier<Terrain> factory) {
        //fields
        this.clazz = clazz;
        this.factory = factory;

        //model
        String name = name().toLowerCase();
        this.resource = new Resource(MODELS_PATH + name + "/" + name + ".obj");
        this.model = ModelManager.load(this.resource);
    }

    @Override
    public Supplier<Terrain> getFactory() {
        return factory;
    }

    @Override
    public void register(Kryo kryo) {
        kryo.register(clazz);
    }
}
