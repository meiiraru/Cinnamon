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

    public static Vector3f intToRGB(int color) {
        int[] rgb = split(color, 3);
        return new Vector3f(rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f);
    }

    public static int rgbToInt(Vector3f rgb) {
        int hex = (int) (rgb.x * 0xFF);
        hex = (hex << 8) + (int) (rgb.y * 0xFF);
        hex = (hex << 8) + (int) (rgb.z * 0xFF);
        return hex;
    }

    public static int rgbaToIntARGB(Vector4f rgba) {
        int hex = (int) (rgba.w * 0xFF);
        hex = (hex << 8) + (int) (rgba.x * 0xFF);
        hex = (hex << 8) + (int) (rgba.y * 0xFF);
        hex = (hex << 8) + (int) (rgba.z * 0xFF);
        return hex;
    }

    public static Vector4f intToRGBA(int color) {
        int[] rgba = split(color, 4);
        return new Vector4f(rgba[0] / 255f, rgba[1] / 255f, rgba[2] / 255f, rgba[3] / 255f);
    }

    public static Vector4f argbIntToRGBA(int color) {
        int[] argb = split(color, 4);
        return new Vector4f(argb[1] / 255f, argb[2] / 255f, argb[3] / 255f, argb[0] / 255f);
    }

    public static Vector3f hsvToRGB(Vector3f hsv) {
        int hex = Color.HSBtoRGB(hsv.x, hsv.y, hsv.z);
        return intToRGB(hex);
    }

    public static Vector3f rgbToHSV(Vector3f rgb) {
        float[] hsv = Color.RGBtoHSB((int) (rgb.x * 255f), (int) (rgb.y * 255f), (int) (rgb.z * 255f), null);
        return new Vector3f(hsv[0], hsv[1], hsv[2]);
    }

    public static String rgbToHex(Vector3f rgb) {
        String color = Integer.toHexString(ColorUtils.rgbToInt(rgb));
        return "0".repeat(Math.max(6 - color.length(), 0)) + color;
    }

    public static Vector3f hexStringToRGB(String string) {
        return hexStringToRGB(string, new Vector3f());
    }

    public static Vector3f hexStringToRGB(String string, Vector3f fallbackColor) {
        if (string == null || string.isBlank())
            return fallbackColor;

        //parse hex color
        StringBuilder hex = new StringBuilder(string);

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

    public static int lerpARGBColor(int a, int b, float t) {
        Vector4f cA = argbIntToRGBA(a);
        Vector4f cB = argbIntToRGBA(b);
        Vector4f lerped = Maths.lerp(cA, cB, t);
        return rgbaToIntARGB(lerped);
    }

    public static int lerpRGBColor(int a, int b, float t) {
        Vector3f cA = intToRGB(a);
        Vector3f cB = intToRGB(b);
        Vector3f lerped = Maths.lerp(cA, cB, t);
        return rgbToInt(lerped);
    }

    public static int lerpHSVColor(int a, int b, float t) {
        return lerpHSVColor(a, b, t, false);
    }

    public static int lerpHSVColor(int a, int b, float t, boolean longAngle) {
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
