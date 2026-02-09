package cinnamon.model;

import cinnamon.render.MatrixStack;
import cinnamon.utils.Maths;
import cinnamon.utils.Rotation;
import org.joml.Math;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class GeometryHelper {

    // * 2D shapes * //

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
                new Vertex().pos(x0, y1, z).uv(u0, v1).mul(matrices),
                new Vertex().pos(x1, y1, z).uv(u1, v1).mul(matrices),
                new Vertex().pos(x1, y0, z).uv(u1, v0).mul(matrices),
                new Vertex().pos(x0, y0, z).uv(u0, v0).mul(matrices),
        };
    }

    public static Vertex[] invQuad(MatrixStack matrices, float x, float y, float width, float height) {
        float x1 = x + width;
        float y1 = y + height;

        return new Vertex[]{
                new Vertex().pos(x , y1, 0).uv(0f, 1f).mul(matrices),
                new Vertex().pos(x , y , 0).uv(0f, 0f).mul(matrices),
                new Vertex().pos(x1, y , 0).uv(1f, 0f).mul(matrices),
                new Vertex().pos(x1, y1, 0).uv(1f, 1f).mul(matrices),
        };
    }

    public static Vertex[] circle(MatrixStack matrices, float x, float y, float radius, int sides, int color) {
        return circle(matrices, x, y, radius, 1f, sides, color);
    }

    public static Vertex[] circle(MatrixStack matrices, float x, float y, float radius, float progress, int sides, int color) {
        if (progress <= 0)
            return new Vertex[0];

        //90 degrees offset
        float f = Math.toRadians(-90f);
        //maximum allowed angle
        float max = Math.toRadians(360f * progress) + f;
        //angle per step
        float aStep = Math.toRadians(360f / Math.max(sides, 3));

        //center vertex
        int vertexCount = (int) Math.ceil(Math.max(sides, 3) * progress);
        Vertex[] vertices = new Vertex[vertexCount + 2];
        vertices[0] = new Vertex().pos(x, y, 0).color(color).mul(matrices);

        for (int i = 1; i < vertices.length; i++) {
            //pos
            float angle = aStep * (i - 1) + f;
            float x1 = Math.cos(angle) * radius;
            float y1 = Math.sin(angle) * radius;

            //over the max
            if (angle > max) {
                float prevAngle = aStep * (i - 2) + f;
                float t = (max - prevAngle) / (angle - prevAngle);
                x1 = Math.lerp(Math.cos(prevAngle) * radius, x1, t);
                y1 = Math.lerp(Math.sin(prevAngle) * radius, y1, t);
            }

            vertices[vertices.length - i] = new Vertex().pos(x + x1, y + y1, 0).color(color).mul(matrices);
        }

        //return
        return vertices;
    }

    public static Vertex[][] arc(MatrixStack matrices, float x, float y, float radius, float start, float end, float thickness, int sides, int color) {
        if (start >= end)
            return new Vertex[0][];

        //90 degrees offset
        float f = Math.toRadians(-90f);
        //minimum allowed angle
        float min = Math.toRadians(360f * start) + f;
        //maximum allowed angle
        float max = Math.toRadians(360f * end) + f;
        //angle per step
        float aStep = Math.toRadians(360f / Math.max(sides, 3));

        //find the first and last valid polygon steps
        float firstStep = (Math.floor((min - f) / aStep) * aStep) + f;
        float lastStep = (Math.ceil((max - f) / aStep) * aStep) + f;

        //thickness radius
        float iRadius = radius - thickness;

        //vertices
        Vertex[][] vertices = new Vertex[(int) Math.ceil((lastStep - firstStep) / aStep)][4];

        for (int i = 0; i < vertices.length; i++) {
            float angle1 = firstStep + aStep * i;
            float angle2 = angle1 + aStep;

            float cos1 = Math.cos(angle1);
            float sin1 = Math.sin(angle1);
            float cos2 = Math.cos(angle2);
            float sin2 = Math.sin(angle2);

            //outer pos
            float x1 = cos1 * radius;
            float y1 = sin1 * radius;
            //inner pos
            float x3 = cos1 * iRadius;
            float y3 = sin1 * iRadius;

            //under the min
            if (angle1 < min) {
                float t = (min - angle1) / (angle2 - angle1);
                x1 = Math.lerp(x1, cos2 * radius, t);
                y1 = Math.lerp(y1, sin2 * radius, t);
                x3 = Math.lerp(x3, cos2 * iRadius, t);
                y3 = Math.lerp(y3, sin2 * iRadius, t);
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
                x2 = Math.lerp(cos1 * radius, x2, t);
                y2 = Math.lerp(sin1 * radius, y2, t);
                x4 = Math.lerp(cos1 * iRadius, x4, t);
                y4 = Math.lerp(sin1 * iRadius, y4, t);
            }

            //create the quad
            vertices[i] = new Vertex[]{
                    new Vertex().pos(x + x1, y + y1, 0).color(color).mul(matrices),
                    new Vertex().pos(x + x3, y + y3, 0).color(color).mul(matrices),
                    new Vertex().pos(x + x4, y + y4, 0).color(color).mul(matrices),
                    new Vertex().pos(x + x2, y + y2, 0).color(color).mul(matrices),
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
        vertices[0] = new Vertex().pos(x, y, 0).uv(0.5f, 0.5f).color(color).mul(matrices);
        vertices[j] = new Vertex().pos(x, y0, 0).uv(0.5f, 0f).color(color).mul(matrices);

        //generate the other vertices
        for (int i = 1; i < j; i++) {
            float xx, yy, u, v;

            //if were over the max, interpolate the position with the previous one
            if (i > max) {
                float[] prev = mesh[(i - 1) % mesh.length];
                float[] curr = mesh[i % mesh.length];
                float t = max - (i - 1);
                xx = Math.lerp(prev[0], curr[0], t);
                yy = Math.lerp(prev[1], curr[1], t);
                u  = Math.lerp(prev[2], curr[2], t);
                v  = Math.lerp(prev[3], curr[3], t);
            } else {
                //just grab the position as is
                float[] pos = mesh[i % mesh.length];
                xx = pos[0]; yy = pos[1]; u = pos[2]; v = pos[3];
            }

            //backwards index
            vertices[j - i] = new Vertex().pos(xx, yy, 0).uv(u, v).color(color).mul(matrices);
        }

        //return
        return vertices;
    }

    public static Vertex[] line(MatrixStack matrices, float x0, float y0, float x1, float y1, float size, int color) {
        matrices.pushMatrix();

        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = Math.sqrt(dx * dx + dy * dy);

        matrices.translate(x0, y0, 0);
        matrices.rotate(Rotation.Z.rotationDeg(Maths.dirToRot(dx, dy)));

        size *= 0.5f;
        Vertex[] ret = rectangle(matrices, 0, -size, len, size, color);

        matrices.popMatrix();

        return ret;
    }

    public static Vertex[] rectangle(MatrixStack matrices, float x0, float y0, float x1, float y1, int color) {
        return rectangle(matrices, x0, y0, x1, y1, 0, color);
    }

    public static Vertex[] rectangle(MatrixStack matrices, float x0, float y0, float x1, float y1, float z, int color) {
        return rectangle(matrices, x0, y0, x1, y1, z, color, color, color, color);
    }

    public static Vertex[] rectangle(MatrixStack matrices, float x0, float y0, float x1, float y1, float z, int topLeftColor, int topRightColor, int bottomLeftColor, int bottomRightColor) {
        return new Vertex[]{
                new Vertex().pos(x0, y1, z).color(bottomLeftColor).mul(matrices),
                new Vertex().pos(x1, y1, z).color(bottomRightColor).mul(matrices),
                new Vertex().pos(x1, y0, z).color(topRightColor).mul(matrices),
                new Vertex().pos(x0, y0, z).color(topLeftColor).mul(matrices),
        };
    }


    // * 3D shapes * //


    public static Vertex[][] plane(MatrixStack matrices, float x0, float y, float z0, float x1, float z1, int cellsX, int cellsZ, int color) {
        //invalid cells
        if (cellsX <= 0 || cellsZ <= 0)
            return new Vertex[0][];

        //prepare cell sizes
        float width = x1 - x0;
        float depth = z1 - z0;
        float cellWidth = width / cellsX;
        float cellDepth = depth / cellsZ;

        //generate vertices
        Vertex[] vertices = new Vertex[(cellsX + 1) * (cellsZ + 1)];
        int index = 0;
        for (int i = 0; i <= cellsX; i++) {
            for (int j = 0; j <= cellsZ; j++) {
                float xAdv = i * cellWidth;
                float zAdv = j * cellDepth;
                vertices[index++] = new Vertex().pos(x0 + xAdv, y, z0 + zAdv).uv(xAdv, zAdv).normal(0, 1, 0).color(color).mul(matrices);
            }
        }

        //fill quads
        Vertex[][] quads = new Vertex[cellsX * cellsZ][];
        index = 0;
        for (int i = 0; i < cellsX; i++) {
            for (int j = 0; j < cellsZ; j++) {
                int v0 = i * (cellsZ + 1) + j;
                int v1 = v0 + 1;
                int v2 = v1 + (cellsZ + 1);
                int v3 = v0 + (cellsZ + 1);

                quads[index++] = new Vertex[]{vertices[v0], vertices[v1], vertices[v2], vertices[v3]};
            }
        }

        return quads;
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

        //create line as box
        float w = width * 0.5f;
        Vertex[][] line = box(matrices, 0, -w, -w, diff.length(), w, w, color);

        //return
        matrices.popMatrix();
        return line;
    }

    public static Vertex[][] box(MatrixStack matrices, float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        float w = x1 - x0; float h = y1 - y0; float d = z1 - z0;

        Vertex[][] box = new Vertex[6][];

        //north
        box[0] = new Vertex[]{
                new Vertex().pos(x0, y1, z0).normal(0, 0, -1).uv(w, 0).color(color).mul(matrices),
                new Vertex().pos(x1, y1, z0).normal(0, 0, -1).uv(0, 0).color(color).mul(matrices),
                new Vertex().pos(x1, y0, z0).normal(0, 0, -1).uv(0, h).color(color).mul(matrices),
                new Vertex().pos(x0, y0, z0).normal(0, 0, -1).uv(w, h).color(color).mul(matrices),
        };
        //west
        box[1] = new Vertex[]{
                new Vertex().pos(x0, y1, z1).normal(-1, 0, 0).uv(d, 0).color(color).mul(matrices),
                new Vertex().pos(x0, y1, z0).normal(-1, 0, 0).uv(0, 0).color(color).mul(matrices),
                new Vertex().pos(x0, y0, z0).normal(-1, 0, 0).uv(0, h).color(color).mul(matrices),
                new Vertex().pos(x0, y0, z1).normal(-1, 0, 0).uv(d, h).color(color).mul(matrices),
        };
        //south
        box[2] = new Vertex[]{
                new Vertex().pos(x1, y1, z1).normal(0, 0, 1).uv(w, 0).color(color).mul(matrices),
                new Vertex().pos(x0, y1, z1).normal(0, 0, 1).uv(0, 0).color(color).mul(matrices),
                new Vertex().pos(x0, y0, z1).normal(0, 0, 1).uv(0, h).color(color).mul(matrices),
                new Vertex().pos(x1, y0, z1).normal(0, 0, 1).uv(w, h).color(color).mul(matrices),
        };
        //east
        box[3] = new Vertex[]{
                new Vertex().pos(x1, y1, z0).normal(1, 0, 0).uv(d, 0).color(color).mul(matrices),
                new Vertex().pos(x1, y1, z1).normal(1, 0, 0).uv(0, 0).color(color).mul(matrices),
                new Vertex().pos(x1, y0, z1).normal(1, 0, 0).uv(0, h).color(color).mul(matrices),
                new Vertex().pos(x1, y0, z0).normal(1, 0, 0).uv(d, h).color(color).mul(matrices),
        };
        //up
        box[4] = new Vertex[]{
                new Vertex().pos(x1, y1, z0).normal(0, 1, 0).uv(w, 0).color(color).mul(matrices),
                new Vertex().pos(x0, y1, z0).normal(0, 1, 0).uv(0, 0).color(color).mul(matrices),
                new Vertex().pos(x0, y1, z1).normal(0, 1, 0).uv(0, d).color(color).mul(matrices),
                new Vertex().pos(x1, y1, z1).normal(0, 1, 0).uv(w, d).color(color).mul(matrices),
        };
        //down
        box[5] = new Vertex[]{
                new Vertex().pos(x1, y0, z1).normal(0, -1, 0).uv(0, d).color(color).mul(matrices),
                new Vertex().pos(x0, y0, z1).normal(0, -1, 0).uv(w, d).color(color).mul(matrices),
                new Vertex().pos(x0, y0, z0).normal(0, -1, 0).uv(w, 0).color(color).mul(matrices),
                new Vertex().pos(x1, y0, z0).normal(0, -1, 0).uv(0, 0).color(color).mul(matrices),
        };

        return box;
    }

    public static Vertex[][] pyramid(MatrixStack matrices, float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        return pyramid(matrices, x0, y0, z0, x1, y1, z1, true, color);
    }

    public static Vertex[][] pyramid(MatrixStack matrices, float x0, float y0, float z0, float x1, float y1, float z1, boolean base, int color) {
        float w = x1 - x0; float h = y1 - y0; float d = z1 - z0;

        float cw = w * 0.5f; float cd = d * 0.5f;
        float cx = x0 + cw;  float cz = z0 + cd;
        float n = 2 * h;

        //faces
        int i = 0;
        Vertex[][] pyramid = new Vertex[base ? 5 : 4][];

        //base
        if (base) {
            pyramid[i++] = new Vertex[]{
                    new Vertex().pos(x0, y0, z0).normal(0, -1, 0).uv(w, 0).color(color).mul(matrices),
                    new Vertex().pos(x1, y0, z0).normal(0, -1, 0).uv(0, 0).color(color).mul(matrices),
                    new Vertex().pos(x1, y0, z1).normal(0, -1, 0).uv(0, d).color(color).mul(matrices),
                    new Vertex().pos(x0, y0, z1).normal(0, -1, 0).uv(w, d).color(color).mul(matrices),
            };
        }

        //sides
        //north
        pyramid[i++] = new Vertex[]{
                new Vertex().pos(x0, y0, z0).normal(0, d, -n).uv(w, h).color(color).mul(matrices),
                new Vertex().pos(cx, y1, cz).normal(0, d, -n).uv(cw, 0).color(color).mul(matrices),
                new Vertex().pos(x1, y0, z0).normal(0, d, -n).uv(0, h).color(color).mul(matrices),
        };
        //west
        pyramid[i++] = new Vertex[]{
                new Vertex().pos(x0, y0, z1).normal(-n, w, 0).uv(d, h).color(color).mul(matrices),
                new Vertex().pos(cx, y1, cz).normal(-n, w, 0).uv(cd, 0).color(color).mul(matrices),
                new Vertex().pos(x0, y0, z0).normal(-n, w, 0).uv(0, h).color(color).mul(matrices),
        };
        //south
        pyramid[i++] = new Vertex[]{
                new Vertex().pos(x1, y0, z1).normal(0, d, n).uv(w, h).color(color).mul(matrices),
                new Vertex().pos(cx, y1, cz).normal(0, d, n).uv(cw, 0).color(color).mul(matrices),
                new Vertex().pos(x0, y0, z1).normal(0, d, n).uv(0, h).color(color).mul(matrices),
        };
        //east
        pyramid[i] = new Vertex[]{
                new Vertex().pos(x1, y0, z0).normal(n, w, 0).uv(d, h).color(color).mul(matrices),
                new Vertex().pos(cx, y1, cz).normal(n, w, 0).uv(cd, 0).color(color).mul(matrices),
                new Vertex().pos(x1, y0, z1).normal(n, w, 0).uv(0, h).color(color).mul(matrices),
        };

        return pyramid;
    }

    public static Vertex[][] cone(MatrixStack matrices, float x, float y, float z, float height, float radius, int sides, int color) {
        return cone(matrices, x, y, z, height, radius, sides, 1f, true, color);
    }

    public static Vertex[][] cone(MatrixStack matrices, float x, float y, float z, float height, float radius, int sides, float progress, boolean base, int color) {
        if (sides < 3)
            return new Vertex[0][];

        //vertices
        Vertex[] sideVerts = new Vertex[sides + 1];
        Vertex[] baseVerts = new Vertex[sides + 1];
        Vertex tip    = new Vertex().pos(x, y + height, z).normal(0, 1, 0).color(color).mul(matrices);
        Vertex center = new Vertex().pos(x, y, z).normal(0, -1, 0).color(color).mul(matrices);

        float angleStep = Math.PI_TIMES_2_f * progress / sides;

        for (int i = 0; i < sides + 1; i++) {
            float theta = angleStep * i;
            float ax = Math.cos(theta); float az = Math.sin(theta);
            float rx = ax * radius;     float rz = az * radius;
            float vx = x + rx;          float vz = z + rz;

            sideVerts[i] = new Vertex().pos(vx, y, vz).normal(ax, 0, az).uv(-theta * radius, height).color(color).mul(matrices);
            if (base)
                baseVerts[i] = new Vertex().pos(vx, y, vz).normal(0, -1, 0).uv(-rx, rz).color(color).mul(matrices);
        }

        //faces
        int extra = progress < 1f && base ? 2 : 0;
        Vertex[][] cone = new Vertex[sides * (base ? 2 : 1) + extra][];

        for (int i = 0; i < sides; i++) {
            int next = i + 1;
            float uTip = (sideVerts[i].getUV().x + sideVerts[next].getUV().x) / 2f;

            cone[i] = new Vertex[]{
                    sideVerts[i],
                    tip.duplicate().uv(uTip, 0),
                    sideVerts[next],
            };

            if (base) {
                cone[i + sides] = new Vertex[]{
                        center,
                        baseVerts[i],
                        baseVerts[next],
                };
            }
        }

        if (extra > 0) {
            Vector3f normal = new Vector3f(0, 0, -1).mul(matrices.peek().normal());
            cone[sides * 2] = new Vertex[]{
                    tip.duplicate().normal(normal).uv(0, 0),
                    sideVerts[0].duplicate().normal(normal).uv(-radius, height),
                    center.duplicate().normal(normal).uv(0, height),
            };
            normal.set(0, 0, 1).rotateY(-angleStep * sides).mul(matrices.peek().normal());
            cone[sides * 2 + 1] = new Vertex[]{
                    tip.duplicate().normal(normal).uv(0, 0),
                    center.duplicate().normal(normal).uv(0, height),
                    sideVerts[sides].duplicate().normal(normal).uv(radius, height),
            };
        }

        return cone;
    }

    public static Vertex[][] cylinder(MatrixStack matrices, float x, float y, float z, float height, float radius, int sides, int color) {
        return cylinder(matrices, x, y, z, height, radius, radius, sides, 1f, true, color);
    }

    public static Vertex[][] cylinder(MatrixStack matrices, float x, float y, float z, float height, float radiusTop, float radiusBottom, int sides, float progress, boolean cap, int color) {
        if (sides < 3)
            return new Vertex[0][];

        //vertices
        int circleLen = sides + 1;
        Vertex[] sideBot = new Vertex[circleLen];
        Vertex[] sideTop = new Vertex[circleLen];
        Vertex[] baseBot = new Vertex[circleLen];
        Vertex[] baseTop = new Vertex[circleLen];

        float yTop = y + height;
        float angleStep = Math.PI_TIMES_2_f * progress / sides;

        Vertex centerTop = new Vertex().pos(x, yTop, z).uv(0, 0).normal(0, 1, 0).color(color).mul(matrices);
        Vertex centerBot = new Vertex().pos(x, y, z).uv(0, 0).normal(0, -1, 0).color(color).mul(matrices);

        for (int i = 0; i < circleLen; i++) {
            float theta = angleStep * i;
            float x1 = Math.cos(theta); float z1 = Math.sin(theta);

            float cxt = x1 * radiusTop;    float czt = z1 * radiusTop;
            float cxb = x1 * radiusBottom; float czb = z1 * radiusBottom;

            float xt = x + cxt; float zt = z + czt;
            float xb = x + cxb; float zb = z + czb;

            float ny = radiusBottom - radiusTop;
            float u = -theta * radiusBottom;

            sideTop[i] = new Vertex().pos(xt, yTop, zt).normal(x1, ny, z1).uv(u, 0).color(color).mul(matrices);
            sideBot[i] = new Vertex().pos(xb, y, zb).normal(x1, ny, z1).uv(u, height).color(color).mul(matrices);

            if (cap) {
                baseTop[i] = new Vertex().pos(xt, yTop, zt).normal(0, 1, 0).uv(cxt, czt).color(color).mul(matrices);
                baseBot[i] = new Vertex().pos(xb, y, zb).normal(0, -1, 0).uv(-cxb, czb).color(color).mul(matrices);
            }
        }

        //faces
        int extra = progress < 1f && cap ? 2 : 0;
        Vertex[][] cylinder = new Vertex[sides * (cap ? 3 : 1) + extra][];

        for (int i = 0; i < sides; i++) {
            int next = i + 1;
            cylinder[i] = new Vertex[]{sideTop[i], sideTop[next], sideBot[next], sideBot[i]};
            if (cap) {
                cylinder[i + sides]     = new Vertex[]{centerTop, baseTop[next], baseTop[i]};
                cylinder[i + sides * 2] = new Vertex[]{centerBot, baseBot[i], baseBot[next]};
            }
        }

        if (extra > 0) {
            Vector3f normal = new Vector3f(0, 0, -1).mul(matrices.peek().normal());
            cylinder[sides * 3] = new Vertex[]{
                    centerTop .duplicate().normal(normal).uv(0, 0),
                    sideTop[0].duplicate().normal(normal).uv(-radiusTop, 0),
                    sideBot[0].duplicate().normal(normal).uv(-radiusBottom, height),
                    centerBot .duplicate().normal(normal).uv(0, height),
            };
            normal.set(0, 0, 1).rotateY(-angleStep * sides).mul(matrices.peek().normal());
            cylinder[sides * 3 + 1] = new Vertex[]{
                    centerTop.duplicate().normal(normal).uv(0, 0),
                    centerBot.duplicate().normal(normal).uv(0, height),
                    sideBot[sides].duplicate().normal(normal).uv(radiusBottom, height),
                    sideTop[sides].duplicate().normal(normal).uv(radiusTop, 0),
            };
        }

        return cylinder;
    }

    public static Vertex[][] tube(MatrixStack matrices, float x, float y, float z, float height, float radius, float innerRadius, int sides, int color) {
        return tube(matrices, x, y, z, height, radius, innerRadius, sides, 1f, true, color);
    }

    public static Vertex[][] tube(MatrixStack matrices, float x, float y, float z, float height, float radius, float innerRadius, int sides, float progress, boolean cap, int color) {
        if (sides < 3)
            return new Vertex[0][];

        //vertices
        int circleLen = sides + 1;
        Vertex[] outBot = new Vertex[circleLen];
        Vertex[] outTop = new Vertex[circleLen];
        Vertex[] inBot  = new Vertex[circleLen];
        Vertex[] inTop  = new Vertex[circleLen];

        Vertex[] topOut = new Vertex[circleLen];
        Vertex[] topIn  = new Vertex[circleLen];
        Vertex[] botOut = new Vertex[circleLen];
        Vertex[] botIn  = new Vertex[circleLen];

        float yTop = y + height;
        float angleStep = Math.PI_TIMES_2_f * progress / sides;

        for (int i = 0; i < circleLen; i++) {
            float theta = angleStep * i;
            float cos = Math.cos(theta); float sin = Math.sin(theta);

            float cox = cos * radius;      float coz = sin * radius;
            float cix = cos * innerRadius; float ciz = sin * innerRadius;

            float ox = x + cox; float oz = z + coz;
            float ix = x + cix; float iz = z + ciz;

            //side vertices
            outBot[i] = new Vertex().pos(ox, y, oz).normal(cos, 0, sin).uv(-theta * radius, height).color(color).mul(matrices);
            outTop[i] = new Vertex().pos(ox, yTop, oz).normal(cos, 0, sin).uv(-theta * radius, 0).color(color).mul(matrices);

            inBot[i] = new Vertex().pos(ix, y, iz).normal(-cos, 0, -sin).uv(theta * innerRadius, height).color(color).mul(matrices);
            inTop[i] = new Vertex().pos(ix, yTop, iz).normal(-cos, 0, -sin).uv(theta * innerRadius, 0).color(color).mul(matrices);

            //cap vertices
            if (cap) {
                topOut[i] = new Vertex().pos(ox, yTop, oz).normal(0, 1, 0).uv(cox, coz).color(color).mul(matrices);
                topIn[i]  = new Vertex().pos(ix, yTop, iz).normal(0, 1, 0).uv(cix, ciz).color(color).mul(matrices);

                botOut[i] = new Vertex().pos(ox, y, oz).normal(0, -1, 0).uv(-cox, coz).color(color).mul(matrices);
                botIn[i]  = new Vertex().pos(ix, y, iz).normal(0, -1, 0).uv(-cix, ciz).color(color).mul(matrices);
            }
        }

        //faces
        int extra = progress < 1f && cap ? 2 : 0;
        Vertex[][] tube = new Vertex[sides * (cap ? 4 : 2) + extra][];
        for (int i = 0; i < sides; i++) {
            int next = i + 1;

            tube[i] = new Vertex[]{outTop[i], outTop[next], outBot[next], outBot[i]};
            tube[i + sides] = new Vertex[]{inTop[next], inTop[i], inBot[i], inBot[next]};

            if (cap) {
                tube[i + sides * 2] = new Vertex[]{topOut[next], topOut[i], topIn[i], topIn[next]};
                tube[i + sides * 3] = new Vertex[]{botOut[i], botOut[next], botIn[next], botIn[i]};
            }
        }

        if (extra > 0) {
            Vector3f normal = new Vector3f(0, 0, -1).mul(matrices.peek().normal());
            tube[sides * 4] = new Vertex[]{
                    inTop[0].duplicate().normal(normal).uv(innerRadius, 0),
                    outTop[0].duplicate().normal(normal).uv(0, 0),
                    outBot[0].duplicate().normal(normal).uv(0, height),
                    inBot[0].duplicate().normal(normal).uv(innerRadius, height),
            };
            normal.set(0, 0, 1).rotateY(-angleStep * sides).mul(matrices.peek().normal());
            tube[sides * 4 + 1] = new Vertex[]{
                    outTop[sides].duplicate().normal(normal).uv(0, 0),
                    inTop[sides].duplicate().normal(normal).uv(-innerRadius, 0),
                    inBot[sides].duplicate().normal(normal).uv(-innerRadius, height),
                    outBot[sides].duplicate().normal(normal).uv(0, height),
            };
        }

        return tube;
    }

    public static Vertex[][] sphere(MatrixStack matrices, float x, float y, float z, float radius, int sides, int color) {
        return sphere(matrices, x, y, z, radius, sides, sides, 1f, 1f, color);
    }

    public static Vertex[][] sphere(MatrixStack matrices, float x, float y, float z, float radius, int hSides, int vSides, float hProgress, float vProgress, int color) {
        if (hSides < 3 || vSides < 2)
            return new Vertex[0][];

        //vertices
        Vertex[] vertices = new Vertex[(hSides + 1) * (vSides + 1)];

        float phi = Math.PI_TIMES_2_f * hProgress / hSides;
        float theta = Math.PI_f * vProgress / vSides;
        int index = 0;

        for (int j = 0; j <= vSides; j++) {
            float theta1 = theta * j;
            float sinTheta1 = Math.sin(theta1); float cosTheta1 = Math.cos(theta1);
            
            float y1 = radius * cosTheta1;

            for (int i = 0; i <= hSides; i++) {
                float phi1 = phi * i;
                float sinPhi1 = Math.sin(phi1); float cosPhi1 = Math.cos(phi1);

                float x1 = radius * sinTheta1 * cosPhi1;
                float z1 = radius * sinTheta1 * sinPhi1;

                float u = -phi1 * radius;
                float v = theta1 * radius;

                vertices[index++] = new Vertex().pos(x + x1, y + y1, z + z1).normal(x1, y1, z1).uv(u, v).color(color).mul(matrices);
            }
        }

        //faces
        Vertex[][] sphere = new Vertex[hSides * vSides][];
        index = 0;
        for (int j = 0; j < vSides; j++) {
            for (int i = 0; i < hSides; i++) {
                int v0 = j * (hSides + 1) + i;
                int v1 = v0 + 1;
                int v2 = v1 + (hSides + 1);
                int v3 = v0 + (hSides + 1);

                sphere[index++] = new Vertex[]{vertices[v0], vertices[v1], vertices[v2], vertices[v3]};
            }
        }

        return sphere;
    }

    public static Vertex[][] capsule(MatrixStack matrices, float x, float y, float z, float height, float radius, int sides, int color) {
        return capsule(matrices, x, y, z, height, radius, sides, sides, 1f, color);
    }

    public static Vertex[][] capsule(MatrixStack matrices, float x, float y, float z, float height, float radius, int hSides, int vSides, float progress, int color) {
        if (hSides < 3 || vSides < 1)
            return new Vertex[0][];

        //vertices
        int totalRings = 2 * vSides + 2;
        Vertex[] vertices = new Vertex[(hSides + 1) * totalRings];

        float phi = Math.PI_TIMES_2_f * progress / hSides;
        float theta = Math.PI_OVER_2_f / vSides;
        int index = 0;

        //y ring
        for (int j = 0; j < totalRings; j++) {
            float theta1;
            float centerY;
            float v;

            //bottom hemisphere
            if (j <= vSides) {
                theta1 = Math.PI_f - j * theta;
                centerY = y + radius;
                v = j * theta * radius;
            }
            //top hemisphere
            else {
                theta1 = Math.PI_OVER_2_f - (j - (vSides + 1)) * theta;
                centerY = y + height - radius;
                v = (radius * Math.PI_OVER_2_f) + (height - 2 * radius) + ((j - (vSides + 1)) * theta * radius);
            }

            float sinTheta1 = Math.sin(theta1); float cosTheta1 = Math.cos(theta1);
            float ringY = centerY + radius * cosTheta1;

            //ring
            for (int i = 0; i <= hSides; i++) {
                float phi1 = i * phi;
                float sinPhi1 = Math.sin(phi1); float cosPhi1 = Math.cos(phi1);

                float x1 = radius * sinTheta1 * cosPhi1;
                float y1 = radius * cosTheta1;
                float z1 = radius * sinTheta1 * sinPhi1;
                vertices[index++] = new Vertex().pos(x + x1, ringY, z + z1).normal(x1, y1, z1).uv(-phi1 * radius, -v).color(color).mul(matrices);
            }
        }

        //faces
        int totalStrips = totalRings - 1;
        Vertex[][] capsule = new Vertex[hSides * totalStrips][];

        index = 0;
        for (int j = 0; j < totalStrips; j++) {
            for (int i = 0; i < hSides; i++) {
                int v0 = j * (hSides + 1) + i;
                int v1 = v0 + 1;
                int v2 = v1 + (hSides + 1);
                int v3 = v0 + (hSides + 1);
                capsule[index++] = new Vertex[]{vertices[v0], vertices[v3], vertices[v2], vertices[v1]};
            }
        }

        return capsule;
    }

    public static Vertex[][] torus(MatrixStack matrices, float x, float y, float z, float radius, float tubeRadius, int sides, int color) {
        return torus(matrices, x, y, z, radius, tubeRadius, sides, sides, 1f, 1f, color);
    }

    public static Vertex[][] torus(MatrixStack matrices, float x, float y, float z, float radius, float tubeRadius, int sides, int tubeSides, float progress, float tubeProgress, int color) {
        if (sides < 3 || tubeSides < 3)
            return new Vertex[0][];

        //vertices
        Vertex[] vertices = new Vertex[(sides + 1) * (tubeSides + 1)];

        float phi = Math.PI_TIMES_2_f * tubeProgress / tubeSides;
        float theta = Math.PI_TIMES_2_f * progress / sides;
        float torusRad = radius - tubeRadius;
        int index = 0;

        for (int i = 0; i <= sides; i++) {
            float theta1 = i * theta;
            float sinTheta1 = Math.sin(theta1); float cosTheta1 = Math.cos(theta1);

            for (int j = 0; j <= tubeSides; j++) {
                float phi1 = j * phi;
                float sinPhi1 = Math.sin(phi1); float cosPhi1 = Math.cos(phi1);
                float tubePhi = torusRad + cosPhi1 * tubeRadius;

                vertices[index++] = new Vertex()
                        .pos(x + tubePhi * cosTheta1, y + tubeRadius * sinPhi1, z + tubePhi * sinTheta1)
                        .normal(cosTheta1 * cosPhi1, sinPhi1, sinTheta1 * cosPhi1)
                        .uv(-theta1 * radius, -phi1 * tubeRadius)
                        .color(color)
                        .mul(matrices);
            }
        }

        //faces
        Vertex[][] quads = new Vertex[sides * tubeSides][4];
        index = 0;
        for (int i = 0; i < sides; i++) {
            for (int j = 0; j < tubeSides; j++) {
                int v0 = i * (tubeSides + 1) + j;
                int v1 = (i + 1) * (tubeSides + 1) + j;
                int v2 = (i + 1) * (tubeSides + 1) + (j + 1);
                int v3 = i * (tubeSides + 1) + (j + 1);
                quads[index++] = new Vertex[]{vertices[v0], vertices[v3], vertices[v2], vertices[v1]};
            }
        }

        return quads;
    }
}
