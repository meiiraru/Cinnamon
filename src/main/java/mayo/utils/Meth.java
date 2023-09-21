package mayo.utils;

import org.joml.Vector2f;
import org.joml.Vector3f;

public class Meth {

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
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

    public static float clamp(float val, float min, float max) {
        return Math.min(Math.max(val, min), max);
    }

    public static Vector3f rotToDir(float pitch, float yaw) {
        double p = Math.toRadians(pitch + 180f);
        double y = Math.toRadians(-yaw);
        double cosP = Math.cos(p);
        return new Vector3f((float) (Math.sin(y) * cosP), (float) Math.sin(p), (float) (Math.cos(y) * cosP));
    }

    public static Vector2f dirToRot(Vector3f dir) {
        float pitch = (float) Math.toDegrees(Math.asin(-dir.y));
        float yaw = (float) Math.toDegrees(Math.atan2(dir.z, dir.x));
        return new Vector2f(pitch, yaw + 90f);
    }

    public static float modulo(float a, float n) {
        return (a % n + n) % n;
    }
}
