package mayo.render.batch;

import mayo.model.Vertex;
import mayo.render.shader.Shader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

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

        Batch batch = factory.get();
        batches.add(batch);
        if (!batch.pushFace(vertices, textureID))
            throw new RuntimeException("Failed to push vertices to a plain new batch");
    }

    public void render(Shader shader) {
        for (Batch batch : batches)
            batch.render(shader);
    }
}
