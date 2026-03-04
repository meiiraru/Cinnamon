package cinnamon.world.sky;

import cinnamon.utils.ColorUtils;
import cinnamon.utils.Maths;
import org.joml.Math;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SkyColors {

    private final List<SkyProperties> timedProperties = new ArrayList<>();

    public void addProperty(SkyProperties properties) {
        timedProperties.add(properties);
        timedProperties.sort(Comparator.comparingInt(a -> a.startTime));
    }

    public void clearProperties() {
        timedProperties.clear();
    }

    public SkyProperties getPropertiesAtTime(float dayTime) {
        if (timedProperties.isEmpty())
            return null;

        int next = Maths.binarySearch(0, timedProperties.size() - 1, i -> dayTime <= timedProperties.get(i).startTime);
        int current = Maths.modulo(next - 1, timedProperties.size());

        SkyProperties currentProps = timedProperties.get(current);
        SkyProperties nextProps = timedProperties.get(next);

        if (currentProps.startTime == nextProps.startTime)
            return currentProps;

        return currentProps.lerp(nextProps, Maths.ratio(dayTime, currentProps.startTime, nextProps.startTime));
    }

    public record SkyProperties(
            int startTime,

            int sunColor,
            int skyColor,
            int ambientLight,
            int fogColor,

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
                    (int) Math.lerp(startTime, other.startTime, t),

                    ColorUtils.lerpRGBColor(sunColor, other.sunColor, t),
                    ColorUtils.lerpRGBColor(skyColor, other.skyColor, t),
                    ColorUtils.lerpRGBColor(ambientLight, other.ambientLight, t),
                    ColorUtils.lerpRGBColor(fogColor, other.fogColor, t),

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
