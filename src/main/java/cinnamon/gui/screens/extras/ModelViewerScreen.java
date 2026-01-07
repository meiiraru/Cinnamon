package cinnamon.gui.screens.extras;

import cinnamon.animation.Animation;
import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.Toast;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.WidgetList;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.Checkbox;
import cinnamon.gui.widgets.types.ComboBox;
import cinnamon.gui.widgets.types.Label;
import cinnamon.gui.widgets.types.ModelViewer;
import cinnamon.lang.LangManager;
import cinnamon.model.ModelManager;
import cinnamon.registry.*;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.model.ModelRenderer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Resource;
import org.joml.Math;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class ModelViewerScreen extends ParentedScreen {

    private static final int listWidth = 144;

    //current opened model
    private String modelName = "";
    private final ComboBox animationList = new ComboBox(0, 0, 60, 14);
    private final ModelViewer modelViewer = new ModelViewer(0, 0, 1, 1);

    private boolean autoRotate = true;

    public ModelViewerScreen(Screen parentScreen) {
        super(parentScreen);
        modelViewer.setSkybox(SkyBoxRegistry.CLOUDS);
        modelViewer.setRenderSkybox(true);
    }

    @Override
    public void init() {
        //add model viewer first
        modelViewer.setPos(listWidth + 4, 4);
        modelViewer.setDimensions(width - listWidth - 8, height - 8);
        addWidget(modelViewer);

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

        ContainerGrid properties = new ContainerGrid(0, 0, 4);
        properties.setAlignment(Alignment.TOP_RIGHT);
        properties.setPos(width - 4, 4);
        addWidget(properties);

        //add material list
        ComboBox materials = new ComboBox(width - animationList.getWidth() - 4, 4, animationList.getWidth(), animationList.getHeight());
        for (MaterialRegistry value : MaterialRegistry.values())
            materials.addEntry(Text.translated("material." + value.name().toLowerCase()), null, b -> modelViewer.setMaterial(value));
        materials.setTooltip(Text.translated("material"));
        materials.setSelected(modelViewer.getMaterial().ordinal());
        properties.addWidget(materials);

        //prepare animations list
        animationList.setPos(materials.getX(), materials.getY() + materials.getHeight() + 4);
        animationList.setTooltip(Text.translated("animation"));
        properties.addWidget(animationList);

        //skyboxes
        ComboBox skyboxes = new ComboBox(materials.getX(), animationList.getY() + animationList.getHeight() + 4, animationList.getWidth(), animationList.getHeight());
        for (SkyBoxRegistry value : SkyBoxRegistry.values())
            skyboxes.addEntry(Text.translated("skybox." + value.name().toLowerCase()), null, b -> modelViewer.setSkybox(value));
        skyboxes.setTooltip(Text.translated("skybox"));
        skyboxes.setSelected(modelViewer.getSkybox().ordinal());
        properties.addWidget(skyboxes);

        //toggle skybox
        Checkbox toggleSkybox = new Checkbox(skyboxes.getX(), skyboxes.getY() + skyboxes.getHeight() + 4, Text.translated("gui.model_viewer_screen.toggle_skybox"));
        toggleSkybox.setToggled(modelViewer.shouldRenderSkybox());
        toggleSkybox.setAction(b -> modelViewer.setRenderSkybox(((Checkbox) b).isToggled()));
        toggleSkybox.setRightToLeft(true);
        properties.addWidget(toggleSkybox);

        //toggle wireframe
        Checkbox toggleWireframe = new Checkbox(skyboxes.getX(), toggleSkybox.getY() + toggleSkybox.getHeight() + 4, Text.translated("gui.model_viewer_screen.toggle_wireframe"));
        toggleWireframe.setToggled(modelViewer.shouldRenderWireframe());
        toggleWireframe.setAction(b -> modelViewer.setRenderWireframe(((Checkbox) b).isToggled()));
        toggleWireframe.setRightToLeft(true);
        properties.addWidget(toggleWireframe);

        //toggle bounds
        Checkbox toggleBounds = new Checkbox(skyboxes.getX(), toggleWireframe.getY() + toggleWireframe.getHeight() + 4, Text.translated("gui.model_viewer_screen.toggle_bounds"));
        toggleBounds.setToggled(modelViewer.shouldRenderBounds());
        toggleBounds.setAction(b -> modelViewer.setRenderBounds(((Checkbox) b).isToggled()));
        toggleBounds.setRightToLeft(true);
        properties.addWidget(toggleBounds);

        //auto rotate
        Checkbox autoRotate = new Checkbox(skyboxes.getX(), toggleBounds.getY() + toggleBounds.getHeight() + 4, Text.translated("gui.model_viewer_screen.auto_rotate"));
        autoRotate.setToggled(this.autoRotate);
        autoRotate.setAction(b -> this.autoRotate = ((Checkbox) b).isToggled());
        autoRotate.setRightToLeft(true);
        properties.addWidget(autoRotate);

        super.init();

        //set initial model
        if (!modelViewer.hasModel())
            setModel(LivingModelRegistry.STRAWBERRY.resource, LangManager.get("living_entity." + LivingModelRegistry.STRAWBERRY.name().toLowerCase()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        //render title
        Text.of(modelName).withStyle(Style.EMPTY.outlined(true)).render(VertexConsumer.MAIN, matrices, (width - listWidth) / 2f + listWidth, 4, Alignment.TOP_CENTER);

        //auto rotate
        if (autoRotate && modelViewer.getDragged() != 0)
            modelViewer.setRotY(modelViewer.getRotY() + client.timer.deltaTime * 15f);
    }

    private boolean setModel(Resource model, String name) {
        ModelRenderer renderer = ModelManager.load(model);
        if (renderer == null) {
            Toast.addToast(Text.translated("gui.model_viewer_screen.load_error")).type(Toast.ToastType.ERROR);
            return false;
        }

        modelViewer.setModel(renderer);
        modelName = name;

        animationList.clearEntries();
        List<String> animations = modelViewer.getAnimations();
        if (!animations.isEmpty()) {
            List<Text> animationTexts = new ArrayList<>();
            Style defaultStyle = Style.EMPTY.color(animationList.getStyle().getInt("text_color"));
            Style selectedStyle = Style.EMPTY.color(animationList.getStyle().getInt("accent_color"));
            modelViewer.stopAllAnimations();
            animationList.addEntry(Text.translated("gui.none"), null, b -> {
                modelViewer.stopAllAnimations();
                for (Text text : animationTexts)
                    text.withStyle(defaultStyle);
            });
            for (String animation : animations) {
                Text text = Text.of(animation);
                animationTexts.add(text);
                animationList.addEntry(text, null, b -> {
                    Animation anim = modelViewer.getAnimation(animation);
                    if (anim.isPlaying()) {
                        anim.stop();
                        text.withStyle(defaultStyle);
                    } else {
                        anim.setLoop(Animation.Loop.LOOP).play();
                        text.withStyle(selectedStyle);
                    }
                });
            }
        } else {
            animationList.addEntry(Text.translated("gui.none"), null, null);
        }
        animationList.select(Math.min(1, animations.size()));
        return true;
    }

    public boolean filesDropped(String[] files) {
        if (files.length == 0)
            return false;

        String file = files[0].replaceAll("\\\\", "/");
        return setModel(new Resource("", file), file);
    }
}
