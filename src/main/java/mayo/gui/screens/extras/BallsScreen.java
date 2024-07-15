package mayo.gui.screens.extras;

import mayo.Client;
import mayo.gui.ParentedScreen;
import mayo.gui.Screen;
import mayo.model.GeometryHelper;
import mayo.render.MatrixStack;
import mayo.render.batch.VertexConsumer;
import mayo.text.Text;
import mayo.utils.Alignment;
import mayo.utils.Colors;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public class BallsScreen extends ParentedScreen {

    private static final float SECONDS_PER_TICK = 1f / Client.TPS;

    private final List<Ball> balls = new ArrayList<>();

    public BallsScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void tick() {
        super.tick();

        for (Ball ball : balls)
            ball.tick();

        for (Ball ball : balls) {
            for (Ball other : balls) {
                if (ball == other)
                    continue;

                //celestial body gravity
                Vector2f diff = other.pos.sub(ball.pos, new Vector2f());
                float dist = diff.lengthSquared();
                diff.div(dist);
                ball.applyForce(diff);

                //collisions
                if (ball.overlaps(other))
                    ball.uncollide(other);
            }
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        matrices.push();
        matrices.translate(width / 2f, height / 2f, 0f);

        for (Ball ball : balls)
            ball.render(matrices, mouseX, mouseY, delta);

        matrices.pop();

        //draw FPS
        font.render(VertexConsumer.FONT, matrices, width - 4, 4, Text.of(client.fps + " fps"), Alignment.RIGHT);
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta) {
        GeometryHelper.rectangle(
                VertexConsumer.GUI, matrices,
                0, 0,
                width, height,
                -999, 0
        );
    }

    @Override
    public boolean mousePress(int button, int action, int mods) {
        boolean sup = super.mousePress(button, action, mods);
        if (sup) return true;

        if (action == GLFW_PRESS && button == GLFW_MOUSE_BUTTON_1) {
            Ball ball = new Ball(client.window.mouseX - width / 2f, client.window.mouseY - height / 2f, (float) (Math.random() * 5) + 5f, Colors.randomRainbow().rgba);
            balls.add(ball);
            return true;
        }

        return false;
    }

    private static class Ball {
        private final Vector2f
                pos = new Vector2f(),
                vel = new Vector2f(),
                acc = new Vector2f();
        private final float radius;
        private final int color;

        public Ball(float x, float y, float radius, int color) {
            this.pos.set(x, y);
            this.radius = radius;
            this.color = color;
        }

        public void tick() {
            //update physics
            vel.add(acc.x * SECONDS_PER_TICK, acc.y * SECONDS_PER_TICK);
            pos.add(vel.x * SECONDS_PER_TICK, vel.y * SECONDS_PER_TICK);
            acc.set(0f);
        }

        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            GeometryHelper.circle(VertexConsumer.GUI, matrices, pos.x, pos.y, radius, 12, color);
        }

        public void applyForce(Vector2f force) {
            acc.add(force);
        }

        public boolean overlaps(Ball other) {
            return this.pos.distance(other.pos) < this.radius + other.radius;
        }

        public void uncollide(Ball other) {
            Vector2f diff = other.pos.sub(this.pos, new Vector2f());
            float dist = diff.length();
            float radi = this.radius + other.radius;

            float intersection = (radi - dist) / 2f;
            diff.normalize(intersection);

            this.pos.sub(diff);
            other.pos.add(diff);
        }
    }
}
