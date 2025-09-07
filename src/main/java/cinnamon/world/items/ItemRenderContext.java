package cinnamon.world.items;

public enum ItemRenderContext {
    FIRST_PERSON(0.5f),
    THIRD_PERSON(0.5f),
    HUD(1f),
    XR(0.35f);

    public final float scale;

    ItemRenderContext(float scale) {
        this.scale = scale;
    }
}
