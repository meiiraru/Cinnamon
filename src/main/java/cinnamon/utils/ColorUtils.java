package cinnamon.utils;

import cinnamon.math.Maths;
import org.joml.Math;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.awt.*;

public class ColorUtils {

    /**
     * splits a color integer into its channels
     * @param color integer to split
     * @param len channels length
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
     * changes a color integer channel to a new value
     * @param color the color integer to change
     * @param index the channel index to change
     * @param value the new channel value (0 - 255)
     * @return the new color integer with the changed channel
     */
    public static int changeIntComponent(int color, int index, int value) {
        int[] channels = split(color, 4);
        channels[index] = value;
        return (channels[0] << 24) + (channels[1] << 16) + (channels[2] << 8) + channels[3];
    }

    /**
     * converts an RGB integer color (0 - 255) to an RGB (0 - 1) {@link org.joml.Vector3f}
     * @param color the RGB integer color
     * @return an RGB {@link org.joml.Vector3f} of that color
     */
    public static Vector3f intToRGB(int color) {
        int[] rgb = split(color, 3);
        return new Vector3f(rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f);
    }

    /**
     * converts an RGB (0 - 1) {@link org.joml.Vector3f} to an RGB integer color (0 - 255)
     * @param color the RGB {@link org.joml.Vector3f} color
     * @return an RGB integer of that color
     * @see ColorUtils#rgbToInt(float, float, float)
     */
    public static int rgbToInt(Vector3f color) {
        return rgbToInt(color.x, color.y, color.z);
    }

    /**
     * converts an RGB (0 - 1) color to an RGB integer color (0 - 255)
     * @param r the red channel
     * @param g the green channel
     * @param b the blue channel
     * @return an RGB integer of that color
     */
    public static int rgbToInt(float r, float g, float b) {
        int hex = (int) (r * 0xFF);
        hex = (hex << 8) + (int) (g * 0xFF);
        hex = (hex << 8) + (int) (b * 0xFF);
        return hex | (0xFF << 24);
    }

    /**
     * converts an RGBA (0 - 1) {@link org.joml.Vector4f} to an ARGB integer color (0 - 255)
     * @param color the RGBA {@link org.joml.Vector4f} color
     * @return an ARGB integer of that color
     * @see ColorUtils#rgbaToIntARGB(float, float, float, float)
     */
    public static int rgbaToIntARGB(Vector4f color) {
        return rgbaToIntARGB(color.x, color.y, color.z, color.w);
    }

    /**
     * converts an RGBA (0 - 1) color to an ARGB integer color (0 - 255)
     * @param r the red channel
     * @param g the green channel
     * @param b the blue channel
     * @param a the alpha channel
     * @return an ARGB integer of that color
     */
    public static int rgbaToIntARGB(float r, float g, float b, float a) {
        int hex = (int) (a * 0xFF);
        hex = (hex << 8) + (int) (r * 0xFF);
        hex = (hex << 8) + (int) (g * 0xFF);
        hex = (hex << 8) + (int) (b * 0xFF);
        return hex;
    }

    /**
     * converts an RGBA integer color (0 - 255) to an RGBA (0 - 1) {@link org.joml.Vector4f}
     * @param color the RGBA integer color
     * @return an RGBA {@link org.joml.Vector4f} of that color
     * @see ColorUtils#argbIntToRGBA(int)
     */
    public static Vector4f intToRGBA(int color) {
        int[] rgba = split(color, 4);
        return new Vector4f(rgba[0] / 255f, rgba[1] / 255f, rgba[2] / 255f, rgba[3] / 255f);
    }

    /**
     * converts an ARGB integer color (0 - 255) to an RGBA (0 - 1) {@link org.joml.Vector4f}
     * @param color the ARGB integer color
     * @return an RGBA {@link org.joml.Vector4f} of that color
     * @see ColorUtils#intToRGBA(int)
     */
    public static Vector4f argbIntToRGBA(int color) {
        int[] argb = split(color, 4);
        return new Vector4f(argb[1] / 255f, argb[2] / 255f, argb[3] / 255f, argb[0] / 255f);
    }

    /**
     * converts an HSV (0 - 1) {@link org.joml.Vector3f} to an RGB (0 - 1) {@link org.joml.Vector3f}
     * @param color the HSV {@link org.joml.Vector3f} color
     * @return an RGB {@link org.joml.Vector3f} of that color
     * @see ColorUtils#hsvToRGB(float, float, float)
     */
    public static Vector3f hsvToRGB(Vector3f color) {
        return hsvToRGB(color.x, color.y, color.z);
    }

    /**
     * converts an HSV (0 - 1) color to an RGB (0 - 1) {@link org.joml.Vector3f}
     * @param h the hue channel
     * @param s the saturation channel
     * @param v the value channel
     * @return an RGB {@link org.joml.Vector3f} of that color
     */
    public static Vector3f hsvToRGB(float h, float s, float v) {
        return intToRGB(hsvToInt(h, s, v));
    }

