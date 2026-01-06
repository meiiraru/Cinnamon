package cinnamon.gui.widgets.types;

import cinnamon.Client;
import cinnamon.animation.Animation;
import cinnamon.gui.widgets.GUIListener;
import cinnamon.gui.widgets.SelectableWidget;
import cinnamon.model.GeometryHelper;
import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.SkyBoxRegistry;
import cinnamon.render.BloomRenderer;
import cinnamon.render.MaterialApplier;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.WorldRenderer;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.model.AnimatedObjRenderer;
import cinnamon.render.model.ModelRenderer;
import cinnamon.render.shader.PostProcess;
import cinnamon.render.shader.Shader;
import cinnamon.settings.Settings;
import cinnamon.utils.AABB;
import cinnamon.utils.Maths;
import cinnamon.utils.Rotation;
import cinnamon.vr.XrManager;
import cinnamon.world.sky.IBLSky;
import cinnamon.world.sky.Sky;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class ModelViewer extends SelectableWidget {

    private static final Framebuffer modelBuffer = new Framebuffer(Framebuffer.COLOR_BUFFER | Framebuffer.DEPTH_BUFFER);
    private static final Sky theSky = new IBLSky();
    static {
        theSky.fogColor = 0x000000;
        theSky.fogStart = 4096f;
        theSky.fogEnd = 4096f;
    }

    //properties
    private ModelRenderer model = null;
    private MaterialRegistry selectedMaterial = MaterialRegistry.DEFAULT;
    private SkyBoxRegistry skybox = SkyBoxRegistry.WHITE;

    private boolean renderBounds, renderSkybox, renderWireframe;
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
        if (XrManager.isInXR())
            setDefaultRot(0f, 0f);
    }

    public static void free() {
        modelBuffer.free();
    }

    @Override
    public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        //render model
        Client client = Client.getInstance();
        if (client.anaglyph3D) {
            client.camera.anaglyph3D(matrices, 1f, 1f, () -> renderModelToBuffer(matrices), this::renderBuffer);
        } else {
            renderModelToBuffer(matrices);
            renderBuffer();
        }
    }

    private void renderModelToBuffer(MatrixStack matrices) {
        if (model == null)
            return;

        //prepare renderer
        Client client = Client.getInstance();
        boolean xr = XrManager.isInXR();
        VertexConsumer.finishAllBatches(client.camera);

        Shader oldShader = Shader.activeShader;
        client.camera.useOrtho(false);
        AABB aabb = model.getAABB();
        matrices.pushMatrix();

        //set up framebuffer
        Framebuffer old = Framebuffer.activeFramebuffer;
        modelBuffer.resizeTo(old);
        modelBuffer.useClear();
        if (xr) old.blit(modelBuffer, false, true, true);

        //set up world renderer
        WorldRenderer.renderSSAO = false;
        WorldRenderer.renderLights = false;
        WorldRenderer.renderSSR = false;
        WorldRenderer.setupFramebuffer();
        WorldRenderer.initGBuffer(client.camera);

        //position
        matrices.translate(posX, -posY, -200f);
        if (xr) matrices.translate(getCenterX(), getCenterY() + posY + posY, 200f);

        //scale
        matrices.scale(scale, xr ? -scale : scale, scale);

        //rotate model
        matrices.rotate(Rotation.X.rotationDeg(-rotX));
        matrices.rotate(Rotation.Y.rotationDeg(-rotY - 180));

        //extra rendering
        if (extraRendering != null)
            extraRendering.accept(matrices);

        //apply model-only render state
        if (renderWireframe)
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        glDisable(GL_CULL_FACE);

        //draw model
        matrices.translate(aabb.getCenter().mul(-1f));
        model.render(matrices, selectedMaterial.material);

        //restore model-only render state
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glEnable(GL_CULL_FACE);

        //finish render
        MaterialApplier.cleanup();
        VertexConsumer.finishAllBatches(client.camera);

        //bake the model renderer
        ((IBLSky) theSky).setSkyBox(skybox.resource);
        WorldRenderer.bakeDeferred(client.camera, theSky);

        //skybox + bloom
        if (renderSkybox)
            theSky.render(client.camera, matrices);

        float bloom = Settings.bloomStrength.get();
        if (!xr && bloom > 0f)
            BloomRenderer.applyBloom(WorldRenderer.outputBuffer, WorldRenderer.PBRFrameBuffer.getEmissive(), 0.8f, bloom);

        //draw bounding box
        if (renderBounds) {
            Vector3f min = aabb.getMin();
            Vector3f max = aabb.getMax();
            VertexConsumer.LINES.consume(GeometryHelper.box(matrices, min.x, min.y, min.z, max.x, max.y, max.z, 0xFFFFFFFF));
            VertexConsumer.LINES.finishBatch(client.camera);
        }

        //finish world render
        WorldRenderer.bake();

        //return to old framebuffer
        old.use();

        //cleanup
        oldShader.use();
        matrices.popMatrix();
        client.camera.useOrtho(true);
    }

    private void renderBuffer() {
        //offset the uv to center the model in the widget
        Client c = Client.getInstance();
        boolean xr = XrManager.isInXR();

        float guiScale = c.window.guiScale;
        float x = ((getWidth() - c.window.scaledWidth) / 2f + c.window.scaledWidth - (getWidth() + getX())) * guiScale / modelBuffer.getWidth();
        float y = ((getHeight() - c.window.scaledHeight) / 2f + getY()) * guiScale / modelBuffer.getHeight();

        //enable scissor test to limit the rendering to the widget area
        if (!xr) {
            glEnable(GL_SCISSOR_TEST);
            glScissor(
                    (int) (getX() * guiScale),
                    (int) (c.window.height - (getHeight() + getY()) * guiScale),
                    (int) Math.max(getWidth() * guiScale, 0),
                    (int) Math.max(getHeight() * guiScale, 0)
            );
        }

        //draw framebuffer result
        Shader old = Shader.activeShader;
        Shader s = PostProcess.BLIT_UV.getShader().use();
        s.setTexture("colorTex", modelBuffer.getColorBuffer(), 0);
        s.setVec2("uvOffset", x, y);

        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        WorldRenderer.renderQuad();

        //reset render state
        old.use();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        if (!xr) glDisable(GL_SCISSOR_TEST);
    }

    public void setMaterial(MaterialRegistry material) {
        this.selectedMaterial = material;
    }

    public void setSkybox(SkyBoxRegistry type) {
        skybox = type;
    }

    public MaterialRegistry getMaterial() {
        return selectedMaterial;
    }

    public SkyBoxRegistry getSkybox() {
        return skybox;
    }

    public boolean hasModel() {
        return model != null;
    }

    public void setModel(ModelRenderer model) {
        this.model = model;
        float maxDimension = model == null ? 1f : Maths.max(model.getAABB().getDimensions());
        scaleReset = defaultScale / maxDimension;
        resetView();
    }

    public boolean shouldRenderBounds() {
        return renderBounds;
    }

    public void setRenderBounds(boolean renderBounds) {
        this.renderBounds = renderBounds;
    }

    public boolean shouldRenderSkybox() {
        return renderSkybox;
    }

    public void setRenderSkybox(boolean renderSkybox) {
        this.renderSkybox = renderSkybox;
    }

    public boolean shouldRenderWireframe() {
        return renderWireframe;
    }

    public void setRenderWireframe(boolean renderWireframe) {
        this.renderWireframe = renderWireframe;
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
