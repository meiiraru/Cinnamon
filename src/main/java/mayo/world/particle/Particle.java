package mayo.world.particle;

import mayo.Client;
import mayo.registry.ParticlesRegistry;
import mayo.render.Camera;
import mayo.render.MatrixStack;
import mayo.utils.AABB;
import mayo.utils.Maths;
import mayo.world.WorldObject;
import org.joml.Vector3f;

public abstract class Particle extends WorldObject {

    protected static final float PARTICLE_SCALING = 1 / 48f;

    private final Vector3f
            oPos = new Vector3f(),
            motion = new Vector3f();
    private final int lifetime;
    private int age;
    private boolean removed = false;
    private boolean emissive;

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
        renderParticle(matrices, delta);

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
}
