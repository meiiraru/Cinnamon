package cinnamon.world.entity;

import cinnamon.Client;
import cinnamon.animation.Animation;
import cinnamon.gui.DebugScreen;
import cinnamon.math.AABB;
import cinnamon.math.Maths;
import cinnamon.model.ModelManager;
import cinnamon.registry.EntityRegistry;
import cinnamon.render.Camera;
import cinnamon.render.DebugRenderer;
import cinnamon.render.LightRenderer;
import cinnamon.render.MatrixStack;
import cinnamon.render.WorldRenderer;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.model.AnimatedObjRenderer;
import cinnamon.render.model.ModelRenderer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Resource;
import cinnamon.world.Mask;
import cinnamon.world.WorldObject;
import cinnamon.world.collisions.Hit;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.world.World;
import cinnamon.world.world.WorldClient;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class Entity extends WorldObject {

    protected final UUID uuid;
    protected final ModelRenderer model;

    protected final Vector3f
            oPos = new Vector3f();
    protected final Quaternionf
            oRot = new Quaternionf(),
            rot = new Quaternionf();

    protected final List<Entity> riders = new ArrayList<>();
    protected Entity riding;

    private boolean removed;

    private String name;

    private boolean silent;
    private boolean visible = true;

    protected Mask renderMask = new Mask();

    private final List<FeatureRenderer> renderFeatures = new ArrayList<>();

    public Entity(UUID uuid, Resource model) {
        this.model = ModelManager.load(model);
        this.uuid = uuid;
        this.updateAABB();
        this.addRenderFeature((source, camera, matrices, delta) -> {
            if (shouldRenderText(camera)) renderTexts(camera, matrices, delta);
        });
    }

    public void preTick() {
        this.oPos.set(pos);
        this.oRot.set(rot);
    }

    @Override
    public void render(Camera camera, MatrixStack matrices, float delta) {
        super.render(camera, matrices, delta);

        if (isVisible()) {
            //apply model pose
            matrices.pushMatrix();
            matrices.translate(getPos(delta));
            applyModelPose(camera, matrices, delta);

            //render model
            renderModel(camera, matrices, delta);
            matrices.popMatrix();
        }

        //render features
        for (FeatureRenderer renderFeature : renderFeatures)
            renderFeature.render(this, camera, matrices, delta);
    }

    protected void renderModel(Camera camera, MatrixStack matrices, float delta) {
        if (model != null)
            model.render(matrices);
    }

    public Animation getAnimation(String name) {
        return model instanceof AnimatedObjRenderer anim ? anim.getAnimation(name) : null;
    }

    protected void applyModelPose(Camera camera, MatrixStack matrices, float delta) {
        matrices.rotate(getRot(delta));
    }

    protected void renderTexts(Camera camera, MatrixStack matrices, float delta) {
        Text text = getHeadText();
        if (text == null)
            return;

        matrices.pushMatrix();

        text.withStyle(Style.EMPTY.outlined(true));

        float s = 1 / 48f;

        matrices.translate(getPos(delta));
        matrices.translate(0f, aabb.getHeight() + 0.15f, 0f);
        camera.billboard(matrices);
        matrices.scale(-s);

        text.render(VertexConsumer.WORLD_MAIN, matrices, 0, 0, Alignment.BOTTOM_CENTER, 48);

        matrices.popMatrix();
    }

    protected Text getHeadText() {
        return name == null || name.isBlank() ? null : Text.of(name);
    }

    public boolean shouldRenderText(Camera camera) {
        return
                !Client.getInstance().hideHUD &&
                (camera.getEntity() != this || DebugScreen.isTabOpen(DebugScreen.Tab.ENTITIES)) &&
                !WorldRenderer.isOutlineRendering() &&
                camera.getPos().distanceSquared(pos) <= 1024;
    }

    public void renderDebugHitbox(MatrixStack matrices, float delta) {
        //bounding box
        DebugRenderer.renderAABB(matrices, aabb, 0xFFFFFFFF);

        //looking dir
        matrices.pushMatrix();
        matrices.translate(getEyePos());
        DebugRenderer.renderArrow(matrices, getLookDir(), 1f, 0xFF0000FF);
        matrices.popMatrix();
    }

    public boolean shouldRenderOutline() {
        return false;
    }

    public int getOutlineColor() {
        return 0xFFFFFFFF;
    }

    public UUID getUUID() {
        return uuid;
    }

    public void onAdded(World world) {
        super.onAdded(world);
    }

    public void remove() {
        this.removed = true;
        this.stopRiding();
        for (Entity rider : new ArrayList<>(riders))
            rider.stopRiding();
    }

    public void impulse(float left, float up, float forwards) {
        if (riding != null) {
            riding.impulse(left, up, forwards);
            return;
        }

        Vector3f move = new Vector3f(-left, up, -forwards);
        move.rotate(rot);

        this.moveTo(
                pos.x + move.x,
                pos.y + move.y,
                pos.z + move.z
        );
    }

    public void moveTo(Vector3f vec) {
        this.moveTo(vec.x, vec.y, vec.z);
    }

    public void moveTo(float x, float y, float z) {
        this.pos.set(x, y, z);
        this.updateAABB();
        this.updateRidersPos();
        this.checkWorldVoid();
        sendServerUpdate();
    }

    public void rotate(Quaternionf quat) {
        if (riding != null)
            riding.rotate(quat);
        rotateTo(getRot().mul(quat));
    }

    public void rotate(float pitch, float yaw, float roll) {
        if (riding != null)
            riding.rotate(pitch, yaw, roll);

        Quaternionf rotation = getRot();
        rotation.rotateZYX(Math.toRadians(roll), Math.toRadians(-yaw), Math.toRadians(-pitch));
        rotateTo(rotation);
    }

    public void rotateTo(Quaternionf quat) {
        this.rot.set(quat);
        sendServerUpdate();
    }

    public void rotateTo(float pitch, float yaw, float roll) {
        this.rot.rotationZYX(Math.toRadians(roll), Math.toRadians(-yaw), Math.toRadians(-pitch));
        sendServerUpdate();
    }

    public void rotateToWithRiders(Quaternionf quat) {
        this.rotateTo(quat);
        this.updateRiders();
    }

    public void rotateToWithRiders(float pitch, float yaw, float roll) {
        this.rotateTo(pitch, yaw, roll);
        this.updateRiders();
    }

    public void lookAt(Vector3f pos) {
        this.lookAt(pos.x, pos.y, pos.z);
    }

    public void lookAt(float x, float y, float z) {
        //get difference
        Vector3f v = new Vector3f(x, y, z).sub(getEyePos());
        v.normalize();

        //set rot
        this.rotateTo(Maths.dirToQuat(v));
    }

    protected void updateAABB() {
        if (this.model == null) {
            this.aabb.set(getPos()).inflate(0.5f, 0f, 0.5f, 0.5f, 1f, 0.5f);
            return;
        }

        //set AABB
        this.aabb.set(this.model.getAABB());

        //make it square
        float diff = (aabb.getWidth() - aabb.getDepth()) * 0.5f;
        if (diff > 0)
            aabb.inflate(0, 0, diff);
        else
            aabb.inflate(-diff, 0, 0);

        //add pos
        aabb.translate(getPos());
    }

    public Vector3f getPos(float delta) {
        return Maths.lerp(oPos, pos, delta);
    }

    public void setPos(float x, float y, float z) {
        super.setPos(x, y, z);
        this.oPos.set(x, y, z);
        this.updateAABB();
        this.updateRidersPos();
        this.checkWorldVoid();
    }

    public Quaternionf getRot(float delta) {
        return new Quaternionf(oRot).slerp(rot, delta);
    }

    public Quaternionf getRot() {
        return rot;
    }

    public void setRot(Quaternionf rot) {
        this.oRot.set(this.rot.set(rot));
    }

    public void setRot(float pitch, float yaw, float roll) {
        this.oRot.set(this.rot.rotationZYX(Math.toRadians(roll), Math.toRadians(-yaw), Math.toRadians(-pitch)));
    }

    public float getEyeHeight() {
        return aabb.getHeight() * 0.5f;
    }

    public Vector3f getEyePos(float delta) {
        return getPos(delta).add(0, getEyeHeight(), 0);
    }

    public Vector3f getEyePos() {
        return new Vector3f(pos.x, pos.y + getEyeHeight(), pos.z);
    }

    public Vector3f getLookDir() {
        return new Vector3f(0f, 0f, -1f).rotate(getRot());
    }

    public Vector3f getLookDir(float delta) {
        return new Vector3f(0f, 0f, -1f).rotate(getRot(delta));
    }

    public World getWorld() {
        return world;
    }

    public boolean isRemoved() {
        return removed;
    }

    public boolean isTargetable() {
        return true;
    }

    public float getPickRange() {
        return 1f;
    }

    public Hit<Terrain> getLookingTerrain(float distance) {
        //prepare positions
        Vector3f pos = getEyePos();
        Vector3f range = getLookDir().mul(distance);
        AABB area = new AABB(pos).expand(range);

        //return hit
        return world.raycastTerrain(area, pos, range, t -> t.isSelectable(this));
    }

    public Hit<Entity> getLookingEntity(float distance) {
        //prepare positions
        Vector3f pos = getEyePos();
        Vector3f range = getLookDir().mul(distance);
        AABB area = new AABB(pos).expand(range);

        //return hit
        return world.raycastEntity(area, pos, range, e -> e != this && e != this.riding && e.isTargetable());
    }

    public Hit<? extends WorldObject> getLookingObject(float distance) {
        Hit<Entity> entityHit = getLookingEntity(distance);
        Hit<Terrain> terrainHit = getLookingTerrain(distance);

        if (entityHit != null && (terrainHit == null || entityHit.collision().near() < terrainHit.collision().near()))
            return entityHit;

        return terrainHit;
    }

    public Entity getRidingEntity() {
        return riding;
    }

    public List<Entity> getRiders() {
        return riders;
    }

    public Entity getControllingEntity() {
        return riders.isEmpty() ? this : riders.getFirst().getControllingEntity();
    }

    public void updateRiders() {
        updateRidersPos();
        updateRidersRot();
    }

    public void updateRidersPos() {
        for (Entity rider : new ArrayList<>(riders)) {
            rider.moveTo(getRiderOffset(rider).add(this.pos));
            rider.updateRidersPos();
        }
    }

    public void updateRidersRot() {
        float pitchDelta = Maths.getPitch(rot) - Maths.getPitch(oRot);
        float yawDelta   = Maths.getYaw(rot)   - Maths.getYaw(oRot);
        float rollDelta  = Maths.getRoll(rot)  - Maths.getRoll(oRot);
        for (Entity rider : new ArrayList<>(riders)) {
            Quaternionf riderRot = rider.getRot();
            rider.rotateTo(Maths.getPitch(riderRot) + pitchDelta, Maths.getYaw(riderRot) + yawDelta, Maths.getRoll(riderRot) + rollDelta);
            rider.updateRidersRot();
        }
    }

    public Vector3f getRiderOffset(Entity rider) {
        Vector3f vec = new Vector3f(0, aabb.getHeight(), 0);
        vec.rotate(rot);
        return vec;
    }

    public boolean addRider(Entity e) {
        e.stopRiding();
        this.riders.add(e);
        e.riding = this;
        updateRiders();
        return true;
    }

    protected void removeRider(Entity e) {
        e.riding = null;
        this.riders.remove(e);
        updateRiders();
    }

    public void stopRiding() {
        if (isRiding())
            this.riding.removeRider(this);
    }

    public boolean isRiding() {
        return this.riding != null;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public boolean onUse(LivingEntity source) {
        return false;
    }

    public boolean onAttacked(LivingEntity source) {
        return false;
    }

    @Override
    public abstract EntityRegistry getType();

    public void sendServerUpdate() {
        //if (!getWorld().isClientside())
        //    ServerConnection.connection.sendToAllUDP(new EntitySync().entity(this));
    }

    @Override
    public boolean shouldRender(Camera camera) {
        return (camera.getEntity() != this || ((WorldClient) getWorld()).isThirdPerson() || WorldRenderer.isShadowRendering())
                && WorldRenderer.activeMask.test(getRenderMask())
                && !(WorldRenderer.isShadowRendering() && getUUID().equals(LightRenderer.getShadowLight().getSource()))
                && camera.getPos().distanceSquared(getPos()) <= getRenderDistance()
                && super.shouldRender(camera);
    }

    public int getRenderDistance() {
        int dist = WorldRenderer.entityRenderDistance;
        return dist * dist;
    }

    protected void checkWorldVoid() {
        if (getWorld() != null && pos.y < getWorld().bottomOfTheWorld)
            remove();
    }

    public boolean isSilent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Mask getRenderMask() {
        return renderMask;
    }

    public void addRenderFeature(FeatureRenderer feature) {
        this.renderFeatures.add(feature);
    }

    public Vector3f getLastPos() {
        return oPos;
    }

    public Quaternionf getLastRot() {
        return oRot;
    }
}
