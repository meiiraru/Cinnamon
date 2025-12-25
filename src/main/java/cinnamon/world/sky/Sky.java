package cinnamon.world.sky;

import cinnamon.model.GeometryHelper;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.shader.Shader;
import cinnamon.utils.Resource;
import cinnamon.utils.Rotation;
import org.joml.Math;
import org.joml.Vector3f;

public abstract class Sky {

    public static final Resource SUN = new Resource("textures/environment/sun.png");

    public int fogColor = 0xBFD3DE;
    public float fogStart = 96;
    public float fogEnd = 192;

    public int ambientLight = 0xBFD3DE;

    protected final Vector3f sunDir = new Vector3f(1, 0, 0);
    protected float sunAngle;
    protected float sunRoll = Math.toRadians(30f);
    protected float cloudSpeed = (float) Math.PI / 2f;

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

    public void setCloudSpeed(float cloudSpeed) {
        this.cloudSpeed = cloudSpeed;
        updateSunDir();
    }

    public Vector3f getSunDirection() {
        return sunDir;
    }

    public abstract int bind(Shader shader, int index);

    public abstract void unbind(int index);
}