    /**
     * converts an HSV (0 - 1) {@link org.joml.Vector3f} to an RGB integer color (0 - 255)
     * @param color the HSV {@link org.joml.Vector3f} color
     * @return an RGB integer of that color
     * @see ColorUtils#hsvToInt(float, float, float)
     */
    public static int hsvToInt(Vector3f color) {
        return hsvToInt(color.x, color.y, color.z);
    }

    /**
     * converts an HSV (0 - 1) color to an RGB integer color (0 - 255)
     * @param h the hue channel
     * @param s the saturation channel
     * @param v the value channel
     * @return an RGB integer of that color
     */
    public static int hsvToInt(float h, float s, float v) {
        return Color.HSBtoRGB(h, s, v);
    }

    /**
     * converts an RGB (0 - 1) {@link org.joml.Vector3f} to an HSV (0 - 1) {@link org.joml.Vector3f}
     * @param color the RGB {@link org.joml.Vector3f} color
     * @return an HSV {@link org.joml.Vector3f} of that color
     * @see ColorUtils#rgbToHSV(float, float, float)
     */
    public static Vector3f rgbToHSV(Vector3f color) {
        return rgbToHSV(color.x, color.y, color.z);
    }

    /**
     * converts an RGB (0 - 1) color to an HSV (0 - 1) {@link org.joml.Vector3f}
     * @param r the red channel
     * @param g the green channel
     * @param b the blue channel
     * @return an HSV {@link org.joml.Vector3f} of that color
     */
    public static Vector3f rgbToHSV(float r, float g, float b) {
        float[] hsv = Color.RGBtoHSB((int) (r * 255f), (int) (g * 255f), (int) (b * 255f), null);
        return new Vector3f(hsv[0], hsv[1], hsv[2]);
    }

    /**
     * converts an RGB integer color (0 - 255) to an HSV (0 - 1) {@link org.joml.Vector3f}
     * @param color the RGB integer color
     * @return an HSV {@link org.joml.Vector3f} of that color
     */
    public static Vector3f intToHSV(int color) {
        int[] rgb = split(color, 3);
        float[] hsv = Color.RGBtoHSB(rgb[0], rgb[1], rgb[2], null);
        return new Vector3f(hsv[0], hsv[1], hsv[2]);
    }

    /**
     * converts an RGB (0 - 1) {@link org.joml.Vector3f} to a Hexadecimal (00 - FF) string
     * <p>
     * the return string will always be 6 characters long without the "#" prefix
     * @param color the RGB {@link org.joml.Vector3f} color
     * @return a Hexadecimal string of that color
     * @see ColorUtils#rgbToHex(float, float, float)
     */
    public static String rgbToHex(Vector3f color) {
        return rgbToHex(color.x, color.y, color.z);
    }

    /**
     * converts an RGB (0 - 1) color to a Hexadecimal (00 - FF) string
     * <p>
     * the return string will always be 8 characters long without the "#" prefix
     * @param r the red channel
     * @param g the green channel
     * @param b the blue channel
     * @return a Hexadecimal string of that color
     * @see ColorUtils#intToHex(int)
     */
    public static String rgbToHex(float r, float g, float b) {
        return intToHex(rgbToInt(r, g, b));
    }

    /**
     * converts an RGB integer color (0 - 255) to a Hexadecimal (00 - FF) string
     * <p>
     * the return string will always be 8 characters long without the "#" prefix
     * @param color the RGB integer color
     * @return a Hexadecimal string of that color
     */
    public static String intToHex(int color) {
        String str = Integer.toHexString(color).toUpperCase();
        int len = str.length();
        str = "0".repeat(Math.max(6 - len, 0)) + str;
        str = "F".repeat(Math.max(8 - len, 0)) + str;
        return str;
    }

    /**
     * converts a Hexadecimal (00 - FF) string to an RGB (0 - 1) {@link org.joml.Vector3f}
     * <p>
     * if the string is invalid, it will return an empty {@link org.joml.Vector3f}
     * @param color the Hexadecimal string color
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
     * @param color the Hexadecimal string color
     * @param fallbackColor the fallback color if the string is invalid
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
        } else if (hex.length() > 6) {
            hex = new StringBuilder(hex.substring(hex.length() - 6));
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
     * converts an HSV (0 - 1) {@link org.joml.Vector3f} to an HSL (0 - 1) {@link org.joml.Vector3f}
     * @param color the HSV {@link org.joml.Vector3f} color
     * @return an HSL {@link org.joml.Vector3f} of that color
     * @see ColorUtils#hsvToHSL(float, float, float)
     */
    public static Vector3f hsvToHSL(Vector3f color) {
        return hsvToHSL(color.x, color.y, color.z);
    }

