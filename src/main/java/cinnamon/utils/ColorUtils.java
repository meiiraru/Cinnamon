package cinnamon.utils;

import org.joml.Vector3f;
import org.joml.Vector4f;

import java.awt.*;

public class ColorUtils {

    /**
     * splits a color integer into its channels
     * @param color - integer to split
     * @param len - channels length
     * @return an int array of the split int
     */
    public static int[] split(int color, int len) {
        int[] array = new int[len];
        for (int i = 0; i < len; i++) {
            int shift = (len * 8) - ((i + 1) * 8);
            array[i] = color >> shift & 0xFF;
        }

        return array;
    }

    /**
     * converts an RGB integer color (0 - 255) to an RGB (0 - 1) {@link org.joml.Vector3f}
     * @param color - the RGB integer color
     * @return an RGB {@link org.joml.Vector3f} of that color
     */
    public static Vector3f intToRGB(int color) {
        int[] rgb = split(color, 3);
        return new Vector3f(rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f);
    }

    /**
     * converts an RGB (0 - 1) {@link org.joml.Vector3f} to an RGB integer color (0 - 255)
     * @param color - the RGB {@link org.joml.Vector3f} color
     * @return an RGB integer of that color
     */
    public static int rgbToInt(Vector3f color) {
        int hex = (int) (color.x * 0xFF);
        hex = (hex << 8) + (int) (color.y * 0xFF);
        hex = (hex << 8) + (int) (color.z * 0xFF);
        return hex;
    }

    /**
     * converts an RGBA (0 - 1) {@link org.joml.Vector4f} to an ARGB integer color (0 - 255)
     * @param color - the RGBA {@link org.joml.Vector4f} color
     * @return an ARGB integer of that color
     */
    public static int rgbaToIntARGB(Vector4f color) {
        int hex = (int) (color.w * 0xFF);
        hex = (hex << 8) + (int) (color.x * 0xFF);
        hex = (hex << 8) + (int) (color.y * 0xFF);
        hex = (hex << 8) + (int) (color.z * 0xFF);
        return hex;
    }

    /**
     * converts an RGBA integer color (0 - 255) to an RGBA (0 - 1) {@link org.joml.Vector4f}
     * @param color - the RGBA integer color
     * @return an RGBA {@link org.joml.Vector4f} of that color
     */
    public static Vector4f intToRGBA(int color) {
        int[] rgba = split(color, 4);
        return new Vector4f(rgba[0] / 255f, rgba[1] / 255f, rgba[2] / 255f, rgba[3] / 255f);
    }

    /**
     * converts an ARGB integer color (0 - 255) to an RGBA (0 - 1) {@link org.joml.Vector4f}
     * @param color - the ARGB integer color
     * @return an RGBA {@link org.joml.Vector4f} of that color
     */
    public static Vector4f argbIntToRGBA(int color) {
        int[] argb = split(color, 4);
        return new Vector4f(argb[1] / 255f, argb[2] / 255f, argb[3] / 255f, argb[0] / 255f);
    }

    /**
     * converts an HSV (0 - 1) {@link org.joml.Vector3f} to an RGB (0 - 1) {@link org.joml.Vector3f}
     * @param color - the HSV {@link org.joml.Vector3f} color
     * @return an RGB {@link org.joml.Vector3f} of that color
     */
    public static Vector3f hsvToRGB(Vector3f color) {
        int hex = Color.HSBtoRGB(color.x, color.y, color.z);
        return intToRGB(hex);
    }

    /**
     * converts an RGB (0 - 1) {@link org.joml.Vector3f} to an HSV (0 - 1) {@link org.joml.Vector3f}
     * @param color - the RGB {@link org.joml.Vector3f} color
     * @return an HSV {@link org.joml.Vector3f} of that color
     */
    public static Vector3f rgbToHSV(Vector3f color) {
        float[] hsv = Color.RGBtoHSB((int) (color.x * 255f), (int) (color.y * 255f), (int) (color.z * 255f), null);
        return new Vector3f(hsv[0], hsv[1], hsv[2]);
    }

    /**
     * converts an RGB (0 - 1) {@link org.joml.Vector3f} to a Hexadecimal (00 - FF) string
     * the return string will always be 6 characters long without the "#" prefix
     * @param color - the RGB {@link org.joml.Vector3f} color
     * @return a Hexadecimal string of that color
     */
    public static String rgbToHex(Vector3f color) {
        String str = Integer.toHexString(ColorUtils.rgbToInt(color));
        return "0".repeat(Math.max(6 - str.length(), 0)) + str;
    }

    /**
     * converts a Hexadecimal (00 - FF) string to an RGB (0 - 1) {@link org.joml.Vector3f}
     * <p>
     * if the string is invalid, it will return an empty {@link org.joml.Vector3f}
     * @param color - the Hexadecimal string color
     * @return an RGB {@link org.joml.Vector3f} of that color
     * @see ColorUtils#hexStringToRGB(String, org.joml.Vector3f)
     */
    public static Vector3f hexStringToRGB(String color) {
        return hexStringToRGB(color, new Vector3f());
    }

