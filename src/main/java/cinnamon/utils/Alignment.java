package cinnamon.utils;

import java.util.function.Function;

public enum Alignment {
    LEFT(width -> 0f),
    RIGHT(width -> -width),
    CENTER(width -> -width / 2f);

    private final Function<Float, Float> anchorFunction;

    Alignment(Function<Float, Float> anchorFunction) {
        this.anchorFunction = anchorFunction;
    }

    public float getOffset(float width) {
        return anchorFunction.apply(width);
    }
}
