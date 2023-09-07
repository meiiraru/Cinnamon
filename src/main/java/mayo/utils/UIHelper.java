package mayo.utils;

import mayo.Client;
import mayo.model.GeometryHelper;
import mayo.model.Renderable;
import mayo.render.BatchRenderer;
import mayo.render.MatrixStack;
import mayo.render.Shaders;
import mayo.render.Texture;

public class UIHelper {

    private static final Texture[] BACKGROUND = new Texture[] {
            new Texture(Client.NAMESPACE, "background/background_0"),
            new Texture(Client.NAMESPACE, "background/background_1"),
            new Texture(Client.NAMESPACE, "background/background_2")
    };

    public static void renderBackground(BatchRenderer renderer, MatrixStack matrices, float delta) {
        Client c = Client.getInstance();
        float speed = 0.125f;
        int textureSize = 64;
        int width = c.scaledWidth + textureSize;
        int height = c.scaledHeight + textureSize;

        for (int i = 0; i < BACKGROUND.length; i++) {
            float x = 0, y = 0;
            float d = (c.ticks + delta) * speed;

            x -= d % textureSize;
            y -= d % textureSize;

            float u1 = (float) width / textureSize;
            float v1 = (float) height / textureSize;

            Renderable r = new Renderable(GeometryHelper.quad(
                    BACKGROUND[i],
                    x, y,
                    width, height,
                    -999,
                    0f, u1,
                    0f, v1
            ));
            renderer.addElement(Shaders.MAIN, matrices, r);

            speed *= 2f;
        }
    }
}
