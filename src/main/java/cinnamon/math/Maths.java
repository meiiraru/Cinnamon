package cinnamon.math;

import org.joml.*;
import org.joml.Math;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A utility class that provides various utility mathematical constants and functions
 */
public final class Maths {

    private Maths() {}

    public static final float
            METER               = 1f,
            CENTIMETER          = 0.01f,
            EPSILON             = 1e-3f,
            KINDA_SMALL_NUMBER  = 1e-6f,
            SMALL_NUMBER        = 1e-9f,
            REALLY_SMALL_NUMBER = 1e-12f;

    /**
     * Linearly interpolates between two {@link Vector4f} values based on a given interpolation factor
     * @param a The starting {@link Vector4f} value
     * @param b The ending {@link Vector4f} value
     * @param t The interpolation factor, typically in the range {@code [0, 1]}
     * @return A new {@link Vector4f} that represents the interpolated value between {@code a} and {@code b}
     */
    public static Vector4f lerp(Vector4f a, Vector4f b, float t) {
        return new Vector4f(
                Math.lerp(a.x, b.x, t),
                Math.lerp(a.y, b.y, t),
                Math.lerp(a.z, b.z, t),
                Math.lerp(a.w, b.w, t)
        );
    }

    /**
     * Linearly interpolates between two {@link Vector3f} values based on a given interpolation factor
     * @param a The starting {@link Vector3f} value
     * @param b The ending {@link Vector3f} value
     * @param t The interpolation factor, typically in the range {@code [0, 1]}
     * @return A new {@link Vector3f} that represents the interpolated value between {@code a} and {@code b}
     */
    public static Vector3f lerp(Vector3f a, Vector3f b, float t) {
        return new Vector3f(
                Math.lerp(a.x, b.x, t),
                Math.lerp(a.y, b.y, t),
                Math.lerp(a.z, b.z, t)
        );
    }

    /**
     * Linearly interpolates between two {@link Vector2f} values based on a given interpolation factor
     * @param a The starting {@link Vector2f} value
     * @param b The ending {@link Vector2f} value
     * @param t The interpolation factor, typically in the range {@code [0, 1]}
     * @return A new {@link Vector2f} that represents the interpolated value between {@code a} and {@code b}
     */
    public static Vector2f lerp(Vector2f a, Vector2f b, float t) {
        return new Vector2f(
                Math.lerp(a.x, b.x, t),
                Math.lerp(a.y, b.y, t)
        );
    }

    /**
     * Wraps an angle in degrees to the range {@code [-180, 180]}
     * @param angle The angle in degrees to be wrapped
     * @return The wrapped angle in degrees, constrained to the range {@code [-180, 180]}
     */
    public static float wrapDegrees(float angle) {
        angle %= 360f;
        if (angle > 180f)
            return angle - 360f;
        if (angle < -180f)
            return angle + 360f;
        return angle;
    }

    /**
     * Calculates the shortest angular difference between two angles in degrees, taking into account the circular nature of angles
     * @param a The first angle in degrees
     * @param b The second angle in degrees
     * @return The shortest angular difference between the two angles in degrees, constrained to the range {@code [-180, 180]}
     */
    public static float shortAngle(float a, float b) {
        return wrapDegrees(b - a);
    }

    /**
     * Linearly interpolates between two angles in degrees, taking into account the circular nature of angles
     * @param a The starting angle in degrees
     * @param b The ending angle in degrees
     * @param t The interpolation factor, typically in the range {@code [0, 1]}
     * @return The interpolated angle in degrees, taking into account the shortest path between the two angles
     */
    public static float lerpAngle(float a, float b, float t) {
        return Math.fma(shortAngle(a, b), t, a);
    }

    /**
     * Linearly interpolates between two {@link Vector2f} angles in degrees, taking into account the circular nature of angles
     * @param a The starting {@link Vector2f} angle in degrees
     * @param b The ending {@link Vector2f} angle in degrees
     * @param t The interpolation factor, typically in the range {@code [0, 1]}
     * @return A new {@link Vector2f} that represents the interpolated angle in degrees, taking into account the shortest path between the two angles
     */
    public static Vector2f lerpAngle(Vector2f a, Vector2f b, float t) {
        return new Vector2f(
                lerpAngle(a.x, b.x, t),
                lerpAngle(a.y, b.y, t)
        );
    }

    /**
     * Linearly interpolates between two {@link Vector3f} angles in degrees, taking into account the circular nature of angles
     * @param a The starting {@link Vector3f} angle in degrees
     * @param b The ending {@link Vector3f} angle in degrees
     * @param t The interpolation factor, typically in the range {@code [0, 1]}
     * @return A new {@link Vector3f} that represents the interpolated angle in degrees, taking into account the shortest path between the two angles
     */
    public static Vector3f lerpAngle(Vector3f a, Vector3f b, float t) {
        return new Vector3f(
                lerpAngle(a.x, b.x, t),
                lerpAngle(a.y, b.y, t),
                lerpAngle(a.z, b.z, t)
        );
    }

