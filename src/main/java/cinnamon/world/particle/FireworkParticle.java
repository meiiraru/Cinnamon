package cinnamon.world.particle;

import cinnamon.utils.ColorUtils;

public class FireworkParticle extends StarParticle {

    private final int fade;
    private final boolean hasFade;

    public FireworkParticle(int lifetime, int color, int fade) {
        super(lifetime, color);
        this.fade = fade;
        this.hasFade = fade != color;
    }

    @Override
    public void tick() {
        super.tick();
        getMotion().mul(0.9f);
        if (hasFade)
            setColor(ColorUtils.lerpARGBColor(color, fade, (float) getAge() / getLifetime()));
    }

    @Override
    protected int getRenderDistance() {
        return 25600; //160 * 160;
    }
}
