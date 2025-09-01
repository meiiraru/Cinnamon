package cinnamon.world.entity.living;

import cinnamon.Client;
import cinnamon.registry.LivingModelRegistry;
import cinnamon.render.MatrixStack;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.AABB;
import cinnamon.utils.Colors;
import cinnamon.utils.Resource;
import cinnamon.utils.Rotation;
import cinnamon.world.DamageType;
import cinnamon.world.WorldObject;
import cinnamon.world.collisions.CollisionResult;
import cinnamon.world.collisions.Hit;
import cinnamon.world.effects.Effect;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import cinnamon.world.items.Inventory;
import cinnamon.world.items.Item;
import cinnamon.world.items.ItemRenderContext;
import cinnamon.world.particle.SmokeParticle;
import cinnamon.world.particle.TextParticle;
import cinnamon.world.terrain.Terrain;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class LivingEntity extends PhysEntity {

    private final Map<Effect.Type, Effect> activeEffects = new HashMap<>();
    private final float eyeHeight;
    private final Inventory inventory;

    private int health;
    private int maxHealth;

    public LivingEntity(UUID uuid, Resource model, int maxHealth, float eyeHeight, int inventorySize) {
        super(uuid, model);
        this.health = this.maxHealth = maxHealth;
        this.eyeHeight = eyeHeight;
        this.inventory = new Inventory(inventorySize);
    }

    public LivingEntity(UUID uuid, LivingModelRegistry entityModel, int maxHealth, int inventorySize) {
        this(uuid, entityModel.resource, maxHealth, entityModel.eyeHeight, inventorySize);
    }

    @Override
    public void tick() {
        super.tick();

        inventory.tick();

        for (Effect effect : activeEffects.values())
            effect.tick();
        activeEffects.entrySet().removeIf(entry -> entry.getValue().isDone());

        if (getHoldingItem() != null)
            getHoldingItem().tick();

        Effect heal = getEffect(Effect.Type.HEAL);
        if (heal != null && (int) (Client.getInstance().ticks % (20f / heal.getAmplitude())) == 0)
            heal(1);
    }

    @Override
    public float getPickRange() {
        return 5.5f;
    }

    @Override
    protected void applyModelPose(MatrixStack matrices, float delta) {
        if (this.riding != null) {
            Vector2f rot = riding.getRot(delta);
            matrices.rotate(Rotation.Y.rotationDeg(-rot.y));
            matrices.rotate(Rotation.X.rotationDeg(-rot.x));
            matrices.rotate(Rotation.Y.rotationDeg(rot.y));
        }
        matrices.rotate(Rotation.Y.rotationDeg(-getRot(delta).y));
    }

    @Override
    protected void renderFeatures(MatrixStack matrices, float delta) {
        super.renderFeatures(matrices, delta);
        renderHandItem(ItemRenderContext.THIRD_PERSON, matrices, delta);
    }

    public void renderHandItem(ItemRenderContext context, MatrixStack matrices, float delta) {
        Item item = getHoldingItem();
        if (item == null)
            return;

        matrices.pushMatrix();
        matrices.translate(getHandPos(false, delta));
        matrices.rotate(getHandRot(false, delta));

        item.render(context, matrices, delta);

        matrices.popMatrix();
    }

    @Override
    public boolean shouldRenderOutline() {
        return activeEffects.containsKey(Effect.Type.GLOWING) || super.shouldRenderOutline();
    }

    @Override
    protected Text getHeadText() {
        Text sup = super.getHeadText();
        Text health = Text.of(getHealth()).append(Text.of(" \u2764").withStyle(Style.EMPTY.color(Colors.RED)));
        return sup == null ? health : sup.append(Text.of("\n").append(health));
    }

    @Override
    protected void collide(Entity entity, CollisionResult result, Vector3f toMove) {
        super.collide(entity, result, toMove);

        if (!(entity instanceof LivingEntity l) || this.isRiding() || entity.isRiding())
            return;

        //entity push
        Vector3f collision = checkEntityCollision(entity).mul(l.getPushForce());
        this.motion.add(collision.x, 0, collision.z);
    }

    @Override
    public void impulse(float left, float up, float forwards) {
        if (riding != null) {
            riding.impulse(left, up, forwards);
            return;
        }

        float l = Math.signum(left);
        float u = this.onGround && up > 0 ? getJumpStrength() : 0f;
        float f = Math.signum(forwards);

        this.impulse.set(l, 0, -f);

        if (impulse.lengthSquared() > 1)
            impulse.normalize();
        impulse.mul(getMoveSpeed());

        impulse.y = u;

        //move the entity in facing direction
        this.impulse.rotateY((float) Math.toRadians(-rot.y));
    }

    protected float getJumpStrength() {
        return 0.4f;
    }

    public boolean heal(int amount) {
        //cannot heal when full life
        if (this.health == this.maxHealth)
            return false;

        //heal
        this.health = Math.min(this.health + amount, this.maxHealth);

        //spawn particle
        spawnHealthChangeParticle(amount, false);

        //sync health
        syncHealth();

        //return that we did heal
        return true;
    }

    @Override
    public boolean damage(Entity source, DamageType type, int amount, boolean crit) {
        if (type == DamageType.EXPLOSION && this.hasEffect(Effect.Type.EXPLOSION_IMMUNITY))
            return false;

        //apply critical
        amount = (int) (amount * (crit ? 1.5f : 1f));

        //damage
        this.health -= amount;

        //on kill, clamp health to 0, and flag it as removed
        if (health <= 0) {
            this.health = 0;
            this.onDeath();
        }

        //spawn particle
        spawnHealthChangeParticle(-amount, crit);

        //sync health
        syncHealth();

        return true;
    }

    protected void onDeath() {
        this.remove();
        this.spawnDeathParticles();
    }

    protected void spawnDeathParticles() {
        for (int i = 0; i < 20; i++) {
            SmokeParticle particle = new SmokeParticle((int) (Math.random() * 15) + 10, 0xFFFFFFFF);
            particle.setPos(aabb.getRandomPoint());
            world.addParticle(particle);
        }
    }

    protected void spawnHealthChangeParticle(int amount, boolean crit) {
        Colors color;
        String text = "";

        //healing
        if (amount > 0) {
            text += "+"; //add signal
            color = Colors.GREEN;
        }
        //no change
        else if (amount == 0)
            color = Colors.LIGHT_GRAY;
            //damage
        else
            color = Colors.RED;

        //add health difference
        text += amount;

        //add crit text
        if (crit)
            text += " \u2728";

        //spawn particle
        TextParticle p = new TextParticle(Text.of(text).withStyle(Style.EMPTY.color(color).outlined(true)), 20, aabb.getRandomPoint());
        p.setEmissive(true);
        world.addParticle(p);
    }

    private void syncHealth() {
        //if (!getWorld().isClientside())
        //    ServerConnection.connection.sendToAllUDP(new SyncHealth().entity(getUUID()).health(health));
    }

    public boolean attackAction() {
        //attack using holding item
        if (getHoldingItem() != null && getHoldingItem().fire())
            return true;

        //attack entity
        Hit<? extends WorldObject> facingEntity = getLookingObject(getPickRange());
        return facingEntity != null && facingEntity.get() instanceof Entity e && e.onAttacked(this);
    }

    public void stopAttacking() {
        Item i = getHoldingItem();
        if (i != null)
            i.stopFiring();
    }

    public boolean useAction() {
        //use holding item
        Item i = getHoldingItem();
        if (i != null && i.use())
            return true;

        //use entity
        Hit<? extends WorldObject> facingEntity = getLookingObject(getPickRange());
        return facingEntity != null && facingEntity.get() instanceof Entity e && e.onUse(this);
    }

    public void stopUsing() {
        Item i = getHoldingItem();
        if (i != null)
            i.stopUsing();
    }

    public boolean giveItem(Item item) {
        int i = inventory.putItem(item);
        if (i == inventory.getSelectedIndex()) 
            item.select(this);
        return i >= 0;
    }

    @Override
    public float getEyeHeight() {
        return this.eyeHeight;
    }

    @Override
    protected float getMoveSpeed() {
        float speed = super.getMoveSpeed();
        if (hasEffect(Effect.Type.SPEED))
            speed += 0.03f * (getEffect(Effect.Type.SPEED).getAmplitude() + 1);
        return speed;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setSelectedItem(int index) {
        if (index != inventory.getSelectedIndex() && getHoldingItem() != null)
            getHoldingItem().unselect();

        inventory.setSelectedIndex(index);
        if (getHoldingItem() != null)
            getHoldingItem().select(this);
    }

    public Item getHoldingItem() {
        return inventory.getSelectedItem();
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getHealth() {
        return health;
    }

    public float getHealthProgress() {
        return (float) getHealth() / getMaxHealth();
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public boolean isDead() {
        return getHealth() <= 0;
    }

    public void giveEffect(Effect effect) {
        Effect old = activeEffects.get(effect.getType());
        //add effect if there was not an effect with the same id
        if (old == null) {
            activeEffects.put(effect.getType(), effect);
            return;
        }

        //if the new amplitude is greater, override old effect
        if (effect.getAmplitude() > old.getAmplitude()) {
            activeEffects.put(effect.getType(), effect);
            return;
        }

        //if amplitude is below, do nothing
        if (effect.getAmplitude() < old.getAmplitude())
            return;

        //amplitude is now guaranteed to be equals

        //if duration is greater, override old effect
        if (effect.getRemainingTime() > old.getRemainingTime())
            activeEffects.put(effect.getType(), effect);

        //at this point the duration is either below or equals, so nothing needs to be changed
    }

    public boolean hasEffect(Effect.Type type) {
        return activeEffects.containsKey(type);
    }

    public Effect getEffect(Effect.Type type) {
        return activeEffects.get(type);
    }

    public Collection<Effect> getActiveEffects() {
        return activeEffects.values();
    }

    @Override
    protected void checkWorldVoid() {
        if (getWorld() != null && getPos().y < getWorld().bottomOfTheWorld)
            damage(null, DamageType.GOD, Integer.MAX_VALUE, false);
    }

    public Vector3f getHandPos(boolean left, float delta) {
        Vector3f pos = getEyePos(delta);
        float x = aabb.getWidth() * 0.5f - 0.125f;

        Vector3f offset = new Vector3f(left ? -x : x, -0.25f, -0.5f);
        offset.rotate(getHandRot(left, delta));
        pos.add(offset);

        return pos;
    }

    public Quaternionf getHandRot(boolean left, float delta) {
        Vector2f rot = getRot(delta);

        float yaw = 1;
        Quaternionf offset = new Quaternionf()
                .rotateY((float) Math.toRadians(left ? -yaw : yaw));

        return new Quaternionf()
                .rotateY((float) Math.toRadians(-rot.y))
                .rotateX((float) Math.toRadians(-rot.x))
                .mul(offset);
    }

    public Vector3f getHandDir(boolean left, float delta) {
        return new Vector3f(0, 0, -1).rotate(getHandRot(left, delta));
    }

    public Hit<Terrain> raycastHandTerrain(boolean left, float delta, float distance) {
        //prepare positions
        Vector3f pos = getHandPos(left, delta);
        Vector3f range = getHandDir(left, delta).mul(distance);
        AABB area = new AABB(pos).expand(range);

        //return hit
        return world.raycastTerrain(area, pos, range);
    }

    public Hit<Entity> raycastHandEntity(boolean left, float delta, float distance) {
        //prepare positions
        Vector3f pos = getHandPos(left, delta);
        Vector3f range = getHandDir(left, delta).mul(distance);
        AABB area = new AABB(pos).expand(range);

        //return hit
        return world.raycastEntity(area, pos, range, e -> e != this && e != this.riding && e.isTargetable());
    }

    public Hit<? extends WorldObject> raycastHand(boolean left, float delta,float distance) {
        Hit<Entity> entityHit = raycastHandEntity(left, delta, distance);
        Hit<Terrain> terrainHit = raycastHandTerrain(left, delta, distance);

        if (entityHit != null && (terrainHit == null || entityHit.collision().near() < terrainHit.collision().near()))
            return entityHit;

        return terrainHit;
    }
}