    /**
     * Linearly interpolates between values in a float array based on a given interpolation factor<br>
     * The interpolation factor {@code t} is used to determine the position between the values in the array,
     * as if it was a percentage of the total length of the array, yet still allowing for interpolation between the values
     * @param array The array of float values to interpolate between
     * @param t The interpolation factor, typically in the range {@code [0, 1]}
     * @return The interpolated float value based on the provided array and interpolation factor
     */
    public static float lerpInArray(float[] array, float t) {
        int len = array.length;
        if (len == 0)
            return 0;

        float index = t * (len - 1);
        int prev = (int) modulo(Math.floor(index), len);
        int next = (int) modulo(Math.ceil(index), len);

        float indexDelta = index - prev;
        return Math.lerp(array[prev], array[next], indexDelta);
    }

    /**
     * Linearly interpolates between values in a {@link Vector3f} array based on a given interpolation factor<br>
     * The interpolation factor {@code t} is used to determine the position between the values in the array,
     * as if it was a percentage of the total length of the array, yet still allowing for interpolation between the values
     * @param array The array of {@link Vector3f} values to interpolate between
     * @param t The interpolation factor, typically in the range {@code [0, 1]}
     * @return A new {@link Vector3f} that represents the interpolated value based on the provided array and interpolation factor
     */
    public static Vector3f lerpInArray(Vector3f[] array, float t) {
        int len = array.length;
        if (len == 0)
            return new Vector3f();

        float index = t * (len - 1);
        int prev = (int) modulo(Math.floor(index), len);
        int next = (int) modulo(Math.ceil(index), len);

        float indexDelta = index - prev;
        return lerp(array[prev], array[next], indexDelta);
    }

    /**
     * Parses a string representation of a 3D vector into a {@link Vector3f} object, using a specified delimiter to separate the components
     * @param data The string representation of the 3D vector, expected to contain three float values separated by the specified delimiter
     * @param delimiter The character used to separate the components of the vector in the string representation
     * @return A new {@link Vector3f} object representing the parsed 3D vector
     */
    public static Vector3f parseVec3(String data, char delimiter) {
        float x = 0f, y = 0f, z = 0f;

        int count = 0;
        int len = data.length();
        int i = 0;

        while (i < len && count < 3) {
            if (data.charAt(i) == delimiter) {
                i++;
                continue;
            }

            //find the end of the current number
            int start = i;
            while (i < len && data.charAt(i) != delimiter)
                i++;

            //parse the float from the substring
            float val = Float.parseFloat(data.substring(start, i));
            if      (count == 0) x = val;
            else if (count == 1) y = val;
            else if (count == 2) z = val;

            count++;
        }

        return new Vector3f(x, y, z);
    }

    /**
     * Parses a string representation of a 2D vector into a {@link Vector2f} object, using a specified delimiter to separate the components
     * @param data The string representation of the 2D vector, expected to contain two float values separated by the specified delimiter
     * @param delimiter The character used to separate the components of the vector in the string representation
     * @return A new {@link Vector2f} object representing the parsed 2D vector
     */
    public static Vector2f parseVec2(String data, char delimiter) {
        float x = 0f, y = 0f;

        int count = 0;
        int len = data.length();
        int i = 0;

        while (i < len && count < 2) {
            if (data.charAt(i) == delimiter) {
                i++;
                continue;
            }

            //find the end of the current number
            int start = i;
            while (i < len && data.charAt(i) != delimiter)
                i++;

            //parse the float from the substring
            float val = Float.parseFloat(data.substring(start, i));
            if      (count == 0) x = val;
            else if (count == 1) y = val;

            count++;
        }

        return new Vector2f(x, y);
    }

    /**
     * Converts rotation angles {@code (pitch, yaw)} in degrees to a direction vector in 3D space
     * @param pitch The pitch angle in degrees, representing the vertical rotation
     * @param yaw The yaw angle in degrees, representing the horizontal rotation
     * @return A new {@link Vector3f} representing the direction vector corresponding to the specified rotation angles
     * @see #dirToRot(Vector3f)
     */
    public static Vector3f rotToDir(float pitch, float yaw) {
        float p = Math.toRadians(pitch + 180f);
        float y = Math.toRadians(-yaw);
        float cosP = Math.cos(p);
        return new Vector3f(Math.sin(y) * cosP, Math.sin(p), Math.cos(y) * cosP);
    }

    /**
     * Converts a rotation angle in degrees to a 2D direction vector
     * @param degrees The rotation angle in degrees, representing the direction in 2D space
     * @return A new {@link Vector2f} representing the direction vector corresponding to the specified rotation angle
     * @see #dirToRot(Vector2f)
     */
    public static Vector2f rotToDir(float degrees) {
        float angle = Math.toRadians(degrees);
        return new Vector2f(Math.cos(angle), Math.sin(angle));
    }

    /**
     * Converts a direction vector in 3D space to rotation angles {@code (pitch, yaw)} in degrees
     * @param dir The direction vector in 3D space, expected to be normalized
     * @return A new {@link Vector2f} representing the rotation angles {@code (pitch, yaw)} in degrees corresponding to the specified direction vector
     * @see #rotToDir(float, float)
     * @see #dirToRot(float, float, float)
     */
    public static Vector2f dirToRot(Vector3f dir) {
        return dirToRot(dir.x, dir.y, dir.z);
    }

