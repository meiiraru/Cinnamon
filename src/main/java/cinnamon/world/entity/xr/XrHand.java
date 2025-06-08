package cinnamon.world.entity.xr;

import cinnamon.Client;
import cinnamon.registry.EntityRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.utils.AABB;
import cinnamon.utils.Maths;
import cinnamon.vr.XrHandTransform;
import cinnamon.vr.XrInput;
import cinnamon.vr.XrRenderer;
import cinnamon.world.collisions.CollisionResult;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import org.joml.Vector3f;

import java.util.UUID;

public class XrHand extends PhysEntity {

    private final int hand;
    private XrGrabbable targetEntity;
    private boolean isGrabbing;

    public XrHand(UUID uuid, int hand) {
        super(uuid, null);
        this.hand = hand;
    }

    @Override
    public void preTick() {
        super.preTick();
        if (!isGrabbing && targetEntity != null && !targetEntity.getAABB().intersects(getAABB()))
            targetEntity = null;
    }

    @Override
    public void tick() {
        super.tick();
        if (targetEntity != null)
            targetEntity.suggestGrab();
    }

    @Override
    protected void tickPhysics() {
        super.tickPhysics();

        XrHandTransform transform = XrRenderer.getHandTransform(hand);

        Camera c = Client.getInstance().camera;
        Vector3f pos = new Vector3f(transform.pos());
        pos.rotate(c.getRot());
        pos.add(c.getPos());
        moveTo(pos);

        Vector3f dir = new Vector3f(0, 0, -1);
        dir.rotate(transform.rot());
        dir.rotate(c.getRot());

        rotateToWithRiders(Maths.dirToRot(dir));
    }

    @Override
    public void render(MatrixStack matrices, float delta) {
        //update the hand position on every frame!
        tickPhysics();
        super.render(matrices, delta);
    }

    @Override
    protected Vector3f tickTerrainCollisions() {
        return new Vector3f();
    }

    @Override
    protected void collide(Entity entity, CollisionResult result, Vector3f toMove) {
        if (targetEntity == null && entity instanceof XrGrabbable grabbable && !grabbable.isGrabbed()) {
            targetEntity = grabbable;
            XrInput.vibrate(hand);
        }
    }

    @Override
    protected void renderModel(MatrixStack matrices, float delta) {
        //nothing to render
    }

    @Override
    protected void updateAABB() {
        this.aabb = new AABB().translate(getPos()).inflate(0.05f);
    }

    @Override
    public float getEyeHeight() {
        return 0;
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.XR_HAND;
    }

    public void grab() {
        if (targetEntity != null && !targetEntity.isGrabbed()) {
            isGrabbing = true;
            targetEntity.grab(this);
        }
    }

    public void release() {
        if (targetEntity != null && targetEntity.getHand() == this)
            targetEntity.release();
        targetEntity = null;
        isGrabbing = false;
    }

    public int getHand() {
        return hand;
    }

    public void applyTransform(XrGrabbable grabbable) {
        XrHandTransform transform = XrRenderer.getHandTransform(hand);

        Camera c = Client.getInstance().camera;
        Vector3f pos = new Vector3f(transform.pos());
        pos.add(new Vector3f(hand % 2 == 0 ? 0.05f : -0.05f, 0, 0).rotate(transform.rot()));
        pos.rotate(c.getRot());
        pos.add(c.getPos());

        grabbable.moveTo(pos);
        grabbable.rotateToWithRiders(getRot());
    }
}
