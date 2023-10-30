package mayo.utils;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class Maths {

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static Vector4f lerp(Vector4f a, Vector4f b, float t) {
        return new Vector4f(
                lerp(a.x, b.x, t),
                lerp(a.y, b.y, t),
                lerp(a.z, b.z, t),
                lerp(a.w, b.w, t)
        );
    }

    public static Vector3f lerp(Vector3f a, Vector3f b, float t) {
        return new Vector3f(
                lerp(a.x, b.x, t),
                lerp(a.y, b.y, t),
                lerp(a.z, b.z, t)
        );
    }

    public static Vector2f lerp(Vector2f a, Vector2f b, float t) {
        return new Vector2f(
                lerp(a.x, b.x, t),
                lerp(a.y, b.y, t)
        );
    }

    public static float lerpRot(float a, float b, float t) {
        return a + wrapDegrees(b - a) * t;
    }

    public static float wrapDegrees(float angle) {
        float f = angle % 360;
        if (f >= 180f) f -= 360f;
        if (f < -180f) f += 360f;
        return f;
    }

    public static Vector3f parseVec3(String x, String y, String z) {
        return new Vector3f(Float.parseFloat(x), Float.parseFloat(y), Float.parseFloat(z));
    }

    public static Vector2f parseVec2(String x, String y) {
        return new Vector2f(Float.parseFloat(x), Float.parseFloat(y));
    }

    public static float clamp(float val, float min, float max) {
        return Math.min(Math.max(val, min), max);
    }

    public static Vector3f rotToDir(float pitch, float yaw) {
        double p = Math.toRadians(pitch + 180f);
        double y = Math.toRadians(-yaw);
        double cosP = Math.cos(p);
        return new Vector3f((float) (Math.sin(y) * cosP), (float) Math.sin(p), (float) (Math.cos(y) * cosP));
    }

    public static Vector2f rotToDir(float degrees) {
        double angle = Math.toRadians(degrees);
        return new Vector2f((float) Math.cos(angle), (float) Math.sin(angle));
    }

    public static Vector2f dirToRot(Vector3f dir) {
        return dirToRot(dir.x, dir.y, dir.z);
    }

    public static Vector2f dirToRot(float x, float y, float z) {
        float pitch = (float) Math.toDegrees(Math.asin(-y));
        float yaw = (float) Math.toDegrees(Math.atan2(z, x));
        return new Vector2f(pitch, yaw + 90f);
    }

    public static float dirToRot(Vector2f dir) {
        return dirToRot(dir.x, dir.y);
    }

    public static float dirToRot(float x, float y) {
        return (float) Math.toDegrees(Math.atan2(y, x));
    }

    public static float modulo(float a, float n) {
        return (a % n + n) % n;
    }

    public static float ratio(float x, float min, float max) {
        return (x - min) / (max - min);
    }

    public static float map(float x, float min1, float max1, float min2, float max2) {
        return lerp(min2, max2, ratio(x, min1, max1));
    }

    public static Vector3f reflect(Vector3f dir, Vector3f normal) {
        //r = d − 2 * (d dot n) * n
        float dot = dir.dot(normal) * 2;
        return dir.sub(normal.x * dot, normal.y * dot, normal.z * dot, new Vector3f());
    }

    public static Vector2f reflect(Vector2f dir, Vector2f normal) {
        float dot = dir.dot(normal) * 2;
        return dir.sub(normal.x * dot, normal.y * dot, new Vector2f());
    }

    public static Vector3f toRadians(Vector3f vec) {
        return new Vector3f(
                (float) Math.toRadians(vec.x),
                (float) Math.toRadians(vec.y),
                (float) Math.toRadians(vec.z)
        );
    }

    public static Vector2f toRadians(Vector2f vec) {
        return new Vector2f(
                (float) Math.toRadians(vec.x),
                (float) Math.toRadians(vec.y)
        );
    }

    public static Vector3f toDegrees(Vector3f vec) {
        return new Vector3f(
                (float) Math.toDegrees(vec.x),
                (float) Math.toDegrees(vec.y),
                (float) Math.toDegrees(vec.z)
        );
    }

    public static Vector2f toDegrees(Vector2f vec) {
        return new Vector2f(
                (float) Math.toDegrees(vec.x),
                (float) Math.toDegrees(vec.y)
        );
    }

    public static Vector3f normal(Vector3f p1, Vector3f p2, Vector3f p3) {
        //calculate the cross product of two vectors to get the normal
        Vector3f edge1 = p2.sub(p1, new Vector3f());
        Vector3f edge2 = p3.sub(p1, new Vector3f());
        return edge1.cross(edge2);
    }

    public static boolean isPointInTriangle(Vector3f a, Vector3f b, Vector3f c, Vector3f point) {
        //calculate the normals of the triangle and our point
        Vector3f normalABC = normal(a, b, c);
        Vector3f normalPAB = normal(point, a, b);
        Vector3f normalPBC = normal(point, b, c);
        Vector3f normalPCA = normal(point, c, a);

        //check if the point is inside the triangle
        return normalABC.dot(normalPAB) >= 0 && normalABC.dot(normalPBC) >= 0 && normalABC.dot(normalPCA) >= 0;
    }

    public static float range(float min, float max) {
        return (float) Math.random() * (max - min) + min;
    }

    public static float min(Vector3f vec) {
        return Math.min(vec.x, Math.min(vec.y, vec.z));
    }

    public static float max(Vector3f vec) {
        return Math.max(vec.x, Math.max(vec.y, vec.z));
    }

    public static int maxIndex(Vector3f vec) {
        if (vec.x >= vec.y && vec.x >= vec.z) {
            return 0;
        } else if (vec.y >= vec.z) {
            return 1;
        } else {
            return 2;
        }
    }

    public static int minIndex(Vector3f vec) {
        if (vec.x < vec.y && vec.x < vec.z) {
            return 0;
        } else if (vec.y < vec.z) {
            return 1;
        } else {
            return 2;
        }
    }

    public static boolean isNaN(Vector3f vec) {
        return Float.isNaN(vec.x) || Float.isNaN(vec.y) || Float.isNaN(vec.z);
    }

    private static final String[] SIZE_UNITS = {"b", "kb", "mb", "gb"};
    public static String prettyByteSize(double size) {
        int i = 0;
        while (i < SIZE_UNITS.length) {
            if (size < 1024) break;
            size /= 1024;
            i++;
        }

        DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(size) + SIZE_UNITS[i];
    }

    public static float magicDelta(float speed, float delta) {
        return (float) (1f - Math.pow(speed, delta));
    }

    public static float fastInvCubeRoot(float x) {
        //convert float to its bit representation
        int i = Float.floatToIntBits(x);

        //magic number operation to approximate the inverse cube root
        i = 0x54A2FA8C - i / 3;

        //convert the bit representation back to a float
        float approxRoot = Float.intBitsToFloat(i);

        //improve the approximation with two iterations of Newton's method
        approxRoot = 0.6666667f * approxRoot + 1f / (3f * approxRoot * approxRoot * x);
        approxRoot = 0.6666667f * approxRoot + 1f / (3f * approxRoot * approxRoot * x);

        //return
        return approxRoot;
    }
}