    /**
     * Converts a normalized direction vector in 3D space to rotation angles {@code (pitch, yaw)} in degrees
     * @param x The x component of the direction vector
     * @param y The y component of the direction vector
     * @param z The z component of the direction vector
     * @return A new {@link Vector2f} representing the rotation angles {@code (pitch, yaw)} in degrees corresponding to the specified direction vector
     * @see #rotToDir(float, float)
     * @see #dirToRot(Vector3f)
     */
    public static Vector2f dirToRot(float x, float y, float z) {
        float pitch = Math.toDegrees(safeAsin(-y));
        float yaw = Math.toDegrees(Math.atan2(z, x));
        return new Vector2f(pitch, yaw + 90f);
    }

    /**
     * Converts a direction vector in 2D space to a rotation angle in degrees
     * @param dir The direction vector in 2D space, expected to be normalized
     * @return The rotation angle in degrees corresponding to the specified direction vector
     * @see #rotToDir(float)
     * @see #dirToRot(float, float)
     */
    public static float dirToRot(Vector2f dir) {
        return dirToRot(dir.x, dir.y);
    }

    /**
     * Converts a normalized direction vector in 2D space to a rotation angle in degrees
     * @param x The x component of the direction vector
     * @param y The y component of the direction vector
     * @return The rotation angle in degrees corresponding to the specified direction vector
     * @see #rotToDir(float)
     * @see #dirToRot(Vector2f)
     */
    public static float dirToRot(float x, float y) {
        return Math.toDegrees(Math.atan2(y, x));
    }

    /**
     * Calculates the modulo of a float value {@code a} with respect to a float value {@code n}, ensuring that the result is always non-negative
     * @param a The float value to be reduced modulo {@code n}
     * @param n The float value representing the modulus
     * @return The non-negative result of {@code a} modulo {@code n}, constrained to the range {@code [0, n]}
     * @see #modulo(int, int)
     */
    public static float modulo(float a, float n) {
        return (a % n + n) % n;
    }

    /**
     * Calculates the modulo of an integer value {@code a} with respect to an integer value {@code n}, ensuring that the result is always non-negative
     * @param a The integer value to be reduced modulo {@code n}
     * @param n The integer value representing the modulus
     * @return The non-negative result of {@code a} modulo {@code n}, constrained to the range {@code [0, n]}
     * @see #modulo(float, float)
     */
    public static int modulo(int a, int n) {
        return (a % n + n) % n;
    }

    /**
     * Calculates the ratio of a value {@code x} within a specified range defined by {@code min} and {@code max}
     * @param x The value for which the ratio is to be calculated
     * @param min The minimum value of the range
     * @param max The maximum value of the range
     * @return The ratio of {@code x} within the range defined by {@code min} and {@code max}
     */
    public static float ratio(float x, float min, float max) {
        return (x - min) / (max - min);
    }

    /**
     * Maps a value {@code x} from one range defined by {@code min1} and {@code max1} to another range defined by {@code min2} and {@code max2}
     * @param x The value to be mapped from the first range to the second range
     * @param min1 The minimum value of the first range
     * @param max1 The maximum value of the first range
     * @param min2 The minimum value of the second range
     * @param max2 The maximum value of the second range
     * @return The mapped value of {@code x} in the second range defined by {@code min2} and {@code max2}
     */
    public static float map(float x, float min1, float max1, float min2, float max2) {
        return Math.lerp(min2, max2, ratio(x, min1, max1));
    }

    /**
     * Safely clamps a float value {@code n} between a minimum value {@code min} and a maximum value {@code max}<br>
     * If {@code min} is greater than {@code max}, the values are swapped to ensure proper clamping
     * @param n The float value to be clamped
     * @param min The minimum value to which {@code n} should be clamped
     * @param max The maximum value to which {@code n} should be clamped
     * @return The clamped float value, constrained to the range defined by {@code min} and {@code max}
     * @see #clamp(int, int, int)
     */
    public static float clamp(float n, float min, float max) {
        return min > max ? Math.clamp(max, min, n) : Math.clamp(min, max, n);
    }

    /**
     * Safely clamps an integer value {@code n} between a minimum value {@code min} and a maximum value {@code max}<br>
     * If {@code min} is greater than {@code max}, the values are swapped to ensure proper clamping
     * @param n The integer value to be clamped
     * @param min The minimum value to which {@code n} should be clamped
     * @param max The maximum value to which {@code n} should be clamped
     * @return The clamped integer value, constrained to the range defined by {@code min} and {@code max}
     * @see #clamp(float, float, float)
     */
    public static int clamp(int n, int min, int max) {
        return min > max ? Math.clamp(max, min, n) : Math.clamp(min, max, n);
    }

    /**
     * Wraps a float value {@code n} within a specified range defined by {@code min} and {@code max}<br>
     * If {@code min} is greater than {@code max}, the values are swapped to ensure proper wrapping
     * @param n The float value to be wrapped
     * @param min The minimum value of the range
     * @param max The maximum value of the range
     * @return The wrapped float value, constrained to the range defined by {@code min} and {@code max}
     */
    public static float clampWarp(float n, float min, float max) {
        if (min > max) {
            float temp = min;
            min = max;
            max = temp;
        }

        float range = max - min;
        return n - range * Math.floor((n - min) / range);
    }

