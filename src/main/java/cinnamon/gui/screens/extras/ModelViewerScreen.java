package cinnamon.gui.screens.extras;

import cinnamon.animation.Animation;
import cinnamon.animation.Bone;
import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.Toast;
import cinnamon.gui.widgets.Widget;
import cinnamon.gui.widgets.types.*;
import cinnamon.lang.LangManager;
import cinnamon.model.GeometryHelper;
import cinnamon.model.ModelManager;
import cinnamon.parsers.ObjExporter;
import cinnamon.registry.*;
import cinnamon.render.DebugRenderer;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.model.AnimatedMeshRenderer;
import cinnamon.render.model.MeshRenderer;
import cinnamon.render.model.ModelRenderer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.FileDialog;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import cinnamon.vr.XrManager;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;

import static cinnamon.Client.LOGGER;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11C.glDepthMask;
import static org.lwjgl.opengl.GL11C.glEnable;

public class ModelViewerScreen extends ParentedScreen {

    //current opened model
    private String modelName = "";
    private Resource modelRes = null;
    private final ContextMenu animationList = new ContextMenu();
    private final ModelViewer modelViewer = new ModelViewer(0, 0, 1, 1);

    private boolean
            autoRotate = true,
            renderGroundPlane = true,
            renderAnimationBones = false;

    public ModelViewerScreen(Screen parentScreen) {
        super(parentScreen);
        modelViewer.setSkybox(SkyBoxRegistry.PHOTO_STUDIO);
        modelViewer.setSkyboxColor(0xFFFFFFFF);
        modelViewer.setRenderSkybox(true);

        animationList.closeOnSelect(false);
    }

