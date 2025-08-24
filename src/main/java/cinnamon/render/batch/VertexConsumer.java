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
    MAIN(MainFlatBatch::new, Shaders.MAIN),
    FONT(FontFlatBatch::new, Shaders.FONT),
    WORLD_MAIN(MainBatch::new, Shaders.WORLD_MAIN),
    WORLD_FONT(FontBatch::new, Shaders.WORLD_FONT),
    LINES(LinesBatch::new, Shaders.LINES),
    SCREEN_UV(ScreenSpaceUVBatch::new, Shaders.SCREEN_SPACE_UV);

    private final BatchRenderer<Batch> renderer;
    private final Shaders shader;

    VertexConsumer(Supplier<Batch> factory, Shaders shader) {
        this.renderer = new BatchRenderer<>(factory);
        this.shader = shader;
    }

    public void consume(Vertex[] vertices) {
        consume(vertices, -1);
    }

    public void consume(Vertex[] vertices, Resource texture, Texture.TextureParams... params) {
        consume(vertices, Texture.of(texture, params).getID());
    }

    public void consume(Vertex[] vertices, int texture) {
        renderer.consume(vertices, texture);
    }

    public void consume(Vertex[][] vertices) {
        consume(vertices, -1);
    }

    public void consume(Vertex[][] vertices, Resource texture, Texture.TextureParams... params) {
        consume(vertices, Texture.of(texture, params).getID());
    }

    public void consume(Vertex[][] vertices, int texture) {
        for (Vertex[] vertex : vertices)
            consume(vertex, texture);
    }

    public int finishBatch(Camera camera) {
        return finishBatch(shader.getShader(), camera);
    }

    public int finishBatch(Shader shader, Camera camera) {
        return renderer.render(shader, camera);
    }

    public static int finishAllBatches(Camera camera) {
        int count = 0;
        for (VertexConsumer consumer : VertexConsumer.values())
            count += consumer.finishBatch(camera);
        return count;
    }

    public static int finishAllBatches(Shader shader, Camera camera) {
        int count = 0;
        for (VertexConsumer consumer : VertexConsumer.values())
            count += consumer.finishBatch(shader, camera);
        return count;
    }

    public static void discardBatches() {
        for (VertexConsumer consumer : VertexConsumer.values())
            consumer.renderer.clear();
    }

    public static void freeBatches() {
        for (VertexConsumer consumer : VertexConsumer.values())
            consumer.renderer.free();
    }
}