    /**
     * Calculates the power of an integer base {@code a} raised to a positive integer exponent {@code b}
     * @param a The integer base value to be raised to the power of {@code b}
     * @param b The positive integer exponent value to which the base {@code a} is raised
     * @return The result of {@code a} raised to the power of {@code b}
     */
    public static int pow(int a, int b) {
        int result = 1;
        while (b > 0) {
            //if exponent is odd, multiply base with result
            if ((b & 1) == 1)
                result *= a;
            //square the base and shift exponent right (divide by 2)
            a *= a;
            b >>= 1;
        }
        return result;
    }

    /**
     * Wraps the {@link java.lang.Math#pow(double, double)} method to work with float values, returning a float result
     * @param a The float base value to be raised to the power of {@code b}
     * @param b The float exponent value to which the base {@code a} is raised
     * @return The result of {@code a} raised to the power of {@code b}, returned as a float
     */
    public static float pow(float a, float b) {
        return (float) java.lang.Math.pow(a, b);
    }

    /**
     * Safely calculates the arcsine of a float value {@code x}, ensuring that the input is clamped to the valid range of {@code [-1, 1]} for the arcsine function
     * @param x The float value for which the arcsine is to be calculated, expected to be in the range {@code [-1, 1]}
     * @return The arcsine of the clamped value of {@code x}, returned in radians
     */
    public static float safeAsin(float x) {
        return Math.asin(clamp(x, -1f, 1f));
    }

    /**
     * Calculates the reflection of a {@link Vector3f} direction off a surface with a given {@link Vector3f} normal
     * @param dir The direction {@link Vector3f} representing the incoming direction of the reflection
     * @param normal The normal {@link Vector3f} of the surface off which the reflection occurs, expected to be normalized
     * @return A new {@link Vector3f} representing the reflected direction based on the incoming direction and surface normal
     * @see #reflect(Vector2f, Vector2f)
     */
    public static Vector3f reflect(Vector3f dir, Vector3f normal) {
        //r = d - 2 * (d dot n) * n
        float dot = dir.dot(normal) * 2;
        return dir.sub(normal.x * dot, normal.y * dot, normal.z * dot, new Vector3f());
    }

    /**
     * Calculates the reflection of a {@link Vector2f} direction off a surface with a given {@link Vector2f} normal
     * @param dir The direction {@link Vector2f} representing the incoming direction of the reflection
     * @param normal The normal {@link Vector2f} of the surface off which the reflection occurs, expected to be normalized
     * @return A new {@link Vector2f} representing the reflected direction based on the incoming direction and surface normal
     * @see #reflect(Vector3f, Vector3f)
     */
    public static Vector2f reflect(Vector2f dir, Vector2f normal) {
        float dot = dir.dot(normal) * 2;
        return dir.sub(normal.x * dot, normal.y * dot, new Vector2f());
    }

    /**
     * Converts a {@link Vector3f} representing rotation angles in degrees to radians
     * @param vec The {@link Vector3f} representing rotation angles in degrees
     * @return A new {@link Vector3f} representing the rotation angles converted to radians
     * @see #toDegrees(Vector3f)
     */
    public static Vector3f toRadians(Vector3f vec) {
        return new Vector3f(
                Math.toRadians(vec.x),
                Math.toRadians(vec.y),
                Math.toRadians(vec.z)
        );
    }

    /**
     * Converts a {@link Vector2f} representing rotation angles in degrees to radians
     * @param vec The {@link Vector2f} representing rotation angles in degrees
     * @return A new {@link Vector2f} representing the rotation angles converted to radians
     * @see #toDegrees(Vector2f)
     */
    public static Vector2f toRadians(Vector2f vec) {
        return new Vector2f(
                Math.toRadians(vec.x),
                Math.toRadians(vec.y)
        );
    }

    /**
     * Converts a {@link Vector3f} representing rotation angles in radians to degrees
     * @param vec The {@link Vector3f} representing rotation angles in radians
     * @return A new {@link Vector3f} representing the rotation angles converted to degrees
     * @see #toRadians(Vector3f)
     */
    public static Vector3f toDegrees(Vector3f vec) {
        return new Vector3f(
                Math.toDegrees(vec.x),
                Math.toDegrees(vec.y),
                Math.toDegrees(vec.z)
        );
    }

    /**
     * Converts a {@link Vector2f} representing rotation angles in radians to degrees
     * @param vec The {@link Vector2f} representing rotation angles in radians
     * @return A new {@link Vector2f} representing the rotation angles converted to degrees
     * @see #toRadians(Vector2f)
     */
    public static Vector2f toDegrees(Vector2f vec) {
        return new Vector2f(
                Math.toDegrees(vec.x),
                Math.toDegrees(vec.y)
        );
    }

    /**
     * Converts a {@link Quaternionf} to Euler angles in degrees, returning a {@link Vector3f} representing the pitch, yaw, and roll angles
     * @param quat The {@link Quaternionf} to be converted to Euler angles
     * @return A new {@link Vector3f} representing the Euler angles in degrees corresponding to the specified quaternion
     * @see #getPitch(Quaternionf)
     * @see #getYaw(Quaternionf)
     * @see #getRoll(Quaternionf)
     */
    public static Vector3f quatToEuler(Quaternionf quat) {
        return new Vector3f(getPitch(quat), getYaw(quat), getRoll(quat));
    }

