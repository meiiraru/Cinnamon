package cinnamon.utils;

import org.joml.*;
import org.joml.Math;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

public class Maths {

    public static Vector4f lerp(Vector4f a, Vector4f b, float t) {
        return new Vector4f(
                Math.lerp(a.x, b.x, t),
                Math.lerp(a.y, b.y, t),
                Math.lerp(a.z, b.z, t),
                Math.lerp(a.w, b.w, t)
        );
    }

    public static Vector3f lerp(Vector3f a, Vector3f b, float t) {
        return new Vector3f(
                Math.lerp(a.x, b.x, t),
                Math.lerp(a.y, b.y, t),
                Math.lerp(a.z, b.z, t)
        );
    }

    public static Vector2f lerp(Vector2f a, Vector2f b, float t) {
        return new Vector2f(
                Math.lerp(a.x, b.x, t),
                Math.lerp(a.y, b.y, t)
        );
    }

    public static float wrapDegrees(float angle) {
        angle %= 360f;
        if (angle > 180f)
            return angle - 360f;
        if (angle < -180f)
            return angle + 360f;
        return angle;
    }

    public static float shortAngle(float a, float b) {
        return wrapDegrees(b - a);
    }

    public static float lerpAngle(float a, float b, float t) {
        return Math.fma(shortAngle(a, b), t, a);
    }

    public static Vector2f lerpAngle(Vector2f a, Vector2f b, float t) {
        return new Vector2f(
                lerpAngle(a.x, b.x, t),
                lerpAngle(a.y, b.y, t)
        );
    }

    public static Vector3f lerpAngle(Vector3f a, Vector3f b, float t) {
        return new Vector3f(
                lerpAngle(a.x, b.x, t),
                lerpAngle(a.y, b.y, t),
                lerpAngle(a.z, b.z, t)
        );
    }

    public static float lerpArray(float[] array, float t) {
        int len = array.length;
        if (len == 0)
            return 0;

        float index = t * (len - 1);
        int prev = (int) modulo(Math.floor(index), len);
        int next = (int) modulo(Math.ceil(index), len);

        float indexDelta = index - prev;
        return Math.lerp(array[prev], array[next], indexDelta);
    }

    public static Vector3f lerpArray(Vector3f[] array, float t) {
        int len = array.length;
        if (len == 0)
            return new Vector3f();

        float index = t * (len - 1);
        int prev = (int) modulo(Math.floor(index), len);
        int next = (int) modulo(Math.ceil(index), len);

        float indexDelta = index - prev;
        return lerp(array[prev], array[next], indexDelta);
    }

    public static Vector3f parseVec3(String vec3, String split) {
        String[] s = vec3.split(split);
        return new Vector3f(Float.parseFloat(s[0]), Float.parseFloat(s[1]), Float.parseFloat(s[2]));
    }

    public static Vector2f parseVec2(String vec2, String split) {
        String[] s = vec2.split(split);
        return new Vector2f(Float.parseFloat(s[0]), Float.parseFloat(s[1]));
    }

    public static Vector3f rotToDir(float pitch, float yaw) {
        float p = Math.toRadians(pitch + 180f);
        float y = Math.toRadians(-yaw);
        float cosP = Math.cos(p);
        return new Vector3f(Math.sin(y) * cosP, Math.sin(p), Math.cos(y) * cosP);
    }

    public static Vector2f rotToDir(float degrees) {
        float angle = Math.toRadians(degrees);
        return new Vector2f(Math.cos(angle), Math.sin(angle));
    }

    public static Vector2f dirToRot(Vector3f dir) {
        return dirToRot(dir.x, dir.y, dir.z);
    }

    public static Vector2f dirToRot(float x, float y, float z) {
        float pitch = Math.toDegrees(Math.asin(clamp(-y, -1f, 1f)));
        float yaw = Math.toDegrees(Math.atan2(z, x));
        return new Vector2f(pitch, yaw + 90f);
    }

    public static float dirToRot(Vector2f dir) {
        return dirToRot(dir.x, dir.y);
    }

    public static float dirToRot(float x, float y) {
        return Math.toDegrees(Math.atan2(y, x));
    }

    public static float modulo(float a, float n) {
        return (a % n + n) % n;
    }

    public static int modulo(int a, int n) {
        return (a % n + n) % n;
    }

    public static float ratio(float x, float min, float max) {
        return (x - min) / (max - min);
    }

