package mayo.model;

import mayo.render.Texture;

public class GeometryHelper {

    public static Mesh rectangle(float x, float y, float width, float height) {
        return rectangle(x, y, width, height, null);
    }

    public static Mesh rectangle(float x, float y, float width, float height, Texture texture) {
        int hf, vf;
        if (texture == null) {
            hf = vf = 1;
        } else {
            hf = texture.gethFrames();
            vf = texture.getvFrames();
        }

        return rectangle(x, y, width, height, texture, 0, 0, hf, vf, 1, 1);
    }

    public static Mesh rectangle(float x1, float y1, float width, float height, Texture texture, float u, float v, int textureW, int textureH, int regionW, int regionH) {
        float u1 = u / textureW;
        float v1 = v / textureH;
        float u2 = u1 + regionW / (float) textureW;
        float v2 = v1 + regionH / (float) textureH;

        float x2 = x1 + width;
        float y2 = y1 + height;

        return new Mesh(new Vertex[]{
                new Vertex(x1, y2, 0f, u1, v2),
                new Vertex(x2, y2, 0f, u2, v2),
                new Vertex(x2, y1, 0f, u2, v1),
                new Vertex(x1, y1, 0f, u1, v1),
        }, texture);
    }
}