    /**
     * Calculates the pitch angle in degrees from a given {@link Quaternionf}
     * @param quat The {@link Quaternionf} from which the pitch angle is to be calculated
     * @return The pitch angle in degrees corresponding to the specified quaternion
     * @see #getYaw(Quaternionf)
     * @see #getRoll(Quaternionf)
     */
    public static float getPitch(Quaternionf quat) {
        return Math.toDegrees(Math.atan2(-2f * quat.x * quat.w + 2f * quat.y * quat.z, 1f - 2f * quat.x * quat.x - 2f * quat.z * quat.z));
    }

    /**
     * Calculates the yaw angle in degrees from a given {@link Quaternionf}
     * @param quat The {@link Quaternionf} from which the yaw angle is to be calculated
     * @return The yaw angle in degrees corresponding to the specified quaternion
     * @see #getPitch(Quaternionf)
     * @see #getRoll(Quaternionf)
     */
    public static float getYaw(Quaternionf quat) {
        return Math.toDegrees(Math.atan2(-2f * quat.y * quat.w + 2f * quat.x * quat.z, 1f - 2f * quat.y * quat.y - 2f * quat.z * quat.z));
    }

    /**
     * Calculates the roll angle in degrees from a given {@link Quaternionf}
     * @param quat The {@link Quaternionf} from which the roll angle is to be calculated
     * @return The roll angle in degrees corresponding to the specified quaternion
     * @see #getPitch(Quaternionf)
     * @see #getYaw(Quaternionf)
     */
    public static float getRoll(Quaternionf quat) {
        return Math.toDegrees(safeAsin(-2f * quat.x * quat.y - 2f * quat.z * quat.w));
    }

    /**
     * Converts a direction {@link Vector3f} to a {@link Quaternionf} representing the rotation needed to face that direction
     * @param dir The direction {@link Vector3f} to be converted to a quaternion
     * @return A new {@link Quaternionf} representing the rotation needed to face the specified direction
     * @see #quatToDir(Quaternionf)
     * @see #dirToQuat(float, float, float)
     */
    public static Quaternionf dirToQuat(Vector3f dir) {
        return dirToQuat(dir.x, dir.y, dir.z);
    }

    /**
     * Converts a direction vector defined by its components {@code (x, y, z)} to a {@link Quaternionf} representing the rotation needed to face that direction
     * @param x The x component of the direction vector
     * @param y The y component of the direction vector
     * @param z The z component of the direction vector
     * @return A new {@link Quaternionf} representing the rotation needed to face the specified direction
     * @see #quatToDir(Quaternionf)
     * @see #dirToQuat(Vector3f)
     */
    public static Quaternionf dirToQuat(float x, float y, float z) {
        float pitch = safeAsin(y);
        float yaw = -Math.atan2(x, z);
        return new Quaternionf().rotationZYX(0f, -yaw, -pitch);
    }

    /**
     * Converts rotation angles {@code (pitch, yaw)} in degrees to a {@link Quaternionf} representing the corresponding rotation<br>
     * Rotation is set in the {@code ZYX} order, where {@code Z} is {@code roll}, {@code Y} is {@code yaw}, and {@code X} is {@code pitch}
     * @param rot The {@link Vector2f} representing rotation angles in degrees, where {@code rot.x} is the pitch and {@code rot.y} is the yaw
     * @return A new {@link Quaternionf} representing the rotation corresponding to the specified rotation angles
     * @see #rotToQuat(float, float)
     */
    public static Quaternionf rotToQuat(Vector2f rot) {
        return rotToQuat(rot.x, rot.y);
    }

    /**
     * Converts rotation angles {@code (pitch, yaw)} in degrees to a {@link Quaternionf} representing the corresponding rotation<br>
     * Rotation is set in the {@code ZYX} order, where {@code Z} is {@code roll}, {@code Y} is {@code yaw}, and {@code X} is {@code pitch}
     * @param pitch The pitch angle in degrees, representing the vertical rotation
     * @param yaw The yaw angle in degrees, representing the horizontal rotation
     * @return A new {@link Quaternionf} representing the rotation corresponding to the specified rotation angles
     * @see #rotToQuat(Vector2f)
     */
    public static Quaternionf rotToQuat(float pitch, float yaw) {
        return new Quaternionf().rotationZYX(0f, Math.toRadians(-yaw), Math.toRadians(-pitch));
    }

    /**
     * Converts rotation angles {@code (pitch, yaw, roll)} in degrees to a {@link Quaternionf} representing the corresponding rotation<br>
     * Rotation is set in the {@code ZYX} order, where {@code Z} is {@code roll}, {@code Y} is {@code yaw}, and {@code X} is {@code pitch}
     * @param rot The {@link Vector3f} representing rotation angles in degrees, where {@code rot.x} is the pitch, {@code rot.y} is the yaw, and {@code rot.z} is the roll
     * @return A new {@link Quaternionf} representing the rotation corresponding to the specified rotation angles
     * @see #rotToQuat(float, float, float)
     */
    public static Quaternionf rotToQuat(Vector3f rot) {
        return rotToQuat(rot.x, rot.y, rot.z);
    }

