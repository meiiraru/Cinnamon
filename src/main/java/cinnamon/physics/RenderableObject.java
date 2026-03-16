package cinnamon.physics;

import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;

public interface RenderableObject {

    boolean render(MatrixStack matrices, Camera camera, float delta);

    boolean shouldRender(MatrixStack matrices, Camera camera);

    default int getRenderPriority() {
        return 0;
    }
}
