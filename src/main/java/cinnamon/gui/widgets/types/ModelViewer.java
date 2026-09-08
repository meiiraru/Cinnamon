package cinnamon.gui.widgets.types;

import cinnamon.Client;
import cinnamon.animation.Animation;
import cinnamon.gui.widgets.GUIListener;
import cinnamon.gui.widgets.SelectableWidget;
import cinnamon.gui.widgets.Tickable;
import cinnamon.input.InputManager;
import cinnamon.math.Maths;
import cinnamon.math.collision.shape.AABB;
import cinnamon.model.GeometryHelper;
import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.SkyBoxRegistry;
import cinnamon.render.BloomRenderer;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.WorldRenderer;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.model.AnimatedMeshRenderer;
import cinnamon.render.model.ModelRenderer;
import cinnamon.render.shader.PostProcess;
import cinnamon.render.shader.Shader;
import cinnamon.settings.Settings;
import cinnamon.vr.XrManager;
import cinnamon.world.sky.CubemapSky;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;

public class ModelViewer extends SelectableWidget implements Tickable {

    private static final Framebuffer modelBuffer = new Framebuffer(Framebuffer.COLOR_BUFFER | Framebuffer.DEPTH_BUFFER);
    private static final Camera camera = new Camera();
    private static final CubemapSky theSky = new CubemapSky();
    static {
        theSky.fogColor = 0x000000;
        theSky.fogStart = 950f;
        theSky.fogEnd   = 1000f;
    }

    //properties
    private ModelRenderer model = null;
    private MaterialRegistry selectedMaterial = MaterialRegistry.DEFAULT;
    private SkyBoxRegistry skybox = SkyBoxRegistry.WHITE;

    private boolean renderBounds, renderSkybox, renderWireframe, cullBackFaces = true;
    private Consumer<MatrixStack> extraRendering;

    private float defaultScale = 1f, scaleFactor = 0.1f;
    private float defaultPitch = -15f, defaultYaw = 210f;

    //view transforms
    private float posX = 0, posY = 0;
    private float scale = defaultScale, scaleReset = defaultScale;
    private float pitch = defaultPitch, yaw = defaultYaw;

    //dragging
    private int dragged = -1;
    private int anchorX = 0, anchorY = 0;
    private float anchorPitch = 0, anchorYaw = 0;
    private float anchorPosX = 0, anchorPosY = 0;

    //flycam controls
    private boolean useFlyCam, flyCamActive;
    private float flyCamAnchX, flyCamAnchY, flyCamPitch, flyCamYaw = 180f;
    private float flyCamX, flyCamY, flyCamZ = -2f * (1f / scale), flyCamLastX, flyCamLastY, flyCamLastZ = flyCamZ;
    private float flyCamSpeed = 0.2f;

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
    public void tick() {
        if (useFlyCam) {
            //save flycam last position
            flyCamLastX = flyCamX;
            flyCamLastY = flyCamY;
            flyCamLastZ = flyCamZ;

            if (!flyCamActive)
                return;

            //update flycam impulse
            float impulseX = 0f, impulseY = 0f, impulseZ = 0f;
            if (InputManager.isKeyPressed(GLFW_KEY_W)) impulseZ += 1f;
            if (InputManager.isKeyPressed(GLFW_KEY_S)) impulseZ -= 1f;
            if (InputManager.isKeyPressed(GLFW_KEY_A)) impulseX += 1f;
            if (InputManager.isKeyPressed(GLFW_KEY_D)) impulseX -= 1f;
            if (InputManager.isKeyPressed(GLFW_KEY_SPACE))      impulseY += 1f;
            if (InputManager.isKeyPressed(GLFW_KEY_LEFT_SHIFT)) impulseY -= 1f;

            //update flycam position
            Vector3f forward = camera.getForwards();
            Vector3f left    = camera.getLeft();

            flyCamX += (forward.x * impulseZ + left.x * impulseX) * flyCamSpeed;
            flyCamY += (forward.y * impulseZ + left.y * impulseX + impulseY) * flyCamSpeed;
            flyCamZ += (forward.z * impulseZ + left.z * impulseX) * flyCamSpeed;
        }
    }