    @Override
    public void init() {
        //add model viewer first
        modelViewer.setPos(0, 16);
        modelViewer.setDimensions(width, height - 16);
        modelViewer.setDefaultScale(XrManager.isInXR() ? 100f : 1f);
        addWidget(modelViewer);

        //file menu
        ContextMenu modelList = new ContextMenu();
        ContextMenu fileMenu = new ContextMenu()
                .addAction(Text.translated("gui.open"), null, _ -> {
                    String file = FileDialog.openFile(FileDialog.Filter.MODEL_FILES);
                    if (file != null)
                        setModel(new Resource("", file), file);
                })
                .addSubMenu(Text.translated("gui.model_viewer_screen.open_vanilla"), modelList)
                .addDivider()
                .addAction(Text.translated("gui.model_viewer_screen.open_location"), null, _ -> {
                    if (modelRes != null && modelRes.getNamespace().isEmpty()) {
                        Path path = Path.of(modelRes.getPath());
                        if (path.toFile().exists())
                            IOUtils.openInExplorer(path);
                    }
                })
                .addAction(Text.translated("gui.model_viewer_screen.reload_model"), null, _ -> {
                    if (modelRes != null) {
                        ModelManager.free(modelRes);
                        setModel(modelRes, modelName);
                    }
                })
                .addDivider()
                .addAction(Text.translated("gui.model_viewer_screen.export_model"), null, _ -> {
                    //open file dialog
                    String folder = FileDialog.openFolder();
                    if (folder != null && modelViewer.getModel() instanceof MeshRenderer mesh) {
                        try {
                            String name = IOUtils.getFilenameWithoutExtension(modelName);
                            if (name.isBlank())
                                name = "model";

                            Path p = ObjExporter.export(name, mesh.getMesh(), client.matrices, Path.of(folder));
                            IOUtils.openInExplorer(p);
                            Toast.addToast(Text.translated("gui.model_viewer_screen.export_success")).type(Toast.ToastType.SUCCESS);
                        } catch (Exception e) {
                            Toast.addToast(Text.translated("gui.model_viewer_screen.export_failed")).type(Toast.ToastType.ERROR);
                            LOGGER.error("Failed to export model", e);
                        }
                    }
                })
                .addDivider()
                .addAction(Text.translated("gui.exit"), null, _ -> close());

        //model list common function
        BiConsumer<Resource, String> addModel = (model, name) ->
                modelList.addAction(Text.translated(name), null, _ -> setModel(model, LangManager.get(name)));

        //add models
        modelList.addAction(new Label(0, 0, Text.translated("entity").withStyle(Style.EMPTY.outlined(true)), Alignment.TOP_CENTER));
        modelList.addDivider();
        for (EntityModelRegistry value : EntityModelRegistry.values())
            addModel.accept(value.resource, "entity." + value.name().toLowerCase());

        modelList.addDivider(true);
        modelList.addAction(new Label(0, 0, Text.translated("living_entity").withStyle(Style.EMPTY.outlined(true)), Alignment.TOP_CENTER));
        modelList.addDivider();
        for (LivingModelRegistry value : LivingModelRegistry.values())
            addModel.accept(value.resource, "living_entity." + value.name().toLowerCase());

        modelList.addDivider(true);
        modelList.addAction(new Label(0, 0, Text.translated("terrain").withStyle(Style.EMPTY.outlined(true)), Alignment.TOP_CENTER));
        modelList.addDivider();
        for (TerrainModelRegistry value : TerrainModelRegistry.values())
            addModel.accept(value.resource, "terrain." + value.name().toLowerCase());

        modelList.addDivider(true);
        modelList.addAction(new Label(0, 0, Text.translated("sound_system_entity").withStyle(Style.EMPTY.outlined(true)), Alignment.TOP_CENTER));
        modelList.addDivider();
        for (SoundSystemEntityRegistry value : SoundSystemEntityRegistry.values())
            addModel.accept(value.resource, "sound_system_entity." + value.name().toLowerCase());

        modelList.addDivider(true);
        modelList.addAction(new Label(0, 0, Text.translated("item").withStyle(Style.EMPTY.outlined(true)), Alignment.TOP_CENTER));
        modelList.addDivider();
        for (ItemModelRegistry value : ItemModelRegistry.values())
            addModel.accept(value.resource, "item." + value.name().toLowerCase());

        modelList.addDivider(true);
        modelList.addAction(new Label(0, 0, Text.translated("misc_entity").withStyle(Style.EMPTY.outlined(true)), Alignment.TOP_CENTER));
        modelList.addDivider();
        for (MiscModelRegistry value : MiscModelRegistry.values())
            addModel.accept(value.resource, "misc_entity." + value.name().toLowerCase());

        //viewer options
        ContextMenu viewerOptions = new ContextMenu();
        viewerOptions.closeOnSelect(false);

        //toggle skybox
        Switch toggleSkybox = new Switch(0, 0, Text.translated("gui.model_viewer_screen.toggle_skybox"));
        toggleSkybox.setToggled(modelViewer.shouldRenderSkybox());
        toggleSkybox.setAction(b -> modelViewer.setRenderSkybox(((Switch) b).isToggled()));
        viewerOptions.addAction(toggleSkybox);

        //toggle wireframe
        Switch toggleWireframe = new Switch(0, 0, Text.translated("gui.model_viewer_screen.toggle_wireframe"));
        toggleWireframe.setToggled(modelViewer.shouldRenderWireframe());
        toggleWireframe.setAction(b -> modelViewer.setRenderWireframe(((Switch) b).isToggled()));
        viewerOptions.addAction(toggleWireframe);

        //toggle bounds
        Switch toggleBounds = new Switch(0, 0, Text.translated("gui.model_viewer_screen.toggle_bounds"));
        toggleBounds.setToggled(modelViewer.shouldRenderBounds());
        toggleBounds.setAction(b -> modelViewer.setRenderBounds(((Switch) b).isToggled()));
        viewerOptions.addAction(toggleBounds);

        //auto rotate
        Switch autoRotate = new Switch(0, 0, Text.translated("gui.model_viewer_screen.auto_rotate"));
        autoRotate.setToggled(this.autoRotate);
        autoRotate.setAction(b -> this.autoRotate = ((Switch) b).isToggled());
        viewerOptions.addAction(autoRotate);

        //ground plane
        Switch groundPlane = new Switch(0, 0, Text.translated("gui.model_viewer_screen.ground_plane"));
        groundPlane.setToggled(this.renderGroundPlane);
        groundPlane.setAction(b -> this.renderGroundPlane = ((Switch) b).isToggled());
        viewerOptions.addAction(groundPlane);

        //animation bones
        Switch animationBones = new Switch(0, 0, Text.translated("gui.model_viewer_screen.animation_bones"));
        animationBones.setToggled(this.renderAnimationBones);
        animationBones.setAction(b -> this.renderAnimationBones = ((Switch) b).isToggled());
        viewerOptions.addAction(animationBones);

        //toggle backface culling
        Switch backfaceCulling = new Switch(0, 0, Text.translated("gui.model_viewer_screen.backface_culling"));
        backfaceCulling.setToggled(modelViewer.shouldCullBackFaces());
        backfaceCulling.setAction(b -> modelViewer.setCullBackFaces(((Switch) b).isToggled()));
        viewerOptions.addAction(backfaceCulling);

        //toggle flycam or orbit camera
        Switch flyCam = new Switch(0, 0, Text.translated("gui.model_viewer_screen.flycam"));
        flyCam.setToggled(modelViewer.isUsingFlyCam());
        flyCam.setAction(b -> modelViewer.setFlyCam(((Switch) b).isToggled()));
        viewerOptions.addAction(flyCam);

        //materials
        ContextMenu materialMenu = new ContextMenu();
        materialMenu.closeOnSelect(false);
        for (MaterialRegistry value : MaterialRegistry.values())
            materialMenu.addAction(Text.translated("material." + value.name().toLowerCase()), null, _ -> modelViewer.setMaterial(value));

        //skybox
        ContextMenu skyboxMenu = new ContextMenu();
        skyboxMenu.closeOnSelect(false);

        //skybox color
        ColorPicker skyboxColor = new ColorPicker(0, 0, 20, 16, false);
        skyboxColor.setColor(modelViewer.getSkyboxColor());
        skyboxColor.setColorChangeListener(modelViewer::setSkyboxColor);
        skyboxColor.setTooltip(Text.translated("gui.model_viewer_screen.skybox_color"));
        skyboxMenu.addAction(skyboxColor);

        skyboxMenu.addDivider();

        for (SkyBoxRegistry value : SkyBoxRegistry.values())
            skyboxMenu.addAction(Text.translated("skybox." + value.name().toLowerCase()), null, _ -> modelViewer.setSkybox(value));

        //create menu bar
        MenuBar menuBar = new MenuBar(0, 8, width, 16, 4, 12, 0);
        menuBar.addTab(Text.translated("gui.model_viewer_screen.file_tab"), fileMenu);
        menuBar.addTab(Text.translated("gui.model_viewer_screen.viewer_tab"), viewerOptions);
        menuBar.addTab(Text.translated("material"), materialMenu);
        menuBar.addTab(Text.translated("animation"), animationList);
        menuBar.addTab(Text.translated("skybox"), skyboxMenu);
        addWidget(menuBar);

        super.init();

        //set initial model
        if (!modelViewer.hasModel())
            setModel(LivingModelRegistry.STRAWBERRY.resource, LangManager.get("living_entity." + LivingModelRegistry.STRAWBERRY.name().toLowerCase()));

        modelViewer.setExtraRendering(matrices -> {
            if (renderGroundPlane) {
                //disable depth write and culling
                glDepthMask(false);
                glDisable(GL_CULL_FACE);

                //ground plane
                VertexConsumer.MAIN.consume(GeometryHelper.plane(matrices, -10f, -0.005f, -10f, 10f, 10f, 1, 1, 0x80000000));
                VertexConsumer.MAIN.consume(GeometryHelper.plane(matrices, -0.5f, -0.005f, -0.5f, 0.5f, 0.5f, 1, 1, 0x80000000));
                //axis lines
                VertexConsumer.MAIN.consume(GeometryHelper.plane(matrices, 0f, -0.005f, -0.005f, 0.5f, 0.005f, 1, 1, 0x80FF0000));
                VertexConsumer.MAIN.consume(GeometryHelper.plane(matrices, -0.005f, -0.005f, 0f, 0.005f, 0.5f, 1, 1, 0x800000FF));

                //bake and restore renderer
                VertexConsumer.finishAllBatches(ModelViewer.getCamera());
                glDepthMask(true);
                glEnable(GL_CULL_FACE);
            }

            //render model pivots
            if (renderAnimationBones && modelViewer.getModel() instanceof AnimatedMeshRenderer obj) {
                glDisable(GL_DEPTH_TEST);
                renderBone(matrices, obj.getBone(), 0.01f);
                VertexConsumer.finishAllBatches(ModelViewer.getCamera());
                glEnable(GL_DEPTH_TEST);
            }
        });
    }

