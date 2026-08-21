package cinnamon.world.world;

import cinnamon.math.collision.shape.Sphere;
import cinnamon.model.GeometryHelper;
import cinnamon.model.MaterialManager;
import cinnamon.model.ModelManager;
import cinnamon.model.material.Material;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.WorldRenderer;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.model.ModelRenderer;
import cinnamon.render.shader.Shader;
import cinnamon.utils.Colors;
import cinnamon.utils.Resource;
import cinnamon.world.light.PointLight;
import org.joml.Math;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

public class TransparentWorld extends WorldClient {

    private static final Resource IMAGE = new Resource("textures/misc/cat-jumping.png");
    private static final Vector3f[] positions = new Vector3f[100];
    private static final PointLight[] lights = new PointLight[20];

    static {
        for (int i = 0; i < positions.length; i++)
            positions[i] = new Vector3f();
        for (int i = 0; i < lights.length; i++)
            lights[i] = new PointLight();
    }

    @Override
    protected void levelLoad() {
        //super.levelLoad();
        for (PointLight light : lights)
            addLight(light);
        player.updateMovementFlags(false, false, true);
        gen();
    }

    @Override
    public void render(MatrixStack matrices, float delta) {
        WorldRenderer.renderSky = false;
        WorldRenderer.renderClouds = false;
        super.render(matrices, delta);
    }

    @Override
    public int renderTerrain(Camera camera, MatrixStack matrices, float delta) {
        matrices.pushMatrix();
        camera.billboard(matrices);
        VertexConsumer.SCREEN_UV.consume(GeometryHelper.quad(matrices, -1, -1, 2, 2), IMAGE);
        matrices.popMatrix();

        return super.renderTerrain(camera, matrices, delta) + 1;
    }

    @Override
    public void renderWater(Camera camera, MatrixStack matrices, float delta) {
        //no water
    }

    @Override
    public void renderTransparent(Camera camera, MatrixStack matrices, float delta) {
        ModelRenderer model = ModelManager.getRenderer(new Resource("models/terrain/sphere/sphere.obj"));
        ModelRenderer diamond = ModelManager.getRenderer(new Resource("models/misc/diamond.obj"));
        Material mat = MaterialManager.get(new Resource("materials/misc/diamond/diamond.mtl")).getFirst();

        for (int i = 0; i < positions.length; i++) {
            Shader.activeShader.applyColorRGBA(Colors.RAINBOW[i % Colors.RAINBOW.length].argb);
            matrices.pushMatrix();
            matrices.translate(positions[i]);
            model.render(matrices, mat);
            matrices.popMatrix();
        }

        int len = 8;
        float angle = Math.PI_TIMES_2_f / len;
        float dt = (getTime() + delta) * 0.5f;
        float dt2 = dt * 0.01f;
        float r = 16f;
        for (int i = 0; i < len; i++) {
            matrices.pushMatrix();
            matrices.translate(Math.sin(i * angle + dt2) * r, 0f, Math.cos(i * angle + dt2) * r);
            matrices.rotateY(dt);
            Shader.activeShader.applyColorRGBA(Colors.RAINBOW[i % Colors.RAINBOW.length].argb);
            diamond.render(matrices, mat);
            matrices.popMatrix();
        }

        Shader.activeShader.applyColorRGBA(Colors.WHITE.argb);
        super.renderTransparent(camera, matrices, delta);
    }

    private void gen() {
        Sphere sphere = new Sphere(14f);
        for (Vector3f position : positions)
            sphere.getRandomPoint(position);
        for (PointLight light : lights) {
            light.pos(sphere.getRandomPoint(new Vector3f()));
            light.setCastShadows(false);
            light.color(0xFFEEEEAA);
        }
    }

    @Override
    public void keyPress(int key, int scancode, int action, int mods) {
        if (action != GLFW_RELEASE && key == GLFW_KEY_F)
            gen();
        super.keyPress(key, scancode, action, mods);
    }
}
