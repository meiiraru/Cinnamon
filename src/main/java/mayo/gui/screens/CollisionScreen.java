package mayo.gui.screens;

import mayo.Client;
import mayo.gui.ParentedScreen;
import mayo.gui.Screen;
import mayo.model.GeometryHelper;
import mayo.render.MatrixStack;
import mayo.render.batch.VertexConsumer;
import mayo.utils.AABB;
import mayo.utils.UIHelper;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public class CollisionScreen extends ParentedScreen {

    private final AABB aabb;
    private final Vector3f pos;

    public CollisionScreen(Screen parentScreen) {
        super(parentScreen);
        this.aabb = new AABB(100, 100, 0, 120, 120, 1);
        this.pos = new Vector3f();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        //direction
        Vector3f dir = new Vector3f(mouseX - pos.x, mouseY - pos.y, 0.5f);

        //collision
        AABB.CollisionResult collision = aabb.collisionRay(pos, dir);
        boolean hasCollision = collision != null;

        //draw box
        GeometryHelper.rectangle(VertexConsumer.GUI, matrices, aabb.minX(), aabb.minY(), aabb.maxX(), aabb.maxY(), hasCollision ? 0xFFAD72FF : 0xFFFF72AD);

        if (hasCollision) {
            //draw collision cube
            Vector3f pos = collision.pos();
            GeometryHelper.rectangle(VertexConsumer.GUI, matrices, pos.x - 3, pos.y - 3, pos.x + 3, pos.y + 3, 0xFF72ADFF);

            //draw collision normal
            Vector3f normal = collision.normal();
            UIHelper.drawLine(VertexConsumer.GUI, matrices, pos.x, pos.y, pos.x + normal.x * 10, pos.y + normal.y * 10, 1, 0xFFFF7272);
        }

        //draw line
        UIHelper.drawLine(VertexConsumer.GUI, matrices, pos.x, pos.y, mouseX, mouseY, 1, hasCollision ? 0xFFFFFF00 : -1);

        //fully unrelated lol
        int i = (client.ticks / 20) % 12 + 1;
        GeometryHelper.circle(VertexConsumer.LINES, matrices, width / 2f, height / 2f, 22f, i, 0xFF323232);
        float f = ((client.ticks + delta) % 20f) / 20f;
        GeometryHelper.circle(VertexConsumer.LINES, matrices, width / 2f, height / 2f, 20f, f, i, -1);
    }

    @Override
    public void mousePress(int button, int action, int mods) {
        if (action == GLFW_PRESS) {
            int mouseX = Client.getInstance().window.mouseX;
            int mouseY = Client.getInstance().window.mouseY;

            switch (button) {
                case GLFW_MOUSE_BUTTON_1 -> pos.set(mouseX, mouseY, 0);
                case GLFW_MOUSE_BUTTON_2 -> aabb.set(mouseX - 10, mouseY - 10, 0, mouseX + 10, mouseY + 10, 1);
            }
        }

        super.mousePress(button, action, mods);
    }
}
