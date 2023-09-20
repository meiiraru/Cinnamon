package mayo.world.entity;

import mayo.Client;
import mayo.model.obj.Mesh;
import mayo.render.MatrixStack;
import mayo.render.batch.VertexConsumer;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.Colors;
import mayo.utils.TextUtils;
import mayo.world.World;
import mayo.world.items.Item;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public abstract class LivingEntity extends Entity {

    private Item holdingItem;
    private int health;
    private int maxHealth;

    public LivingEntity(Mesh model, World world, Vector3f dimensions, int maxHealth) {
        super(model, world, dimensions);
        this.health = this.maxHealth = maxHealth;
    }

    @Override
    protected void renderTexts(MatrixStack matrices, float delta) {
        Client c = Client.getInstance();
        matrices.push();
        float s = 1/48f;

        Text text = Text.of(getHealth() + " ").withStyle(Style.EMPTY.outlined(true)).append(Text.of("\u2795").withStyle(Style.EMPTY.color(Colors.RED)));

        Matrix4f mat = matrices.peek();
        mat.translate(0f, getDimensions().y + 0.15f, 0f);
        c.camera.billboard(mat);
        mat.scale(-s, -s, s);
        mat.translate(0f, -c.font.height(text), 0f);

        c.font.render(VertexConsumer.FONT, mat, 0, 0, text, TextUtils.Alignment.CENTER, 10);

        matrices.pop();
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

    public void setHealth(int health) {
        this.health = health;
    }
}
