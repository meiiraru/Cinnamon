package cinnamon.world;

import cinnamon.parsers.CurveToMesh;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.Model;
import cinnamon.render.shader.Shader;
import cinnamon.utils.Curve;
import cinnamon.utils.Maths;
import cinnamon.world.entity.living.Player;
import cinnamon.world.entity.vehicle.Cart;
import cinnamon.world.items.CurveMaker;
import org.joml.Vector3f;

import java.util.UUID;

public class RollerCoasterWorld extends WorldClient {

    private Model model;
    private Vector3f[] path;
    private Cart cart;

    private float t, toAdd;
    private int p0, p1;

    private float speed = 0.3f;

    @Override
    public void tick() {
        super.tick();

        if (cart == null)
            return;

        if (!cart.getRiders().isEmpty())
            t += toAdd;

        while (t >= 1f) {
            t--;
            float oldDistance = path[p0].distance(path[p1]);

            p0 = p1;
            p1 = (p1 + 1) % path.length;

            if (p1 == p0)
                continue;

            float distance = path[p0].distance(path[p1]);
            while (distance == 0f) {
                p0 = p1;
                p1 = (p1 + 1) % path.length;
                distance = path[p0].distance(path[p1]);
            }

            toAdd = speed / distance;
            t = Maths.map(t, 0f, 1f, 0f, oldDistance / distance);
        }

        cart.moveTo(Maths.lerp(path[p0], path[p1], t));
        cart.rotateTo(Maths.dirToRot(path[p1].sub(path[p0], new Vector3f())));
    }

    public void setCurve(Curve curve) throws Exception {
        //set model and path
        model = new Model(CurveToMesh.generateMesh(curve, false));
        path = curve.getCurve().toArray(new Vector3f[0]);

        //set cart
        if (cart != null)
            cart.remove();
        cart = new Cart(UUID.randomUUID());
        cart.setRailed(true);
        addEntity(cart);

        cart.moveTo(path[0]);
        cart.rotateTo(Maths.dirToRot(path[1 % path.length].sub(path[0], new Vector3f())));

        t = 1f;
        toAdd = 0f;
        p0 = path.length - 1;
        p1 = 0;
    }

    @Override
    protected void renderWorld(Camera camera, MatrixStack matrices, float delta) {
        super.renderWorld(camera, matrices, delta);

        if (model != null) {
            Shader.activeShader.applyMatrixStack(matrices);
            model.render();
        }
    }

    @Override
    public void givePlayerItems(Player player) {
        player.giveItem(new CurveMaker(1, 10, 10));
        super.givePlayerItems(player);
    }
}
