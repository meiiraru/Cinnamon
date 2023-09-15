package mayo.utils;

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

    public static Vector3f parseVec3(String x, String y, String z) {
        return new Vector3f(Float.parseFloat(x), Float.parseFloat(y), Float.parseFloat(z));
    }

    public static float clamp(float val, float min, float max) {
        return Math.min(Math.max(val, min), max);
    }
}
