package cinnamon.world.entity;

import cinnamon.math.MatrixStack;
import cinnamon.render.Camera;

public interface FeatureRenderer {
    void render(Entity source, Camera camera, MatrixStack matrices, float delta);
}
