package cinnamon.model;

import cinnamon.render.MatrixStack;
import cinnamon.utils.Maths;
import cinnamon.utils.Rotation;
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


    // * 3D shapes * //


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

    public static Vertex[] plane(MatrixStack matrices, float x0, float y, float z0, float x1, float z1, int color) {
        return new Vertex[]{
                Vertex.of(x0, y, z0).normal(0, 1, 0).color(color).mul(matrices),
                Vertex.of(x0, y, z1).normal(0, 1, 0).color(color).mul(matrices),
                Vertex.of(x1, y, z1).normal(0, 1, 0).color(color).mul(matrices),
                Vertex.of(x1, y, z0).normal(0, 1, 0).color(color).mul(matrices),
        };
    }

    public static Vertex[][] cube(MatrixStack matrices, float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        Vertex[][] cube = new Vertex[6][4];
        Vertex
                v0 = Vertex.of(x0, y1, z0).normal(-1,  1, -1).color(color).mul(matrices),
                v1 = Vertex.of(x1, y1, z0).normal( 1,  1, -1).color(color).mul(matrices),
                v2 = Vertex.of(x1, y0, z0).normal( 1, -1, -1).color(color).mul(matrices),
                v3 = Vertex.of(x0, y0, z0).normal(-1, -1, -1).color(color).mul(matrices),
                v4 = Vertex.of(x1, y1, z1).normal( 1,  1,  1).color(color).mul(matrices),
                v5 = Vertex.of(x0, y1, z1).normal(-1,  1,  1).color(color).mul(matrices),
                v6 = Vertex.of(x0, y0, z1).normal(-1, -1,  1).color(color).mul(matrices),
                v7 = Vertex.of(x1, y0, z1).normal( 1, -1,  1).color(color).mul(matrices);

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

    public static Vertex[][] pyramid(MatrixStack matrices, float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        //corners
        Vertex v0 = Vertex.of(x0, y0, z0).normal(-1, -1, -1).color(color).mul(matrices);
        Vertex v1 = Vertex.of(x1, y0, z0).normal( 1, -1, -1).color(color).mul(matrices);
        Vertex v2 = Vertex.of(x1, y0, z1).normal( 1, -1,  1).color(color).mul(matrices);
        Vertex v3 = Vertex.of(x0, y0, z1).normal(-1, -1,  1).color(color).mul(matrices);
        //tip
        Vertex tip = Vertex.of(x0 + (x1 - x0) / 2f, y1, z0 + (z1 - z0) / 2f).normal(0, 1, 0).color(color).mul(matrices);

        return new Vertex[][]{
                //base
                {v0, v1, v2, v3},
                //sides
                {v0, tip, v1},
                {v1, tip, v2},
                {v2, tip, v3},
                {v3, tip, v0}
        };
    }

    public static Vertex[][] cone(MatrixStack matrices, float x, float y, float z, float radius, float height, int sides, int color) {
        int vertexCount = Math.max(sides, 3);
        float angleStep = (2f * (float) Math.PI) / vertexCount;

        //prepare constant vertices
        Vertex[][] cone = new Vertex[vertexCount * 2][3];
        Vertex tip = Vertex.of(x, y + height, z).normal(0, 1, 0).color(color).mul(matrices);
        Vertex base = Vertex.of(x, y, z).normal(0, -1, 0).color(color).mul(matrices);

        //generate circle vertices
        for (int i = 0; i < vertexCount; i++) {
            float angle1 = angleStep * i;
            float angle2 = angleStep * (i + 1);

            float x1 = (float) Math.cos(angle1) * radius; float z1 = (float) Math.sin(angle1) * radius;
            float x2 = (float) Math.cos(angle2) * radius; float z2 = (float) Math.sin(angle2) * radius;

            Vertex v1 = Vertex.of(x + x1, y, z + z1).normal(x1, 0, z1).color(color).mul(matrices);
            Vertex v2 = Vertex.of(x + x2, y, z + z2).normal(x2, 0, z2).color(color).mul(matrices);

            cone[i] = new Vertex[]{tip, v2, v1}; //side
            cone[i + vertexCount] = new Vertex[]{base, v1, v2}; //base
        }

        return cone;
    }

    public static Vertex[][] cylinder(MatrixStack matrices, float x, float y, float z, float radius, float height, int sides, int color) {
        int vertexCount = Math.max(sides, 3);
        float angleStep = (2f * (float) Math.PI) / vertexCount;

        //prepare constant vertices
        Vertex[][] cylinder = new Vertex[vertexCount * 3][];
        Vertex top = Vertex.of(x, y + height, z).normal(0, 1, 0).color(color).mul(matrices);
        Vertex bottom = Vertex.of(x, y, z).normal(0, -1, 0).color(color).mul(matrices);

        //generate circle vertices
        for (int i = 0; i < vertexCount; i++) {
            float theta1 = angleStep * i;
            float theta2 = angleStep * (i + 1);

            float x1 = (float) Math.cos(theta1) * radius; float z1 = (float) Math.sin(theta1) * radius;
            float x2 = (float) Math.cos(theta2) * radius; float z2 = (float) Math.sin(theta2) * radius;

            Vertex v1 = Vertex.of(x + x1, y, z + z1).normal(x1, -1, z1).color(color).mul(matrices);
            Vertex v2 = Vertex.of(x + x2, y, z + z2).normal(x2, -1, z2).color(color).mul(matrices);
            Vertex v3 = Vertex.of(x + x2, y + height, z + z2).normal(x2, 1, z2).color(color).mul(matrices);
            Vertex v4 = Vertex.of(x + x1, y + height, z + z1).normal(x1, 1, z1).color(color).mul(matrices);

            cylinder[i] = new Vertex[]{v1, v4, v3, v2}; //side
            cylinder[i + vertexCount] = new Vertex[]{top, v3, v4}; //top cap
            cylinder[i + 2 * vertexCount] = new Vertex[]{bottom, v1, v2}; //bottom cap
        }

        return cylinder;
    }

    public static Vertex[][] sphere(MatrixStack matrices, float x, float y, float z, float radius, int sides, int color) {
        //total quads = stacks * slices
        int slices = Math.max(sides, 3);
        //int stacks = slices;
        Vertex[][] sphere = new Vertex[slices * slices][4];

        int index = 0;
        float step = (float) Math.PI / slices;
        for (int i = 0; i < slices; i++) {
            float theta1 = step * i;
            float theta2 = step * (i + 1);

            float sinTheta1 = (float) Math.sin(theta1); float cosTheta1 = (float) Math.cos(theta1);
            float sinTheta2 = (float) Math.sin(theta2); float cosTheta2 = (float) Math.cos(theta2);

            for (int j = 0; j < slices; j++) {
                float phi1 = step * j * 2f;
                float phi2 = step * (j + 1) * 2f;

                float sinPhi1 = (float) Math.sin(phi1); float cosPhi1 = (float) Math.cos(phi1);
                float sinPhi2 = (float) Math.sin(phi2); float cosPhi2 = (float) Math.cos(phi2);

                float x1 = radius * sinTheta1 * cosPhi1; float y1 = radius * cosTheta1; float z1 = radius * sinTheta1 * sinPhi1;
                float x2 = radius * sinTheta2 * cosPhi1; float y2 = radius * cosTheta2; float z2 = radius * sinTheta2 * sinPhi1;
                float x3 = radius * sinTheta2 * cosPhi2; float z3 = radius * sinTheta2 * sinPhi2;
                float x4 = radius * sinTheta1 * cosPhi2; float z4 = radius * sinTheta1 * sinPhi2;

                sphere[index++] = new Vertex[]{
                        Vertex.of(x + x1, y + y1, z + z1).normal(x1, y1, z1).color(color).mul(matrices),
                        Vertex.of(x + x4, y + y1, z + z4).normal(x4, y1, z4).color(color).mul(matrices),
                        Vertex.of(x + x3, y + y2, z + z3).normal(x3, y2, z3).color(color).mul(matrices),
                        Vertex.of(x + x2, y + y2, z + z2).normal(x2, y2, z2).color(color).mul(matrices),
                };
            }
        }

        return sphere;
    }

    public static Vertex[][] capsule(MatrixStack matrices, float x, float y, float z, float radius, float height, int sides, int color) {
        int slices = Math.max(sides, 3);
        int hemiStacks = slices / 2;

        Vertex[][] capsule = new Vertex[slices + (hemiStacks * slices) * 2][];
        int index = 0;

        //cylinder
        float cylinderY  = y + radius;
        float cylinderY1 = y + height - radius;
        float cylinderStep = 2f * (float) Math.PI / slices;

        //cylinder sides
        for (int i = 0; i < slices; i++) {
            float theta1 = cylinderStep * i;
            float theta2 = cylinderStep * (i + 1);

            float x1 = radius * (float) Math.cos(theta1), z1 = radius * (float) Math.sin(theta1);
            float x2 = radius * (float) Math.cos(theta2), z2 = radius * (float) Math.sin(theta2);

            capsule[index++] = new Vertex[]{
                    Vertex.of(x + x1, cylinderY , z + z1).normal(x1, 0, z1).color(color).mul(matrices),
                    Vertex.of(x + x1, cylinderY1, z + z1).normal(x1, 0, z1).color(color).mul(matrices),
                    Vertex.of(x + x2, cylinderY1, z + z2).normal(x2, 0, z2).color(color).mul(matrices),
                    Vertex.of(x + x2, cylinderY , z + z2).normal(x2, 0, z2).color(color).mul(matrices),
            };
        }

        //hemispheres
        float hemisphereStep = (float) Math.PI / hemiStacks;
        float sliceStep = 2f * (float) Math.PI / slices;

        //top hemisphere
        for (int i = 0; i < hemiStacks; i++) {
            float theta1 = hemisphereStep / 2f * i;
            float theta2 = hemisphereStep / 2f * (i + 1);

            float sin1 = (float) Math.sin(theta1), cos1 = (float) Math.cos(theta1);
            float sin2 = (float) Math.sin(theta2), cos2 = (float) Math.cos(theta2);

            for (int j = 0; j < slices; j++) {
                float phi1 = sliceStep * j;
                float phi2 = sliceStep * (j + 1);

                float cosPhi1 = (float) Math.cos(phi1), sinPhi1 = (float) Math.sin(phi1);
                float cosPhi2 = (float) Math.cos(phi2), sinPhi2 = (float) Math.sin(phi2);

                float x1 = radius * sin1 * cosPhi1; float y1 = cos1 * radius; float z1 = radius * sin1 * sinPhi1;
                float x2 = radius * sin2 * cosPhi1; float y2 = cos2 * radius; float z2 = radius * sin2 * sinPhi1;
                float x3 = radius * sin2 * cosPhi2; float z3 = radius * sin2 * sinPhi2;
                float x4 = radius * sin1 * cosPhi2; float z4 = radius * sin1 * sinPhi2;

                capsule[index++] = new Vertex[]{
                        Vertex.of(x + x1, cylinderY1 + y1, z + z1).normal(x1, y1, z1).color(color).mul(matrices),
                        Vertex.of(x + x4, cylinderY1 + y1, z + z4).normal(x4, y1, z4).color(color).mul(matrices),
                        Vertex.of(x + x3, cylinderY1 + y2, z + z3).normal(x3, y2, z3).color(color).mul(matrices),
                        Vertex.of(x + x2, cylinderY1 + y2, z + z2).normal(x2, y2, z2).color(color).mul(matrices),
                };
            }
        }

        //bottom hemisphere
        for (int i = 0; i < hemiStacks; i++) {
            float theta1 = hemisphereStep / 2f * i;
            float theta2 = hemisphereStep / 2f * (i + 1);

            float sin1 = (float) Math.sin(theta1), cos1 = (float) -Math.cos(theta1);
            float sin2 = (float) Math.sin(theta2), cos2 = (float) -Math.cos(theta2);

            for (int j = 0; j < slices; j++) {
                float phi1 = sliceStep * j;
                float phi2 = sliceStep * (j + 1);

                float cosPhi1 = (float) Math.cos(phi1), sinPhi1 = (float) Math.sin(phi1);
                float cosPhi2 = (float) Math.cos(phi2), sinPhi2 = (float) Math.sin(phi2);

                float x1 = radius * sin1 * cosPhi1; float y1 = cos1 * radius; float z1 = radius * sin1 * sinPhi1;
                float x2 = radius * sin2 * cosPhi1; float y2 = cos2 * radius; float z2 = radius * sin2 * sinPhi1;
                float x3 = radius * sin2 * cosPhi2; float z3 = radius * sin2 * sinPhi2;
                float x4 = radius * sin1 * cosPhi2; float z4 = radius * sin1 * sinPhi2;

                capsule[index++] = new Vertex[]{
                        Vertex.of(x + x1, y + radius + y1, z + z1).normal(x1, y1, z1).color(color).mul(matrices),
                        Vertex.of(x + x2, y + radius + y2, z + z2).normal(x2, y2, z2).color(color).mul(matrices),
                        Vertex.of(x + x3, y + radius + y2, z + z3).normal(x3, y2, z3).color(color).mul(matrices),
                        Vertex.of(x + x4, y + radius + y1, z + z4).normal(x4, y1, z4).color(color).mul(matrices),
                };
            }
        }

        return capsule;
    }
}
