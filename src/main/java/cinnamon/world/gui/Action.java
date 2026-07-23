package cinnamon.world.gui;

import cinnamon.math.Rotation;
import cinnamon.math.collision.OBB;
import cinnamon.model.GeometryHelper;
import cinnamon.model.ModelManager;
import cinnamon.model.material.Material;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.model.ModelRenderer;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Resource;
import cinnamon.utils.TextUtils;
import cinnamon.utils.UIHelper;
import org.joml.Vector3f;

public class Action {

    public static final float ICON_SIZE = 8f;

    private final Runnable action;

    private int backgroundColor = 0x88000000;
    private int selectedColor = 0x88FFFFFF;

    private Text title, description;
    private Text formattedTitle, formattedDesc;
    private Resource icon, model;
    private ModelRenderer modelRenderer;
    private Material modelMaterial;

    private boolean hovered = false;

    public Action(Text title, Runnable action) {
        this.action = action;
        this.setTitle(title);
    }

    public void render(MatrixStack matrices, float delta) {
        if (formattedTitle != null)
            renderTitle(matrices, delta);

        if (modelRenderer == null || icon != null)
            renderIcon(matrices, delta);

        if (modelRenderer != null)
            renderModel(matrices, delta);
    }

    protected void renderTitle(MatrixStack matrices, float delta) {
        float ty = TextUtils.getHeight(formattedTitle);
        matrices.translate(0f, -ty / 2f, 0f);
        matrices.pushMatrix();
        matrices.translate(0f, 0f, ICON_SIZE * 4f);
        formattedTitle.render(VertexConsumer.MAIN, matrices, 0f, ICON_SIZE + 2f, Alignment.TOP_CENTER);
        matrices.popMatrix();
    }

    protected void renderIcon(MatrixStack matrices, float delta) {
        VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, -ICON_SIZE, -ICON_SIZE, ICON_SIZE, ICON_SIZE, 0xFFFFFFFF), icon);
    }

    protected void renderModel(MatrixStack matrices, float delta) {
        OBB bb = new OBB(modelRenderer.getAABB())
                .rotate(Rotation.X.rotationDeg(22.5f))
                .rotate(Rotation.Y.rotationDeg(135f));

        Vector3f center = bb.getCenter();
        Vector3f dims = bb.getDimensions();

        float maxDim = Math.max(Math.abs(dims.x), Math.max(Math.abs(dims.y), Math.abs(dims.z)));
        float scale = ICON_SIZE * 2f / maxDim;

        matrices.pushMatrix()
                .translate(0f, 0, scale)
                .scale(scale, -scale, scale)
                .rotate(Rotation.X.rotationDeg(22.5f))
                .rotate(Rotation.Y.rotationDeg(135f))
                .translate(-center.x, -center.y, -center.z);

        UIHelper.finishBatches();
        modelRenderer.render(matrices, modelMaterial);

        matrices.popMatrix();
    }

    public void run() {
        action.run();
    }

    public Action setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public Action setSelectedColor(int selectedColor) {
        this.selectedColor = selectedColor;
        return this;
    }

    public int getSelectedColor() {
        return selectedColor;
    }

    public int getColor() {
        return hovered ? selectedColor : backgroundColor;
    }

    public Action setHovered(boolean hovered) {
        this.hovered = hovered;
        return this;
    }

    public boolean isHovered() {
        return hovered;
    }

    public Action setTitle(Text title) {
        this.title = title;
        this.formattedTitle = title == null ? null : TextUtils.join(TextUtils.warpToWidth(Text.empty().withStyle(ActionWheel.STYLE).append(title), ActionWheel.INNER_RADIUS * 2));
        return this;
    }

    public Text getTitle() {
        return title;
    }

    public Text getFormattedTitle() {
        return formattedTitle;
    }

    public Action setDescription(Text description) {
        this.description = description;
        this.formattedDesc = description == null ? null : TextUtils.join(TextUtils.warpToWidth(description, 150));
        return this;
    }

    public Text getDescription() {
        return description;
    }

    public Text getFormattedDescription() {
        return formattedDesc;
    }

    public Action setIcon(Resource icon) {
        this.icon = icon;
        return this;
    }

    public Resource getIcon() {
        return icon;
    }

    public Action setModel(Resource model) {
        return setModel(model, null);
    }

    public Action setModel(Resource model, Material material) {
        this.model = model;
        this.modelRenderer = ModelManager.load(model);
        this.modelMaterial = material;
        return this;
    }

    public Resource getModel() {
        return model;
    }
}
