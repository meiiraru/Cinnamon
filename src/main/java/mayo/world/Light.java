package mayo.world;

import mayo.render.shader.Shader;
import org.joml.Vector3f;

public class Light {

    private final Vector3f pos = new Vector3f();
    private float brightness = 32f;
    private int color = -1;

    public void pushToShader(Shader shader, int index) {
        shader.setVec3("lights[" + index + "].pos", pos);
        shader.setColor("lights[" + index + "].color", color);
        shader.setFloat("lights[" + index + "].range", brightness);
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
