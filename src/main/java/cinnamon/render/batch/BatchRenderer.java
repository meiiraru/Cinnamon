package cinnamon.render.batch;

import cinnamon.model.Vertex;
import cinnamon.render.shader.Shader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static cinnamon.Client.LOGGER;

public class BatchRenderer<T extends Batch> {

    private final List<Batch> batches = new ArrayList<>();
    private final Supplier<T> factory;

    public BatchRenderer(Supplier<T> factory) {
        this.factory = factory;
    }

    public void consume(Vertex[] vertices, int textureID) {
        for (Batch batch : batches) {
            if (batch.pushFace(vertices, textureID))
                return;
        }

        Batch batch;
        do {
            batch = factory.get();
            batches.add(batch);

            if (batches.size() > 100)
                LOGGER.warn("Renderer of {} has reached over 100 batches!", batch.getClass().getSimpleName());
        } while (!batch.pushFace(vertices, textureID));
    }

    public void render(Shader shader) {
        for (Batch batch : batches)
            batch.render(shader);
    }

    public void free() {
        for (Batch batch : batches)
            batch.free();
        batches.clear();
    }
}
