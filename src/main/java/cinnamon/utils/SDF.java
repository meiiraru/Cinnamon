package cinnamon.utils;

import org.joml.Math;
import org.joml.Vector3f;

public interface SDF {
    default float distance(Vector3f p) {
        return distance(p.x(), p.y(), p.z());
    }
    float distance(float x, float y, float z);

    //distance = |p - center| - radius
    class Sphere implements SDF {
        private final float radius;

        public Sphere(float radius) {
            this.radius = radius;
        }

        @Override
        public float distance(float px, float py, float pz) {
            return Vector3f.length(px, py, pz) - radius;
        }
    }

    //vec3 q = abs(p) - b
    //return length(max(q, 0)) + min(max(q.x, max(q.y, q.z)), 0)
    class Box implements SDF {
        private final float extentX, extentY, extentZ;

        public Box(Vector3f extents) {
            this(extents.x(), extents.y(), extents.z());
        }

        public Box(float extentX, float extentY, float extentZ) {
            this.extentX = extentX;
            this.extentY = extentY;
            this.extentZ = extentZ;
        }

        @Override
        public float distance(float px, float py, float pz) {
            //absolute p sub half-extents
            float qx = Math.abs(px) - extentX;
            float qy = Math.abs(py) - extentY;
            float qz = Math.abs(pz) - extentZ;

            //distance to the faces, if outside
            float outsideDist = Vector3f.length(Math.max(qx, 0f), Math.max(qy, 0f), Math.max(qz, 0f));

            //negative distance if inside, based on furthest axis from center
            float insideDist = Math.min(Math.max(qx, Math.max(qy, qz)), 0f);

            //final signed distance
            return outsideDist + insideDist;
        }
    }

    //distance = p dot normal - height
    class Plane implements SDF {
        private final float normalX, normalY, normalZ;
        private final float height;

        public Plane(Vector3f normal, float height) {
            this(normal.x(), normal.y(), normal.z(), height);
        }

        public Plane(float normalX, float normalY, float normalZ, float height) {
            this.normalX = normalX;
            this.normalY = normalY;
            this.normalZ = normalZ;
            this.height = height;
        }

        @Override
        public float distance(float px, float py, float pz) {
            return Math.fma(px, normalX, Math.fma(py, normalY, pz * normalZ)) - height;
        }
    }

    class Operation {
        public static SDF union(SDF sdf1, SDF sdf2) {
            return (px, py, pz) -> Math.min(sdf1.distance(px, py, pz), sdf2.distance(px, py, pz));
        }

        public static SDF intersection(SDF sdf1, SDF sdf2) {
            return (px, py, pz) -> Math.max(sdf1.distance(px, py, pz), sdf2.distance(px, py, pz));
        }

        public static SDF subtract(SDF sdf1, SDF sdf2) {
            return (px, py, pz) -> Math.max(sdf1.distance(px, py, pz), -sdf2.distance(px, py, pz));
        }

        public static SDF translate(SDF sdf, Vector3f pos) {
            return translate(sdf, pos.x(), pos.y(), pos.z());
        }

        public static SDF translate(SDF sdf, float x, float y, float z) {
            return (px, py, pz) -> sdf.distance(px - x, py - y, pz - z);
        }

        public static SDF scale(SDF sdf, float factor) {
            return (px, py, pz) -> sdf.distance(px / factor, py / factor, pz / factor) * factor;
        }

        public static SDF round(SDF sdf, float radius) {
            return (px, py, pz) -> sdf.distance(px, py, pz) - radius;
        }

        public static SDF repeat(SDF sdf, Vector3f spacing) {
            return repeat(sdf, spacing.x(), spacing.y(), spacing.z());
        }

        //q = p - spacing * round(p / spacing)
        public static SDF repeat(SDF sdf, float spacingX, float spacingY, float spacingZ) {
            return (px, py, pz) -> {
                float x = px - spacingX * Math.round(px / spacingX);
                float y = py - spacingY * Math.round(py / spacingY);
                float z = pz - spacingZ * Math.round(pz / spacingZ);
                return sdf.distance(x, y, z);
            };
        }

        //clamp p to the elongation length, stretching the SDF along p
        public static SDF elongateX(SDF sdf, float length) {
            return (px, py, pz) -> {
                float qx = px - Math.max(0f, Math.min(length, px));
                return sdf.distance(qx, py, pz);
            };
        }

        public static SDF elongateY(SDF sdf, float length) {
            return (px, py, pz) -> {
                float qy = py - Math.max(0f, Math.min(length, py));
                return sdf.distance(px, qy, pz);
            };
        }

        public static SDF elongateZ(SDF sdf, float length) {
            return (px, py, pz) -> {
                float qz = pz - Math.max(0f, Math.min(length, pz));
                return sdf.distance(px, py, qz);
            };
        }
    }
}