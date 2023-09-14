package mayo.utils;

import org.joml.Vector3f;
import org.joml.Vector4f;

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

    public static Vector4f intToRGBA(int color) {
        int[] rgba = split(color, 4);
        return new Vector4f(rgba[0] / 255f, rgba[1] / 255f, rgba[2] / 255f, rgba[3] / 255f);
    }

    public static Vector4f argbIntToRGBA(int color) {
        int[] argb = split(color, 4);
        return new Vector4f(argb[1] / 255f, argb[2] / 255f, argb[3] / 255f, argb[0] / 255f);
    }
}