    public static float map(float x, float min1, float max1, float min2, float max2) {
        return Math.lerp(min2, max2, ratio(x, min1, max1));
    }

    public static float clamp(float n, float min, float max) {
        return min > max ? Math.clamp(max, min, n) : Math.clamp(min, max, n);
    }

    public static int clamp(int n, int min, int max) {
        return min > max ? Math.clamp(max, min, n) : Math.clamp(min, max, n);
    }

    public static float clampWarp(float n, float min, float max) {
        if (min > max) {
            float temp = min;
            min = max;
            max = temp;
        }

        float range = max - min;
        return n - range * Math.floor((n - min) / range);
    }

    public static int pow(int a, int b) {
        int p = 1;
        for (int i = 0; i < b; i++)
            p *= a;
        return p;
    }

    public static float pow(float a, float b) {
        return (float) java.lang.Math.pow(a, b);
    }

    public static Vector3f reflect(Vector3f dir, Vector3f normal) {
        //r = d - 2 * (d dot n) * n
        float dot = dir.dot(normal) * 2;
        return dir.sub(normal.x * dot, normal.y * dot, normal.z * dot, new Vector3f());
    }

    public static Vector2f reflect(Vector2f dir, Vector2f normal) {
        float dot = dir.dot(normal) * 2;
        return dir.sub(normal.x * dot, normal.y * dot, new Vector2f());
    }

    public static Vector3f toRadians(Vector3f vec) {
        return new Vector3f(
                Math.toRadians(vec.x),
                Math.toRadians(vec.y),
                Math.toRadians(vec.z)
        );
    }

    public static Vector2f toRadians(Vector2f vec) {
        return new Vector2f(
                Math.toRadians(vec.x),
                Math.toRadians(vec.y)
        );
    }

    public static Vector3f toDegrees(Vector3f vec) {
        return new Vector3f(
                Math.toDegrees(vec.x),
                Math.toDegrees(vec.y),
                Math.toDegrees(vec.z)
        );
    }

    public static Vector2f toDegrees(Vector2f vec) {
        return new Vector2f(
                Math.toDegrees(vec.x),
                Math.toDegrees(vec.y)
        );
    }

    public static Vector3f quatToEuler(Quaternionf quat) {
        return new Vector3f(getPitch(quat), getYaw(quat), getRoll(quat));
    }

    public static float getPitch(Quaternionf quat) {
        return Math.toDegrees(Math.atan2(-2f * quat.x * quat.w + 2f * quat.y * quat.z, 1f - 2f * quat.x * quat.x - 2f * quat.z * quat.z));
    }

    public static float getYaw(Quaternionf quat) {
        return Math.toDegrees(Math.atan2(-2f * quat.y * quat.w + 2f * quat.x * quat.z, 1f - 2f * quat.y * quat.y - 2f * quat.z * quat.z));
    }

    public static float getRoll(Quaternionf quat) {
        return Math.toDegrees(Math.asin(-2f * quat.x * quat.y - 2f * quat.z * quat.w));
    }

    public static Quaternionf dirToQuat(Vector3f dir) {
        float pitch = Math.asin(dir.y);
        float yaw = Math.atan2(dir.x, dir.z);
        return new Quaternionf().rotationZYX(0f, yaw, -pitch);
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
        return (float) (Math.random() * (max - min) + min);
    }

