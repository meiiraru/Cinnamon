package cinnamon.utils;

import org.joml.Vector3f;

//https://github.com/microsoft/PowerToys/blob/main/src/common/ManagedCommon/ColorNameHelper.cs
public class ColorNameFinder {

    private static final int[] hueLimitsForSatLevel1 = { //sat: 20-75
            8, 0, 0, 44, 0, 0, 0, 63, 0, 0, 122, 0, 134, 0, 0, 0, 0, 166, 176, 241, 0, 256, 0,
    };
    private static final int[] hueLimitsForSatLevel2 = { //sat: 75-115
            0, 10, 0, 32, 46, 0, 0, 0, 61, 0, 106, 0, 136, 144, 0, 0, 0, 158, 166, 241, 0, 0, 256,
    };
    private static final int[] hueLimitsForSatLevel3 = { //sat: 115-150
            0, 8, 0, 0, 39, 46, 0, 0, 0, 71, 120, 0, 131, 144, 0, 0, 163, 0, 177, 211, 249, 0, 256,
    };
    private static final int[] hueLimitsForSatLevel4 = { //sat: 150-240
            0, 11, 26, 0, 0, 38, 45, 0, 0, 56, 100, 121, 129, 0, 140, 0, 180, 0, 0, 224, 241, 0, 256,
    };
    private static final int[] hueLimitsForSatLevel5 = { //sat: 240-255
            0, 13, 27, 0, 0, 36, 45, 0, 0, 59, 118, 0, 127, 136, 142, 0, 185, 0, 0, 216, 239, 0, 256,
    };

    private static final int[] lumLimitsForHueIndexLow = {
            130, 100, 115, 100, 100, 100, 110, 75, 100, 90, 100, 100, 100, 100, 80, 100, 100, 100, 100, 100, 100, 100, 100,
    };
    private static final int[] lumLimitsForHueIndexHigh = {
            170, 170, 170, 155, 170, 170, 170, 170, 170, 115, 170, 170, 170, 170, 170, 170, 170, 170, 150, 150, 170, 140, 165,
    };

    private static final String[] colorNamesLight = {
            "color.coral",
            "color.rose",
            "color.light_orange",
            "color.tan",
            "color.tan",
            "color.light_yellow",
            "color.light_yellow",
            "color.tan",
            "color.light_green",
            "color.lime",
            "color.light_green",
            "color.light_green",
            "color.aqua",
            "color.sky_blue",
            "color.light_turquoise",
            "color.pale_blue",
            "color.light_blue",
            "color.ice_blue",
            "color.periwinkle",
            "color.lavender",
            "color.pink",
            "color.tan",
            "color.rose",
    };
    private static final String[] colorNamesMid = {
            "color.coral",
            "color.red",
            "color.orange",
            "color.brown",
            "color.tan",
            "color.gold",
            "color.yellow",
            "color.olive_green",
            "color.olive_green",
            "color.green",
            "color.green",
            "color.bright_green",
            "color.teal",
            "color.aqua",
            "color.turquoise",
            "color.pale_blue",
            "color.blue",
            "color.blue_gray",
            "color.indigo",
            "color.purple",
            "color.pink",
            "color.brown",
            "color.red",
    };
    private static final String[] colorNamesDark = {
            "color.brown",
            "color.dark_red",
            "color.brown",
            "color.brown",
            "color.brown",
            "color.dark_yellow",
            "color.dark_yellow",
            "color.brown",
            "color.dark_green",
            "color.dark_green",
            "color.dark_green",
            "color.dark_green",
            "color.dark_teal",
            "color.dark_teal",
            "color.dark_teal",
            "color.dark_blue",
            "color.dark_blue",
            "color.blue_gray",
            "color.indigo",
            "color.dark_purple",
            "color.plum",
            "color.brown",
            "color.dark_red",
    };

    /**
     * Computes the color name for a given HSL color
     * @param hsl the HSL {@link org.joml.Vector3f} color
     * @return the color translation key {@code String}
     */
    public static String getColorName(Vector3f hsl) {
        //convert to 0-255 range
        int hue = (int) (hsl.x * 255);
        int sat = (int) (hsl.y * 255);
        int lum = (int) (hsl.z * 255);

        //achromatic
        if (lum > 240)
            return "color.white";
        else if (lum < 20)
            return "color.black";

        if (sat <= 20) {
            if (lum > 170)
                return "color.light_gray";
            else if (lum > 100)
                return "color.gray";
            else
                return "color.dark_gray";
        }

        //get hue limits based on saturation
        int[] pHueLimits;
        if (sat <= 75)
            pHueLimits = hueLimitsForSatLevel1;
        else if (sat <= 115)
            pHueLimits = hueLimitsForSatLevel2;
        else if (sat <= 150)
            pHueLimits = hueLimitsForSatLevel3;
        else if (sat <= 240)
            pHueLimits = hueLimitsForSatLevel4;
        else
            pHueLimits = hueLimitsForSatLevel5;

        //find the hue index
        int colorIndex = -1;
        for (int i = 0; i < colorNamesMid.length; i++) {
            if (hue < pHueLimits[i]) {
                colorIndex = i;
                break;
            }
        }

        //no color found (should not happen)
        if (colorIndex == -1)
            return "";

        //check for luminosity index and return the color name
        if (lum > lumLimitsForHueIndexHigh[colorIndex])
            return colorNamesLight[colorIndex];
        else if (lum < lumLimitsForHueIndexLow[colorIndex])
            return colorNamesDark[colorIndex];
        else
            return colorNamesMid[colorIndex];
    }
}
