package mayo.world.entity.projectile;

import mayo.registry.EntityModelRegistry;
import mayo.registry.EntityRegistry;
import mayo.render.MatrixStack;
import mayo.render.shader.Shader;
import mayo.utils.ColorUtils;
import mayo.utils.Maths;
import mayo.world.DamageType;
import mayo.world.collisions.CollisionResolver;
import mayo.world.collisions.CollisionResult;
import mayo.world.entity.Entity;
import org.joml.Vector3f;

public class Potato extends Projectile {

    public static final int DAMAGE = 8;
    public static final float EXPLOSION_RANGE = 3f;
    public static final float EXPLOSION_STRENGTH = 1f;
    public static final int LIFETIME = 100;
    public static final float SPEED = 1.25f;
    public static final float CRIT_CHANCE = 0.15f;
    private static final Vector3f BOUNCINESS = new Vector3f(0.25f, 0, 0.25f);

    public Potato(Entity owner) {
        super(EntityModelRegistry.POTATO.model, DAMAGE, LIFETIME, SPEED, CRIT_CHANCE, owner);
    }

    @Override
    public void tick() {
        super.tick();

        Vector3f vec = new Vector3f(motion);
        if (vec.lengthSquared() > 0f)
            vec.normalize();

        this.rotateTo(Maths.dirToRot(vec));
    }

    @Override
    protected void resolveCollision(CollisionResult collision, Vector3f motion, Vector3f move) {
        CollisionResolver.bounce(collision, motion, move, BOUNCINESS);
    }

    @Override
    public void render(MatrixStack matrices, float delta) {
        Shader.activeShader.applyColor(ColorUtils.lerpRGBColor(0xFF8888, -1, Math.min(lifetime / 30f, 1f)));
        super.render(matrices, delta);
        Shader.activeShader.applyColor(-1);
    }

    @Override
    public void remove() {
        super.remove();
        world.explode(pos, EXPLOSION_RANGE, EXPLOSION_STRENGTH, this);
    }

    @Override
    public boolean damage(Entity source, DamageType type, int amount, boolean crit) {
        if (type == DamageType.EXPLOSION) {
            remove();
            return true;
        }

        return super.damage(source, type, amount, crit);
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.POTATO;
    }
}
