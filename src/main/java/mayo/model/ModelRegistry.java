package mayo.model;

import mayo.render.Model;
import mayo.utils.Resource;
import mayo.world.terrain.*;

import java.util.function.Supplier;

public class ModelRegistry {

    public enum Living {
        PICKLE(1.5f),
        STRAWBERRY(0.625f),
        TOMATO(0.75f),
        CHERRY(0.625f),
        ICE_CREAM_SANDWICH(1.375f),
        DONUT(1.875f),
        ICE_CREAM(0.75f),
        PANCAKE(1.125f),
        COXINHA(0.75f);

        private static final String MODELS_PATH = "models/entities/living/";

        public final Resource resource;
        public final float eyeHeight;
        public final Model model;

        Living(float eyeHeight) {
            String name = name().toLowerCase();
            this.resource = new Resource(MODELS_PATH + name + "/" + name + ".obj");
            this.eyeHeight = eyeHeight;
            this.model = ModelManager.load(this.resource);
        }

        public static Living random() {
            Living[] models = values();
            return models[(int) (Math.random() * models.length)];
        }
    }

    public enum Terrain {
        GRASS(Grass::new),
        PILLAR(Pillar::new),
        TEAPOT(Teapot::new),
        LIGHT_POLE(LightPole::new),
        TORII_GATE(ToriiGate::new),
        FENCE(Fence::new),
        TREE(Tree::new),
        BOX(Box::new),
        SPHERE(Sphere::new);

        private static final String MODELS_PATH = "models/terrain/";

        public final Resource resource;
        public final Model model;
        private final Supplier<mayo.world.terrain.Terrain> supplier;

        Terrain(Supplier<mayo.world.terrain.Terrain> supplier) {
            String name = name().toLowerCase();
            this.resource = new Resource(MODELS_PATH + name + "/" + name + ".obj");
            this.model = ModelManager.load(this.resource);
            this.supplier = supplier;
        }

        public mayo.world.terrain.Terrain get() {
            return this.supplier.get();
        }
    }
}