    @Override
    public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        //render model
        Client client = Client.getInstance();
        if (client.anaglyph3D) {
            client.camera.anaglyph3D(matrices, -1f / 64f, 1f, () -> renderModelToBuffer(matrices, delta), this::renderBuffer);
        } else {
            renderModelToBuffer(matrices, delta);
            renderBuffer();
        }
    }

    private void renderModelToBuffer(MatrixStack matrices, float delta) {
        if (model == null)
            return;

        //prepare renderer
        Client client = Client.getInstance();
        boolean xr = XrManager.isInXR();
        VertexConsumer.finishAllBatches(client.camera);

        Shader oldShader = Shader.activeShader;
        AABB aabb = model.getAABB();
        matrices.pushMatrix();

        //set up framebuffer
        Framebuffer old = Framebuffer.activeFramebuffer;
        modelBuffer.resizeTo(old);
        modelBuffer.useClear();
        if (xr) old.blit(modelBuffer, false, true, true);

        //set up world renderer
        WorldRenderer.renderLights = false;
        WorldRenderer.renderSSR = false;
        WorldRenderer.renderSSAO = !xr && cullBackFaces;
        WorldRenderer.setupFramebuffer();

        camera.copyFrom(client.camera, false);
        camera.useOrtho(false);

        if (useFlyCam) {
            camera.setPos(
                    Math.lerp(flyCamLastX, flyCamX, delta),
                    Math.lerp(flyCamLastY, flyCamY, delta),
                    Math.lerp(flyCamLastZ, flyCamZ, delta)
            );
            camera.setRot(flyCamPitch, flyCamYaw, 0f);
        } else {
            camera.setPos(0, 0, 0);
            camera.setRot(-pitch, -yaw, 0f);
            camera.move(-posX, posY, 2f * (1f / scale), true);
        }

        //skybox
        if (renderSkybox)
            theSky.render(camera, matrices);

        //center model
        matrices.translate(aabb.getCenter().mul(-1f));

        //apply model-only render state
        if (renderWireframe)
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        if (!cullBackFaces)
            glDisable(GL_CULL_FACE);

        //draw model
        WorldRenderer.initGBuffer(camera);
        model.render(matrices, selectedMaterial.material);

        //restore model-only render state
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glEnable(GL_CULL_FACE);

        //finish render
        WorldRenderer.finishMaterials(camera);

        //ssao
        WorldRenderer.renderSSAO(camera);

        //bake the model renderer
        theSky.setSkyBox(skybox.resource);
        WorldRenderer.bakeDeferred(camera, theSky);

        //bloom
        float bloom = Settings.bloomStrength.get();
        if (bloom > 0f)
            BloomRenderer.applyBloom(WorldRenderer.outputBuffer, WorldRenderer.PBRFrameBuffer.getEmissive(), 1.5f, bloom);

        //draw bounding box
        if (renderBounds) {
            Vector3f min = aabb.getMin();
            Vector3f max = aabb.getMax();
            VertexConsumer.LINES.consume(GeometryHelper.box(matrices, min.x, min.y, min.z, max.x, max.y, max.z, 0xFFFFFFFF));
            VertexConsumer.LINES.finishBatch(camera);
        }

        //extra rendering
        if (extraRendering != null)
            extraRendering.accept(matrices);
        VertexConsumer.finishAllBatches(camera);

        //finish world render
        WorldRenderer.bake();

        //return to old framebuffer
        old.use();

        //cleanup
        oldShader.use();
        matrices.popMatrix();
    }

    private void renderBuffer() {
        //offset the uv to center the model in the widget
        Client c = Client.getInstance();
        boolean xr = XrManager.isInXR();

        float guiScale = c.window.guiScale;
        float x = ((getWidth() - c.window.getGUIWidth()) / 2f + c.window.getGUIWidth() - (getWidth() + getX())) * guiScale / modelBuffer.getWidth();
        float y = ((getHeight() - c.window.getGUIHeight()) / 2f + getY()) * guiScale / modelBuffer.getHeight();

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
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        if (!xr) glDisable(GL_SCISSOR_TEST);
    }

    public void setMaterial(MaterialRegistry material) {
        this.selectedMaterial = material;
    }

    public void setSkybox(SkyBoxRegistry type) {
        skybox = type;
    }

    public void setSkyboxColor(int color) {
        theSky.setTint(color);
    }

    public int getSkyboxColor() {
        return theSky.getTint();
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

    public ModelRenderer getModel() {
        return model;
    }

    public static Framebuffer getModelBuffer() {
        return modelBuffer;
    }

    public static Camera getCamera() {
        return camera;
    }

    public static CubemapSky getSky() {
        return theSky;
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

    public boolean shouldCullBackFaces() {
        return cullBackFaces;
    }

    public void setCullBackFaces(boolean cullBackFaces) {
        this.cullBackFaces = cullBackFaces;
    }

    public void setExtraRendering(Consumer<MatrixStack> extraRendering) {
        this.extraRendering = extraRendering;
    }

    public boolean isUsingFlyCam() {
        return useFlyCam;
    }

    public void setFlyCam(boolean flyCam) {
        this.useFlyCam = flyCam;
        if (!flyCam && flyCamActive) {
            Client.getInstance().window.unlockMouse();
            flyCamActive = false;
        }
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

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setDefaultRot(float pitch, float yaw) {
        this.defaultPitch = pitch;
        this.defaultYaw   = yaw;
    }

    public float getDefaultPitch() {
        return defaultPitch;
    }

    public float getDefaultYaw() {
        return defaultYaw;
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

    public float getFlyCamX() {
        return flyCamX;
    }

    public void setFlyCamX(float flyCamX) {
        this.flyCamX = flyCamX;
    }

    public float getFlyCamY() {
        return flyCamY;
    }

    public void setFlyCamY(float flyCamY) {
        this.flyCamY = flyCamY;
    }

    public float getFlyCamZ() {
        return flyCamZ;
    }

    public void setFlyCamZ(float flyCamZ) {
        this.flyCamZ = flyCamZ;
    }

    public float getFlyCamPitch() {
        return flyCamPitch;
    }

    public void setFlyCamPitch(float flyCamPitch) {
        this.flyCamPitch = flyCamPitch;
    }

    public float getFlyCamYaw() {
        return flyCamYaw;
    }

    public void setFlyCamYaw(float flyCamYaw) {
        this.flyCamYaw = flyCamYaw;
    }

    public float getFlyCamSpeed() {
        return flyCamSpeed;
    }

    public void setFlyCamSpeed(float flyCamSpeed) {
        this.flyCamSpeed = flyCamSpeed;
    }

    private void resetView() {
        posX = posY = 0f;
        scale = scaleReset;
        pitch = defaultPitch;
        yaw = defaultYaw;
        flyCamX = flyCamY = 0f;
        flyCamZ = -2f * (1f / scale);
        flyCamPitch = 0f;
        flyCamYaw = 180f;
        flyCamSpeed = 0.2f;
    }

    public List<String> getAnimations() {
        if (model instanceof AnimatedMeshRenderer animModel)
            return animModel.getAnimations();
        return List.of();
    }

    public void stopAllAnimations() {
        if (model instanceof AnimatedMeshRenderer animModel)
            animModel.stopAllAnimations();
    }

    public Animation getAnimation(String anim) {
        if (model instanceof AnimatedMeshRenderer animModel)
            return animModel.getAnimation(anim);
        return null;
    }

    @Override
    public boolean isHovered() {
        return flyCamActive || super.isHovered();
    }

    @Override
    public GUIListener keyPress(int key, int scancode, int action, int mods) {
        GUIListener sup = super.keyPress(key, scancode, action, mods);
        if (sup != null) return sup;

        if (useFlyCam && action == GLFW_PRESS && flyCamActive) {
            switch (key) {
                case GLFW_KEY_ESCAPE -> {
                    Client.getInstance().window.unlockMouse();
                    flyCamActive = false;
                }
                case GLFW_KEY_R -> resetView();
            }
        }
        return this;
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

        if (useFlyCam) {
            switch (button) {
                case GLFW_MOUSE_BUTTON_1 -> {
                    w.lockMouse();
                    flyCamActive = true;
                    flyCamAnchX = w.mouseX;
                    flyCamAnchY = w.mouseY;
                }
                case GLFW_MOUSE_BUTTON_2 -> {
                    w.unlockMouse();
                    flyCamActive = false;
                }
                case GLFW_MOUSE_BUTTON_3 -> resetView();
            }
            return this;
        }

        dragged = button;
        anchorX = w.mouseX;
        anchorY = w.mouseY;

        switch (button) {
            case GLFW_MOUSE_BUTTON_1 -> {
                anchorPitch = pitch;
                anchorYaw   = yaw;
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

        Window w = Client.getInstance().window;

        if (useFlyCam) {
            if (!flyCamActive)
                return this;

            float scale = w.guiScale;
            double dx = (x - flyCamAnchX) * scale * (Settings.invertX.get() ? -1 : 1);
            double dy = (y - flyCamAnchY) * scale * (Settings.invertY.get() ? -1 : 1);
            flyCamAnchX = x;
            flyCamAnchY = y;

            double sensi = InputManager.getSensiMultiplier();
            flyCamPitch += (float) (dy * sensi);
            flyCamYaw   += (float) (dx * sensi);

            flyCamPitch = Maths.clamp(flyCamPitch, -89f, 89f);

            //even with mouse locked, the mouse can still move around and hover over other widgets
            //so we make it always centered to this widget to prevent that
            float newX = getCenterX();
            float newY = getCenterY();

            //we allow a glance of 5px to the sides of the widget before we reset the mouse position to the center
            if (Math.abs(newX - x) > 5 || Math.abs(newY - y) > 5) {
                w.setMousePos(newX * scale, newY * scale);
                flyCamAnchX = newX;
                flyCamAnchY = newY;
            }

            return this;
        }

        if (dragged == -1)
            return null;

        int dx = x - anchorX;
        int dy = y - anchorY;

        if (dragged == GLFW_MOUSE_BUTTON_1) {
            pitch = anchorPitch - dy;
            yaw   = anchorYaw   - dx;

            //fix pitch to not let it be upside down
            //update the anchor pitch to prevent snapping when dragging
            if (pitch > 89f) {
                pitch = 89f;
                anchorPitch = pitch + dy;
            } else if (pitch < -89f) {
                pitch = -89f;
                anchorPitch = pitch + dy;
            }
        } else if (dragged == GLFW_MOUSE_BUTTON_2) {
            float s = 1f / scale * 0.012f;
            posX = anchorPosX + dx * s;
            posY = anchorPosY + dy * s;
        }

        w.warpMouse((deltaX, deltaY) -> {
            anchorX += deltaX;
            anchorY += deltaY;
        });

        return this;
    }

    @Override
    public GUIListener mouseScroll(double x, double y) {
        if (model == null)
            return null;

        float s = 1f + (float) Math.signum(y) * scaleFactor;

        if (useFlyCam) {
            if (flyCamActive)
                flyCamSpeed *= s;
        } else {
            scale *= s;
        }

        return this;
    }

    @Override
    public GUIListener windowFocused(boolean focused) {
        if (!focused) dragged = -1;
        return super.windowFocused(focused);
    }

    public int getDragged() {
        return dragged;
    }
}
