package cinnamon.render.batch;

import cinnamon.model.Vertex;
import cinnamon.render.batch.Batch.*;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;
import cinnamon.utils.Resource;
import org.joml.Matrix4f;

import java.util.function.Supplier;

public enum VertexConsumer {
    GUI(GUIBatch::new, Shaders.GUI),
    LINES(LinesBatch::new, Shaders.LINES),
    WORLD_FONT(FontBatch::new, Shaders.WORLD_FONT),
    FONT(FontFlatBatch::new, Shaders.FONT),
    WORLD_MAIN(MainBatch::new, Shaders.WORLD_MAIN),
    MAIN(MainFlatBatch::new, Shaders.MAIN);

    private final BatchRenderer<Batch> renderer;
    private final Shaders shader;

    VertexConsumer(Supplier<Batch> factory, Shaders shader) {
        this.renderer = new BatchRenderer<>(factory);
        this.shader = shader;
    }

    public void consume(Vertex[] vertices, Resource texture) {
        consume(vertices, texture, false);
    }

    public void consume(Vertex[] vertices, Resource texture, boolean smooth) {
        consume(vertices, Texture.of(texture, smooth).getID());
    }

    public void consume(Vertex[] vertices, int texture) {
        renderer.consume(vertices, texture);
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

    public static void freeBatches() {
        for (VertexConsumer consumer : VertexConsumer.values())
            consumer.renderer.free();
    }
}
