package mayo.render.batch;

import mayo.model.Vertex;
import mayo.render.batch.Batch.*;
import mayo.render.shader.Shader;
import mayo.render.shader.Shaders;
import org.joml.Matrix4f;

import java.util.function.Supplier;

public enum VertexConsumer {
    GUI(GUIBatch::new, Shaders.GUI),
    LINES(LinesBatch::new, Shaders.LINES),
    FONT(FontBatch::new, Shaders.FONT),
    FONT_FLAT(FontFlatBatch::new, Shaders.FONT_FLAT),
    MAIN(MainBatch::new, Shaders.MAIN),
    MAIN_FLAT(MainFlatBatch::new, Shaders.MAIN_FLAT);

    private final BatchRenderer<Batch> renderer;
    private final Shaders shader;

    VertexConsumer(Supplier<Batch> factory, Shaders shader) {
        this.renderer = new BatchRenderer<>(factory);
        this.shader = shader;
    }

    public void consume(Vertex[] vertices, int texID) {
        renderer.consume(vertices, texID);
    }

    public void finishBatch(Matrix4f proj, Matrix4f view) {
        Shader s = shader.getShader().use();
        s.setup(proj, view);

        renderer.render(s);
    }

    public void finishBatch(Shader shader) {
        renderer.render(shader);
    }

    public static void finishAllBatches(Matrix4f proj, Matrix4f view) {
        for (VertexConsumer consumer : VertexConsumer.values())
            consumer.finishBatch(proj, view);
    }

    public static void finishAllBatches(Shader shader) {
        for (VertexConsumer consumer : VertexConsumer.values())
            consumer.finishBatch(shader);
    }
}
