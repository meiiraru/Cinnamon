package mayo.world.entity.living;

import mayo.Client;
import mayo.model.LivingEntityModels;
import mayo.render.MatrixStack;
import mayo.render.Model;
import mayo.render.batch.VertexConsumer;
import mayo.render.shader.Shader;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.Colors;
import mayo.utils.Meth;
import mayo.utils.Rotation;
import mayo.utils.TextUtils;
import mayo.world.World;
import mayo.world.entity.Entity;
import mayo.world.items.Item;
import mayo.world.particle.TextParticle;
import org.joml.Vector3f;

public abstract class LivingEntity extends Entity {

    private final float eyeHeight;

    private Item holdingItem;
    private int health;
    private int maxHealth;

    public LivingEntity(Model model, World world, int maxHealth, float eyeHeight) {
        super(model, world);
        this.health = this.maxHealth = maxHealth;
        this.eyeHeight = eyeHeight;
    }

    public LivingEntity(LivingEntityModels entityModel, World world, int maxHealth) {
        this(entityModel.model, world, maxHealth, entityModel.eyeHeight);
    }

    @Override
    public void tick() {
        super.tick();

        if (getHoldingItem() != null)
            getHoldingItem().tick();
    }

    @Override
    protected void renderModel(MatrixStack matrices, float delta) {
        matrices.push();

        //apply rot
        matrices.rotate(Rotation.Y.rotationDeg(-Meth.lerp(oRot.y, rot.y, delta)));

        //render model
        Shader.activeShader.setMatrixStack(matrices);
        model.render();

        matrices.pop();
    }

    @Override
    protected void renderTexts(MatrixStack matrices, float delta) {
        Client c = Client.getInstance();
        matrices.push();
        float s = 1/48f;

        Text text = Text.of(getHealth() + " ").withStyle(Style.EMPTY.outlined(true)).append(Text.of("\u2795").withStyle(Style.EMPTY.color(Colors.RED)));

        matrices.translate(0f, getDimensions().y + 0.15f, 0f);
        c.camera.billboard(matrices);
        matrices.scale(-s);
        matrices.translate(0f, -TextUtils.getHeight(text, c.font), 0f);

        c.font.render(VertexConsumer.FONT_WORLD, matrices, 0, 0, text, TextUtils.Alignment.CENTER, 50);

        matrices.pop();
    }

    @Override
    public void move(float left, float up, float forwards) {
        Vector3f move = new Vector3f(left, up, -forwards);
        if (move.lengthSquared() > 1f)
            move.normalize();

        move.rotateY((float) Math.toRadians(-rot.y));

        this.moveTo(
                pos.x + move.x,
                pos.y + move.y,
                pos.z + move.z
        );
    }

    @Override
    protected void collide(Entity entity) {
        super.collide(entity);
        //todo - push back both entities based on the side they are colliding
    }

    public boolean heal(int amount) {
        //cannot heal when full life
        if (this.health == this.maxHealth)
            return false;

        //heal
        this.health = Math.min(this.health + amount, this.maxHealth);

        //spawn particle
        spawnHealthChangeParticle(amount, false);

        //return that we did heal
        return true;
    }

    public void damage(int amount, boolean crit) {
        //apply critical
        amount *= crit ? 2 : 1;

        //damage
        this.health -= amount;

        //on kill, clamp health to 0, and flag it as removed
        if (health <= 0) {
            this.health = 0;
            this.removed = true;
        }

        //spawn particle
        spawnHealthChangeParticle(-amount, crit);
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
        world.addParticle(new TextParticle(Text.of( text).withStyle(Style.EMPTY.color(color).outlined(true)), 20, getAABB().getRandomPoint()));
    }

    public void attack() {
        //todo - get entity in front and attack it

        //attack using holding item
        Item item = getHoldingItem();
        if (item != null && item.hasAttack())
            item.attack(this);
    }

    public void use() {
        //todo - try to use current object
        //todo - try to use facing entity

        //use holding item
        if (getHoldingItem() != null)
            getHoldingItem().use(this);
    }

    @Override
    public float getEyeHeight() {
        return this.eyeHeight;
    }

    public void setHoldingItem(Item holdingItem) {
        this.holdingItem = holdingItem;
    }

    public Item getHoldingItem() {
        return holdingItem;
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
}
