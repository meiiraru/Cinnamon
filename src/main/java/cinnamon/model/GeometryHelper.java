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
        for (int i = 0; i <= cellsX; i++)
            for (int j = 0; j <= cellsZ; j++)
                vertices[index++] = Vertex.of(x0 + i * cellWidth, y, z0 + j * cellDepth).normal(0, 1, 0).color(color).mul(matrices);

        //fill quads
        Vertex[][] quads = new Vertex[cellsX * cellsZ][4];
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
        Vertex[][] box = new Vertex[6][4];
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
        box[0] = new Vertex[]{v0, v1, v2, v3};
        //west
        box[1] = new Vertex[]{v5, v0, v3, v6};
        //south
        box[2] = new Vertex[]{v4, v5, v6, v7};
        //east
        box[3] = new Vertex[]{v1, v4, v7, v2};
        //up
        box[4] = new Vertex[]{v1, v0, v5, v4};
        //down
        box[5] = new Vertex[]{v7, v6, v3, v2};

        return box;
    }

    public static Vertex[][] pyramid(MatrixStack matrices, float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        return pyramid(matrices, x0, y0, z0, x1, y1, z1, true, color);
    }

    public static Vertex[][] pyramid(MatrixStack matrices, float x0, float y0, float z0, float x1, float y1, float z1, boolean base, int color) {
        //corners
        Vertex v0 = Vertex.of(x0, y0, z0).normal(-1, -1, -1).color(color).mul(matrices);
        Vertex v1 = Vertex.of(x1, y0, z0).normal( 1, -1, -1).color(color).mul(matrices);
        Vertex v2 = Vertex.of(x1, y0, z1).normal( 1, -1,  1).color(color).mul(matrices);
        Vertex v3 = Vertex.of(x0, y0, z1).normal(-1, -1,  1).color(color).mul(matrices);
        //tip
        Vertex tip = Vertex.of(x0 + (x1 - x0) / 2f, y1, z0 + (z1 - z0) / 2f).normal(0, 1, 0).color(color).mul(matrices);

        //faces
        int i = 0;
        Vertex[][] pyramid = new Vertex[base ? 5 : 4][];

        //base
        if (base)
            pyramid[i++] = new Vertex[]{v0, v1, v2, v3};

        //sides
        pyramid[i++] = new Vertex[]{v0, tip, v1};
        pyramid[i++] = new Vertex[]{v1, tip, v2};
        pyramid[i++] = new Vertex[]{v2, tip, v3};
        pyramid[i]   = new Vertex[]{v3, tip, v0};

        return pyramid;
    }

    public static Vertex[][] cone(MatrixStack matrices, float x, float y, float z, float height, float radius, int sides, int color) {
        return cone(matrices, x, y, z, height, radius, sides, 1f, true, color);
    }

    public static Vertex[][] cone(MatrixStack matrices, float x, float y, float z, float height, float radius, int sides, float progress, boolean base, int color) {
        int faces = Math.max(sides, 3);
        float angleStep = ((float) Math.PI * progress * 2f) / faces;

        //generate vertices
        Vertex[] circle = new Vertex[sides];
        Vertex vTip = Vertex.of(x, y + height, z).normal(0, 1, 0).color(color).mul(matrices);
        Vertex vBase = Vertex.of(x, y, z).normal(0, -1, 0).color(color).mul(matrices);

        for (int i = 0; i < sides; i++) {
            float theta = angleStep * i;
            float x1 = (float) Math.cos(theta); float z1 = (float) Math.sin(theta);
            circle[i] = Vertex.of(x + x1 * radius, y, z + z1 * radius).normal(x1, 0, z1).color(color).mul(matrices);
        }

        //generate faces
        Vertex[][] cone = new Vertex[faces * (base ? 2 : 1)][3];
        for (int i = 0; i < faces; i++) {
            int next = (i + 1) % sides;
            cone[i] = new Vertex[]{vTip, circle[next], circle[i]}; //side
            if (base)
                cone[i + faces] = new Vertex[]{vBase, circle[i], circle[next]}; //base
        }

        return cone;
    }

    public static Vertex[][] cylinder(MatrixStack matrices, float x, float y, float z, float height, float radius, int sides, int color) {
        return cylinder(matrices, x, y, z, height, radius, radius, sides, 1f, true, color);
    }

    public static Vertex[][] cylinder(MatrixStack matrices, float x, float y, float z, float height, float radiusTop, float radiusBottom, int sides, float progress, boolean cap, int color) {
        int vertexCount = Math.max(sides, 3);
        float angleStep = ((float) Math.PI * progress * 2f) / vertexCount;

        //generate vertices
        Vertex[] circle = new Vertex[sides * 2];
        Vertex top = Vertex.of(x, y + height, z).normal(0, 1, 0).color(color).mul(matrices);
        Vertex bottom = Vertex.of(x, y, z).normal(0, -1, 0).color(color).mul(matrices);

        int index = 0;
        float r = radiusBottom;

        for (int j = 0; j < 2; j++) {
            for (int i = 0; i < sides; i++) {
                float theta = angleStep * i;
                float x1 = (float) Math.cos(theta); float z1 = (float) Math.sin(theta);
                circle[index++] = Vertex.of(x + x1 * r, y + j * height, z + z1 * r).normal(x1, 0, z1).color(color).mul(matrices);
            }
            r = radiusTop;
        }

        //generate faces
        Vertex[][] cylinder = new Vertex[vertexCount * (cap ? 3 : 2)][4];
        for (int i = 0; i < vertexCount; i++) {
            int next = (i + 1) % sides;
            cylinder[i] = new Vertex[]{circle[i + sides], circle[next + sides], circle[next], circle[i]}; //side
            if (cap) {
                cylinder[i + vertexCount] = new Vertex[]{top, circle[next + sides], circle[i + sides]}; //top
                cylinder[i + vertexCount * 2] = new Vertex[]{bottom, circle[i], circle[next]}; //bottom
            }
        }

        return cylinder;
    }

    public static Vertex[][] sphere(MatrixStack matrices, float x, float y, float z, float radius, int sides, int color) {
        return sphere(matrices, x, y, z, radius, sides, sides, 1f, 1f, color);
    }

    public static Vertex[][] sphere(MatrixStack matrices, float x, float y, float z, float radius, int wSides, int hSides, float wProgress, float hProgress, int color) {
        //total quads = stacks * slices
        int wSlices = Math.max(wSides, 3);
        int hSlices = Math.max(hSides, 2);

        //angle steps (progress)
        float phi = ((float) Math.PI * wProgress * 2f) / wSlices;
        float theta = (float) Math.PI * hProgress / hSlices;

        //generate vertices
        Vertex[] vertices = new Vertex[(wSlices + 1) * (hSlices + 1)];
        int index = 0;
        for (int j = 0; j <= hSlices; j++) {
            float theta1 = theta * j;
            float sinTheta1 = (float) Math.sin(theta1); float cosTheta1 = (float) Math.cos(theta1);
            for (int i = 0; i <= wSlices; i++) {
                float phi1 = phi * i;
                float sinPhi1 = (float) Math.sin(phi1); float cosPhi1 = (float) Math.cos(phi1);

                float x1 = radius * sinTheta1 * cosPhi1;
                float y1 = radius * cosTheta1;
                float z1 = radius * sinTheta1 * sinPhi1;

                vertices[index++] = Vertex.of(x + x1, y + y1, z + z1).normal(x1, y1, z1).color(color).mul(matrices);
            }
        }

        //generate faces
        Vertex[][] sphere = new Vertex[(wSlices) * (hSlices)][4];
        index = 0;
        for (int j = 0; j < hSlices; j++) {
            for (int i = 0; i < wSlices; i++) {
                int v0 = j * (wSlices + 1) + i;
                int v1 = v0 + 1;
                int v2 = v1 + (wSlices + 1);
                int v3 = v0 + (wSlices + 1);

                sphere[index++] = new Vertex[]{vertices[v0], vertices[v1], vertices[v2], vertices[v3]};
            }
        }

        return sphere;
    }

    public static Vertex[][] capsule(MatrixStack matrices, float x, float y, float z, float height, float radius, int sides, int color) {
        return capsule(matrices, x, y, z, height, radius, sides, sides, 1f, color);
    }

    public static Vertex[][] capsule(MatrixStack matrices, float x, float y, float z, float height, float radius, int wSides, int hSides, float progress, int color) {
        int wSlices = Math.max(wSides, 3);
        int hSlices = Math.max(hSides, 1);

        //vertical rings
        int totalRings = 2 * hSlices + 2;
        Vertex[] vertices = new Vertex[(wSlices + 1) * totalRings];

        float phi = (float) (Math.PI * 2f * progress) / wSlices;
        float theta = (float) (Math.PI / 2f) / hSlices;

        //generate vertices for each ring
        int index = 0;
        for (int j = 0; j < totalRings; j++) {
            float theta1;
            float centerY;

            if (j <= hSlices) { //bottom hemisphere
                theta1 = (float) Math.PI - j * theta;
                centerY = y + radius;
            } else { //top hemisphere
                theta1 = (float) (Math.PI / 2.0f) - (j - (hSlices + 1)) * theta;
                centerY = y + height - radius;
            }

            float sinTheta1 = (float) Math.sin(theta1); float cosTheta1 = (float) Math.cos(theta1);
            float ringY = centerY + radius * cosTheta1;

            //generate vertices around the ring
            for (int i = 0; i <= wSlices; i++) {
                float phi1 = i * phi;
                float sinPhi1 = (float) Math.sin(phi1); float cosPhi1 = (float) Math.cos(phi1);

                float x1 = radius * sinTheta1 * cosPhi1;
                float y1 = radius * cosTheta1;
                float z1 = radius * sinTheta1 * sinPhi1;
                vertices[index++] = Vertex.of(x + x1, ringY, z + z1).normal(x1, y1, z1).color(color).mul(matrices);
            }
        }

        //generate quads between rings
        int totalStrips = totalRings - 1;
        Vertex[][] capsule = new Vertex[wSlices * totalStrips][4];

        index = 0;
        for (int j = 0; j < totalStrips; j++) {
            for (int i = 0; i < wSlices; i++) {
                int v0 = j * (wSlices + 1) + i;
                int v1 = v0 + 1;
                int v2 = v1 + (wSlices + 1);
                int v3 = v0 + (wSlices + 1);

                capsule[index++] = new Vertex[]{vertices[v0], vertices[v3], vertices[v2], vertices[v1]};
            }
        }

        return capsule;
    }

    public static Vertex[][] torus(MatrixStack matrices, float x, float y, float z, float radius, float tubeRadius, int sides, int color) {
        return torus(matrices, x, y, z, radius, tubeRadius, sides, sides, 1f, color);
    }

    public static Vertex[][] torus(MatrixStack matrices, float x, float y, float z, float radius, float tubeRadius, int sides, int tubeSides, float progress, int color) {
        int mainSeg = Math.max(sides, 3);
        int tubeSeg = Math.max(tubeSides, 3);

        //calculate the main segments count to generate based on progress
        int segCount = (int) Math.ceil(mainSeg * progress);
        if (segCount <= 0)
            return new Vertex[0][];

        Vertex[] vertices = new Vertex[(segCount + 1) * (tubeSeg + 1)];

        float phi = (float) (Math.PI * 2f) / tubeSeg;
        float theta = (float) (Math.PI * 2f) / mainSeg;

        //generate vertices
        int index = 0;
        for (int i = 0; i <= segCount; i++) {
            float theta1 = i * theta;
            float sinTheta1 = (float) Math.sin(theta1); float cosTheta1 = (float) Math.cos(theta1);

            for (int j = 0; j <= tubeSeg; j++) {
                float phi1 = j * phi;
                float sinPhi1 = (float) Math.sin(phi1); float cosPhi1 = (float) Math.cos(phi1);

                float x1 = (radius + tubeRadius * cosPhi1) * cosTheta1;
                float y1 = tubeRadius * sinPhi1;
                float z1 = (radius + tubeRadius * cosPhi1) * sinTheta1;

                float nx = cosTheta1 * cosPhi1;
                float nz = sinTheta1 * cosPhi1;

                vertices[index++] = Vertex.of(x + x1, y + y1, z + z1)
                        .normal(nx, sinPhi1, nz)
                        .color(color)
                        .mul(matrices);
            }
        }

        //generate quads
        Vertex[][] quads = new Vertex[segCount * tubeSeg][4];
        index = 0;
        for (int i = 0; i < segCount; i++) {
            for (int j = 0; j < tubeSeg; j++) {
                int v0 = i * (tubeSeg + 1) + j;
                int v1 = (i + 1) * (tubeSeg + 1) + j;
                int v2 = (i + 1) * (tubeSeg + 1) + (j + 1);
                int v3 = i * (tubeSeg + 1) + (j + 1);

                quads[index++] = new Vertex[]{vertices[v0], vertices[v3], vertices[v2], vertices[v1]};
            }
        }

        return quads;
    }
}
