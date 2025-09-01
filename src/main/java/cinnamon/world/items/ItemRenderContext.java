package cinnamon.world.items;

public enum ItemRenderContext {
    FIRST_PERSON(0.75f),
    THIRD_PERSON(0.75f),
    HUD(1f),
    XR(0.35f);

    public final float scale;

    ItemRenderContext(float scale) {
        this.scale = scale;
    }
}