    /**
     * converts an HSV (0 - 1) color to an HSL (0 - 1) color
     * @param h the hue channel
     * @param s the saturation channel
     * @param v the value channel
     * @return an HSL {@link org.joml.Vector3f} of that color
     */
    public static Vector3f hsvToHSL(float h, float s, float v) {
        float l = (2f - s) * v / 2f;
        float newS = s;

        if (l > 0f) {
            if (l == 1f)
                newS = 0;
            else if (l < 0.5f)
                newS = newS * v / (l * 2);
            else
                newS = newS * v / (2 - l * 2);
        }

        return new Vector3f(h, newS, l);
    }

    /**
     * computes a linear interpolation between two ARGB integer colors (0 - 255)
     * @param a the first ARGB integer color
     * @param b the second ARGB integer color
     * @param t the interpolation value
     * @return the interpolated ARGB integer color
     * @see ColorUtils#lerpRGBColor(int, int, float)
     */
    public static int lerpARGBColor(int a, int b, float t) {
        Vector4f cA = argbIntToRGBA(a);
        Vector4f cB = argbIntToRGBA(b);
        Vector4f lerped = Maths.lerp(cA, cB, t);
        return rgbaToIntARGB(lerped);
    }

    /**
     * computes a linear interpolation between two RGB integer colors (0 - 255)
     * @param a the first RGB integer color
     * @param b the second RGB integer color
     * @param t the interpolation value
     * @return the interpolated RGB integer color
     * @see ColorUtils#lerpARGBColor(int, int, float)
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
        Vector3f cA = intToHSV(a);
        Vector3f cB = intToHSV(b);
        return hsvToInt(lerpHSV(cA, cB, t, longAngle));
    }

    /**
     * computes a linear interpolation between two HSV {@link org.joml.Vector3f} colors (0 - 1)
     * <p>
     * the interpolation can be set to keep the longest Hue between the two colors
     * @param a the first HSV {@link org.joml.Vector3f} color
     * @param b the second HSV {@link org.joml.Vector3f} color
     * @param t the interpolation value
     * @param longAngle if the interpolation should go through the longest Hue angle
     * @return the interpolated HSV {@link org.joml.Vector3f} color
     */
    public static Vector3f lerpHSV(Vector3f a, Vector3f b, float t, boolean longAngle) {
        float h, s;

        //do not change hue (x) nor saturation (y) when black (brightness 0)
        //do not change hue (x) when gray (saturation 0)
        if (a.z == 0f) {
            h = b.x;
            s = b.y;
        } else if (b.z == 0f) {
            h = a.x;
            s = a.y;
        } else {
            if (a.y == 0f) {
                h = b.x;
            } else if (b.y == 0f) {
                h = a.x;
            } else if (longAngle) {
                float xA = a.x;
                float xB = b.x;
                if (a.x > b.x) xB += 1f;
                else xA += 1f;
                h = Math.lerp(xA, xB, t) % 1f;
            } else {
                float angle = Maths.shortAngle(a.x * 360f, b.x * 360f) / 360f;
                h = Math.lerp(a.x, a.x + angle, t);
            }
            s = Math.lerp(a.y, b.y, t);
        }

        float v = Math.lerp(a.z, b.z, t);
        return new Vector3f(h, s, v);
    }

    /**
     * inverts an RGB (0 - 1) {@link org.joml.Vector3f} color
     * @param color the RGB {@link org.joml.Vector3f} color
     * @return an inverted RGB {@link org.joml.Vector3f} of that color
     * @see ColorUtils#invertColor(float, float, float)
     */
    public static Vector3f invertColor(Vector3f color) {
        return invertColor(color.x, color.y, color.z);
    }

    /**
     * inverts an RGB (0 - 1) color
     * @param r the red channel
     * @param g the green channel
     * @param b the blue channel
     * @return an inverted RGB {@link org.joml.Vector3f} of that color
     */
    public static Vector3f invertColor(float r, float g, float b) {
        return new Vector3f(1f - r, 1f - g, 1f - b);
    }

    /**
     * find the closest {@link Colors} to a given RGB integer color (0 - 255)
     * @param color the RGB integer color
     * @return the closest {@link Colors} to that color
     */
    public static Colors findColor(int color) {
        int[] rgb = split(color, 3);

        int minDist = Integer.MAX_VALUE;
        Colors closestColor = Colors.BLACK;

        for (Colors c : Colors.values()) {
            int r = rgb[0] - c.r;
            int g = rgb[1] - c.g;
            int b = rgb[2] - c.b;
            int dist = r * r + g * g + b * b;

            if (dist < minDist) {
                minDist = dist;
                closestColor = c;
            }
        }

        return closestColor;
    }
}
