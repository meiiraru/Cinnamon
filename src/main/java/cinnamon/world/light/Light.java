package cinnamon.world.light;

import cinnamon.render.shader.Shader;
import org.joml.Vector3f;

public class Light {

    private final Vector3f
            pos = new Vector3f(),
            attenuation = new Vector3f(1f, 4.5f / 16f, 75 / (16f * 16f));
    private float brightness = 16f;
    private int color = -1;

    public void pushToShader(Shader shader, int index) {
        String prefix = "lights[" + index + "].";
        pushToShader(shader, prefix);
    }

    protected void pushToShader(Shader shader, String prefix) {
        shader.setVec3(prefix + "pos", pos);
        shader.setColor(prefix + "color", color);
        shader.setVec3(prefix + "attenuation", attenuation);

        shader.setBool(prefix + "directional", this instanceof DirectionalLight);
        shader.setBool(prefix + "spotlight", this instanceof Spotlight);
    }

    public Vector3f getPos() {
        return pos;
    }

    public Light pos(Vector3f pos) {
        return pos(pos.x, pos.y, pos.z);
    }

    public Light pos(float x, float y, float z) {
        this.pos.set(x, y, z);
        return this;
    }

    public float getBrightness() {
        return brightness;
    }

    public Vector3f getAttenuation() {
        return attenuation;
    }

    public Light brightness(float brightness) {
        return this.brightness(brightness, 1f, 4.5f, 75f);
    }

    public Light brightness(float brightness, float constant, float linear, float quadratic) {
        this.brightness = brightness;
        this.attenuation.set(
                constant,
                linear / brightness,
                quadratic / (brightness * brightness)
        );
        return this;
    }

    public int getColor() {
        return color;
    }

    public Light color(int color) {
        this.color = color;
        return this;
    }
}