    @Override
    public void close() {
        //free previous model
        if (modelRes != null)
            ModelManager.free(modelRes);

        super.close();
    }

    @Override
    protected void addBackButton() {
        //super.addBackButton();
    }

    private static void renderBone(MatrixStack matrices, Bone bone, float size) {
        matrices.pushMatrix();
        bone.getTransform().applyTransform(matrices);

        if (!bone.isModel()) {
            matrices.pushMatrix();
            matrices.translate(bone.getTransform().getPivot());
            VertexConsumer.LINES.consume(GeometryHelper.box(matrices, -size, -size, -size, size, size, size, 0xAA00F0FF));
            matrices.popMatrix();
        }

        for (Bone child : bone.getChildren())
            renderBone(matrices, child, size);
        matrices.popMatrix();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        //render title
        Text.of(modelName).withStyle(Style.EMPTY.outlined(true)).render(VertexConsumer.MAIN, matrices, 4, 16 + 4);

        //auto rotate
        if (autoRotate && modelViewer.getDragged() != 0 && !modelViewer.isUsingFlyCam())
            modelViewer.setYaw(modelViewer.getYaw() + client.timer.deltaTime() * 15f);

        //gizmo
        float len = 20f, scale = 50f;

        matrices.pushMatrix();
        matrices.translate(4 + len, height - len - 4, 0);
        matrices.scale(scale, -scale, scale);
        matrices.rotate(ModelViewer.getCamera().getRot().invert(new Quaternionf()));

        float invLen = len / scale;
        DebugRenderer.renderArrow(matrices, 1, 0, 0, invLen, 0xFFFF0000);
        DebugRenderer.renderArrow(matrices, 0, 1, 0, invLen, 0xFF00FF00);
        DebugRenderer.renderArrow(matrices, 0, 0, 1, invLen, 0xFF0000FF);

        matrices.popMatrix();

        if (modelViewer.shouldRenderBounds()) {
            Vector3f dimensions = modelViewer.getModel().getAABB().getDimensions();
            Text.of("").withStyle(Style.EMPTY.outlined(true))
                    .append("X: %.5f".formatted(dimensions.x))
                    .append("\n")
                    .append("Y: %.5f".formatted(dimensions.y))
                    .append("\n")
                    .append("Z: %.5f".formatted(dimensions.z))
                    .render(VertexConsumer.MAIN, matrices, 4, height / 2f, Alignment.CENTER_LEFT);
        }
    }

