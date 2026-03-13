package cinnamon.world.sky;

import cinnamon.math.Rotation;
import cinnamon.model.GeometryHelper;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.shader.Shader;
import cinnamon.utils.Resource;
import org.joml.Math;
import org.joml.Vector3f;

public abstract class Sky {

    public static final Resource SUN = new Resource("textures/environment/sun.png");

    public int
            fogColor = 0xBFD3DE,
            skyColor = 0x4444D0,
            sunColor = 0xFF8822,
            cloudsColor = 0x7F7F7F,
            ambientLight = 0xBFD3DE;

    public float
            fogStart = 96,
            fogEnd = 192;

    public float
            fogIntensity = 1f,
            sunIntensity = 1f,
            starsIntensity = 1f;

    protected final Vector3f sunDir = new Vector3f(1, 0, 0);
    protected float sunAngle;
    protected float sunRoll = Math.toRadians(30f);

    public boolean
            renderSky = true,
            renderSun = false;

    public void render(Camera camera, MatrixStack matrices) {
        //render sky
        if (renderSky)
            renderSky(camera, matrices);

        //render sun
        if (renderSun)
            renderSun(camera, matrices);
    }

    protected abstract void renderSky(Camera camera, MatrixStack matrices);

    protected void renderSun(Camera camera, MatrixStack matrices) {
        //move to camera position
        matrices.pushMatrix();
        matrices.translate(camera.getPos());

        //translate sun
        matrices.rotate(Rotation.Y.rotationDeg(90f));
        matrices.rotate(Rotation.Z.rotation(sunRoll));
        matrices.rotate(Rotation.X.rotationDeg(-sunAngle));
        matrices.translate(0, 0, 512);

        //render sun
        VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices, -32, -32, 64, 64), SUN);
        VertexConsumer.MAIN.finishBatch(camera);

        //cleanup rendering
        matrices.popMatrix();
    }

    public void applyUniforms(Shader shader, Camera camera) {
        //camera
        shader.setVec3("camPos", camera.getPosition());

        //fog
        shader.setFloat("fogStart", fogStart);
        shader.setFloat("fogEnd", fogEnd);
        shader.setColor("fogColor", fogColor);

        //ambient light
        shader.setColor("ambientLight", ambientLight);
    }

    protected void updateSunDir() {
        this.sunDir.set(-1, 0, 0);
        this.sunDir.rotateZ(Math.toRadians(sunAngle));
        this.sunDir.rotateX(sunRoll);
    }

    public void setSunAngle(float angle) {
        this.sunAngle = angle;
        updateSunDir();
    }

    public void setSunRoll(float roll) {
        this.sunRoll = Math.toRadians(roll);
        updateSunDir();
    }

    public Vector3f getSunDirection() {
        return sunDir;
    }

    public abstract int bind(Shader shader, int index);

    public abstract void unbind(int index);

    public void free() {}
}
