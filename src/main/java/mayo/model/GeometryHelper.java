package mayo.model;

import org.joml.Matrix4f;

public class GeometryHelper {

    public static Vertex[] quad(float x, float y, float width, float height) {
        return quad(1, 1, x, y, width, height);
    }

    public static Vertex[] quad(int hFrames, int vFrames, float x, float y, float width, float height) {
        return quad(x, y, width, height, 0, 0, 1, 1, hFrames, vFrames);
    }

    public static Vertex[] quad(float x, float y, float width, float height, float u, float v, int regionW, int regionH, int textureW, int textureH) {
        float u0 = u / textureW;
        float v0 = v / textureH;
        float u1 = (u + regionW) / (float) textureW;
        float v1 = (v + regionH) / (float) textureH;

        return quad(x, y, width, height, 0f, u0, u1, v0, v1);
    }

    public static Vertex[] quad(float x0, float y0, float width, float height, float z, float u0, float u1, float v0, float v1) {
        float x1 = x0 + width;
        float y1 = y0 + height;

        return new Vertex[]{
                Vertex.of(x0, y1, z).uv(u0, v1),
                Vertex.of(x1, y1, z).uv(u1, v1),
                Vertex.of(x1, y0, z).uv(u1, v0),
                Vertex.of(x0, y0, z).uv(u0, v0),
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
}
