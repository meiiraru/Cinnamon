package cinnamon.world.particle;

import cinnamon.render.Camera;
import cinnamon.utils.ColorUtils;
import cinnamon.world.world.WorldClient;

public class FireworkParticle extends StarParticle {

    private final int fade;
    private final boolean hasFade, trail, twinkle;

    public FireworkParticle(int lifetime, int color, int fade, boolean trail, boolean twinkle) {
        super(lifetime, color);
        this.fade = fade;
        this.hasFade = fade != color;
        this.trail = trail;
        this.twinkle = twinkle;
    }

    @Override
    public void tick() {
        super.tick();
        getMotion().mul(0.9f);

        if (hasFade && age >= lifetime / 3) {
            //float t = (float) age / lifetime;
            float t = (age - lifetime / 3f) / (lifetime * 2f / 3f);
            setColor(ColorUtils.lerpARGBColor(color, fade, t));
        }

        if (trail && age < lifetime / 2 && (age + lifetime) % 2 == 0) {
            FireworkParticle p = new FireworkParticle(lifetime, color, fade, false, twinkle);
            p.setPos(getPos());
            p.setEmissive(emissive);
            p.setScale(scale);
            p.age = lifetime / 2;
            ((WorldClient) world).addParticle(p);
        }
    }

    @Override
    public boolean shouldRender(Camera camera) {
        return (!twinkle || age < lifetime / 3 || (age + lifetime) / 3 % 2 == 0) && super.shouldRender(camera);
    }

    @Override
    protected int getRenderDistance() {
        return 25600; //160 * 160;
    }
}
