package cinnamon.world.world;

import cinnamon.parsers.CurveToMesh;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.model.ModelRenderer;
import cinnamon.render.model.ObjRenderer;
import cinnamon.utils.Curve;
import cinnamon.utils.Maths;
import cinnamon.world.entity.living.Player;
import cinnamon.world.entity.vehicle.Cart;
import cinnamon.world.items.CurveMaker;
import cinnamon.world.light.Spotlight;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.UUID;

public class RollerCoasterWorld extends WorldClient {

    private ModelRenderer model;
    private Vector3f[] path;
    private Cart cart;

    private float t, toAdd;
    private int p0, p1;

    private float speed = 0.3f;

    @Override
    protected void tempLoad() {
        super.tempLoad();
        addLight(new Spotlight().pos(0f, 5f, 0f).color(0xAD72FF));
    }

    @Override
    public void tick() {
        super.tick();

        if (cart == null)
            return;

        boolean hasRider = !cart.getRiders().isEmpty();

        if (hasRider) {
            t += toAdd;
            cart.setMotion(0f, 0f, 1f);
        } else {
            cart.setMotion(0f, 0f, 0f);
        }

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

        //pos
        cart.moveTo(Maths.lerp(path[p0], path[p1], t));

        //rot
        Vector3f p00 = p0 - 1 < 0 ? path[path.length - 2] : path[p0 - 1]; //last entry is the same as the first
        Vector3f p0Dir = path[p0].sub(p00, new Vector3f()).normalize();
        Vector3f p1Dir = path[p1].sub(path[p0], new Vector3f()).normalize();
        cart.rotateToWithRiders(Maths.dirToRot(Maths.lerp(p0Dir, p1Dir, t)));

        //speed
        speed = Math.lerp(speed, Math.max(0.3f + 0.3f * Math.max(cart.getRot().x, -22.5f) / 45f, 0.01f), 0.1f);
    }

    public void setCurve(Curve curve) throws Exception {
        //set model and path
        model = new ObjRenderer(CurveToMesh.generateMesh(curve, false, true));
        path = curve.getCurve().toArray(new Vector3f[0]);

        //set cart
        if (cart != null)
            cart.remove();
        cart = new Cart(UUID.randomUUID());
        cart.setRailed(true);
        addEntity(cart);

        cart.moveTo(path[0]);
        cart.rotateTo(Maths.dirToRot(path[1 % path.length].sub(path[0], new Vector3f()).normalize()));

        t = 1f;
        toAdd = 0f;
        p0 = path.length - 1;
        p1 = 0;
    }

    @Override
    public int renderTerrain(Camera camera, MatrixStack matrices, float delta) {
        int count = super.renderTerrain(camera, matrices, delta);

        if (model != null) {
            model.render(matrices);
            count++;
        }

        return count;
    }

    @Override
    public void givePlayerItems(Player player) {
        player.giveItem(new CurveMaker(1));
        super.givePlayerItems(player);
    }
}
