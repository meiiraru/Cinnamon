package mayo.utils;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class Meth {

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
        return dirToRot(dir.x, dir.y, dir.z);
    }

    public static Vector2f dirToRot(float x, float y, float z) {
        float pitch = (float) Math.toDegrees(Math.asin(-y));
        float yaw = (float) Math.toDegrees(Math.atan2(z, x));
        return new Vector2f(pitch, yaw + 90f);
    }

    public static float modulo(float a, float n) {
        return (a % n + n) % n;
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
        for (int j = 0; j < 2; j++)
            approxRoot = 0.6666667f * approxRoot + 1f / (3f * approxRoot * approxRoot * x);

        //return
        return approxRoot;
    }
}
