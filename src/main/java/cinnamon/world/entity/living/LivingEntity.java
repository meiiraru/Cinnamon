package cinnamon.world.entity.living;

import cinnamon.Client;
import cinnamon.networking.ServerConnection;
import cinnamon.networking.packet.SyncHealth;
import cinnamon.registry.LivingModelRegistry;
import cinnamon.render.MatrixStack;
import cinnamon.render.Model;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Colors;
import cinnamon.utils.Rotation;
import cinnamon.utils.TextUtils;
import cinnamon.world.DamageType;
import cinnamon.world.collisions.Hit;
import cinnamon.world.effects.Effect;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import cinnamon.world.entity.vehicle.Vehicle;
import cinnamon.world.items.Inventory;
import cinnamon.world.items.Item;
import cinnamon.world.items.ItemRenderContext;
import cinnamon.world.particle.SmokeParticle;
import cinnamon.world.particle.TextParticle;
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

    public LivingEntity(UUID uuid, Model model, int maxHealth, float eyeHeight, int inventorySize) {
        super(uuid, model);
        this.health = this.maxHealth = maxHealth;
        this.eyeHeight = eyeHeight;
        this.inventory = new Inventory(inventorySize);
    }

    public LivingEntity(UUID uuid, LivingModelRegistry entityModel, int maxHealth, int inventorySize) {
        this(uuid, entityModel.model, maxHealth, entityModel.eyeHeight, inventorySize);
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
        matrices.rotate(Rotation.Y.rotationDeg(-getRot(delta).y));
    }

    @Override
    protected void renderFeatures(MatrixStack matrices, float delta) {
        //holding item
        Item item = getHoldingItem();
        if (item == null)
            return;

        matrices.push();
        matrices.translate(aabb.getWidth() * 0.5f + 0.1f, getEyeHeight() - 0.25f, -0.25f);
        matrices.scale(0.75f);

        matrices.peek().normal().rotate(Rotation.Y.rotationDeg(-rot.y * 2f));

        item.render(ItemRenderContext.THIRD_PERSON, matrices, delta);

        matrices.pop();
    }

    @Override
    protected void renderTexts(MatrixStack matrices, float delta) {
        Client c = Client.getInstance();
        matrices.push();
        float s = 1 / 48f;

        Text text = getHeadText();

        matrices.translate(0f, aabb.getHeight() + 0.15f, 0f);
        c.camera.billboard(matrices);
        matrices.peek().pos().scale(-s);
        matrices.translate(0f, -TextUtils.getHeight(text, c.font), 0f);

        c.font.render(VertexConsumer.WORLD_FONT, matrices, 0, 0, text, Alignment.CENTER, 50);

        matrices.pop();
    }

    protected Text getHeadText() {
        return Text.of(getHealth()).withStyle(Style.EMPTY.outlined(true)).append(Text.of("\u2795").withStyle(Style.EMPTY.color(Colors.RED)));
    }

    @Override
    protected void collide(Entity entity) {
        super.collide(entity);

        if (!(this instanceof Player) && entity instanceof Vehicle v && !this.isRiding()) {
            v.addRider(this);
            return;
        }

        if (!(entity instanceof LivingEntity l) || this.isRiding() || entity.isRiding())
            return;

        //entity push
        Vector3f collision = checkEntityCollision(entity).mul(l.getPushForce());
        this.motion.add(collision.x, 0, collision.z);
    }

    @Override
    public void move(float left, float up, float forwards) {
        if (riding != null) {
            riding.move(left, up, forwards);
            return;
        }

        float l = Math.signum(left);
        float u = this.onGround && up > 0 ? getJumpStrength() : 0f;
        float f = Math.signum(forwards);

        this.move.set(l, 0, -f);

        if (move.lengthSquared() > 1)
            move.normalize();
        move.mul(getMoveSpeed());

        move.y = u;

        //move the entity in facing direction
        this.move.rotateY((float) Math.toRadians(-rot.y));
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
            SmokeParticle particle = new SmokeParticle((int) (Math.random() * 15) + 10, -1);
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
        if (!getWorld().isClientside())
            ServerConnection.connection.sendToAllUDP(new SyncHealth().entity(getUUID()).health(health));
    }

    public boolean attackAction() {
        //attack using holding item
        if (getHoldingItem() != null) {
            getHoldingItem().attack(this);
            return true;
        }

        //attack entity
        Hit<Entity> facingEntity = getLookingEntity(getPickRange());
        if (facingEntity != null) {
            facingEntity.get().onAttacked(this);
            return true;
        }

        return false;
    }

    public void stopAttacking() {
        Item i = getHoldingItem();
        if (i != null && i.isAttacking())
            i.stopAttacking(this);
    }

    public void useAction() {
        //use holding item
        Item i = getHoldingItem();
        if (i != null) {
            i.use(this);
            return;
        }

        //use entity
        Hit<Entity> facingEntity = getLookingEntity(getPickRange());
        if (facingEntity != null)
            facingEntity.get().onUse(this);
    }

    public void stopUsing() {
        Item i = getHoldingItem();
        if (i != null && i.isUsing())
            i.stopUsing(this);
    }

    public boolean giveItem(Item item) {
        return inventory.putItem(item);
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
            getHoldingItem().unselect(this);
        inventory.setSelectedIndex(index);
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
}