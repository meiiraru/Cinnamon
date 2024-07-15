package cinnamon.render.texture;

import cinnamon.utils.Resource;

public class SpriteTexture {

    private final Resource resource;
    private final int uFrames, vFrames;

    public SpriteTexture(Resource texture, int uFrames, int vFrames) {
        this.resource = texture;
        this.uFrames = uFrames;
        this.vFrames = vFrames;
    }

    public Resource getResource() {
        return resource;
    }

    public int getUFrames() {
        return uFrames;
    }

    public int getVFrames() {
        return vFrames;
    }

    public float getSpriteWidth() {
        return 1f / getUFrames();
    }

    public float getSpriteHeight() {
        return 1f / getVFrames();
    }
}
