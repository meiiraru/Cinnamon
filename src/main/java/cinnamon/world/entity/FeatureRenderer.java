package cinnamon.world.entity;

import cinnamon.render.MatrixStack;

public interface FeatureRenderer {
    void render(Entity source, MatrixStack matrices, float delta);
}
