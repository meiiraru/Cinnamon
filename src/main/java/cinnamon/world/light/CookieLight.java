package cinnamon.world.light;

import cinnamon.utils.Resource;

public class CookieLight extends Spotlight {

    public static final Resource DEFAULT_TEX = new Resource("textures/environment/light/cookie_debug.png");

    private Resource texture = DEFAULT_TEX;

    public CookieLight() {
        super();
        angle(20f, 20f);
    }

    @Override
    public int getType() {
        return 4;
    }

    public Resource getTexture() {
        return texture;
    }

    public CookieLight texture(Resource texture) {
        this.texture = texture;
        return this;
    }
}
