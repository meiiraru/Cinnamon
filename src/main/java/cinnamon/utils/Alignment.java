package cinnamon.utils;

import java.util.function.Function;

public enum Alignment {
    TOP_LEFT(width -> 0f, height -> 0f),
    TOP_CENTER(width -> -width / 2f, height -> 0f),
    TOP_RIGHT(width -> -width, height -> 0f),

    CENTER_LEFT(width -> 0f, height -> -height / 2f),
    CENTER(width -> -width / 2f, height -> -height / 2f),
    CENTER_RIGHT(width -> -width, height -> -height / 2f),

    BOTTOM_LEFT(width -> 0f, height -> -height),
    BOTTOM_CENTER(width -> -width / 2f, height -> -height),
    BOTTOM_RIGHT(width -> -width, height -> -height);

    private final Function<Float, Float> widthFunc, heightFunc;

    Alignment(Function<Float, Float> widthFunc, Function<Float, Float> heightFunc) {
        this.widthFunc = widthFunc;
        this.heightFunc = heightFunc;
    }

    public float getWidthOffset(float width) {
        return widthFunc.apply(width);
    }

    public float getHeightOffset(float height) {
        return heightFunc.apply(height);
    }

    public Pair<Float, Float> getOffset(float width, float height) {
        return new Pair<>(getWidthOffset(width), getHeightOffset(height));
    }
}
