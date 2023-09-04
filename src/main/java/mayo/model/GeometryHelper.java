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

    public static Mesh rectangle(float x0, float y0, float width, float height, Texture texture, float u, float v, int textureW, int textureH, int regionW, int regionH) {
        float u0 = u / textureW;
        float v0 = v / textureH;
        float u1 = u0 + regionW / (float) textureW;
        float v1 = v0 + regionH / (float) textureH;

        float x1 = x0 + width;
        float y1 = y0 + height;

        return new Mesh(new Vertex[]{
                new Vertex(x0, y0, 0f, u0, v0),
                new Vertex(x1, y0, 0f, u1, v0),
                new Vertex(x1, y1, 0f, u1, v1),
                new Vertex(x0, y1, 0f, u0, v1),
        }, texture);
    }
}
