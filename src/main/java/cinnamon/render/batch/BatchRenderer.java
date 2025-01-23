package cinnamon.render.batch;

import cinnamon.model.Vertex;
import cinnamon.render.Camera;
import cinnamon.render.shader.Shader;

import java.util.function.Supplier;

import static cinnamon.Client.LOGGER;

public class BatchRenderer<T extends Batch> {

    private final Batch[] batches = new Batch[32];
    private final Supplier<T> factory;

    public BatchRenderer(Supplier<T> factory) {
        this.factory = factory;
    }

    public void consume(Vertex[] vertices, int textureID) {
        if (vertices == null || vertices.length == 0)
            return;

        int i = 0;
        for (; i < batches.length; i++) {
            if (batches[i] == null)
                break;
            if (batches[i].pushFace(vertices, textureID))
                return;
        }

        if (i >= batches.length)
            return;

        batches[i] = factory.get();
        batches[i].pushFace(vertices, textureID);

        if (i == batches.length - 1)
            LOGGER.warn("Renderer of {} has reached over {} batches!", batches[i].getClass().getSimpleName(), batches.length);
    }

    public int render(Shader shader, Camera camera) {
        Shader old = Shader.activeShader;
        boolean active = false;
        int count = 0;

        for (Batch batch : batches) {
            if (batch == null)
                break;

            if (!batch.hasFace())
                continue;

            if (!active) {
                shader.use();
                if (camera != null)
                    shader.setup(camera);
                active = true;
            }

            count += batch.render(shader);
        }

        if (count > 0)
            old.use();

        return count;
    }

    public void clear() {
        for (Batch batch : batches) {
            if (batch == null)
                break;
            batch.clear();
        }
    }

    public void free() {
        for (int i = 0; i < batches.length; i++) {
            if (batches[i] == null)
                break;

            batches[i].free();
            batches[i] = null;
        }
    }
}
