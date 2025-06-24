package cinnamon.utils;

import org.joml.Vector3f;

public enum Colors {

    PINK(0xFF72AD),
    PURPLE(0xAD72FF),
    BLUE(0x72ADFF),
    CYAN(0x72FFAD),
    LIME(0x72FF72),
    GREEN(0x6AA84F),
    YELLOW(0xFFFF72),
    GOLD(0xB39B5B),
    ORANGE(0xFFAD72),
    RED(0xFF7272),
    BROWN(0x8E625F),
    LILAC(0xC8A2C8),

    BLACK(0),
    LIGHT_BLACK(0x323232),
    DARK_GRAY(0x666666),
    LIGHT_GRAY(0xBBBBBB),
    WHITE(0xFFFFFF);

    public static final Colors[]
            RAINBOW = {PINK, PURPLE, BLUE, CYAN, LIME, YELLOW, ORANGE, RED},
            GRAYSCALE = {BLACK, LIGHT_BLACK, DARK_GRAY, LIGHT_GRAY, WHITE};

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
        return Maths.randomArr(RAINBOW);
    }
}
