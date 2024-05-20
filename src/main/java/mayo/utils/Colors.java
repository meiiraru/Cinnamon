package mayo.utils;

import org.joml.Vector3f;

public enum Colors {

    //rainbow
    PINK(0xFF72AD),
    PURPLE(0xAD72FF),
    BLUE(0x72ADFF),
    CYAN(0x72FFAD),
    LIME(0x72FF72),
    GREEN(0x6AA84F),
    YELLOW(0xFFFF72),
    ORANGE(0xFFAD72),
    RED(0xFF7272),
    LILAC(0xC8A2C8),

    //grayscale
    BROWN(0x8E625F),
    BLACK(0),
    LIGHT_BLACK(0x323232),
    DARK_GRAY(0x666666),
    LIGHT_GRAY(0xBBBBBB),
    WHITE(0xFFFFFF);

    public final int rgb, rgba, r, g, b;
    public final Vector3f vec;

    Colors(int rgb) {
        this.rgb = rgb;
        this.rgba = rgb | 0xFF000000;
        this.r = (rgb >> 16) & 0xFF;
        this.g = (rgb >> 8) & 0xFF;
        this.b = rgb & 0xFF;
        this.vec = ColorUtils.intToRGB(rgb);
    }

    public static Colors randomRainbow() {
        Colors[] colors = values();
        return colors[(int) (Math.random() * 10)];
    }

    public static Colors random() {
        Colors[] colors = values();
        return colors[(int) (Math.random() * colors.length)];
    }
}
