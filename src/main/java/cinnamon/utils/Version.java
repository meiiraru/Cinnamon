package cinnamon.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cinnamon.Client.LOGGER;

public class Version implements Comparable<Version> {

    private static final Resource VERSION = new Resource("version");
    //g1 major - g2 minor - g3 patch - g4 prerelease - g5 build
    private static final Pattern PATTERN = Pattern.compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$");

    public static final Version CLIENT_VERSION;

    static {
        String ver;
        try {
            ver = IOUtils.readString(VERSION);
        } catch (Exception e) {
            LOGGER.error("Unable to get client version", e);
            ver = "0.0.0";
        }

        CLIENT_VERSION = new Version(ver);
    }

    private final String src;
    private final int major, minor, patch;
    private final String pre;
    private final boolean invalid;

    public Version(String version) {
        src = version;

        Matcher matcher = PATTERN.matcher(version);
        if (!matcher.matches()) {
            major = minor = patch = 0;
            pre = null;
            invalid = true;
            return;
        }

        int maj, min, pat;
        try {
            maj = Integer.parseInt(matcher.group(1));
            min = Integer.parseInt(matcher.group(2));
            pat = Integer.parseInt(matcher.group(3));
        } catch (Exception ignored) {
            major = minor = patch = 0;
            pre = null;
            invalid = true;
            return;
        }

        this.major = maj;
        this.minor = min;
        this.patch = pat;
        this.pre = matcher.group(4);
        //this.build = matcher.group(5);
        this.invalid = false;
    }

    @Override
    public int compareTo(Version o) {
        //invalid version - no comparison
        if (invalid || o.invalid)
            return 0;

        //test major minor and patch
        int j = major - o.major;
        if (j != 0) return j;
        j = minor - o.minor;
        if (j != 0) return j;
        j = patch - o.patch;
        if (j != 0) return j;

        //test if they do not have a prerelease
        if (pre == null && o.pre != null)
            return 1;
        if (pre != null && o.pre == null)
            return -1;

        if (pre == null) //&& o.pre == null
            return 0;

        //test the prerelease
        String[] pre1 = pre.split("\\.");
        String[] pre2 = o.pre.split("\\.");

        for (int i = 0; i < pre1.length; i++) {
            //test if pre1 is greater than pre2
            if (i >= pre2.length)
                return 1;

            String s1 = pre1[i];
            String s2 = pre2[i];

            try {
                //test with integer
                int i1 = Integer.parseInt(s1);
                int i2 = Integer.parseInt(s2);
                j = i1 - i2;
            } catch (Exception ignored) {
                //otherwise test with string
                j = s1.compareTo(s2);
            }

            if (j != 0)
                return j;
        }

        //test if pre2 is greater than pre1
        if (pre1.length < pre2.length)
            return -1;

        //all of that just to find they are the same version
        return 0;
    }

    public int major() {
        return major;
    }

    public int minor() {
        return minor;
    }

    public int patch() {
        return patch;
    }

    public String prerelease() {
        return pre;
    }

    public String toStringNoPre() {
        return invalid ? src : major + "." + minor + "." + patch;
    }

    public String toStringNoBuild() {
        return invalid ? src : toStringNoPre() + (pre == null ? "" : "-" + pre);
    }

    @Override
    public String toString() {
        return src;
    }
}
