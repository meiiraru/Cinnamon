package cinnamon.world.entity.projectile;

import cinnamon.registry.EntityModelRegistry;
import cinnamon.registry.EntityRegistry;
import cinnamon.render.MatrixStack;
import cinnamon.render.shader.Shader;
import cinnamon.utils.ColorUtils;
import cinnamon.utils.Maths;
import cinnamon.world.DamageType;
import cinnamon.world.collisions.CollisionResolver;
import cinnamon.world.collisions.CollisionResult;
import cinnamon.world.entity.Entity;
import org.joml.Vector3f;

import java.util.UUID;

public class Potato extends Projectile {

    public static final int DAMAGE = 8;
    public static final float EXPLOSION_RANGE = 3f;
    public static final float EXPLOSION_STRENGTH = 1f;
    public static final int LIFETIME = 100;
    public static final float SPEED = 1.25f;
    public static final float CRIT_CHANCE = 0.15f;
    private static final Vector3f BOUNCINESS = new Vector3f(0.25f, 0, 0.25f);

    public Potato(UUID uuid, UUID owner) {
        super(uuid, EntityModelRegistry.POTATO.model, DAMAGE, LIFETIME, SPEED, CRIT_CHANCE, owner);
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