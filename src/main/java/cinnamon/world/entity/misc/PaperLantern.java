package cinnamon.world.entity.misc;

import cinnamon.math.Maths;
import cinnamon.math.collision.Hit;
import cinnamon.math.collision.shape.Sphere;
import cinnamon.registry.EntityModelRegistry;
import cinnamon.registry.EntityRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import cinnamon.world.entity.projectile.Projectile;
import cinnamon.world.light.Light;
import cinnamon.world.light.PointLight;
import cinnamon.world.world.World;
import cinnamon.world.world.WorldClient;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.UUID;

public class PaperLantern extends PhysEntity {

    protected final Light light = new PointLight().falloff(2f, 5f).volumetricStrength(0f).color(0xFFAA55).intensity(1f).glareIntensity(0f).shadowIntensity(0f).source(getUUID());
    protected final int rotateDir;

    protected int animY;

    public PaperLantern(UUID uuid) {
        super(uuid, EntityModelRegistry.PAPER_LANTERN.resource);
        this.rotateDir = Math.random() < 0.5 ? -1 : 1;
        this.animY = Maths.range(-100, 100);
        setGravity(0f);
    }

    @Override
    public void tick() {
        super.tick();
        this.rotate(0, rotateDir, 0);
        animY++;
    }

    @Override
    protected void applyImpulse() {
        this.motion.add(impulse.mul(0.125f));
        this.impulse.set(0);
        onGround = false;
    }

    @Override
    protected void motionFallout() {
        this.motion.mul(0.91f);
    }

    @Override
    public void render(Camera camera, MatrixStack matrices, float delta) {
        super.render(camera, matrices, delta);
        Vector3f pos = getPos(delta);
        pos.y += getAABB().getHeight() / 2f;
        light.pos(pos);
    }

    @Override
    protected void applyModelPose(Camera camera, MatrixStack matrices, float delta) {
        matrices.translate(0, Math.sin((animY + delta) * 0.05f) * getAABB().getHeight() * 0.075f, 0);
        super.applyModelPose(camera, matrices, delta);
    }

    @Override
    public void onAdded(World world) {
        super.onAdded(world);
        ((WorldClient) world).addLight(light);
        light.pos(getAABB().getCenter());
    }

    @Override
    public void onRemoved() {
        ((WorldClient) getWorld()).removeLight(light);
        super.onRemoved();
    }

    @Override
    public boolean onAttacked(Entity source, Hit hit) {
        knockback(source.getLookDir(), 0.1f);
        return true;
    }

    @Override
    protected void collidedFromEntity(PhysEntity entity, Hit result, Vector3f toMove) {
        if (this.isRemoved())
            return;

        if (!(entity instanceof Projectile proj)) {
            super.collidedFromEntity(entity, result, toMove);
            return;
        }

        proj.remove();
        getWorld().explode(new Sphere(getTransform().getPos(), 0.2f), 0.1f, this, false);
        this.remove();
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.PAPER_LANTERN;
    }
}
