package cinnamon.world.entity.xr;

import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.utils.Resource;
import cinnamon.world.entity.PhysEntity;

import java.util.UUID;

public abstract class XrGrabbable extends PhysEntity {

    private boolean suggestGrab;
    private XrHand hand;

    public XrGrabbable(UUID uuid, Resource model) {
        super(uuid, model);
    }

    @Override
    public void preTick() {
        super.preTick();
        suggestGrab = false;
    }

    @Override
    protected void tickPhysics() {
        if (!isGrabbed())
            super.tickPhysics();
    }

    @Override
    public void render(MatrixStack matrices, float delta) {
        //update the hand position on every frame!
        moveToHand();
        super.render(matrices, delta);
    }

    @Override
    public boolean shouldRender(Camera camera) {
        return isGrabbed() || super.shouldRender(camera);
    }

    protected void moveToHand() {
        if (hand != null)
            hand.applyTransform(this);
    }

    public void grab(XrHand hand) {
        this.hand = hand;
        moveToHand();
    }

    public void release() {
        this.hand = null;
    }

    public boolean isGrabbed() {
        return hand != null;
    }

    public XrHand getHand() {
        return hand;
    }

    @Override
    public boolean shouldRenderOutline() {
        return suggestGrab || super.shouldRenderOutline();
    }

    public void suggestGrab() {
        this.suggestGrab = true;
    }
}
