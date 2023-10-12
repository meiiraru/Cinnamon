package mayo.model;

import mayo.render.MatrixStack;
import mayo.render.batch.VertexConsumer;

public class GeometryHelper {

    public static Vertex[] quad(MatrixStack matrices, float x, float y, float width, float height) {
        return quad(matrices, x, y, width, height, 1, 1);
    }

    public static Vertex[] quad(MatrixStack matrices, float x, float y, float width, float height, int hFrames, int vFrames) {
        return quad(matrices, x, y, width, height, 0, 0, 1f, 1f, hFrames, vFrames);
    }

    public static Vertex[] quad(MatrixStack matrices, float x, float y, float width, float height, float u, float v, float regionW, float regionH, int textureW, int textureH) {
        float u0 = u / textureW;
        float v0 = v / textureH;
        float u1 = (u + regionW) / (float) textureW;
        float v1 = (v + regionH) / (float) textureH;

        return quad(matrices, x, y, width, height, 0f, u0, u1, v0, v1);
    }

    public static Vertex[] quad(MatrixStack matrices, float x0, float y0, float width, float height, float z, float u0, float u1, float v0, float v1) {
        float x1 = x0 + width;
        float y1 = y0 + height;

        return new Vertex[]{
                Vertex.of(x0, y1, z).uv(u0, v1).mul(matrices),
                Vertex.of(x1, y1, z).uv(u1, v1).mul(matrices),
                Vertex.of(x1, y0, z).uv(u1, v0).mul(matrices),
                Vertex.of(x0, y0, z).uv(u0, v0).mul(matrices),
        };
    }

    public static Vertex[] quad(MatrixStack matrices, float x0, float y0, float z, float width, float height, int color) {
        float x1 = x0 + width;
        float y1 = y0 + height;

        return new Vertex[]{
                Vertex.of(x0, y1, z).color(color).mul(matrices),
                Vertex.of(x1, y1, z).color(color).mul(matrices),
                Vertex.of(x1, y0, z).color(color).mul(matrices),
                Vertex.of(x0, y0, z).color(color).mul(matrices),
        };
    }

    public static void rectangle(VertexConsumer consumer, MatrixStack matrices, float x0, float y0, float x1, float y1, int color) {
        consumer.consume(
                new Vertex[]{
                        Vertex.of(x0, y1, 0).color(color).mul(matrices),
                        Vertex.of(x1, y1, 0).color(color).mul(matrices),
                        Vertex.of(x1, y0, 0).color(color).mul(matrices),
                        Vertex.of(x0, y0, 0).color(color).mul(matrices),
                }, 0
        );
    }

    public static void pushCube(VertexConsumer consumer, MatrixStack matrices, float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        //north
        consumer.consume(
                new Vertex[]{
                        Vertex.of(x0, y1, z0).color(color).normal(0, 0, -1).mul(matrices),
                        Vertex.of(x1, y1, z0).color(color).normal(0, 0, -1).mul(matrices),
                        Vertex.of(x1, y0, z0).color(color).normal(0, 0, -1).mul(matrices),
                        Vertex.of(x0, y0, z0).color(color).normal(0, 0, -1).mul(matrices),
                }, 0
        );
        //west
        consumer.consume(
                new Vertex[]{
                        Vertex.of(x0, y1, z0).color(color).normal(-1, 0, 0).mul(matrices),
                        Vertex.of(x0, y0, z0).color(color).normal(-1, 0, 0).mul(matrices),
                        Vertex.of(x0, y0, z1).color(color).normal(-1, 0, 0).mul(matrices),
                        Vertex.of(x0, y1, z1).color(color).normal(-1, 0, 0).mul(matrices),
                }, 0
        );
        //south
        consumer.consume(
                new Vertex[]{
                        Vertex.of(x1, y1, z1).color(color).normal(0, 0, 1).mul(matrices),
                        Vertex.of(x0, y1, z1).color(color).normal(0, 0, 1).mul(matrices),
                        Vertex.of(x0, y0, z1).color(color).normal(0, 0, 1).mul(matrices),
                        Vertex.of(x1, y0, z1).color(color).normal(0, 0, 1).mul(matrices),
                }, 0
        );
        //east
        consumer.consume(
                new Vertex[]{
                        Vertex.of(x1, y1, z0).color(color).normal(1, 0, 0).mul(matrices),
                        Vertex.of(x1, y1, z1).color(color).normal(1, 0, 0).mul(matrices),
                        Vertex.of(x1, y0, z1).color(color).normal(1, 0, 0).mul(matrices),
                        Vertex.of(x1, y0, z0).color(color).normal(1, 0, 0).mul(matrices),
                }, 0
        );
        //up
        consumer.consume(
                new Vertex[]{
                        Vertex.of(x1, y1, z0).color(color).normal(0, 1, 0).mul(matrices),
                        Vertex.of(x0, y1, z0).color(color).normal(0, 1, 0).mul(matrices),
                        Vertex.of(x0, y1, z1).color(color).normal(0, 1, 0).mul(matrices),
                        Vertex.of(x1, y1, z1).color(color).normal(0, 1, 0).mul(matrices),
                }, 0
        );
        //down
        consumer.consume(
                new Vertex[]{
                        Vertex.of(x0, y0, z0).color(color).normal(0, -1, 0).mul(matrices),
                        Vertex.of(x1, y0, z0).color(color).normal(0, -1, 0).mul(matrices),
                        Vertex.of(x1, y0, z1).color(color).normal(0, -1, 0).mul(matrices),
                        Vertex.of(x0, y0, z1).color(color).normal(0, -1, 0).mul(matrices),
                }, 0
        );
    }
}
