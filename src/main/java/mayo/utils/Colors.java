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

    public final int rgb;
    public final int rgba;
    public final Vector3f vec;

    Colors(int rgb) {
        this.rgb = rgb;
        this.rgba = rgb + (0xFF << 24);
        this.vec = ColorUtils.intToRGB(rgb);
    }
}
