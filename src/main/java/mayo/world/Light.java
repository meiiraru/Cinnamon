package mayo.world;

import mayo.render.shader.Shader;
import org.joml.Vector3f;

public class Light {

    private final Vector3f
            pos = new Vector3f(),
            attenuation = new Vector3f(1f, 4.5f / 16f, 75 / (16f * 16f));
    private float brightness = 16f;
    private int color = -1;

    public void pushToShader(Shader shader, int index) {
        shader.setVec3("lights[" + index + "].pos", pos);
        shader.setColor("lights[" + index + "].color", color);
        shader.setVec3("lights[" + index + "].attenuation", attenuation);
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

    public Light brightness(float brightness) {
        this.brightness = brightness;
        this.attenuation.set(
                1f, //constant
                4.5f / brightness, //linear
                75 / (brightness * brightness) //quadratic
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