    public static int range(int min, int max) {
        return (int) (Math.random() * (max - min) + min);
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

    public static boolean isNaN(Vector2f vec) {
        return Float.isNaN(vec.x) || Float.isNaN(vec.y);
    }

    public static Vector2f rotate(Vector2f vec, float angle) {
        float rad = Math.toRadians(angle);
        float cos = Math.cos(rad);
        float sin = Math.sin(rad);
        return vec.set(
                cos * vec.x - sin * vec.y,
                sin * vec.x + cos * vec.y
        );
    }

    public static Matrix3f translateMat3(Matrix3f mat, float x, float y) {
        mat.m00 += x * mat.m20;
        mat.m01 += x * mat.m21;
        mat.m02 += x * mat.m22;

        mat.m10 += y * mat.m20;
        mat.m11 += y * mat.m21;
        mat.m12 += y * mat.m22;

        return mat;
    }

    public static int nextPowerOfTwo(float x) {
        int value = (int) Math.ceil(x);
        int power = 1;
        while (power < value)
            power <<= 1;
        return power;
    }

    public static <T> T randomArr(T[] arr) {
        return arr[(int) (Math.random() * arr.length)];
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

    public static float factorial(int n) {
        return n <= 1 ? 1f : n * factorial(n - 1);
    }

    public static String getPermutation(int index, Object... elements) {
        if (elements.length == 1)
            return String.valueOf(elements[0]);

        int sizeGroup = (int) factorial(elements.length - 1);
        int quotient = index / sizeGroup;
        int reminder = index % sizeGroup;

        Object[] newElements = new Object[elements.length - 1];
        for (int i = 0, j = 0; i < elements.length; i++) {
            if (i == quotient)
                continue;
            newElements[j++] = elements[i];
        }

        return elements[quotient] + getPermutation(reminder, newElements);
    }

    public static Vector3f randomDir() {
        float pitch = (float) Math.random() * 360;
        float yaw = (float) Math.random() * 360;
        return rotToDir(pitch, yaw);
    }

    public static Matrix3f getDirMat(Vector3f dir) {
        Quaternionf rotation = dirToQuat(dir);
        return new Matrix3f(
                new Vector3f(1f, 0f, 0f).rotate(rotation),
                new Vector3f(0f, 1f, 0f).rotate(rotation),
                new Vector3f(0f, 0f, -1f).rotate(rotation)
        );
    }

    public static Vector3f spread(Matrix3f dirMatrix, float pitch, float yaw) {
        float r1 = (float) Math.toRadians(Math.random() * 2f - 1f) * yaw;
        float r2 = (float) Math.toRadians(Math.random() * 2f - 1f) * pitch;

        Vector3f rotVec = new Vector3f(
                Math.sin(r1) * Math.cos(r2),
                Math.sin(r2),
                Math.cos(r1) * Math.cos(r2)
        );

        return rotVec.mul(dirMatrix);
    }

    public static Vector3f spread(Vector3f dir, float pitch, float yaw) {
        Matrix3f dirMatrix = getDirMat(dir);
        return spread(dirMatrix, pitch, yaw);
    }

    public static int binarySearch(int start, int end, Predicate<Integer> test) {
        while (start < end) {
            int mid = start + (end - start) / 2;
            if (test.test(mid))
                end = mid;
            else
                start = mid + 1;
        }
        return start;
    }

    public static float hermite(float p0, float p3, float r0, float r3, float weight, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return (2 * t3 - 3 * t2 + 1) * p0 +
                (-2 * t3 + 3 * t2) * p3 +
                (t3 - 2 * t2 + t) * weight * r0 +
                (t3 - t2) * weight * r3;
    }

    public static float bezier(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return (-t3 + 3 * t2 - 3 * t + 1) * p0 +
                (3 * t3 - 6 * t2 + 3 * t) * p1 +
                (-3 * t3 + 3 * t2) * p2 +
                t3 * p3;
    }

    public static float bSpline(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return ((-t3 + 3 * t2 - 3 * t + 1) * p0 +
                (3 * t3 - 6 * t2 + 4) * p1 +
                (-3 * t3 + 3 * t2 + 3 * t + 1) * p2 +
                t3 * p3) / 6f;
    }

    public static float bezierDeCasteljau(float t, float... controlPoints) {
        float[] points = Arrays.copyOf(controlPoints, controlPoints.length);
        int n = points.length - 1;

        while (n > 0) {
            for (int i = 0; i < n; i++)
                points[i] = Math.lerp(points[i], points[i + 1], t);
            n--;
        }

        return points[0];
    }

    public static float catmullRom(float p0, float p1, float p2, float p3, float t) {
        return 0.5f * ((2 * p1) +
                (-p0 + p2) * t +
                (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t +
                (-p0 + 3 * p1 - 3 * p2 + p3) * t * t * t);
    }

    private static final float
            C1 = 1.70158f,
            C2 = C1 * 1.525f,
            C3 = C1 + 1f,
            C4 = Math.PI_TIMES_2_f / 3f,
            C5 = Math.PI_TIMES_2_f / 4.5f,
            N1 = 7.5625f,
            D1 = 2.75f;

    public enum Easing {
        IN_SINE(x -> 1f - Math.cos((x * Math.PI_f) / 2f)),
        OUT_SINE(x -> Math.sin((x * Math.PI_f) / 2f)),
        IN_OUT_SINE(x -> -(Math.cos(Math.PI_f * x) - 1f) / 2f),

        IN_QUAD(x -> x * x),
        OUT_QUAD(x -> 1f - (1f - x) * (1f - x)),
        IN_OUT_QUAD(x -> x < 0.5f ? 2f * x * x : 1f - pow(-2f * x + 2f, 2f) / 2f),

        IN_CUBIC(x -> x * x * x),
        OUT_CUBIC(x -> 1f - pow(1f - x, 3f)),
        IN_OUT_CUBIC(x -> x < 0.5f ? 4f * x * x * x : 1f - pow(-2f * x + 2f, 3f) / 2f),

        IN_QUART(x -> x * x * x * x),
        OUT_QUART(x -> 1f - pow(1f - x, 4f)),
        IN_OUT_QUART(x -> x < 0.5f ? 8f * x * x * x * x : 1f - pow(-2f * x + 2f, 4f) / 2f),

        IN_QUINT(x -> x * x * x * x * x),
        OUT_QUINT(x -> 1f - pow(1f - x, 5f)),
        IN_OUT_QUINT(x -> x < 0.5f ? 16f * x * x * x * x * x : 1f - pow(-2f * x + 2f, 5f) / 2f),

        IN_EXPO(x -> x == 0f ? 0f : pow(2f, 10f * x - 10f)),
        OUT_EXPO(x ->  x == 1f ? 1f : 1f - pow(2f, -10f * x)),
        IN_OUT_EXPO(x -> x == 0f ? 0f : x == 1f ? 1f : x < 0.5f ? pow(2f, 20f * x - 10f) / 2f : (2f - pow(2f, -20f * x + 10f)) / 2f),

        IN_CIRC(x -> 1f - Math.sqrt(1f - pow(x, 2f))),
        OUT_CIRC(x -> Math.sqrt(1f - pow(x - 1f, 2f))),
        IN_OUT_CIRC(x -> x < 0.5f ? (1f - Math.sqrt(1f - pow(2f * x, 2f))) / 2f : (Math.sqrt(1f - pow(-2f * x + 2f, 2f)) + 1f) / 2f),

        IN_BACK(x -> C3 * x * x * x - C1 * x * x),
        OUT_BACK(x -> 1f + C3 * pow(x - 1f, 3f) + C1 * pow(x - 1f, 2f)),
        IN_OUT_BACK(x -> x < 0.5f ? (pow(2f * x, 2f) * ((C2 + 1f) * 2f * x - C2)) / 2f : (pow(2f * x - 2f, 2f) * ((C2 + 1f) * (x * 2f - 2f) + C2) + 2f) / 2f),

        IN_ELASTIC(x -> x == 0f ? 0f : x == 1f ? 1f : -pow(2f, 10f * x - 10f) * Math.sin((x * 10f - 10.75f) * C4)),
        OUT_ELASTIC(x -> x == 0f ? 0f : x == 1f ? 1f : pow(2f, -10f * x) * Math.sin((x * 10f - 0.75f) * C4) + 1f),
        IN_OUT_ELASTIC(x -> x == 0f ? 0f : x == 1f ? 1f : x < 0.5f ? -(pow(2f, 20f * x - 10f) * Math.sin((20f * x - 11.125f) * C5)) / 2f : (pow(2f, -20f * x + 10f) * Math.sin((20f * x - 11.125f) * C5)) / 2f + 1f),

        OUT_BOUNCE(x -> {
            if (x < 1f / D1)
                return N1 * x * x;
            else if (x < 2f / D1)
                return N1 * (x -= 1.5f / D1) * x + 0.75f;
            else if (x < 2.5 / D1)
                return N1 * (x -= 2.25f / D1) * x + 0.9375f;
            else
                return N1 * (x -= 2.625f / D1) * x + 0.984375f;
        }),
        IN_BOUNCE(x -> 1f - OUT_BOUNCE.get(1f - x)),
        IN_OUT_BOUNCE(x -> x < 0.5f ? (1 - OUT_BOUNCE.get(1f - 2f * x)) / 2f : (1 + OUT_BOUNCE.get(2f * x - 1f)) / 2f);

        private final Function<Float, Float> func;

        Easing(Function<Float, Float> func) {
            this.func = func;
        }

        public float get(float x) {
            return func.apply(x);
        }
    }
}