    private boolean setModel(Resource model, String name) {
        //try loading new model
        ModelRenderer renderer = ModelManager.getRenderer(model);
        if (renderer == null) {
            Toast.addToast(Text.translated("gui.model_viewer_screen.load_error")).type(Toast.ToastType.ERROR);
            return false;
        }

        //free previous model
        if (!model.equals(modelRes))
            ModelManager.free(modelRes);

        //set new model properties
        modelViewer.setModel(renderer);
        modelName = name;
        modelRes = model;

        //load animations
        animationList.clearActions();
        List<String> animations = modelViewer.getAnimations();
        if (!animations.isEmpty()) {
            modelViewer.stopAllAnimations();

            animationList.addAction(Text.translated("gui.none"), null, _ -> {
                modelViewer.stopAllAnimations();
                for (Widget action : animationList.getActions()) {
                    if (action instanceof Switch toggle)
                        toggle.setToggled(false);
                }
            });

            animations.sort(String::compareTo);
            for (String animation : animations) {
                Switch toggle = new Switch(0, 0, Text.of(animation));
                toggle.setToggled(modelViewer.getAnimation(animation).isPlaying());
                toggle.setAction(b -> {
                    if (((Switch) b).isToggled()) {
                        Animation anim = modelViewer.getAnimation(animation);
                        anim.setLoop(Animation.Loop.LOOP).play();
                    } else {
                        modelViewer.getAnimation(animation).stop();
                    }
                });
                animationList.addAction(toggle);
            }
        } else {
            animationList.addAction(Text.translated("gui.none"), null, null);
        }

        return true;
    }

    public boolean filesDropped(String[] files) {
        if (files.length == 0)
            return false;
        return setModel(new Resource("", files[0]), files[0]);
    }
}
