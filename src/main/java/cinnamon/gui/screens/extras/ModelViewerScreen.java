package cinnamon.gui.screens.extras;

import cinnamon.animation.Animation;
import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.WidgetList;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.ComboBox;
import cinnamon.gui.widgets.types.Label;
import cinnamon.model.GeometryHelper;
import cinnamon.model.ModelManager;
import cinnamon.model.material.Material;
import cinnamon.registry.*;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.model.AnimatedObjRenderer;
import cinnamon.render.model.ModelRenderer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Resource;
import cinnamon.utils.Rotation;
import cinnamon.utils.UIHelper;
import cinnamon.world.SkyBox;

import java.util.function.BiFunction;

import static org.lwjgl.glfw.GLFW.*;

public class ModelViewerScreen extends ParentedScreen {

    private static final int listWidth = 144;
    private static final float scaleFactor = 3f;

    private final Framebuffer modelBuffer = new Framebuffer(1, 1, Framebuffer.COLOR_BUFFER | Framebuffer.DEPTH_BUFFER);
    private final SkyBox skybox = new SkyBox();

    //current opened model
    private ModelRenderer currentModel = null;
    private String modelName = "none";
    private Material selectedMaterial = null;
    private final ComboBox animationList = new ComboBox(0, 0, 60, 14);

    //view transforms
    private float posX = 0, posY = 0;
    private float scale = 64;
    private float rotX = -22.5f, rotY = 30;

    //dragging
    private int dragged = -1;
    private int anchorX = 0, anchorY = 0;
    private float anchorRotX = 0, anchorRotY = 0;
    private float anchorPosX = 0, anchorPosY = 0;

    public ModelViewerScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void init() {
        //create model list
        WidgetList models = new WidgetList(4, 4, listWidth - 4, height - 8, 4);
        models.setAlignment(Alignment.LEFT);

        //button common function
        BiFunction<Resource, String, Button> createButton = (model, name) ->
                new Button(0, 0, models.getWidth() - models.getScrollbarWidth() - 4, 16, Text.of(name), b -> setModel(model, name));

        //add models
        models.addWidget(new Label(0, 0, Text.of("Entity"), font));
        for (EntityModelRegistry value : EntityModelRegistry.values())
            models.addWidget(createButton.apply(value.resource, value.name()));

        models.addWidget(new Label(0, 0, Text.of("Living Entity"), font));
        for (LivingModelRegistry value : LivingModelRegistry.values())
            models.addWidget(createButton.apply(value.resource, value.name()));

        models.addWidget(new Label(0, 0, Text.of("Terrain"), font));
        for (TerrainRegistry value : TerrainRegistry.values())
            models.addWidget(createButton.apply(value.resource, value.name()));

        models.addWidget(new Label(0, 0, Text.of("Terrain Entities"), font));
        for (TerrainEntityRegistry value : TerrainEntityRegistry.values())
            models.addWidget(createButton.apply(value.resource, value.name()));

        models.addWidget(new Label(0, 0, Text.of("Items"), font));
        for (ItemModelRegistry value : ItemModelRegistry.values())
            models.addWidget(createButton.apply(value.resource, value.name()));

        //add widget to the list
        addWidget(models);

        //add material list
        ComboBox materials = new ComboBox(width - animationList.getWidth() - 4, 4, animationList.getWidth(), animationList.getHeight());
        for (MaterialRegistry value : MaterialRegistry.values())
            materials.addEntry(Text.of(value.name()), null, b -> selectedMaterial = value.material);
        materials.setTooltip(Text.of("Materials"));
        addWidget(materials);

        //select first material
        selectedMaterial = null;
        materials.select(0);

        //prepare animations list
        animationList.setPos(materials.getX(), materials.getY() + materials.getHeight() + 4);
        animationList.setTooltip(Text.of("Animations"));
        addWidget(animationList);

        //skyboxes
        ComboBox skyboxes = new ComboBox(materials.getX(), animationList.getY() + animationList.getHeight() + 4, animationList.getWidth(), animationList.getHeight());
        for (SkyBox.Type value : SkyBox.Type.values())
            skyboxes.addEntry(Text.of(value.name()), null, b -> skybox.type = value);
        skyboxes.setTooltip(Text.of("Skybox"));
        addWidget(skyboxes);

        skybox.type = SkyBox.Type.CLOUDS;
        skyboxes.select(skybox.type.ordinal());

        super.init();

        //set initial model
        if (currentModel == null)
            setModel(LivingModelRegistry.STRAWBERRY.resource, LivingModelRegistry.STRAWBERRY.name());

        modelBuffer.resize(client.window.width, client.window.height);
    }

    @Override
    public void removed() {
        super.removed();
        modelBuffer.free();
    }

