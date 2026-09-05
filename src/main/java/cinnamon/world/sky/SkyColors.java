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

    public SkyProperties getProperty(int timeMinutes) {
        return propertiesMap.get(timeMinutes);
    }

    public SkyProperties getPropertiesAtTime(float dayMinutes, SkyProperties out) {
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

        return properties1.lerp(properties2, dt, out);
    }

    public static class SkyProperties {
        private int sunColor, skyColor, ambientLight, fogColor, cloudsColor, starsColor;
        private float fogStart, fogEnd;
        private float sunIntensity, fogIntensity, starsIntensity;
        private int sunlightColor;
        private float sunlightIntensity, sunlightShadowIntensity;

        public int sunColor() {
            return sunColor;
        }

        public int skyColor() {
            return skyColor;
        }

        public int ambientLight() {
            return ambientLight;
        }

        public int fogColor() {
            return fogColor;
        }

        public int cloudsColor() {
            return cloudsColor;
        }

        public int starsColor() {
            return starsColor;
        }

        public float fogStart() {
            return fogStart;
        }

        public float fogEnd() {
            return fogEnd;
        }

        public float sunIntensity() {
            return sunIntensity;
        }

        public float fogIntensity() {
            return fogIntensity;
        }

        public float starsIntensity() {
            return starsIntensity;
        }

        public int sunlightColor() {
            return sunlightColor;
        }

        public float sunlightIntensity() {
            return sunlightIntensity;
        }

        public float sunlightShadowIntensity() {
            return sunlightShadowIntensity;
        }

        public SkyProperties sunColor(int sunColor) {
            this.sunColor = sunColor;
            return this;
        }

        public SkyProperties skyColor(int skyColor) {
            this.skyColor = skyColor;
            return this;
        }

        public SkyProperties ambientLight(int ambientLight) {
            this.ambientLight = ambientLight;
            return this;
        }

        public SkyProperties fogColor(int fogColor) {
            this.fogColor = fogColor;
            return this;
        }

        public SkyProperties cloudsColor(int cloudsColor) {
            this.cloudsColor = cloudsColor;
            return this;
        }

        public SkyProperties starsColor(int starsColor) {
            this.starsColor = starsColor;
            return this;
        }

        public SkyProperties fogStart(float fogStart) {
            this.fogStart = fogStart;
            return this;
        }

        public SkyProperties fogEnd(float fogEnd) {
            this.fogEnd = fogEnd;
            return this;
        }

        public SkyProperties sunIntensity(float sunIntensity) {
            this.sunIntensity = sunIntensity;
            return this;
        }

        public SkyProperties fogIntensity(float fogIntensity) {
            this.fogIntensity = fogIntensity;
            return this;
        }

        public SkyProperties starsIntensity(float starsIntensity) {
            this.starsIntensity = starsIntensity;
            return this;
        }

        public SkyProperties sunlightColor(int sunlightColor) {
            this.sunlightColor = sunlightColor;
            return this;
        }

        public SkyProperties sunlightIntensity(float sunlightIntensity) {
            this.sunlightIntensity = sunlightIntensity;
            return this;
        }

        public SkyProperties sunlightShadowIntensity(float sunlightShadowIntensity) {
            this.sunlightShadowIntensity = sunlightShadowIntensity;
            return this;
        }

        public SkyProperties lerp(SkyProperties other, float t, SkyProperties out) {
            return out
                    .sunColor(ColorUtils.lerpRGBColor(sunColor, other.sunColor, t))
                    .skyColor(ColorUtils.lerpRGBColor(skyColor, other.skyColor, t))
                    .ambientLight(ColorUtils.lerpRGBColor(ambientLight, other.ambientLight, t))
                    .fogColor(ColorUtils.lerpRGBColor(fogColor, other.fogColor, t))
                    .cloudsColor(ColorUtils.lerpRGBColor(cloudsColor, other.cloudsColor, t))
                    .starsColor(ColorUtils.lerpRGBColor(starsColor, other.starsColor, t))

                    .fogStart(Math.lerp(fogStart, other.fogStart, t))
                    .fogEnd(Math.lerp(fogEnd, other.fogEnd, t))

                    .sunIntensity(Math.lerp(sunIntensity, other.sunIntensity, t))
                    .fogIntensity(Math.lerp(fogIntensity, other.fogIntensity, t))
                    .starsIntensity(Math.lerp(starsIntensity, other.starsIntensity, t))

                    .sunlightColor(ColorUtils.lerpRGBColor(sunlightColor, other.sunlightColor, t))
                    .sunlightIntensity(Math.lerp(sunlightIntensity, other.sunlightIntensity, t))
                    .sunlightShadowIntensity(Math.lerp(sunlightShadowIntensity, other.sunlightShadowIntensity, t));
        }
    }
}
