package cinnamon.gui.widgets.types;

import cinnamon.Client;
import cinnamon.animation.Animation;
import cinnamon.gui.widgets.GUIListener;
import cinnamon.gui.widgets.SelectableWidget;
import cinnamon.model.GeometryHelper;
import cinnamon.registry.MaterialRegistry;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.model.AnimatedObjRenderer;
import cinnamon.render.model.ModelRenderer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;
import cinnamon.utils.AABB;
import cinnamon.utils.Maths;
import cinnamon.utils.Rotation;
import cinnamon.world.SkyBox;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class ModelViewer extends SelectableWidget {

    private static final Framebuffer modelBuffer = new Framebuffer(1, 1, Framebuffer.COLOR_BUFFER | Framebuffer.DEPTH_BUFFER);
    private static final SkyBox the_skybox = new SkyBox();

    //properties
    private ModelRenderer model = null;
    private MaterialRegistry selectedMaterial = MaterialRegistry.DEFAULT;
    private SkyBox.Type skybox = SkyBox.Type.WHITE;

    private boolean renderBounds;
    private Consumer<MatrixStack> extraRendering;

    private float defaultScale = 128f, scaleFactor = 0.1f;
    private float defaultRotX = -22.5f, defaultRotY = 30f;

    //view transforms
    private float posX = 0, posY = 0;
    private float scale = defaultScale, scaleReset = defaultScale;
    private float rotX = defaultRotX, rotY = defaultRotY;

    //dragging
    private int dragged = -1;
    private int anchorX = 0, anchorY = 0;
    private float anchorRotX = 0, anchorRotY = 0;
    private float anchorPosX = 0, anchorPosY = 0;

    public ModelViewer(int x, int y, int width, int height) {
        super(x, y, width, height);
        setSelectable(false);
    }

    public static void free() {
        modelBuffer.free();
    }

    @Override
    public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        //render model
        Client client = Client.getInstance();
        if (client.anaglyph3D) {
            client.camera.anaglyph3D(matrices, 1f, 1f, () -> renderModelToBuffer(matrices), () -> renderBuffer(matrices));
        } else {
            renderModelToBuffer(matrices);
            renderBuffer(matrices);
        }
    }

    private void renderModelToBuffer(MatrixStack matrices) {
        if (model == null)
            return;

        //prepare render
        Client client = Client.getInstance();
        VertexConsumer.finishAllBatches(client.camera);

        Shader oldShader = Shader.activeShader;
        client.camera.useOrtho(false);
        AABB aabb = model.getAABB();
        matrices.push();

        //setup shader
        Shader s = Shaders.WORLD_MODEL_PBR.getShader().use();
        s.setup(client.camera);
        s.setVec3("camPos", 0, 0, 0);
        s.setFloat("fogStart", 1024);
        s.setFloat("fogEnd", 2048);
        the_skybox.type = skybox;
        the_skybox.pushToShader(s, Texture.MAX_TEXTURES - 1);
        glDisable(GL_CULL_FACE);

        //set up framebuffer
        modelBuffer.useClear();
        modelBuffer.resizeTo(Framebuffer.DEFAULT_FRAMEBUFFER);

        //position
        matrices.translate(posX, -posY, -200);

        //scale
        matrices.scale(scale, scale, scale);

        //rotate model
        matrices.rotate(Rotation.X.rotationDeg(-rotX));
        matrices.rotate(Rotation.Y.rotationDeg(-rotY - 180));

        //extra rendering
        if (extraRendering != null)
            extraRendering.accept(matrices);

        //draw model
        matrices.translate(aabb.getCenter().mul(-1f));
        model.render(matrices, selectedMaterial.material);

        //draw bounding box
        if (renderBounds) {
            Vector3f min = aabb.getMin();
            Vector3f max = aabb.getMax();
            VertexConsumer.LINES.consume(GeometryHelper.cube(matrices, min.x, min.y, min.z, max.x, max.y, max.z, 0xFFFFFFFF));
        }

        //finish render
        VertexConsumer.finishAllBatches(client.camera);
        Framebuffer.DEFAULT_FRAMEBUFFER.use();

        //cleanup
        glEnable(GL_CULL_FACE);
        oldShader.use();
        matrices.pop();
        client.camera.useOrtho(true);
    }

    private void renderBuffer(MatrixStack matrices) {
        //draw framebuffer result
        float guiScale = Client.getInstance().window.guiScale;
        float x = (1f - (getWidth() * guiScale) / modelBuffer.getWidth()) / 2f;
        float y = (1f - (getHeight() * guiScale) / modelBuffer.getHeight()) / 2f;
        VertexConsumer.GUI.consume(GeometryHelper.quad(matrices, getX(), getY(), getWidth(), getHeight(), -1f, x, 1f - x, 1f - y, y), modelBuffer.getColorBuffer());
    }

    public void setMaterial(MaterialRegistry material) {
        this.selectedMaterial = material;
    }

    public void setSkybox(SkyBox.Type type) {
        skybox = type;
    }

    public MaterialRegistry getMaterial() {
        return selectedMaterial;
    }

    public SkyBox.Type getSkybox() {
        return skybox;
    }

    public boolean hasModel() {
        return model != null;
    }

    public void setModel(ModelRenderer model) {
        this.model = model;
        float maxDimension = Maths.max(model.getAABB().getDimensions());
        scaleReset = defaultScale / maxDimension;
        resetView();
    }

    public boolean shouldRenderBounds() {
        return renderBounds;
    }

    public void setRenderBounds(boolean renderBounds) {
        this.renderBounds = renderBounds;
    }

    public void setExtraRendering(Consumer<MatrixStack> extraRendering) {
        this.extraRendering = extraRendering;
    }

    public float getPosX() {
        return posX;
    }

    public float getPosY() {
        return posY;
    }

    public void setPosX(float posX) {
        this.posX = posX;
    }

    public void setPosY(float posY) {
        this.posY = posY;
    }

    public float getRotX() {
        return rotX;
    }

    public float getRotY() {
        return rotY;
    }

    public void setRotX(float rotX) {
        this.rotX = rotX;
    }

    public void setRotY(float rotY) {
        this.rotY = rotY;
    }

    public void setDefaultRot(float rotX, float rotY) {
        this.defaultRotX = rotX;
        this.defaultRotY = rotY;
    }

    public float getDefaultRotX() {
        return defaultRotX;
    }

    public float getDefaultRotY() {
        return defaultRotY;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setDefaultScale(float scale) {
        this.defaultScale = scale;
    }

    public float getDefaultScale() {
        return defaultScale;
    }

    public void setScaleFactor(float scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    private void resetView() {
        posX = 0;
        posY = 0;
        scale = scaleReset;
        rotX = defaultRotX;
        rotY = defaultRotY;
    }

    public List<String> getAnimations() {
        if (model instanceof AnimatedObjRenderer animModel)
            return animModel.getAnimations();
        return List.of();
    }

    public void stopAllAnimations() {
        if (model instanceof AnimatedObjRenderer animModel)
            animModel.stopAllAnimations();
    }

    public Animation getAnimation(String anim) {
        if (model instanceof AnimatedObjRenderer animModel)
            return animModel.getAnimation(anim);
        return null;
    }

    @Override
    public GUIListener mousePress(int button, int action, int mods) {
        if (dragged != -1) {
            dragged = -1;
            return this;
        }

        if (model == null || !isHovered() || action != GLFW_PRESS) {
            dragged = -1;
            return null;
        }

        Window w = Client.getInstance().window;
        dragged = button;
        anchorX = w.mouseX;
        anchorY = w.mouseY;

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

        return this;
    }

    @Override
    public GUIListener mouseMove(int x, int y) {
        GUIListener sup = super.mouseMove(x, y);
        if (sup != null) return sup;

        if (dragged == -1)
            return null;

        float dx = x - anchorX;
        float dy = y - anchorY;

        if (dragged == GLFW_MOUSE_BUTTON_1) {
            rotX = anchorRotX - dy;
            rotY = anchorRotY - dx;
        } else if (dragged == GLFW_MOUSE_BUTTON_2) {
            posX = anchorPosX + dx;
            posY = anchorPosY + dy;
        }

        Client.getInstance().window.warpMouse((deltaX, deltaY) -> {
            anchorX += deltaX;
            anchorY += deltaY;
        });

        return this;
    }

    @Override
    public GUIListener scroll(double x, double y) {
        if (model == null)
            return null;

        scale *= 1f + (float) Math.signum(y) * scaleFactor;

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

        return this;
    }

    @Override
    public GUIListener windowFocused(boolean focused) {
        if (!focused) dragged = -1;
        return super.windowFocused(focused);
    }
}
