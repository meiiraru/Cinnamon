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
        if (progress <= 0)
            return new Vertex[0];

        //90 degrees offset
        float f = (float) Math.toRadians(-90f);
        //maximum allowed angle
        float max = (float) Math.toRadians(360f * progress) + f;
        //angle per step
        float aStep = (float) Math.toRadians(360f / Math.max(sides, 3));

        //center vertex
        int vertexCount = (int) Math.ceil(Math.max(sides, 3) * progress);
        Vertex[] vertices = new Vertex[vertexCount + 2];
        vertices[0] = Vertex.of(x, y, 0).color(color).mul(matrices);

        for (int i = 1; i < vertices.length; i++) {
            //pos
            float angle = aStep * (i - 1) + f;
            float x1 = (float) Math.cos(angle) * radius;
            float y1 = (float) Math.sin(angle) * radius;

            //over the max
            if (angle > max) {
                float prevAngle = aStep * (i - 2) + f;
                float t = (max - prevAngle) / (angle - prevAngle);
                x1 = Maths.lerp((float) Math.cos(prevAngle) * radius, x1, t);
                y1 = Maths.lerp((float) Math.sin(prevAngle) * radius, y1, t);
            }

            vertices[vertices.length - i] = Vertex.of(x + x1, y + y1, 0).color(color).mul(matrices);
        }

        //return
        return vertices;
    }

    public static Vertex[][] arc(MatrixStack matrices, float x, float y, float radius, float start, float end, float thickness, int sides, int color) {
        if (start >= end)
            return new Vertex[0][];

        //90 degrees offset
        float f = (float) Math.toRadians(-90f);
        //minimum allowed angle
        float min = (float) Math.toRadians(360f * start) + f;
        //maximum allowed angle
        float max = (float) Math.toRadians(360f * end) + f;
        //angle per step
        float aStep = (float) Math.toRadians(360f / Math.max(sides, 3));

        //find the first and last valid polygon steps
        float firstStep = (float) (Math.floor((min - f) / aStep) * aStep) + f;
        float lastStep = (float) (Math.ceil((max - f) / aStep) * aStep) + f;

        //thickness radius
        float iRadius = radius - thickness;

        //vertices
        Vertex[][] vertices = new Vertex[(int) Math.ceil((lastStep - firstStep) / aStep)][4];

        for (int i = 0; i < vertices.length; i++) {
            float angle1 = firstStep + aStep * i;
            float angle2 = angle1 + aStep;

            float cos1 = (float) Math.cos(angle1);
            float sin1 = (float) Math.sin(angle1);
            float cos2 = (float) Math.cos(angle2);
            float sin2 = (float) Math.sin(angle2);

            //outer pos
            float x1 = cos1 * radius;
            float y1 = sin1 * radius;
            //inner pos
            float x3 = cos1 * iRadius;
            float y3 = sin1 * iRadius;

            //under the min
            if (angle1 < min) {
                float t = (min - angle1) / (angle2 - angle1);
                x1 = Maths.lerp(x1, cos2 * radius, t);
                y1 = Maths.lerp(y1, sin2 * radius, t);
                x3 = Maths.lerp(x3, cos2 * iRadius, t);
                y3 = Maths.lerp(y3, sin2 * iRadius, t);
            }

            //outer pos
            float x2 = cos2 * radius;
            float y2 = sin2 * radius;
            //inner pos
            float x4 = cos2 * iRadius;
            float y4 = sin2 * iRadius;

            //over the max
            if (angle2 > max) {
                float t = (max - angle1) / (angle2 - angle1);
                x2 = Maths.lerp(cos1 * radius, x2, t);
                y2 = Maths.lerp(sin1 * radius, y2, t);
                x4 = Maths.lerp(cos1 * iRadius, x4, t);
                y4 = Maths.lerp(sin1 * iRadius, y4, t);
            }

            //create the quad
            vertices[i] = new Vertex[]{
                    Vertex.of(x + x1, y + y1, 0).color(color).mul(matrices),
                    Vertex.of(x + x3, y + y3, 0).color(color).mul(matrices),
                    Vertex.of(x + x4, y + y4, 0).color(color).mul(matrices),
                    Vertex.of(x + x2, y + y2, 0).color(color).mul(matrices),
            };
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
        final float[][] mesh = new float[][]{
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

            //backwards index
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
        matrices.pushMatrix();

        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);

        matrices.translate(x0, y0, 0);
        matrices.rotate(Rotation.Z.rotationDeg(Maths.dirToRot(dx, dy)));

        size *= 0.5f;
        Vertex[] ret = rectangle(matrices, 0, -size, len, size, color);

        matrices.popMatrix();

        return ret;
    }

    public static Vertex[][] line(MatrixStack matrices, float x0, float y0, float z0, float x1, float y1, float z1, float width, int color) {
        //grab direction
        Vector3f diff = new Vector3f(x1 - x0, y1 - y0, z1 - z0);
        Vector3f dir = diff.normalize(new Vector3f());

        //rotate matrices to align with direction
        matrices.pushMatrix();
        matrices.translate(x0, y0, z0);

        Vector2f rot = Maths.dirToRot(dir);
        matrices.rotate(Rotation.Y.rotationDeg(-rot.y + 90));
        matrices.rotate(Rotation.Z.rotationDeg(-rot.x));

        //create line as cube
        float w = width * 0.5f;
        Vertex[][] line = cube(matrices, 0, -w, -w, diff.length(), w, w, color);

        //return
        matrices.popMatrix();
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
