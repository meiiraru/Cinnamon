package cinnamon.world.entity.misc;

import cinnamon.utils.Maths;
import cinnamon.world.particle.FireworkParticle;
import cinnamon.world.world.World;
import org.joml.Vector3f;

import java.util.function.Function;

public class FireworkStar {

    public static final Function<Float, Vector3f> DEFAULT_FUNC = delta -> Maths.randomDir().mul(Maths.range(0.2f, 0.4f));
    public static final int STAR_PARTICLE_COUNT = 100;

    protected final int color, fade;
    protected final int particleCount;
    protected final Function<Float, Vector3f> star;

    public  FireworkStar(int color) {
        this(color, color, STAR_PARTICLE_COUNT, DEFAULT_FUNC);
    }

    public FireworkStar(int color, int fade, int particleCount, Function<Float, Vector3f> starShape) {
        this.color = color;
        this.fade = fade;
        this.particleCount = particleCount;
        this.star = starShape;
    }

    public void explode(Firework firework) {
        Vector3f pos = firework.getPos();
        World w = firework.getWorld();

        for (int i = 0; i < particleCount; i++) {
            FireworkParticle particle = new FireworkParticle((int) Maths.range(20, 60), color, fade);
            particle.setScale(2f);
            particle.setPos(pos);
            particle.setMotion(star.apply((float) i / particleCount));
            w.addParticle(particle);
        }
    }
}
