package mayo.render.batch;

import mayo.model.Vertex;
import org.joml.Matrix4f;

import java.util.function.Supplier;

public enum VertexConsumer {
    FONT(FontBatch::new);

    private final BatchRenderer<Batch> renderer;

    VertexConsumer(Supplier<Batch> factory) {
        this.renderer = new BatchRenderer<>(factory);
    }

    public BatchRenderer<Batch> getRenderer() {
        return renderer;
    }

    public void consume(Vertex[] vertices, int texID) {
        renderer.consume(vertices, texID);
    }

    public void finishBatch(Matrix4f proj, Matrix4f view) {
        renderer.render(proj, view);
    }

    public static void finishAllBatches(Matrix4f proj, Matrix4f view) {
        for (VertexConsumer consumer : VertexConsumer.values()) {
            consumer.finishBatch(proj, view);
        }
    }
}