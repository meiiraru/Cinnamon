package cinnamon.world.entity;

import cinnamon.Client;
import cinnamon.animation.Animation;
import cinnamon.model.GeometryHelper;
import cinnamon.model.ModelManager;
import cinnamon.registry.EntityRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.WorldRenderer;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.model.AnimatedObjRenderer;
import cinnamon.render.model.ModelRenderer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.*;
import cinnamon.world.DamageType;
import cinnamon.world.WorldObject;
import cinnamon.world.collisions.Hit;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.world.World;
import cinnamon.world.world.WorldClient;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class Entity extends WorldObject {

    protected final UUID uuid;
    protected final ModelRenderer model;

    protected final Vector3f
            oPos = new Vector3f();
    protected final Vector2f
            oRot = new Vector2f(),
            rot = new Vector2f();

    protected AABB aabb;

    protected final List<Entity> riders = new ArrayList<>();
    protected Entity riding;

    private boolean removed;

    private String name;

    public Entity(UUID uuid, Resource model) {
        this.model = ModelManager.load(model);
        this.uuid = uuid;
        this.updateAABB();
    }

    public void tick() {
        this.oPos.set(pos);
        this.oRot.set(rot);
    }

    public void render(MatrixStack matrices, float delta) {
        matrices.pushMatrix();

        //apply pos
        matrices.translate(getPos(delta));

        //apply model pose
        matrices.pushMatrix();
        applyModelPose(matrices, delta);

        //render model
        renderModel(matrices, delta);

        //render features
        renderFeatures(matrices, delta);

        matrices.popMatrix();

        //render head text
        if (shouldRenderText()) {
            matrices.pushMatrix();
            renderTexts(matrices, delta);
            matrices.popMatrix();
        }

        matrices.popMatrix();
    }

    protected void renderModel(MatrixStack matrices, float delta) {
        model.render(matrices);
    }

    public Animation getAnimation(String name) {
        return model instanceof AnimatedObjRenderer anim ? anim.getAnimation(name) : null;
    }

    protected void applyModelPose(MatrixStack matrices, float delta) {
        Vector2f rot = getRot(delta);
        matrices.rotate(Rotation.Y.rotationDeg(-rot.y));
        matrices.rotate(Rotation.X.rotationDeg(-rot.x));
    }

    protected void renderFeatures(MatrixStack matrices, float delta) {}

    protected void renderTexts(MatrixStack matrices, float delta) {
        Text text = getHeadText();
        if (text == null)
            return;

        text.withStyle(Style.EMPTY.outlined(true));

        Client c = Client.getInstance();
        float s = 1 / 48f;

        matrices.translate(0f, aabb.getHeight() + 0.15f, 0f);
        c.camera.billboard(matrices);
        matrices.scale(-s);

        text.render(VertexConsumer.WORLD_FONT, matrices, 0, 0, Alignment.BOTTOM_CENTER, 50);
    }

    protected Text getHeadText() {
        return name == null || name.isBlank() ? null : Text.of(name);
    }

    public boolean shouldRenderText() {
        Client client = Client.getInstance();
        return
                !((WorldClient) world).hudHidden() &&
                (client.camera.getEntity() != this || client.debug) &&
                !WorldRenderer.isRenderingOutlines() &&
                client.camera.getPos().distanceSquared(pos) <= 1024;
    }

    public void renderDebugHitbox(MatrixStack matrices, float delta) {
        //bounding box
        Vector3f min = aabb.getMin();
        Vector3f max = aabb.getMax();
        VertexConsumer.LINES.consume(GeometryHelper.cube(matrices, min.x, min.y, min.z, max.x, max.y, max.z, 0xFFFFFFFF));

        //eye pos
        Vector3f eye = getEyePos();
        if (this instanceof LivingEntity) {
            float f = 0.01f;
            VertexConsumer.LINES.consume(GeometryHelper.cube(matrices, min.x, eye.y - f, min.z, max.x, eye.y + f, max.z, 0xFFFF0000));
        }

        //looking dir
        matrices.pushMatrix();
        matrices.translate(eye);
        matrices.rotate(Rotation.Y.rotationDeg(-rot.y + 90));
        matrices.rotate(Rotation.Z.rotationDeg(-rot.x));

        VertexConsumer.LINES.consume(GeometryHelper.quad(
                matrices,
                0f, 0f, 0f,
                aabb.getWidth() + 1f, 0f,
                0xFF0000FF

        ));

        matrices.popMatrix();
    }

    public boolean shouldRenderOutline() {
        return true;
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

    public boolean damage(Entity source, DamageType type, int amount, boolean crit) {
        return false;
    }

    public void move(float left, float up, float forwards) {
        if (riding != null) {
            riding.move(left, up, forwards);
            return;
        }

        Vector3f move = new Vector3f(-left, up, -forwards);

        move.rotateX((float) Math.toRadians(-rot.x));
        move.rotateY((float) Math.toRadians(-rot.y));

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
        sendServerUpdate();
    }

    public void rotate(float pitch, float yaw) {
        if (riding != null)
            riding.rotate(pitch, yaw);

        rotateTo(rot.x + pitch, rot.y + yaw);
    }

    public void rotateTo(Vector2f vec) {
        this.rotateTo(vec.x, vec.y);
    }

    public void rotateTo(float pitch, float yaw) {
        this.rot.set(Maths.wrapDegrees(pitch), Maths.wrapDegrees(yaw));
        sendServerUpdate();
    }

    public void rotateToWithRiders(Vector2f vec) {
        this.rotateToWithRiders(vec.x, vec.y);
    }

    public void rotateToWithRiders(float pitch, float yaw) {
        this.rotateTo(pitch, yaw);
        this.updateRidersRot();
    }

    public void lookAt(Vector3f pos) {
        this.lookAt(pos.x, pos.y, pos.z);
    }

    public void lookAt(float x, float y, float z) {
        //get difference
        Vector3f v = new Vector3f(x, y, z).sub(getEyePos());
        v.normalize();

        //set rot
        this.rotateTo(Maths.dirToRot(v));
    }

    protected void updateAABB() {
        //set AABB
        this.aabb = this.model.getAABB();

        //make it square
        float diff = (aabb.getWidth() - aabb.getDepth()) * 0.5f;
        if (diff > 0)
            aabb.inflate(0, 0, diff);
        else
            aabb.inflate(-diff, 0, 0);

        //add pos
        aabb.translate(pos);
    }

    public Vector3f getPos(float delta) {
        return Maths.lerp(oPos, pos, delta);
    }

    public void setPos(float x, float y, float z) {
        super.setPos(x, y, z);
        this.oPos.set(x, y, z);
        this.updateAABB();
        this.updateRidersPos();
    }

    public Vector2f getRot(float delta) {
        return Maths.lerpAngle(oRot, rot, delta);
    }

    public Vector2f getRot() {
        return rot;
    }

    public void setRot(Vector2f rot) {
        this.setRot(rot.x, rot.y);
    }

    public void setRot(float pitch, float yaw) {
        this.oRot.set(this.rot.set(Maths.wrapDegrees(pitch), Maths.wrapDegrees(yaw)));
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
        return Maths.rotToDir(rot.x, rot.y);
    }

    public Vector3f getLookDir(float delta) {
        Vector2f rot = getRot(delta);
        return Maths.rotToDir(rot.x, rot.y);
    }

    @Override
    public AABB getAABB() {
        return aabb;
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
        AABB area = new AABB(aabb).expand(range);

        //return hit
        return world.raycastTerrain(area, pos, range);
    }

    public Hit<Entity> getLookingEntity(float distance) {
        //prepare positions
        Vector3f pos = getEyePos();
        Vector3f range = getLookDir().mul(distance);
        AABB area = new AABB(aabb).expand(range);

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

    public void updateRidersPos() {
        for (Entity rider : new ArrayList<>(riders))
            rider.moveTo(getRiderOffset(rider).add(this.pos));
    }

    public void updateRidersRot() {
        float xDelta = rot.x - oRot.x;
        float yDelta = rot.y - oRot.y;
        for (Entity rider : new ArrayList<>(riders))
            rider.rotateTo(rider.rot.x + xDelta, rider.rot.y + yDelta);
    }

    public Vector3f getRiderOffset(Entity rider) {
        return new Vector3f(0, aabb.getHeight(), 0);
    }

    public void addRider(Entity e) {
        e.stopRiding();
        this.riders.add(e);
        e.riding = this;
    }

    public void stopRiding() {
        if (isRiding()) {
            this.riding.riders.remove(this);
            this.riding = null;
        }
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

    public void onUse(LivingEntity source) {}

    public void onAttacked(LivingEntity source) {}

    @Override
    public abstract EntityRegistry getType();

    public void sendServerUpdate() {
        //if (!getWorld().isClientside())
        //    ServerConnection.connection.sendToAllUDP(new EntitySync().entity(this));
    }

    @Override
    public boolean shouldRender(Camera camera) {
        return camera.getPos().distanceSquared(getPos()) <= getRenderDistance() && super.shouldRender(camera);
    }

    public float getRenderDistance() {
        float f = getWorld().entityRenderDistance;
        return 1024f * f * f;
    }
}