    /**
     * Converts rotation angles {@code (pitch, yaw, roll)} in degrees to a {@link Quaternionf} representing the corresponding rotation<br>
     * Rotation is set in the {@code ZYX} order, where {@code Z} is {@code roll}, {@code Y} is {@code yaw}, and {@code X} is {@code pitch}
     * @param pitch The pitch angle in degrees, representing the vertical rotation
     * @param yaw The yaw angle in degrees, representing the horizontal rotation
     * @param roll The roll angle in degrees, representing the rotation around the forward axis
     * @return A new {@link Quaternionf} representing the rotation corresponding to the specified rotation angles
     * @see #rotToQuat(Vector3f)
     */
    public static Quaternionf rotToQuat(float pitch, float yaw, float roll) {
        return new Quaternionf().rotationZYX(Math.toRadians(roll), Math.toRadians(-yaw), Math.toRadians(-pitch));
    }

    /**
     * Converts a {@link Quaternionf} to a direction {@link Vector3f} in 3D space, representing the forward direction of the rotation defined by the quaternion
     * @param quat The {@link Quaternionf} to be converted to a direction vector
     * @return A new {@link Vector3f} representing the forward direction corresponding to the specified quaternion
     * @see #dirToQuat(Vector3f)
     */
    public static Vector3f quatToDir(Quaternionf quat) {
        return new Vector3f(0f, 0f, -1f).rotate(quat);
    }

    /**
     * Calculates the normal vector of a triangle defined by three points in 3D space
     * @param p1 The first point of the triangle
     * @param p2 The second point of the triangle
     * @param p3 The third point of the triangle
     * @return A new {@link Vector3f} representing the normal vector of the triangle defined by the three points
     */
    public static Vector3f normal(Vector3f p1, Vector3f p2, Vector3f p3) {
        //calculate the cross product of two vectors to get the normal
        Vector3f edge1 = p2.sub(p1, new Vector3f());
        Vector3f edge2 = p3.sub(p1, new Vector3f());
        return edge1.cross(edge2);
    }

    /**
     * Determines whether a point is inside a triangle defined by three points in 3D space
     * @param a The first point of the triangle
     * @param b The second point of the triangle
     * @param c The third point of the triangle
     * @param point The point to be tested for inclusion within the triangle
     * @return {@code true} if the point is inside the triangle, {@code false} otherwise
     */
    public static boolean isPointInTriangle(Vector3f a, Vector3f b, Vector3f c, Vector3f point) {
        //calculate the normals of the triangle and our point
        Vector3f normalABC = normal(a, b, c);
        Vector3f normalPAB = normal(point, a, b);
        Vector3f normalPBC = normal(point, b, c);
        Vector3f normalPCA = normal(point, c, a);

        //check if the point is inside the triangle
        return normalABC.dot(normalPAB) >= 0 && normalABC.dot(normalPBC) >= 0 && normalABC.dot(normalPCA) >= 0;
    }

    /**
     * Generates a random float value within a specified range defined by {@code min} and {@code max}
     * @param min The minimum value of the range
     * @param max The maximum value of the range
     * @return A random float value within the range defined by {@code min} and {@code max}
     * @see #range(int, int)
     */
    public static float range(float min, float max) {
        return (float) (Math.random() * (max - min) + min);
    }

    /**
     * Generates a random integer value within a specified range defined by {@code min} and {@code max}
     * @param min The minimum value of the range
     * @param max The maximum value of the range
     * @return A random integer value within the range defined by {@code min} and {@code max}
     * @see #range(float, float)
     */
    public static int range(int min, int max) {
        return (int) (Math.random() * (max - min) + min);
    }

    /**
     * Calculates the minimum component value of a {@link Vector3f}
     * @param vec The {@link Vector3f} for which the minimum component value is to be calculated
     * @return The minimum component value of the specified vector
     */
    public static float min(Vector3f vec) {
        return Math.min(vec.x, Math.min(vec.y, vec.z));
    }

    /**
     * Calculates the maximum component value of a {@link Vector3f}
     * @param vec The {@link Vector3f} for which the maximum component value is to be calculated
     * @return The maximum component value of the specified vector
     */
    public static float max(Vector3f vec) {
        return Math.max(vec.x, Math.max(vec.y, vec.z));
    }

    /**
     * Calculates the index of the maximum component value of a {@link Vector3f}
     * @param vec The {@link Vector3f} for which the index of the maximum component value is to be calculated
     * @return The index of the maximum component value of the specified vector,
     * where {@code 0} corresponds to the x component, {@code 1} corresponds to the y component, and {@code 2} corresponds to the z component
     * @see #minIndex(Vector3f)
     */
    public static int maxIndex(Vector3f vec) {
        if (vec.x >= vec.y && vec.x >= vec.z) {
            return 0;
        } else if (vec.y >= vec.z) {
            return 1;
        } else {
            return 2;
        }
    }

    /**
     * Calculates the index of the minimum component value of a {@link Vector3f}
     * @param vec The {@link Vector3f} for which the index of the minimum component value is to be calculated
     * @return The index of the minimum component value of the specified vector,
     * where {@code 0} corresponds to the x component, {@code 1} corresponds to the y component, and {@code 2} corresponds to the z component
     * @see #maxIndex(Vector3f)
     */
    public static int minIndex(Vector3f vec) {
        if (vec.x < vec.y && vec.x < vec.z) {
            return 0;
        } else if (vec.y < vec.z) {
            return 1;
        } else {
            return 2;
        }
    }

