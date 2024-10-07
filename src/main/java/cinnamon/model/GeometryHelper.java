package cinnamon.model;

import cinnamon.render.MatrixStack;
import cinnamon.utils.Maths;
import cinnamon.utils.Rotation;
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
        return circle(matrices, x, y, radius, 1, sides, color);
    }

    public static Vertex[] circle(MatrixStack matrices, float x, float y, float radius, float completeness, int sides, int color) {
        //number of faces
        int faceCount = (int) Math.ceil(sides * completeness);
        if (faceCount <= 0)
            return new Vertex[0];

        //90 degrees offset
        float f = (float) Math.toRadians(-90f);
        //maximum allowed angle
        float max = (float) Math.toRadians(360 * completeness) + f;
        //angle per step
        float aStep = (float) Math.toRadians(360f / sides);

        //first circle position
        float x1 = x + (float) Math.cos(f) * radius;
        float y1 = y + (float) Math.sin(f) * radius;

        Vertex[] vertices = new Vertex[faceCount * 3];
        for (int i = 0; i < faceCount; i++) {
            //vertex index
            int j = i * 3;

            //center vertex
            vertices[j] = Vertex.of(x, y, 0).color(color).mul(matrices);
            //first pos
            vertices[j + 2] = Vertex.of(x1, y1, 0).color(color).mul(matrices);

            //second pos
            float a = aStep * (i + 1) + f;
            float x2 = x + (float) Math.cos(a) * radius;
            float y2 = y + (float) Math.sin(a) * radius;

            if (a > max) {
                //linear interpolation between positions
                float aOld = a - aStep;
                float t = (max - aOld) / (a - aOld);
                x2 = Maths.lerp(x1, x2, t);
                y2 = Maths.lerp(y1, y2, t);
            }

            x1 = x2; y1 = y2;
            vertices[j + 1] = Vertex.of(x1, y1, 0).color(color).mul(matrices);
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

    public static Vertex[] line(MatrixStack matrices, float x0, float y0, float z0, float x1, float y1, float z1, float width, int color) {
        Vector3f a = new Vector3f(x0, y0, z0);
        Vector3f b = new Vector3f(x1, y1, z1);

        //calculate perpendicular vector
        Vector3f dir = b.sub(a, new Vector3f()).normalize();
        Vector3f p = Maths.rotToDir(0, 90).cross(dir).normalize().mul(width * 0.5f);

        //return
        return new Vertex[]{
                Vertex.of(a.x - p.x, a.y - p.y, a.z - p.z).color(color).mul(matrices),
                Vertex.of(b.x - p.x, b.y - p.y, b.z - p.z).color(color).mul(matrices),
                Vertex.of(b.x + p.x, b.y + p.y, b.z + p.z).color(color).mul(matrices),
                Vertex.of(a.x + p.x, a.y + p.y, a.z + p.z).color(color).mul(matrices)
        };
    }

    public static Vertex[][] cube(MatrixStack matrices, float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        Vertex[][] cube = new Vertex[6][];

        //north
        cube[0] = new Vertex[]{
                Vertex.of(x0, y1, z0).color(color).normal(0, 0, -1).mul(matrices),
                Vertex.of(x1, y1, z0).color(color).normal(0, 0, -1).mul(matrices),
                Vertex.of(x1, y0, z0).color(color).normal(0, 0, -1).mul(matrices),
                Vertex.of(x0, y0, z0).color(color).normal(0, 0, -1).mul(matrices),
        };
        //west
        cube[1] = new Vertex[]{
                Vertex.of(x0, y1, z0).color(color).normal(-1, 0, 0).mul(matrices),
                Vertex.of(x0, y0, z0).color(color).normal(-1, 0, 0).mul(matrices),
                Vertex.of(x0, y0, z1).color(color).normal(-1, 0, 0).mul(matrices),
                Vertex.of(x0, y1, z1).color(color).normal(-1, 0, 0).mul(matrices),
        };
        //south
        cube[2] = new Vertex[]{
                Vertex.of(x1, y1, z1).color(color).normal(0, 0, 1).mul(matrices),
                Vertex.of(x0, y1, z1).color(color).normal(0, 0, 1).mul(matrices),
                Vertex.of(x0, y0, z1).color(color).normal(0, 0, 1).mul(matrices),
                Vertex.of(x1, y0, z1).color(color).normal(0, 0, 1).mul(matrices),
        };
        //east
        cube[3] = new Vertex[]{
                Vertex.of(x1, y1, z0).color(color).normal(1, 0, 0).mul(matrices),
                Vertex.of(x1, y1, z1).color(color).normal(1, 0, 0).mul(matrices),
                Vertex.of(x1, y0, z1).color(color).normal(1, 0, 0).mul(matrices),
                Vertex.of(x1, y0, z0).color(color).normal(1, 0, 0).mul(matrices),
        };
        //up
        cube[4] = new Vertex[]{
                Vertex.of(x1, y1, z0).color(color).normal(0, 1, 0).mul(matrices),
                Vertex.of(x0, y1, z0).color(color).normal(0, 1, 0).mul(matrices),
                Vertex.of(x0, y1, z1).color(color).normal(0, 1, 0).mul(matrices),
                Vertex.of(x1, y1, z1).color(color).normal(0, 1, 0).mul(matrices),
        };
        //down
        cube[5] = new Vertex[]{
                Vertex.of(x0, y0, z0).color(color).normal(0, -1, 0).mul(matrices),
                Vertex.of(x1, y0, z0).color(color).normal(0, -1, 0).mul(matrices),
                Vertex.of(x1, y0, z1).color(color).normal(0, -1, 0).mul(matrices),
                Vertex.of(x0, y0, z1).color(color).normal(0, -1, 0).mul(matrices),
        };

        return cube;
    }
}
