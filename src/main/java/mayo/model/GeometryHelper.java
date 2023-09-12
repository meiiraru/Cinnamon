package mayo.model;

import mayo.render.Texture;

import java.util.List;

public class GeometryHelper {

    public static Mesh quad(float x, float y, float width, float height) {
        return quad(null, x, y, width, height);
    }

    public static Mesh quad(Texture texture, float x, float y, float width, float height) {
        int hf, vf;
        if (texture == null) {
            hf = vf = 1;
        } else {
            hf = texture.gethFrames();
            vf = texture.getvFrames();
        }

        return quad(texture, x, y, width, height, 0, 0, 1, 1, hf, vf);
    }

    public static Mesh quad(Texture texture, float x, float y, float width, float height, float u, float v, int regionW, int regionH, int textureW, int textureH) {
        float u0 = u / textureW;
        float v0 = v / textureH;
        float u1 = (u + regionW) / (float) textureW;
        float v1 = (v + regionH) / (float) textureH;

        return quad(texture, x, y, width, height, 0f, u0, u1, v0, v1);
    }

    public static Mesh quad(Texture texture, float x0, float y0, float width, float height, float z, float u0, float u1, float v0, float v1) {
        float x1 = x0 + width;
        float y1 = y0 + height;

        return new Mesh(new Vertex[]{
                Vertex.of(x0, y0, z).uv(u0, v0),
                Vertex.of(x1, y0, z).uv(u1, v0),
                Vertex.of(x1, y1, z).uv(u1, v1),
                Vertex.of(x0, y1, z).uv(u0, v1),
        }, texture);
    }

    public static List<Vertex> quad(float x0, float y0, float z, float width, float height, int color, int level) {
        float x1 = x0 + width;
        float y1 = y0 + height;

        //temp
        float u0 = 0.8457031f;
        float u1 = 0.8613281f;
        float v0 = 0.037109375f;
        float v1 = 0.052734375f;

        return List.of(
                Vertex.of(x0, y0, z).uv(u0, v0).color(color).index(level),
                Vertex.of(x1, y0, z).uv(u1, v0).color(color).index(level),
                Vertex.of(x1, y1, z).uv(u1, v1).color(color).index(level),
                Vertex.of(x0, y1, z).uv(u0, v1).color(color).index(level)
        );
    }
}
