package mayo.model;

import mayo.render.batch.VertexConsumer;
import org.joml.Matrix4f;

public class GeometryHelper {

    public static Vertex[] quad(Matrix4f matrix, float x, float y, float width, float height) {
        return quad(matrix, x, y, width, height, 1, 1);
    }

    public static Vertex[] quad(Matrix4f matrix, float x, float y, float width, float height, int hFrames, int vFrames) {
        return quad(matrix, x, y, width, height, 0, 0, 1f, 1f, hFrames, vFrames);
    }

    public static Vertex[] quad(Matrix4f matrix, float x, float y, float width, float height, float u, float v, float regionW, float regionH, int textureW, int textureH) {
        float u0 = u / textureW;
        float v0 = v / textureH;
        float u1 = (u + regionW) / (float) textureW;
        float v1 = (v + regionH) / (float) textureH;

        return quad(matrix, x, y, width, height, 0f, u0, u1, v0, v1);
    }

    public static Vertex[] quad(Matrix4f matrix, float x0, float y0, float width, float height, float z, float u0, float u1, float v0, float v1) {
        float x1 = x0 + width;
        float y1 = y0 + height;

        return new Vertex[]{
                Vertex.of(x0, y1, z).uv(u0, v1).mulPosition(matrix),
                Vertex.of(x1, y1, z).uv(u1, v1).mulPosition(matrix),
                Vertex.of(x1, y0, z).uv(u1, v0).mulPosition(matrix),
                Vertex.of(x0, y0, z).uv(u0, v0).mulPosition(matrix),
        };
    }

    public static Vertex[] quad(Matrix4f matrix, float x0, float y0, float z, float width, float height, int color, int index) {
        float x1 = x0 + width;
        float y1 = y0 + height;

        return new Vertex[]{
                Vertex.of(x0, y1, z).color(color).mulPosition(matrix).index(index),
                Vertex.of(x1, y1, z).color(color).mulPosition(matrix).index(index),
                Vertex.of(x1, y0, z).color(color).mulPosition(matrix).index(index),
                Vertex.of(x0, y0, z).color(color).mulPosition(matrix).index(index),
        };
    }

    public static void pushCube(VertexConsumer consumer, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        //north
        consumer.consume(
                new Vertex[]{
                        Vertex.of(x0, y1, z0).color(color).normal(0, 0, -1).mulPosition(matrix),
                        Vertex.of(x1, y1, z0).color(color).normal(0, 0, -1).mulPosition(matrix),
                        Vertex.of(x1, y0, z0).color(color).normal(0, 0, -1).mulPosition(matrix),
                        Vertex.of(x0, y0, z0).color(color).normal(0, 0, -1).mulPosition(matrix),
                }, 0
        );
        //west
        consumer.consume(
                new Vertex[]{
                        Vertex.of(x0, y1, z0).color(color).normal(-1, 0, 0).mulPosition(matrix),
                        Vertex.of(x0, y0, z0).color(color).normal(-1, 0, 0).mulPosition(matrix),
                        Vertex.of(x0, y0, z1).color(color).normal(-1, 0, 0).mulPosition(matrix),
                        Vertex.of(x0, y1, z1).color(color).normal(-1, 0, 0).mulPosition(matrix),
                }, 0
        );
        //south
        consumer.consume(
                new Vertex[]{
                        Vertex.of(x1, y1, z1).color(color).normal(0, 0, 1).mulPosition(matrix),
                        Vertex.of(x0, y1, z1).color(color).normal(0, 0, 1).mulPosition(matrix),
                        Vertex.of(x0, y0, z1).color(color).normal(0, 0, 1).mulPosition(matrix),
                        Vertex.of(x1, y0, z1).color(color).normal(0, 0, 1).mulPosition(matrix),
                }, 0
        );
        //east
        consumer.consume(
                new Vertex[]{
                        Vertex.of(x1, y1, z0).color(color).normal(1, 0, 0).mulPosition(matrix),
                        Vertex.of(x1, y1, z1).color(color).normal(1, 0, 0).mulPosition(matrix),
                        Vertex.of(x1, y0, z1).color(color).normal(1, 0, 0).mulPosition(matrix),
                        Vertex.of(x1, y0, z0).color(color).normal(1, 0, 0).mulPosition(matrix),
                }, 0
        );
        //up
        consumer.consume(
                new Vertex[]{
                        Vertex.of(x1, y1, z0).color(color).normal(0, 1, 0).mulPosition(matrix),
                        Vertex.of(x0, y1, z0).color(color).normal(0, 1, 0).mulPosition(matrix),
                        Vertex.of(x0, y1, z1).color(color).normal(0, 1, 0).mulPosition(matrix),
                        Vertex.of(x1, y1, z1).color(color).normal(0, 1, 0).mulPosition(matrix),
                }, 0
        );
        //down
        consumer.consume(
                new Vertex[]{
                        Vertex.of(x0, y0, z0).color(color).normal(0, -1, 0).mulPosition(matrix),
                        Vertex.of(x1, y0, z0).color(color).normal(0, -1, 0).mulPosition(matrix),
                        Vertex.of(x1, y0, z1).color(color).normal(0, -1, 0).mulPosition(matrix),
                        Vertex.of(x0, y0, z1).color(color).normal(0, -1, 0).mulPosition(matrix),
                }, 0
        );
    }
}
