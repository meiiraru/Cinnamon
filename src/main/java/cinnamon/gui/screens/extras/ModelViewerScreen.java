package cinnamon.gui.screens.extras;

import cinnamon.animation.Animation;
import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.WidgetList;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.ComboBox;
import cinnamon.gui.widgets.types.Label;
import cinnamon.gui.widgets.types.ModelViewer;
import cinnamon.lang.LangManager;
import cinnamon.model.ModelManager;
import cinnamon.registry.*;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Resource;

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
        modelViewer.setSkybox(SkyBoxRegistry.CLOUDS);
        modelViewer.setRenderSkybox(true);
    }

    @Override
    public void init() {
        //create model list
        WidgetList models = new WidgetList(4, 4, listWidth - 4, height - 8, 4);
        models.setAlignment(Alignment.TOP_LEFT);

        //button common function
        BiFunction<Resource, String, Button> createButton = (model, name) ->
                new Button(0, 0, models.getWidth() - models.getScrollbarWidth() - 4, 16, Text.translated(name), b -> setModel(model, LangManager.get(name)));

        //add models
        models.addWidget(new Label(0, 0, Text.translated("entity")));
        for (EntityModelRegistry value : EntityModelRegistry.values())
            models.addWidget(createButton.apply(value.resource, "entity." + value.name().toLowerCase()));

        models.addWidget(new Label(0, 0, Text.translated("living_entity")));
        for (LivingModelRegistry value : LivingModelRegistry.values())
            models.addWidget(createButton.apply(value.resource, "living_entity." + value.name().toLowerCase()));

        models.addWidget(new Label(0, 0, Text.translated("terrain")));
        for (TerrainModelRegistry value : TerrainModelRegistry.values())
            models.addWidget(createButton.apply(value.resource, "terrain." + value.name().toLowerCase()));

        models.addWidget(new Label(0, 0, Text.translated("terrain_entity")));
        for (TerrainEntityRegistry value : TerrainEntityRegistry.values())
            models.addWidget(createButton.apply(value.resource, "terrain_entity." + value.name().toLowerCase()));

        models.addWidget(new Label(0, 0, Text.translated("item")));
        for (ItemModelRegistry value : ItemModelRegistry.values())
            models.addWidget(createButton.apply(value.resource, "item." + value.name().toLowerCase()));

        //add widget to the list
        addWidget(models);

        //add material list
        ComboBox materials = new ComboBox(width - animationList.getWidth() - 4, 4, animationList.getWidth(), animationList.getHeight());
        for (MaterialRegistry value : MaterialRegistry.values())
            materials.addEntry(Text.translated("material." + value.name().toLowerCase()), null, b -> modelViewer.setMaterial(value));
        materials.setTooltip(Text.translated("material"));
        materials.setSelected(modelViewer.getMaterial().ordinal());
        addWidget(materials);

        //prepare animations list
        animationList.setPos(materials.getX(), materials.getY() + materials.getHeight() + 4);
        animationList.setTooltip(Text.translated("animation"));
        addWidget(animationList);

        //skyboxes
        ComboBox skyboxes = new ComboBox(materials.getX(), animationList.getY() + animationList.getHeight() + 4, animationList.getWidth(), animationList.getHeight());
        for (SkyBoxRegistry value : SkyBoxRegistry.values())
            skyboxes.addEntry(Text.translated("skybox." + value.name().toLowerCase()), null, b -> modelViewer.setSkybox(value));
        skyboxes.setTooltip(Text.translated("skybox"));
        skyboxes.setSelected(modelViewer.getSkybox().ordinal());
        addWidget(skyboxes);

        //add model viewer
        modelViewer.setPos(listWidth + 4, 4);
        modelViewer.setDimensions(width - listWidth - 8, height - 8);

        super.init();

        //add last of the last
        addWidget(modelViewer);

        //set initial model
        if (!modelViewer.hasModel())
            setModel(LivingModelRegistry.STRAWBERRY.resource, LangManager.get("living_entity." + LivingModelRegistry.STRAWBERRY.name().toLowerCase()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        //render title
        Text.of(modelName).withStyle(Style.EMPTY.outlined(true)).render(VertexConsumer.FONT, matrices, (width - listWidth) / 2f + listWidth, 4, Alignment.TOP_CENTER);
    }

    private void setModel(Resource model, String name) {
        modelViewer.setModel(ModelManager.load(model));
        modelName = name;

        animationList.clearEntries();
        List<String> animations = modelViewer.getAnimations();
        if (!animations.isEmpty()) {
            animationList.addEntry(Text.translated("gui.none"), null, b -> modelViewer.stopAllAnimations());
            for (String animation : animations)
                animationList.addEntry(Text.of(animation), null, b -> {
                    modelViewer.stopAllAnimations();
                    modelViewer.getAnimation(animation).setLoop(Animation.Loop.LOOP).play();
                });
        } else {
            animationList.addEntry(Text.translated("gui.none"), null, null);
        }
        animationList.setSelected(0);
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
