package cinnamon.world.light;

import cinnamon.utils.Resource;

public class CookieLight extends Spotlight {

    public static final Resource COOKIE = new Resource("textures/environment/light/cookie_debug.png");

    private Resource cookieTexture = COOKIE;

    public CookieLight() {
        super();
        angle(20f, 20f);
    }

    @Override
    public int getType() {
        return 4;
    }

    public Resource getCookieTexture() {
        return cookieTexture;
    }

    public CookieLight cookieTexture(Resource cookieTexture) {
        this.cookieTexture = cookieTexture;
        return this;
    }
}
