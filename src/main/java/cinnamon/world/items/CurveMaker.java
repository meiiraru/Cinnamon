package cinnamon.world.items;

import cinnamon.Client;
import cinnamon.gui.Toast;
import cinnamon.model.GeometryHelper;
import cinnamon.model.ModelManager;
import cinnamon.render.MatrixStack;
import cinnamon.render.Model;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Text;
import cinnamon.utils.Curve;
import cinnamon.utils.Resource;
import cinnamon.utils.Rotation;
import cinnamon.world.world.RollerCoasterWorld;
import cinnamon.world.collisions.Hit;
import cinnamon.world.entity.Entity;
import cinnamon.world.terrain.Terrain;
import org.joml.Vector3f;

import java.util.List;

import static cinnamon.Client.LOGGER;

public class CurveMaker extends CooldownItem {

    private static final String ID = "Curve Maker";
    private static final Model MODEL = ModelManager.load(new Resource("models/items/curve_maker/curve_maker.obj"));

    private final Curve curve = new Curve.BSpline().loop(true).steps(10);

    public CurveMaker(int stackCount, int depleatCooldown, int useCooldown) {
        super(ID, stackCount, MODEL, depleatCooldown, useCooldown);
    }

    @Override
    public void render(ItemRenderContext context, MatrixStack matrices, float delta) {
        boolean changed = context == ItemRenderContext.HUD;

        if (changed) {
            matrices.push();
            matrices.rotate(Rotation.X.rotationDeg(-35));
            matrices.rotate(Rotation.Y.rotationDeg(90));
        }

        super.render(context, matrices, delta);

        if (changed)
            matrices.pop();
    }

    @Override
    public void worldRender(MatrixStack matrices, float delta) {
        super.worldRender(matrices, delta);

        //control points
        float s = 0.25f;
        for (Vector3f vec : curve.getControlPoints())
            VertexConsumer.MAIN.consume(GeometryHelper.cube(matrices, vec.x - s, vec.y - s, vec.z - s, vec.x + s, vec.y + s, vec.z + s, 0xFFFFFFFF));

        //curves
        renderCurve(matrices, curve.getInternalCurve(), 0x8888FF);
        renderCurve(matrices, curve.getExternalCurve(), 0x8888FF);
        renderCurve(matrices, curve.getCurve(), 0xFFFFFF);
    }

    private static Vector3f getPos(Entity source) {
        Hit<Terrain> terrain = source.getLookingTerrain(source.getPickRange());
        if (terrain != null)
            return terrain.pos();

        return source.getLookDir().mul(source.getPickRange()).add(source.getEyePos());
    }

    private static void renderCurve(MatrixStack matrices, List<Vector3f> curve, int color) {
        int size = curve.size();
        for (int i = 0; i < size - 1; i++) {
            Vector3f a = curve.get(i);
            Vector3f b = curve.get(i + 1);
            VertexConsumer.LINES.consume(GeometryHelper.line(matrices, a.x, a.y, a.z, b.x, b.y, b.z, 0f, color + (0xFF << 24)));
        }
    }

    @Override
    public void attack(Entity source) {
        super.attack(source);

        if (!canUse())
            return;

        Vector3f pos = getPos(source);
        curve.addPoint(pos.x, pos.y, pos.z);
        setUseCooldown();
    }

    @Override
    public void use(Entity source) {
        super.use(source);

        if (!canUse() || !(source.getWorld() instanceof RollerCoasterWorld rc))
            return;

        try {
            rc.setCurve(this.curve);
            curve.clear();
        } catch (Exception e) {
            LOGGER.error("", e);
            Toast.addToast(Text.of(e.getMessage()), Client.getInstance().font).type(Toast.ToastType.ERROR);
        }

        setUseCooldown();
    }

    @Override
    public void unselect(Entity source) {
        super.unselect(source);
        curve.clear();
    }
}
