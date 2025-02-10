package cinnamon.gui.screens.extras;

import cinnamon.animation.Animation;
import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.WidgetList;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.ComboBox;
import cinnamon.gui.widgets.types.Label;
import cinnamon.gui.widgets.types.ModelViewer;
import cinnamon.registry.*;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Resource;
import cinnamon.world.SkyBox;

import java.util.List;
import java.util.function.BiFunction;

import static cinnamon.Client.LOGGER;

public class ModelViewerScreen extends ParentedScreen {

    private static final int listWidth = 144;

    //current opened model
    private String modelName = "";
    private final ComboBox animationList = new ComboBox(0, 0, 60, 14);
    private final ModelViewer modelViewer = new ModelViewer(0, 0, 1, 1);

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
            materials.addEntry(Text.of(value.name()), null, b -> modelViewer.setMaterial(value));
        materials.setTooltip(Text.of("Materials"));
        materials.select(modelViewer.getMaterial().ordinal());
        addWidget(materials);

        //prepare animations list
        animationList.setPos(materials.getX(), materials.getY() + materials.getHeight() + 4);
        animationList.setTooltip(Text.of("Animations"));
        addWidget(animationList);

        //skyboxes
        ComboBox skyboxes = new ComboBox(materials.getX(), animationList.getY() + animationList.getHeight() + 4, animationList.getWidth(), animationList.getHeight());
        for (SkyBox.Type value : SkyBox.Type.values())
            skyboxes.addEntry(Text.of(value.name()), null, b -> modelViewer.setSkybox(value));
        skyboxes.setTooltip(Text.of("Skybox"));
        skyboxes.select(modelViewer.getSkybox().ordinal());
        addWidget(skyboxes);

        //add model viewer
        modelViewer.setPos(listWidth + 4, 4);
        modelViewer.setDimensions(width - listWidth - 8, height - 8);

        super.init();

        //set initial model
        if (!modelViewer.hasModel())
            setModel(LivingModelRegistry.STRAWBERRY.resource, LivingModelRegistry.STRAWBERRY.name());
    }

    @Override
    protected void addBackButton() {
        super.addBackButton();
        //add last of the last
        addWidget(modelViewer);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        //render title
        font.render(VertexConsumer.FONT, matrices, (width - listWidth) / 2f + listWidth, 4, Text.of(modelName).withStyle(Style.EMPTY.outlined(true)), Alignment.CENTER);
    }

    private void setModel(Resource model, String name) {
        modelViewer.setModel(model);
        modelName = name;

        animationList.clearEntries();
        List<String> animations = modelViewer.getAnimations();
        if (!animations.isEmpty()) {
            animationList.addEntry(Text.of("None"), null, b -> modelViewer.stopAllAnimations());
            for (String animation : animations)
                animationList.addEntry(Text.of(animation), null, b -> {
                    modelViewer.stopAllAnimations();
                    modelViewer.getAnimation(animation).setLoop(Animation.Loop.LOOP).play();
                });
        } else {
            animationList.addEntry(Text.of("None"), null, null);
        }
        animationList.select(0);
    }

    public boolean filesDropped(String[] files) {
        if (files.length == 0)
            return false;

        try {
            String file = files[0].replaceAll("\\\\", "/");
            setModel(new Resource("", file), file);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to load model", e);
        }

        return false;
    }
}
