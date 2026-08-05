package cinnamon.gui;

import cinnamon.Client;
import cinnamon.input.InputManager;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.Resource;
import org.joml.Math;

public class Twinkle {

    public static final Resource TWINKLE_TEX = new Resource("textures/gui/twinkle.png");

    protected final TwinkleParticle[] particles;

    protected int lastMouseX, lastMouseY;

    public Twinkle(int maxParticleCount, int particleLife, float particleSize) {
        this.particles = new TwinkleParticle[maxParticleCount];
        for (int i = 0; i < maxParticleCount; i++)
            particles[i] = new TwinkleParticle(particleLife, particleSize);

        this.lastMouseX = InputManager.getMouseX();
        this.lastMouseY = InputManager.getMouseY();
    }

    public void tick() {
        //add particle on mouse change
        int mouseX = InputManager.getMouseX();
        int mouseY = InputManager.getMouseY();
        if (mouseX != lastMouseX || mouseY != lastMouseY) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            addParticle();
        }

        //tick particles
        for (int i = 0; i < particles.length; i++)
            particles[i].tick(i);
    }

    public void render(MatrixStack matrices, float delta) {
        for (TwinkleParticle particle : particles)
            particle.render(matrices, delta);
    }

    protected void addParticle() {
        //find first dead particle
        TwinkleParticle particle = null;
        for (TwinkleParticle p : particles) {
            if (p.isDead()) {
                particle = p;
                break;
            }
        }

        //reset the particle
        if (particle != null)
            particle.reset(lastMouseX, lastMouseY);
    }

    public void killAll() {
        for (TwinkleParticle particle : particles)
            particle.kill();
    }

    protected static class TwinkleParticle {
        private final int initialLife;
        private final float size;

        private int life;
        private float x, y, ox, oy;
        private int v = 0;

        public TwinkleParticle(int initialLife, float size) {
            this.initialLife = initialLife;
            this.size = size;
        }

        public void tick(int index) {
            if (isDead())
                return;

            ox = x;
            oy = y;
            x += (index % 5f - 2f) / 5f; //horizontal drift
            y += 1f + (float) Math.random() * 3f; //gravity

            //check if out of bounds
            if (y - size > Client.getInstance().window.getGUIHeight()) {
                life = 0;
                return;
            }

            //update life and animation frame
            life--;
            if (life == initialLife * 2/3 || life == initialLife / 3)
                v++;
        }

        public void render(MatrixStack matrices, float delta) {
            if (isDead())
                return;

            float hs = size * 0.5f;
            float x = Math.floor(Math.lerp(this.ox, this.x, delta) - hs);
            float y = Math.floor(Math.lerp(this.oy, this.y, delta) - hs);
            VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices, x, y, size, size, 0f, v, 1f, 1f, 1, 3), TWINKLE_TEX);
        }

        public boolean isDead() {
            return life <= 0;
        }

        public void reset(float x, float y) {
            this.x = x;
            this.y = y;
            this.life = initialLife;
            this.v = 0;
        }

        public void kill() {
            this.life = 0;
        }
    }
}
