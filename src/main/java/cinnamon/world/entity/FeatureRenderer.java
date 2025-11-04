package cinnamon.world.entity;

import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;

public interface FeatureRenderer {
    void render(Entity source, Camera camera, MatrixStack matrices, float delta);
}
