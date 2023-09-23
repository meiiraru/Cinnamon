package mayo.world.particle;

import mayo.Client;
import mayo.render.MatrixStack;
import mayo.utils.Meth;
import org.joml.Vector3f;

public abstract class Particle {

    private final Vector3f
            oPos = new Vector3f(),
            pos = new Vector3f(),
            motion = new Vector3f();
    private int lifetime;
    private boolean removed = false;

    public Particle(int lifetime) {
        this.lifetime = lifetime;
    }

    public void tick() {
        //change pos
        move(motion);

        //tick time
        lifetime--;
        if (lifetime <= 0) {
            lifetime = 0;
            removed = true;
        }
    }

    public void render(MatrixStack matrices, float delta) {
        matrices.push();

        //apply pos
        matrices.translate(Meth.lerp(oPos, pos, delta));

        //apply billboard
        Client.getInstance().camera.billboard(matrices.peek());

        //actual render
        renderParticle(matrices, delta);

        matrices.pop();
    }

    protected abstract void renderParticle(MatrixStack matrices, float delta);

    public Vector3f getPos(float delta) {
        return Meth.lerp(oPos, pos, delta);
    }

    public void setPos(Vector3f pos) {
        this.setPos(pos.x, pos.y, pos.z);
    }

    public void setPos(float x, float y, float z) {
        this.oPos.set(this.pos.set(x, y, z));
    }

    public void move(Vector3f vec) {
        move(vec.x, vec.y, vec.z);
    }

    public void move(float x, float y, float z) {
        this.oPos.set(pos);
        this.pos.add(x, y, z);
    }

    public boolean isRemoved() {
        return removed;
    }

    public void setMotion(Vector3f motion) {
        this.setMotion(motion.x, motion.y, motion.z);
    }

    public void setMotion(float x, float y, float z) {
        this.motion.set(x, y, z);
    }
}
