package cinnamon.world.sky;

import cinnamon.utils.ColorUtils;
import org.joml.Math;

import java.util.SortedMap;
import java.util.TreeMap;

public class SkyColors {

    private static final int MINUTES_PER_DAY = 24 * 60;

    private final SortedMap<Integer, SkyProperties> propertiesMap = new TreeMap<>();

    public void clear() {
        propertiesMap.clear();
    }

    public void addProperty(int timeMinutes, SkyProperties properties) {
        propertiesMap.put(timeMinutes, properties);
    }

    public SkyProperties getPropertiesAtTime(float dayMinutes) {
        if (propertiesMap.isEmpty())
            return null;

        int time1 = propertiesMap.lastKey();
        int time2 = propertiesMap.firstKey();

        for (int time : propertiesMap.keySet()) {
            if (time <= dayMinutes) {
                time1 = time;
            } else {
                time2 = time;
                break;
            }
        }

        SkyProperties properties1 = propertiesMap.get(time1);
        SkyProperties properties2 = propertiesMap.get(time2);

        if (time1 == time2)
            return properties1;

        if (time1 > dayMinutes) time1 -= MINUTES_PER_DAY;
        if (time2 < dayMinutes) time2 += MINUTES_PER_DAY;

        float dt = (dayMinutes - time1) / (float) (time2 - time1);

        return properties1.lerp(properties2, dt);
    }

    public record SkyProperties(
            int sunColor,
            int skyColor,
            int ambientLight,
            int fogColor,
            int cloudsColor,

            float fogStart,
            float fogEnd,

            float sunIntensity,
            float fogIntensity,
            float starsIntensity,

            int sunlightColor,
            float sunlightIntensity,
            float sunlightShadowIntensity
    ) {
        public SkyProperties lerp(SkyProperties other, float t) {
            return new SkyProperties(
                    ColorUtils.lerpRGBColor(sunColor, other.sunColor, t),
                    ColorUtils.lerpRGBColor(skyColor, other.skyColor, t),
                    ColorUtils.lerpRGBColor(ambientLight, other.ambientLight, t),
                    ColorUtils.lerpRGBColor(fogColor, other.fogColor, t),
                    ColorUtils.lerpRGBColor(cloudsColor, other.cloudsColor, t),

                    Math.lerp(fogStart, other.fogStart, t),
                    Math.lerp(fogEnd, other.fogEnd, t),

                    Math.lerp(sunIntensity, other.sunIntensity, t),
                    Math.lerp(fogIntensity, other.fogIntensity, t),
                    Math.lerp(starsIntensity, other.starsIntensity, t),

                    ColorUtils.lerpRGBColor(sunlightColor, other.sunlightColor, t),
                    Math.lerp(sunlightIntensity, other.sunlightIntensity, t),
                    Math.lerp(sunlightShadowIntensity, other.sunlightShadowIntensity, t)
            );
        }
    }
}
