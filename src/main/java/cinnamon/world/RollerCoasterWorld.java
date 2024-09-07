package cinnamon.world;

import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.Model;
import cinnamon.render.shader.Shader;
import cinnamon.utils.Maths;
import cinnamon.world.entity.living.Player;
import cinnamon.world.entity.vehicle.Cart;
import cinnamon.world.items.CurveMaker;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class RollerCoasterWorld extends WorldClient {

    public Model curve;
    public Vector3f[] curvePath;
    public Cart cart;

    private float t = 0f;

    @Override
    public void tick() {
        super.tick();

        if (cart == null)
            return;

        //tick time
        if (!cart.getRiders().isEmpty()) {
            t += 0.001f;
            if (t >= 1f)
                t = 0f;
        }

        //get min and max index from the curve array
        int min = (int) Math.floor(t * (curvePath.length - 1));
        int max = (int) Math.ceil(t * (curvePath.length - 1));

        //get the new position of the cart
        float delta = (t * (curvePath.length - 1)) - min;
        Vector3f pos = Maths.lerp(curvePath[min], curvePath[max], delta);

        //set the position of the cart
        cart.moveTo(pos);

        //get direction from min and max
        Vector3f dir = new Vector3f(curvePath[max]).sub(curvePath[min]).normalize();
        Vector2f rot = Maths.dirToRot(dir);
        if (!Maths.isNaN(rot))
            cart.rotateTo(rot);
    }

    @Override
    protected void renderWorld(Camera camera, MatrixStack matrices, float delta) {
        super.renderWorld(camera, matrices, delta);

        if (curve != null) {
            Shader.activeShader.applyMatrixStack(matrices);
            curve.render();
        }
    }

    @Override
    public void givePlayerItems(Player player) {
        player.giveItem(new CurveMaker(1, 10, 10));
        super.givePlayerItems(player);
    }
}