    /**
     * converts a Hexadecimal (00 - FF) string to an RGB (0 - 1) {@link org.joml.Vector3f}
     * <p>
     * if the string is invalid, it will return the fallback color
     * <p>
     * the string is allowed to have the "#" prefix
     * <p>
     * it is also accepted to have a short (RGB) for the string, having 3 characters in total,
     * this short string will be expanded to a full (RRGGBB) 6 characters string
     * @param color - the Hexadecimal string color
     * @param fallbackColor - the fallback color if the string is invalid
     * @return an RGB {@link org.joml.Vector3f} of that color
     */
    public static Vector3f hexStringToRGB(String color, Vector3f fallbackColor) {
        if (color == null || color.isBlank())
            return fallbackColor;

        //parse hex color
        StringBuilder hex = new StringBuilder(color);

        if (hex.toString().startsWith("#")) hex = new StringBuilder(hex.substring(1));

        //short hex
        if (hex.length() == 3) {
            char[] bgChar = hex.toString().toCharArray();
            hex = new StringBuilder("" + bgChar[0] + bgChar[0] + bgChar[1] + bgChar[1] + bgChar[2] + bgChar[2]);
        } else {
            hex.append("0".repeat(Math.max(6 - hex.length(), 0)));
        }

        //return
        try {
            return intToRGB(Integer.parseInt(hex.substring(0, 6), 16));
        } catch (Exception ignored) {
            return fallbackColor;
        }
    }

    /**
     * computes a linear interpolation between two ARGB integer colors (0 - 255)
     * @param a - the first ARGB integer color
     * @param b - the second ARGB integer color
     * @param t - the interpolation value
     * @return the interpolated ARGB integer color
     */
    public static int lerpARGBColor(int a, int b, float t) {
        Vector4f cA = argbIntToRGBA(a);
        Vector4f cB = argbIntToRGBA(b);
        Vector4f lerped = Maths.lerp(cA, cB, t);
        return rgbaToIntARGB(lerped);
    }

    /**
     * computes a linear interpolation between two RGB integer colors (0 - 255)
     * @param a - the first RGB integer color
     * @param b - the second RGB integer color
     * @param t - the interpolation value
     * @return the interpolated RGB integer color
     */
    public static int lerpRGBColor(int a, int b, float t) {
        Vector3f cA = intToRGB(a);
        Vector3f cB = intToRGB(b);
        Vector3f lerped = Maths.lerp(cA, cB, t);
        return rgbToInt(lerped);
    }

    /**
     * computes a linear interpolation between two RGB integer colors (0 - 255) using HSV
     * <p>
     * the interpolation will try to keep the shortest Hue between the two colors
     * @param a the first RGB integer color
     * @param b the second RGB integer color
     * @param t the interpolation value
     * @return the interpolated RGB integer color
     * @see ColorUtils#lerpRGBColorThroughHSV(int, int, float, boolean)
     */
    public static int lerpRGBColorThroughHSV(int a, int b, float t) {
        return lerpRGBColorThroughHSV(a, b, t, false);
    }

    /**
     * computes a linear interpolation between two RGB integer colors (0 - 255) using HSV
     * <p>
     * the interpolation can be set to keep the longest Hue between the two colors
     * @param a the first RGB integer color
     * @param b the second RGB integer color
     * @param t the interpolation value
     * @param longAngle if the interpolation should go through the longest Hue angle
     * @return the interpolated RGB integer color
     */
    public static int lerpRGBColorThroughHSV(int a, int b, float t, boolean longAngle) {
        Vector3f cA = rgbToHSV(intToRGB(a));
        Vector3f cB = rgbToHSV(intToRGB(b));
        float h, s;

        //do not change hue (x) nor saturation (y) when black (brightness 0)
        //do not change hue (x) when gray (saturation 0)
        if (cA.z == 0f) {
            h = cB.x;
            s = cB.y;
        } else if (cB.z == 0f) {
            h = cA.x;
            s = cA.y;
        } else {
            if (cA.y == 0f) {
                h = cB.x;
            } else if (cB.y == 0f) {
                h = cA.x;
            } else if (longAngle) {
                float xA = cA.x;
                float xB = cB.x;
                if (cA.x > cB.x) xB += 1f;
                else xA += 1f;
                h = Maths.lerp(xA, xB, t) % 1f;
            } else {
                float angle = Maths.shortAngle(cA.x * 360f, cB.x * 360f) / 360f;
                h = Maths.lerp(cA.x, cA.x + angle, t);
            }
            s = Maths.lerp(cA.y, cB.y, t);
        }

        float v = Maths.lerp(cA.z, cB.z, t);
        return rgbToInt(hsvToRGB(new Vector3f(h, s, v)));
    }
}
