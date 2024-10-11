package cinnamon.world.particle;

import cinnamon.Client;
import cinnamon.model.GeometryHelper;
import cinnamon.registry.ParticlesRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.AABB;
import cinnamon.utils.Maths;
import cinnamon.world.WorldObject;
import org.joml.Vector3f;

public abstract class Particle extends WorldObject {

    public static final float PARTICLE_SCALING = 1 / 48f;

    protected final Vector3f
            oPos = new Vector3f(),
            motion = new Vector3f();
    protected final int lifetime;
    protected int age;
    protected boolean removed = false;
    protected boolean emissive;

    public Particle(int lifetime) {
        this.lifetime = lifetime;
    }

    public void tick() {
        oPos.set(pos);
        //change pos
        move(motion);

        //tick time
        age++;
        removed |= age > lifetime;
    }

    public void render(MatrixStack matrices, float delta) {
        matrices.push();

        //apply pos
        matrices.translate(Maths.lerp(oPos, pos, delta));

        //apply billboard
        Client.getInstance().camera.billboard(matrices);

        //actual render
        matrices.push();
        renderParticle(matrices, delta);
        matrices.pop();

        matrices.pop();
    }

    @Override
    public boolean shouldRender(Camera camera) {
        return camera.getPos().distanceSquared(pos) <= getRenderDistance() && camera.isInsideFrustum(pos.x, pos.y, pos.z);
    }

    protected int getRenderDistance() {
        return 4098;
    }

    protected abstract void renderParticle(MatrixStack matrices, float delta);

    public Vector3f getPos(float delta) {
        return Maths.lerp(oPos, pos, delta);
    }

    @Override
    public void setPos(float x, float y, float z) {
        super.setPos(x, y, z);
        this.oPos.set(x, y, z);
    }

    public void move(Vector3f vec) {
        move(vec.x, vec.y, vec.z);
    }

    public void move(float x, float y, float z) {
        this.pos.add(x, y, z);
    }

    @Override
    public AABB getAABB() {
        return new AABB(pos, pos);
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
        AABB aabb = getAABB();
        Vector3f min = aabb.getMin();
        Vector3f max = aabb.getMax();
        VertexConsumer.LINES.consume(GeometryHelper.cube(matrices, min.x, min.y, min.z, max.x, max.y, max.z, 0xFFFFFFFF));
    }
}
