package cinnamon.world.entity.projectile;

import cinnamon.events.Await;
import cinnamon.registry.EntityModelRegistry;
import cinnamon.registry.EntityRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.WorldRenderer;
import cinnamon.render.shader.Shader;
import cinnamon.utils.AABB;
import cinnamon.utils.ColorUtils;
import cinnamon.utils.Maths;
import cinnamon.world.collisions.CollisionResolver;
import cinnamon.world.collisions.CollisionResult;
import cinnamon.world.particle.DustParticle;
import cinnamon.world.world.WorldClient;
import org.joml.Math;
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

    private float oScale = 1f, scale = 1f;
    private int state = 0, easing = 0;

    public Potato(UUID uuid, UUID owner) {
        super(uuid, EntityModelRegistry.POTATO.resource, DAMAGE, LIFETIME, SPEED, CRIT_CHANCE, owner);
        setGravity(1f);
    }

    @Override
    public void tick() {
        super.tick();

        Vector3f vec = new Vector3f(motion);
        if (vec.lengthSquared() > 0f)
            vec.normalize();

        this.rotateTo(Maths.dirToRot(vec));

        if (!getWorld().isClientside())
            return;

        easing++;
        if (lifetime % (LIFETIME / 3) == 0) {
            oScale = scale;
            scale = 1f + 0.3f * state;
            easing = 0;
            state++;
            spawnSmokeParticle(state > 1 ? 10 : 3);
        }
    }

    protected void spawnSmokeParticle(int count) {
        for (int i = 0; i < count; i++) {
            DustParticle particle = new DustParticle(20, ColorUtils.lerpARGBColor(0xFFAAAAAA, 0xFFFFFFFF, (float) Math.random()));
            particle.setPos(new AABB(getAABB()).scale(scale).getRandomPoint());
            particle.setScale(scale);
            ((WorldClient) getWorld()).addParticle(particle);
        }
    }

    @Override
    protected void resolveCollision(CollisionResult collision, Vector3f totalMove) {
        CollisionResolver.bounce(collision, getMotion(), totalMove, BOUNCINESS);
    }

    @Override
    protected void renderModel(Camera camera, MatrixStack matrices, float delta) {
        //scale
        float t = (easing + delta) > 10 ? 1f : Maths.Easing.OUT_ELASTIC.get((easing + delta) / 10);
        matrices.pushMatrix();
        matrices.scale(Math.lerp(oScale, scale, t));

        //color
        if (!WorldRenderer.isOutlineRendering()) {
            float t2 = Math.min(lifetime / 30f, 1f);
            Shader.activeShader.applyColorRGBA(ColorUtils.lerpARGBColor(0xFFFF8888, 0xFFFFFFFF, t2));
        }

        //render
        super.renderModel(camera, matrices, delta);

        //reset
        matrices.popMatrix();
        Shader.activeShader.applyColorRGBA(0xFFFFFFFF);
    }

    @Override
    public void remove() {
        super.remove();
        world.explode(new AABB(pos, pos).inflate(EXPLOSION_RANGE), EXPLOSION_STRENGTH, this, false);
    }

    public void triggerExplosion() {
        new Await(2, () -> {
            if (!isRemoved())
                remove();
        });
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.POTATO;
    }
}
