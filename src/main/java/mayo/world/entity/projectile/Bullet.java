package mayo.world.entity.projectile;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Colors;
import mayo.utils.Meth;
import mayo.utils.Resource;
import mayo.world.World;
import mayo.world.effects.Effect;
import mayo.world.entity.Entity;
import mayo.world.entity.living.LivingEntity;
import mayo.world.particle.DustParticle;

public class Bullet extends Projectile {

    public static final Model MODEL = ModelManager.load(new Resource("models/entities/bullet/bullet.obj"));
    public static final int DAMAGE = 3;
    public static final int LIFETIME = 30;
    public static final float SPEED = 0.75f;
    public static final float CRIT_CHANCE = 0.15f;

    public Bullet(World world, Entity owner) {
        super(MODEL, world, getConstructorDamage(owner), LIFETIME, SPEED, getConstructorCrit(owner), owner);
    }

    private static int getConstructorDamage(Entity owner) {
        if (owner instanceof LivingEntity le) {
            if (le.hasEffect(Effect.Type.PACIFIST))
                return 0;
            Effect boost = le.getEffect(Effect.Type.DAMAGE_BOOST);
            if (boost != null)
                return DAMAGE + boost.getAmplitude();
        }

        return DAMAGE;
    }

    private static boolean getConstructorCrit(Entity owner) {
        if (owner instanceof LivingEntity le) {
            if (le.hasEffect(Effect.Type.ALWAYS_CRIT))
                return true;
            if (le.hasEffect(Effect.Type.NEVER_CRIT))
                return false;
        }

        return Math.random() < CRIT_CHANCE;
    }

    @Override
    public void onRemove() {
        super.onRemove();
        if (getDamage() == 0) confetti();
    }

    public void confetti() {
        for (int i = 0; i < 20; i++) {
            DustParticle particle = new DustParticle((int) (Math.random() * 40) + 20, Colors.randomRainbow().rgba);
            particle.setPos(this.getPos());
            particle.setMotion(Meth.rotToDir((float) Math.random() * 360, (float) Math.random() * 360).mul((float) Math.random() * 0.075f + 0.075f));
            particle.setScale(1.5f);
            world.addParticle(particle);
        }
    }
}
