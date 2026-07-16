package cinnamon.world.particle;

import cinnamon.math.Maths;
import cinnamon.math.collision.AABB;
import cinnamon.math.collision.Collider;
import cinnamon.registry.ParticlesRegistry;
import cinnamon.render.Camera;
import cinnamon.render.DebugRenderer;
import cinnamon.render.MatrixStack;
import cinnamon.utils.Mask;
import cinnamon.world.WorldObject;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import cinnamon.world.terrain.Terrain;
import org.joml.Vector3f;

public abstract class Particle extends WorldObject {

    public static final float PARTICLE_SCALING = 1 / 48f;

    protected final Vector3f
            oPos = new Vector3f(),
            motion = new Vector3f();
    protected final int lifetime;
    protected int age;
    protected boolean removed = false;
    protected boolean billboard = true;
    protected boolean emissive;
    protected Mask collisionMask = new Mask();

    public Particle(int lifetime) {
        this.lifetime = lifetime;
    }

    @Override
    public void tick() {
        super.tick();

        oPos.set(transform.getPos());
        //change pos
        move(motion);

        //tick time
        if (lifetime >= 0 && ++age > lifetime)
            remove();
    }

    @Override
    public void render(Camera camera, MatrixStack matrices, float delta) {
        super.render(camera, matrices, delta);

        matrices.pushMatrix();

        //apply pos
        matrices.translate(getPos(delta));

        //apply billboard
        if (billboard)
            camera.billboard(matrices);

        //actual render
        matrices.pushMatrix();
        renderParticle(camera, matrices, delta);
        matrices.popMatrix();

        matrices.popMatrix();
    }

    @Override
    public boolean shouldRender(Camera camera) {
        return camera.getPos().distanceSquared(transform.getPos()) <= getRenderDistance() && super.shouldRender(camera);
    }

    protected int getRenderDistance() {
        return 4098; //64 * 64;
    }

    protected abstract void renderParticle(Camera camera, MatrixStack matrices, float delta);

    public Vector3f getPos(float delta) {
        return Maths.lerp(oPos, transform.getPos(), delta);
    }

    public void setPos(Vector3f pos) {
        this.setPos(pos.x, pos.y, pos.z);
    }

    public void setPos(float x, float y, float z) {
        this.transform.setPos(x, y, z);
        this.oPos.set(transform.getPos());
    }

    public void move(Vector3f vec) {
        move(vec.x, vec.y, vec.z);
    }

    public void move(float x, float y, float z) {
        Vector3f pos = transform.getPos();
        pos.add(x, y, z);
        transform.setPos(pos);
        calculateBounds();
    }

    @Override
    public void calculateBounds() {
        aabb.set(transform.getPos());
    }

    public boolean isRemoved() {
        return removed;
    }

    public Vector3f getMotion() {
        return motion;
    }

    public void setMotion(Vector3f motion) {
        this.setMotion(motion.x, motion.y, motion.z);
    }

    public void setMotion(float x, float y, float z) {
        this.motion.set(x, y, z);
    }

    public int getLifetime() {
        return lifetime;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void remove() {
        this.removed = true;
    }

    public void setEmissive(boolean emissive) {
        this.emissive = emissive;
    }

    public boolean isEmissive() {
        return emissive;
    }

    @Override
    public abstract ParticlesRegistry getType();

    public void renderDebugHitbox(MatrixStack matrices, float delta) {
        DebugRenderer.renderAABB(matrices, getAABB(), 0xFFFFFFFF);
    }

    public Mask getCollisionMask() {
        return collisionMask;
    }

    protected boolean collideTerrain() {
        AABB aabb = getAABB();
        for (Terrain terrain : world.getTerrains(aabb)) {
            if (!collisionMask.test(terrain.getCollisionMask()))
                continue;

            for (Collider<?> terrainColl : terrain.getPreciseCollider()) {
                if (aabb.intersects(terrainColl))
                    return true;
            }
        }

        return false;
    }

    protected boolean collideEntities() {
        AABB aabb = getAABB();
        for (Entity entity : world.getEntities(aabb)) {
            if (entity instanceof PhysEntity && aabb.intersectsAABB(entity.getAABB()))
                return true;
        }

        return false;
    }
}