    /**
     * Checks if any component of a {@link Vector3f} is @{code NaN} (Not a Number)
     * @param vec The {@link Vector3f} to be checked for @{code NaN} components
     * @return {@code true} if any component of the vector is @{code NaN}, {@code false} otherwise
     */
    public static boolean isNaN(Vector3f vec) {
        return Float.isNaN(vec.x) || Float.isNaN(vec.y) || Float.isNaN(vec.z);
    }

    /**
     * Checks if any component of a {@link Vector2f} is @{code NaN} (Not a Number)
     * @param vec The {@link Vector2f} to be checked for @{code NaN} components
     * @return {@code true} if any component of the vector is @{code NaN}, {@code false} otherwise
     */
    public static boolean isNaN(Vector2f vec) {
        return Float.isNaN(vec.x) || Float.isNaN(vec.y);
    }

    /**
     * Checks if any component of a {@link Quaternionf} is @{code NaN} (Not a Number)
     * @param quat The {@link Quaternionf} to be checked for @{code NaN} components
     * @return {@code true} if any component of the quaternion is @{code NaN}, {@code false} otherwise
     */
    public static boolean isNaN(Quaternionf quat) {
        return Float.isNaN(quat.x) || Float.isNaN(quat.y) || Float.isNaN(quat.z) || Float.isNaN(quat.w);
    }

    /**
     * Applies a translation transformation to a {@link Matrix3f} by modifying its elements based on the specified translation values {@code (x, y)}
     * @param mat The {@link Matrix3f} to which the translation transformation is to be applied
     * @param x The translation value along the x axis
     * @param y The translation value along the y axis
     * @return The modified {@link Matrix3f} after applying the translation transformation
     */
    public static Matrix3f translateMat3(Matrix3f mat, float x, float y) {
        mat.m00 += x * mat.m20;
        mat.m01 += x * mat.m21;
        mat.m02 += x * mat.m22;

        mat.m10 += y * mat.m20;
        mat.m11 += y * mat.m21;
        mat.m12 += y * mat.m22;

        return mat;
    }

    /**
     * Calculates the next power of two greater than or equal to a given float value {@code x}
     * @param x The float value for which the next power of two is to be calculated
     * @return The next power of two greater than or equal to {@code x}
     */
    public static int nextPowerOfTwo(float x) {
        int value = (int) Math.ceil(x);
        int power = 1;
        while (power < value)
            power <<= 1;
        return power;
    }

    /**
     * Selects a random element from an array of type {@code T} and returns it
     * @param arr The array of type {@code T} from which a random element is to be selected
     * @return A random element from the specified array
     */
    public static <T> T randomArr(T[] arr) {
        return arr[(int) (Math.random() * arr.length)];
    }

    private static final String[] SIZE_UNITS = {"b", "kb", "mb", "gb"};

    /**
     * Formats a byte size value into a human-readable string representation with appropriate units (bytes, kilobytes, megabytes, gigabytes)
     * @param size The byte size value to be formatted
     * @return A string representation of the byte size value with appropriate units, formatted to two decimal places
     */
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

    /**
     * Calculates an approximation of the inverse cube root of a float value {@code x}
     * @param x The float value for which the inverse cube root is to be calculated
     * @return An approximation of the inverse cube root of {@code x}
     */
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

    /**
     * Calculates the factorial of a non-negative integer {@code n} recursively
     * @param n The non-negative integer for which the factorial is to be calculated
     * @return The factorial of {@code n}, calculated recursively
     */
    public static float factorial(int n) {
        return n <= 1 ? 1f : n * factorial(n - 1);
    }

    /**
     * Generates a permutation of the provided elements based on the specified index, returning a string representation of the permutation
     * @param index The index of the permutation to be generated, where {@code 0} corresponds to the first permutation and {@code (n! - 1)} corresponds to the last permutation
     * @param elements The elements to be permuted
     * @return A string representation of the permutation of the provided elements based on the specified index
     * @see #factorial(int)
     */
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

    /**
     * Generates a random direction vector in 3D space, represented as a {@link Vector3f}, with random pitch and yaw angles
     * @return A new {@link Vector3f} representing a random direction in 3D space
     */
    public static Vector3f randomDir() {
        float pitch = (float) Math.random() * 360;
        float yaw = (float) Math.random() * 360;
        return rotToDir(pitch, yaw);
    }

    /**
     * Generates a random direction {@link Vector3f} in 3D space with a specified spread defined by pitch and yaw angles, based on an initial {@link Vector3f} direction
     * @param dir The initial direction {@link Vector3f} from which the random spread is calculated
     * @param pitch The maximum pitch angle in degrees for the random spread
     * @param yaw The maximum yaw angle in degrees for the random spread
     * @return A new {@link Vector3f} representing a random direction in 3D space with the specified spread from the initial direction
     */
    public static Vector3f spread(Vector3f dir, float pitch, float yaw) {
        float r1 = Math.toRadians(((float) Math.random() * 2f - 1f) * yaw);
        float r2 = Math.toRadians(((float) Math.random() * 2f - 1f) * pitch);

        Vector3f rotVec = new Vector3f(
                Math.sin(r1) * Math.cos(r2),
                Math.sin(r2),
                Math.cos(r1) * Math.cos(r2)
        );

        return rotVec.rotate(dirToQuat(dir));
    }

