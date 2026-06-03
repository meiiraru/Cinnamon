package cinnamon.world.entity;

import cinnamon.Client;
import cinnamon.animation.Animation;
import cinnamon.gui.DebugScreen;
import cinnamon.math.Maths;
import cinnamon.math.collision.AABB;
import cinnamon.math.collision.Hit;
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
import cinnamon.utils.Pair;
import cinnamon.utils.Resource;
import cinnamon.world.Mask;
import cinnamon.world.WorldObject;
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
            oRot = new Quaternionf();
    protected final Vector3f
            oScale = new Vector3f(1f);

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
        this.addRenderFeature((source, camera, matrices, delta) -> {
            if (shouldRenderText(camera)) renderTexts(camera, matrices, delta);
        });
    }

    public void preTick() {
        this.oPos.set(transform.getPos());
        this.oRot.set(transform.getRot());
        this.oScale.set(transform.getScale());
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
        matrices.scale(getScale(delta));
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
        matrices.scale(getScale(delta));

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
                camera.getPos().distanceSquared(transform.getPos()) <= 1024;
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
        move.rotate(transform.getRot());

        Vector3f pos = transform.getPos();
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
        this.transform.setPos(x, y, z);
        this.calculateBounds();
        this.updateRidersPos();
        this.checkWorldVoid();
        sendServerUpdate();
    }

    public void rotate(Quaternionf quat) {
        if (riding != null)
            riding.rotate(quat);
        rotateTo(transform.getRot().mul(quat));
    }

    public void rotate(float pitch, float yaw, float roll) {
        if (riding != null)
            riding.rotate(pitch, yaw, roll);

        Quaternionf rotation = transform.getRot();
        rotation.rotateZYX(Math.toRadians(roll), Math.toRadians(-yaw), Math.toRadians(-pitch));
        rotateTo(rotation);
    }

    public void rotateTo(Quaternionf quat) {
        this.transform.setRot(quat);
        sendServerUpdate();
    }

    public void rotateTo(float pitch, float yaw, float roll) {
        this.transform.setRot(pitch, yaw, roll);
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
        Vector3f v = getEyePos().sub(x, y, z);
        v.normalize();

        //set rot
        this.rotateTo(Maths.dirToQuat(v));
    }

    public void scale(Vector3f scale) {
        this.scale(scale.x, scale.y, scale.z);
    }

    public void scale(float scale) {
        this.scale(scale, scale, scale);
    }

    public void scale(float x, float y, float z) {
        Vector3f scale = transform.getScale();
        this.scaleTo(scale.x * x, scale.y * y, scale.z * z);
    }

    public void scaleTo(Vector3f scale) {
        this.scaleTo(scale.x, scale.y, scale.z);
    }

    public void scaleTo(float scale) {
        this.scaleTo(scale, scale, scale);
    }

    public void scaleTo(float x, float y, float z) {
        float w = Math.max(x, z);
        this.transform.setScale(w, y, w);
        this.moveTo(transform.getPos());
    }

    @Override
    public void calculateBounds() {
        Vector3f scale = transform.getScale();
        if (this.model == null) {
            this.aabb
                    .set(transform.getPos())
                    .inflate(0.5f, 0f, 0.5f, 0.5f, 1f, 0.5f)
                    .scaleAnchorBottom(scale);
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
        aabb.translate(transform.getPos());

        //add scale
        aabb.scaleAnchorBottom(scale);
    }

    public Vector3f getPos(float delta) {
        return Maths.lerp(oPos, transform.getPos(), delta);
    }

    public void setPos(Vector3f vec) {
        this.setPos(vec.x, vec.y, vec.z);
    }

    public void setPos(float x, float y, float z) {
        this.transform.setPos(x, y, z);
        this.oPos.set(this.transform.getPos());
        this.calculateBounds();
        this.updateRidersPos();
        this.checkWorldVoid();
    }

    public Quaternionf getRot(float delta) {
        return new Quaternionf(oRot).slerp(transform.getRot(), delta);
    }

    public void setRot(Quaternionf rot) {
        this.transform.setRot(rot);
        this.oRot.set(this.transform.getRot());
    }

    public void setRot(float pitch, float yaw, float roll) {
        this.transform.setRot(pitch, yaw, roll);
        this.oRot.set(this.transform.getRot());
    }

    public Vector3f getScale(float delta) {
        return Maths.lerp(oScale, transform.getScale(), delta);
    }

    public void setScale(Vector3f scale) {
        this.setScale(scale.x, scale.y, scale.z);
    }

    public void setScale(float scale) {
        this.setScale(scale, scale, scale);
    }

    public void setScale(float x, float y, float z) {
        float w = Math.max(x, z);
        this.transform.setScale(w, y, w);
        this.oScale.set(this.transform.getScale());
        this.moveTo(transform.getPos());
    }

    public float getEyeHeight() {
        return aabb.getHeight() * 0.5f;
    }

    public Vector3f getEyePos(float delta) {
        return getPos(delta).add(0, getEyeHeight(), 0);
    }

    public Vector3f getEyePos() {
        Vector3f pos = transform.getPos();
        return new Vector3f(pos.x, pos.y + getEyeHeight(), pos.z);
    }

    public Vector3f getLookDir() {
        return new Vector3f(0f, 0f, -1f).rotate(transform.getRot());
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

    public Pair<Hit, Terrain> getLookingTerrain(float distance) {
        //prepare positions
        Vector3f pos = getEyePos();
        Vector3f range = getLookDir().mul(distance);
        AABB area = new AABB(pos).expand(range);

        //return hit
        return world.raycastTerrain(area, pos, range, t -> t.isSelectable(this));
    }

    public Pair<Hit, Entity> getLookingEntity(float distance) {
        if (world == null)
            return null;

        //prepare positions
        Vector3f pos = getEyePos();
        Vector3f range = getLookDir().mul(distance);
        AABB area = new AABB(pos).expand(range);

        //return hit
        return world.raycastEntity(area, pos, range, e -> e != this && e != this.riding && e.isTargetable());
    }

    public Pair<Hit, ? extends WorldObject> getLookingObject(float distance) {
        if (world == null)
            return null;

        Pair<Hit, Entity> entityHit = getLookingEntity(distance);
        Pair<Hit, Terrain> terrainHit = getLookingTerrain(distance);

        if (entityHit != null && (terrainHit == null || entityHit.first().tNear() < terrainHit.first().tNear()))
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
            rider.moveTo(getRiderOffset(rider).add(this.transform.getPos()));
            rider.updateRidersPos();
        }
    }

    public void updateRidersRot() {
        Quaternionf rot = this.transform.getRot();
        float pitchDelta = Maths.getPitch(rot) - Maths.getPitch(oRot);
        float yawDelta   = Maths.getYaw(rot)   - Maths.getYaw(oRot);
        float rollDelta  = Maths.getRoll(rot)  - Maths.getRoll(oRot);
        for (Entity rider : new ArrayList<>(riders)) {
            Quaternionf riderRot = rider.getTransform().getRot();
            rider.rotateTo(Maths.getPitch(riderRot) + pitchDelta, Maths.getYaw(riderRot) + yawDelta, Maths.getRoll(riderRot) + rollDelta);
            rider.updateRidersRot();
        }
    }

    public Vector3f getRiderOffset(Entity rider) {
        Vector3f vec = new Vector3f(0, aabb.getHeight(), 0);
        vec.rotate(transform.getRot());
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
                && camera.getPos().distanceSquared(transform.getPos()) <= getRenderDistance()
                && super.shouldRender(camera);
    }

    public int getRenderDistance() {
        int dist = WorldRenderer.entityRenderDistance;
        return dist * dist;
    }

    protected void checkWorldVoid() {
        if (getWorld() != null && transform.getPos().y < getWorld().bottomOfTheWorld)
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