    @Override
    protected void preRender(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.preRender(matrices, mouseX, mouseY, delta);

        //render model
        renderModel(matrices);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        //render title
        font.render(VertexConsumer.FONT, matrices, (width - listWidth) / 2f + listWidth, 4, Text.of(modelName).withStyle(Style.EMPTY.outlined(true)), Alignment.CENTER);
    }

    private void renderModel(MatrixStack matrices) {
        if (currentModel == null)
            return;

        //prepare render
        Shader oldShader = Shader.activeShader;
        matrices.push();

        //position
        matrices.translate(posX, -posY, -200);

        //scale
        matrices.scale(scale, scale, scale);

        //rotate model
        matrices.rotate(Rotation.X.rotationDeg(-rotX));
        matrices.rotate(Rotation.Y.rotationDeg(-rotY - 180));

        //offset model to center
        matrices.translate(currentModel.getAABB().getCenter().mul(-1f));

        //setup shader
        Shader s = Shaders.WORLD_MODEL_PBR.getShader().use();
        s.setup(client.camera.getPerspectiveMatrix(), client.camera.getViewMatrix());
        s.setVec3("camPos", client.camera.getPos());
        s.setFloat("fogStart", 1024);
        s.setFloat("fogEnd", 2048);
        skybox.pushToShader(s, Texture.MAX_TEXTURES - 1);

        //render to another framebuffer
        modelBuffer.useClear();
        currentModel.render(matrices, selectedMaterial);
        Framebuffer.DEFAULT_FRAMEBUFFER.use();

        //cleanup
        oldShader.use();
        matrices.pop();

        //draw framebuffer result
        UIHelper.pushScissors(listWidth, 4, width - listWidth - 4, height - 8);
        VertexConsumer.GUI.consume(GeometryHelper.quad(matrices, listWidth / 2f, 0, width, height, 0, 0f, 1f, 1f, 0f), modelBuffer.getColorBuffer());
        UIHelper.popScissors();
    }

    private void setModel(Resource model, String name) {
        currentModel = ModelManager.load(model);
        modelName = name;

        animationList.clearEntries();
        if (currentModel instanceof AnimatedObjRenderer animModel) {
            animationList.addEntry(Text.of("None"), null, b -> animModel.stopAllAnimations());
            for (String animation : animModel.getAnimations())
                animationList.addEntry(Text.of(animation), null, b -> {
                    animModel.stopAllAnimations();
                    animModel.getAnimation(animation).setLoop(Animation.Loop.LOOP).play();
                });
        } else {
            animationList.addEntry(Text.of("None"), null, null);
        }
        animationList.select(0);
    }

    private void resetView() {
        posX = 0;
        posY = 0;
        scale = 92;
        rotX = -22.5f;
        rotY = 30;
    }

    @Override
    public boolean mousePress(int button, int action, int mods) {
        boolean sup = super.mousePress(button, action, mods);
        if (sup) return true;

        if (currentModel == null || action != GLFW_PRESS) {
            dragged = -1;
            return false;
        }

        dragged = button;
        anchorX = client.window.mouseX;
        anchorY = client.window.mouseY;

        switch (button) {
            case GLFW_MOUSE_BUTTON_1 -> {
                anchorRotX = rotX;
                anchorRotY = rotY;
            }
            case GLFW_MOUSE_BUTTON_2 -> {
                anchorPosX = posX;
                anchorPosY = posY;
            }
            case GLFW_MOUSE_BUTTON_3 -> resetView();
        }

        return true;
    }

    @Override
    public boolean mouseMove(int x, int y) {
        if (dragged == -1)
            return super.mouseMove(x, y);

        float dx = x - anchorX;
        float dy = y - anchorY;

        if (dragged == GLFW_MOUSE_BUTTON_1) {
            rotX = anchorRotX - dy;
            rotY = anchorRotY - dx;
        } else if (dragged == GLFW_MOUSE_BUTTON_2) {
            posX = anchorPosX + dx;
            posY = anchorPosY + dy;

            //limit to boundaries
            float limitX = (width - listWidth - 4) / 2f;
            float limitY = (height - 4) / 2f;
            posX = Math.clamp(posX, -limitX, limitX);
            posY = Math.clamp(posY, -limitY, limitY);
        }

        return true;
    }

    @Override
    public boolean scroll(double x, double y) {
        boolean sup = super.scroll(x, y);
        if (sup) return true;

        if (currentModel == null)
            return false;

        scale = Math.max(scale + (float) Math.signum(y) * scaleFactor, 8f);

        /*
        //zoom around the mouse position
        float mx = client.window.mouseX - (width - listWidth) / 2f - listWidth;
        float my = client.window.mouseY - height / 2f;
        float oldScale = scale;
        scale = Math.max(scale + (float) Math.signum(y) * scaleFactor, 16f);
        float scaleDiff = scale - oldScale;
        posX -= mx * scaleDiff / oldScale;
        posY -= my * scaleDiff / oldScale;
        */

        return true;
    }
}
