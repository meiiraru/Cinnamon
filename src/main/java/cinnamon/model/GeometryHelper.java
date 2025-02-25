package cinnamon.model;

import cinnamon.render.MatrixStack;
import cinnamon.utils.Maths;
import cinnamon.utils.Rotation;
import org.joml.Vector2f;
import org.joml.Vector3f;

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

    public static Vertex[] circle(MatrixStack matrices, float x, float y, float radius, int sides, int color) {
        return circle(matrices, x, y, radius, 1f, sides, color);
    }

    public static Vertex[] circle(MatrixStack matrices, float x, float y, float radius, float progress, int sides, int color) {
        //number of vertices
        int vertexCount = (int) Math.ceil(Math.max(sides, 3) * progress);
        if (vertexCount <= 0)
            return new Vertex[0];

        //90 degrees offset
        float f = (float) Math.toRadians(-90f);
        //maximum allowed angle
        float max = (float) Math.toRadians(360f * progress) + f;
        //angle per step
        float aStep = (float) Math.toRadians(360f / Math.max(sides, 3));

        //center vertex
        Vertex[] vertices = new Vertex[vertexCount + 2];
        vertices[0] = Vertex.of(x, y, 0).color(color).mul(matrices);

        for (int i = 1; i < vertices.length; i++) {
            //pos
            float angle = aStep * (i - 1) + f;
            float x1 = x + (float) Math.cos(angle) * radius;
            float y1 = y + (float) Math.sin(angle) * radius;

            //over the max
            if (angle > max) {
                float prevAngle = aStep * (i - 2) + f;
                float t = (max - prevAngle) / (angle - prevAngle);
                x1 = Maths.lerp(x + (float) Math.cos(prevAngle) * radius, x1, t);
                y1 = Maths.lerp(y + (float) Math.sin(prevAngle) * radius, y1, t);
            }

            vertices[vertices.length - i] = Vertex.of(x1, y1, 0).color(color).mul(matrices);
        }

        //return
        return vertices;
    }

    public static Vertex[] progressSquare(MatrixStack matrices, float x, float y, float radius, int color) {
        return progressSquare(matrices, x, y, radius, 1f, color);
    }

    public static Vertex[] progressSquare(MatrixStack matrices, float x, float y, float radius, float progress, int color) {
        //no progress, no vertices
        if (progress <= 0f)
            return new Vertex[0];

        //generate mesh
        float x0 = x - radius;
        float y0 = y - radius;
        float x1 = x + radius;
        float y1 = y + radius;

        //x y u v
        float[][] mesh = new float[][]{
                {x , y0, 0.5f, 0f  }, //top center
                {x1, y0, 1f  , 0f  }, //top right
                {x1, y , 1f  , 0.5f}, //center right
                {x1, y1, 1f  , 1f  }, //bottom right
                {x , y1, 0.5f, 1f  }, //bottom center
                {x0, y1, 0f  , 1f  }, //bottom left
                {x0, y , 0f  , 0.5f}, //center left
                {x0, y0, 0f  , 0f  }, //top left
        };

        float max = mesh.length * progress;

        //top center and the actual center are always present
        int vertexCount = (int) Math.ceil(max) + 2;
        Vertex[] vertices = new Vertex[vertexCount];

        //center vertices
        int j = vertexCount - 1;
        vertices[0] = Vertex.of(x, y, 0).uv(0.5f, 0.5f).color(color).mul(matrices);
        vertices[j] = Vertex.of(x, y0, 0).uv(0.5f, 0f).color(color).mul(matrices);

        //generate the other vertices
        for (int i = 1; i < j; i++) {
            float xx, yy, u, v;

            //if were over the max, interpolate the position with the previous one
            if (i > max) {
                float[] prev = mesh[(i - 1) % mesh.length];
                float[] curr = mesh[i % mesh.length];
                float t = max - (i - 1);
                xx = Maths.lerp(prev[0], curr[0], t);
                yy = Maths.lerp(prev[1], curr[1], t);
                u  = Maths.lerp(prev[2], curr[2], t);
                v  = Maths.lerp(prev[3], curr[3], t);
            } else {
                //just grab the position as is
                float[] pos = mesh[i % mesh.length];
                xx = pos[0]; yy = pos[1]; u = pos[2]; v = pos[3];
            }

            //backwards index - counter-clockwise
            vertices[j - i] = Vertex.of(xx, yy, 0).uv(u, v).color(color).mul(matrices);
        }

        //return
        return vertices;
    }

    public static Vertex[] rectangle(MatrixStack matrices, float x0, float y0, float x1, float y1, int color) {
        return rectangle(matrices, x0, y0, x1, y1, 0, color);
    }

    public static Vertex[] rectangle(MatrixStack matrices, float x0, float y0, float x1, float y1, float z, int color) {
        return rectangle(matrices, x0, y0, x1, y1, z, color, color, color, color);
    }

    public static Vertex[] rectangle(MatrixStack matrices, float x0, float y0, float x1, float y1, float z, int topLeftColor, int topRightColor, int bottomLeftColor, int bottomRightColor) {
        return new Vertex[]{
                Vertex.of(x0, y1, z).color(bottomLeftColor).mul(matrices),
                Vertex.of(x1, y1, z).color(bottomRightColor).mul(matrices),
                Vertex.of(x1, y0, z).color(topRightColor).mul(matrices),
                Vertex.of(x0, y0, z).color(topLeftColor).mul(matrices),
        };
    }

    public static Vertex[] line(MatrixStack matrices, float x0, float y0, float x1, float y1, float size, int color) {
        matrices.push();

        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);

        matrices.translate(x0, y0, 0);
        matrices.rotate(Rotation.Z.rotationDeg(Maths.dirToRot(dx, dy)));

        size *= 0.5f;
        Vertex[] ret = rectangle(matrices, 0, -size, len, size, color);

        matrices.pop();

        return ret;
    }

    public static Vertex[][] line(MatrixStack matrices, float x0, float y0, float z0, float x1, float y1, float z1, float width, int color) {
        //grab direction
        Vector3f diff = new Vector3f(x1 - x0, y1 - y0, z1 - z0);
        Vector3f dir = diff.normalize(new Vector3f());

        //rotate matrices to align with direction
        matrices.push();
        matrices.translate(x0, y0, z0);

        Vector2f rot = Maths.dirToRot(dir);
        matrices.rotate(Rotation.Y.rotationDeg(-rot.y + 90));
        matrices.rotate(Rotation.Z.rotationDeg(-rot.x));

        //create line as cube
        float w = width * 0.5f;
        Vertex[][] line = cube(matrices, 0, -w, -w, diff.length(), w, w, color);

        //return
        matrices.pop();
        return line;
    }

    public static Vertex[][] cube(MatrixStack matrices, float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        Vertex[][] cube = new Vertex[6][4];
        Vertex
                v0 = Vertex.of(x0, y1, z0).color(color).normal(-1,  1, -1).mul(matrices),
                v1 = Vertex.of(x1, y1, z0).color(color).normal( 1,  1, -1).mul(matrices),
                v2 = Vertex.of(x1, y0, z0).color(color).normal( 1, -1, -1).mul(matrices),
                v3 = Vertex.of(x0, y0, z0).color(color).normal(-1, -1, -1).mul(matrices),
                v4 = Vertex.of(x1, y1, z1).color(color).normal( 1,  1,  1).mul(matrices),
                v5 = Vertex.of(x0, y1, z1).color(color).normal(-1,  1,  1).mul(matrices),
                v6 = Vertex.of(x0, y0, z1).color(color).normal(-1, -1,  1).mul(matrices),
                v7 = Vertex.of(x1, y0, z1).color(color).normal( 1, -1,  1).mul(matrices);

        //north
        cube[0] = new Vertex[]{v0, v1, v2, v3};
        //west
        cube[1] = new Vertex[]{v5, v0, v3, v6};
        //south
        cube[2] = new Vertex[]{v4, v5, v6, v7};
        //east
        cube[3] = new Vertex[]{v1, v4, v7, v2};
        //up
        cube[4] = new Vertex[]{v1, v0, v5, v4};
        //down
        cube[5] = new Vertex[]{v7, v6, v3, v2};

        return cube;
    }
}
