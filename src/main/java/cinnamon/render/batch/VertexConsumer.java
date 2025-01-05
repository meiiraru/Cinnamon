package cinnamon.render.batch;

import cinnamon.model.Vertex;
import cinnamon.render.Camera;
import cinnamon.render.batch.Batch.*;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;
import cinnamon.utils.Resource;

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
        consume(vertices, texture, false, false);
    }

    public void consume(Vertex[] vertices, Resource texture, boolean smooth, boolean mipmap) {
        consume(vertices, Texture.of(texture, smooth, mipmap).getID());
    }

    public void consume(Vertex[] vertices) {
        consume(vertices, -1);
    }

    public void consume(Vertex[][] vertices) {
        for (Vertex[] vertex : vertices)
            consume(vertex);
    }

    public void consume(Vertex[] vertices, int texture) {
        renderer.consume(vertices, texture);
    }

    public void finishBatch(Camera camera) {
        Shader old = Shader.activeShader;
        Shader s = shader.getShader().use();
        s.setup(camera.getProjectionMatrix(), camera.getViewMatrix());
        finishBatch(s);
        old.use();
    }

    public void finishBatch(Shader shader) {
        renderer.render(shader);
    }

    public static void finishAllBatches(Camera camera) {
        for (VertexConsumer consumer : VertexConsumer.values())
            consumer.finishBatch(camera);
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
