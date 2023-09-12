package mayo.render;

import mayo.model.Vertex;
import mayo.render.batch.Batch;
import mayo.render.batch.FontBatch;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class FontRenderer {

    private final List<Batch> batches = new ArrayList<>();

    public void consume(Vertex[] vertices, int textureID) {
        for (Batch batch : batches) {
            if (batch.pushFace(vertices, textureID))
                return;
        }

        Batch batch = new FontBatch();
        batches.add(batch);
        if (!batch.pushFace(vertices, textureID))
            throw new RuntimeException("Failed to push text to the renderer");
    }

    public void render(Matrix4f proj, Matrix4f view) {
        for (Batch batch : batches)
            batch.render(proj, view);
    }
}