    /**
     * Performs a binary search within a specified range defined by {@code start} and {@code end}, using a provided {@link Predicate} to test for the desired condition
     * @param start The starting index of the search range (inclusive)
     * @param end The ending index of the search range (exclusive)
     * @param test A {@link Predicate} that tests whether a given index satisfies the desired condition
     * @return The index of the first element in the range that satisfies the condition defined by the {@code test} predicate, or {@code end} if no such element is found
     */
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

    /**
     * Calculates the Halton sequence value for a given index and base, returning a float value in the range {@code [0, 1]}
     * @param index The index of the Halton sequence to be calculated, where {@code 0} corresponds to the first value in the sequence
     * @param base The base value used to generate the Halton sequence, which determines the distribution of the sequence values
     * @return A float value in the range {@code [0, 1]} representing the Halton sequence value for the specified index and base
     */
    public static float haltonSequence(int index, int base) {
        float result = 0f;
        float f = 1f / base;
        int i = index;

        while (i > 0) {
            result += f * (i % base);
            i = (int) Math.floor(i / (float) base);
            f /= base;
        }

        return result;
    }

    /**
     * Calculates a point on a Hermite curve defined by two endpoints {@code p0} and {@code p3},
     * with tangents {@code r0} and {@code r3}, using a specified weight and parameter {@code t}
     * @param p0 The first endpoint of the Hermite curve
     * @param p3 The second endpoint of the Hermite curve
     * @param r0 The tangent vector at the first endpoint {@code p0}
     * @param r3 The tangent vector at the second endpoint {@code p3}
     * @param weight The weight factor that changes the influence of the tangents on the curve shape
     * @param t The parameter value in the range {@code [0, 1]} that determines the position along the Hermite curve
     * @return A float value representing the point on the Hermite curve corresponding to the specified parameter {@code t}
     */
    public static float hermite(float p0, float p3, float r0, float r3, float weight, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return (2 * t3 - 3 * t2 + 1) * p0 +
                (-2 * t3 + 3 * t2) * p3 +
                (t3 - 2 * t2 + t) * weight * r0 +
                (t3 - t2) * weight * r3;
    }

    /**
     * Calculates a point on a cubic Bezier curve defined by four control points {@code p0}, {@code p1}, {@code p2}, and {@code p3},
     * using a specified parameter {@code t} in the range {@code [0, 1]}
     * @param p0 The first control point of the Bezier curve
     * @param p1 The second control point of the Bezier curve
     * @param p2 The third control point of the Bezier curve
     * @param p3 The fourth control point of the Bezier curve
     * @param t The parameter value in the range {@code [0, 1]} that determines the position along the Bezier curve
     * @return A float value representing the point on the Bezier curve corresponding to the specified parameter {@code t}
     */
    public static float bezier(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return (-t3 + 3 * t2 - 3 * t + 1) * p0 +
                (3 * t3 - 6 * t2 + 3 * t) * p1 +
                (-3 * t3 + 3 * t2) * p2 +
                t3 * p3;
    }

    /**
     * Calculates a point on a B-spline curve defined by four control points {@code p0}, {@code p1}, {@code p2}, and {@code p3},
     * using a specified parameter {@code t} in the range {@code [0, 1]}
     * @param p0 The first control point of the B-spline curve
     * @param p1 The second control point of the B-spline curve
     * @param p2 The third control point of the B-spline curve
     * @param p3 The fourth control point of the B-spline curve
     * @param t The parameter value in the range {@code [0, 1]} that determines the position along the B-spline curve
     * @return A float value representing the point on the B-spline curve corresponding to the specified parameter {@code t}
     */
    public static float bSpline(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return ((-t3 + 3 * t2 - 3 * t + 1) * p0 +
                (3 * t3 - 6 * t2 + 4) * p1 +
                (-3 * t3 + 3 * t2 + 3 * t + 1) * p2 +
                t3 * p3) / 6f;
    }

    /**
     * Calculates a point on a Bezier curve using the De Casteljau's algorithm, given a parameter {@code t} in the range {@code [0, 1]} and an array of control points
     * @param t The parameter value in the range {@code [0, 1]} that determines the position along the Bezier curve
     * @param controlPoints An array of control points defining the Bezier curve, where each element represents a control point value
     * @return A float value representing the point on the Bezier curve corresponding to the specified parameter {@code t} and control points
     */
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

    /**
     * Calculates a point on a Catmull-Rom spline defined by four control points {@code p0}, {@code p1}, {@code p2}, and {@code p3},
     * using a specified parameter {@code t} in the range {@code [0, 1]}
     * @param p0 The first control point of the Catmull-Rom spline
     * @param p1 The second control point of the Catmull-Rom spline
     * @param p2 The third control point of the Catmull-Rom spline
     * @param p3 The fourth control point of the Catmull-Rom spline
     * @param t The parameter value in the range {@code [0, 1]} that determines the position along the Catmull-Rom spline
     * @return A float value representing the point on the Catmull-Rom spline corresponding to the specified parameter {@code t}
     */
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

    /**
     * Enum containing various easing functions for smooth interpolation between values, commonly used in animations and transitions<br>
     * Each easing function takes a float value {@code x} in the range {@code [0, 1]} and returns a float value representing the eased output
     */
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
