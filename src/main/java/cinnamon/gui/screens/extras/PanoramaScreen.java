package cinnamon.gui.screens.extras;

import cinnamon.Client;
import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.model.ModelManager;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.model.ModelRenderer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.*;
import cinnamon.vr.XrManager;
import cinnamon.vr.XrRenderer;
import org.joml.Matrix3f;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public class PanoramaScreen extends ParentedScreen {

    private static final Resource MODEL = new Resource("models/misc/inverted_sphere.obj");
    private final ModelRenderer sphere;

    private Texture texture;

    private boolean dragged;
    private int anchorX, anchorY;
    private float anchorRotX, anchorRotY;
    private float rotX, rotY;

    public PanoramaScreen(Screen parentScreen) {
        super(parentScreen);
        sphere = ModelManager.load(MODEL);
    }

    @Override
    protected void addBackButton() {
        //super.addBackButton();
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta) {
        if (texture == null)
            renderSolidBackground(Colors.BLUE.rgba);
    }

    @Override
    protected boolean shouldRenderMouse() {
        return false; //no
    }

    @Override
    protected void preRender(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.preRender(matrices, mouseX, mouseY, delta);
        if (texture == null) {
            Text.translated("gui.panorama_viewer.help").withStyle(Style.EMPTY.shadow(true)).render(VertexConsumer.FONT, matrices, width / 2f, 4f, Alignment.TOP_CENTER);
            return;
        }

        //prepare matrices
        matrices.pushMatrix();
        matrices.identity();

        matrices.translate(client.camera.getPosition());

        boolean xr = XrManager.isInXR();
        if (!xr) {
            matrices.rotate(Rotation.X.rotationDeg(rotX));
            matrices.rotate(Rotation.Y.rotationDeg(rotY));
        }

        Matrix3f uv = new Matrix3f();
        //vertical stereo
        if (texture.getWidth() <= texture.getHeight()) {
            Maths.translateMat3(uv.scale(1f, 0.5f, 1f), 0f, 0.5f * (xr ? XrRenderer.getRenderIndex() : 0));
        }
        //horizontal stereo
        else if (texture.getWidth() > texture.getHeight() * 2) {
            Maths.translateMat3(uv.scale(0.5f, 1f, 1f), 0.5f * (xr ? XrRenderer.getRenderIndex() : 0), 0f);
        }

        //prepare shaders
        Shader oldS = Shader.activeShader;
        Shader s = Shaders.MODEL_UV.getShader().use();
        client.camera.useOrtho(false);
        s.setup(client.camera);
        s.setMat3("uvMatrix", uv);
        s.setTexture("textureSampler", texture, 0);

        //render model
        sphere.renderWithoutMaterial(matrices);

        //cleanup
        Texture.unbindTex(0);
        matrices.popMatrix();
        oldS.use();
        client.camera.useOrtho(true);
    }

    @Override
    public boolean mousePress(int button, int action, int mods) {
        boolean sup = super.mousePress(button, action, mods);
        if (sup || dragged) {
            dragged = false;
            return true;
        }

        if (action != GLFW_PRESS)
            return false;

        dragged = true;
        anchorX = client.window.mouseX;
        anchorY = client.window.mouseY;
        anchorRotX = rotX;
        anchorRotY = rotY;

        return true;
    }

    @Override
    public boolean mouseMove(int x, int y) {
        if (super.mouseMove(x, y))
            return true;

        if (!dragged)
            return false;

        float dx = anchorX - x;
        float dy = anchorY - y;

        rotX = anchorRotX - dy;
        rotY = anchorRotY - dx;

        Client.getInstance().window.warpMouse((deltaX, deltaY) -> {
            anchorX += deltaX;
            anchorY += deltaY;
        });

        return true;
    }

    @Override
    public boolean filesDropped(String[] files) {
        texture = Texture.of(new Resource("", files[0]), true, false);
        return true;
    }
}
