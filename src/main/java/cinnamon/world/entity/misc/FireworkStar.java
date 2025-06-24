package cinnamon.world.entity.misc;

import cinnamon.utils.Maths;
import cinnamon.world.particle.FireworkParticle;
import cinnamon.world.world.World;
import org.joml.Vector3f;

import java.util.function.Function;

public class FireworkStar {

    protected final Integer[] color, fade;
    protected final boolean trail, twinkle;
    protected final int particleCount;
    protected final Function<Float, Vector3f> star;

    protected final boolean hasFade;

    public FireworkStar(Integer... color) {
        this(color, null, false, false);
    }

    public FireworkStar(Integer[] color, Integer[] fade, boolean trail, boolean twinkle) {
        this(color, fade, trail, twinkle, Shape.BALL);
    }

    public FireworkStar(Integer[] color, Integer[] fade, boolean trail, boolean twinkle, Shape shape) {
        this(color, fade, trail, twinkle, shape.count, shape.func);
    }

    public FireworkStar(Integer[] color, Integer[] fade, boolean trail, boolean twinkle, int particleCount, Function<Float, Vector3f> starShape) {
        this.color = color;
        this.fade = fade;
        this.trail = trail;
        this.twinkle = twinkle;
        this.particleCount = Math.max(particleCount, 1);
        this.star = starShape;
        this.hasFade = fade != null && fade.length > 0;
    }

    public void explode(Firework firework) {
        Vector3f pos = firework.getPos();
        World w = firework.getWorld();
        float yaw = (float) Math.toRadians(firework.getRot().y);

        for (int i = 0; i < particleCount; i++) {
            int color = Maths.randomArr(this.color);
            int fade = hasFade ? Maths.randomArr(this.fade) : color;
            FireworkParticle particle = new FireworkParticle((int) Maths.range(20, 60), color, fade, trail, twinkle);
            particle.setScale(2f);
            particle.setEmissive(true);
            particle.setPos(pos);
            particle.setMotion(star.apply((float) i / particleCount).rotateY(yaw));
            w.addParticle(particle);
        }
    }

    public enum Shape {
        BRUST(50, delta -> new Vector3f((float) Math.random(), (float) Math.random(), (float) Math.random())),
        BALL(150, delta -> {
            float theta = 2f * (float) Math.PI * delta;
            float phi = (float) Math.acos(2f * Math.random() - 1f);

            return new Vector3f(
                    (float) (Math.sin(phi) * Math.cos(theta)),
                    (float) (Math.sin(phi) * Math.sin(theta)),
                    (float) (Math.cos(phi)));
        }),
        HEART(70, delta -> {
            //get heart angle in radians
            float t = (float) (delta * 2 * Math.PI);

            //parametric heart equation
            //x(t) = 16 * sin^3(t)
            //y(t) = 13 * cos(t) - 5 * cos(2t) - 2 * cos(3t) - cos(4t)
            float xShape = (float) (16 * Math.pow(Math.sin(t), 3));
            float yShape = (float) (13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t));

            float scale = 1f / 17f;
            return new Vector3f(xShape * scale, yShape * scale, 0f);
        }),
        STAR(100, delta -> {
            float t = (float) (delta * 2 * Math.PI);

            //f(x) = (cos(t), sin(t))
            //f(-3t) + 2f(2t)
            float xShape = (float) (Math.cos(-3 * t) + 2 * Math.cos(2 * t));
            float yShape = (float) (Math.sin(-3 * t) + 2 * Math.sin(2 * t));

            float scale = 1f / 3f;
            return new Vector3f(xShape * scale, yShape * scale, 0f);
        });

        public final int count;
        public final Function<Float, Vector3f> func;

        Shape(int count, Function<Float, Vector3f> func) {
            this.count = count;
            this.func = func;
        }
    }
}
