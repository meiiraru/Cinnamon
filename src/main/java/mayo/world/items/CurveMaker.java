package mayo.world.items;

import mayo.Client;
import mayo.gui.Toast;
import mayo.model.GeometryHelper;
import mayo.model.ModelManager;
import mayo.parsers.CurveExporter;
import mayo.render.MatrixStack;
import mayo.render.Model;
import mayo.render.batch.VertexConsumer;
import mayo.text.Text;
import mayo.utils.Curve;
import mayo.utils.Resource;
import mayo.utils.Rotation;
import mayo.world.collisions.Hit;
import mayo.world.entity.Entity;
import mayo.world.terrain.Terrain;
import org.joml.Vector3f;

import java.util.List;

public class CurveMaker extends Item {

    private static final String ID = "Curve Maker";
    private static final Model MODEL = ModelManager.load(new Resource("models/items/curve_maker/curve_maker.obj"));

    private final Curve curve = new Curve.BSpline().loop(true).steps(10);

    public CurveMaker(int stackCount) {
        super(ID, stackCount, MODEL);
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
            GeometryHelper.drawLine(VertexConsumer.MAIN, matrices, a.x, a.y, a.z, b.x, b.y, b.z, 0.1f, color + (0xFF << 24));
        }
    }

    @Override
    public void attack(Entity source) {
        super.attack(source);

        Vector3f pos = getPos(source);
        curve.addPoint(pos.x, pos.y, pos.z);
    }

    @Override
    public void use(Entity source) {
        super.use(source);

        if (!source.getWorld().isClientside())
            return;

        try {
            CurveExporter.exportCurve(curve);
        } catch (Exception e) {
            Toast.addToast(Text.of(e.getMessage()), Client.getInstance().font);
        }
    }

    @Override
    public void unselect(Entity source) {
        super.unselect(source);
        curve.clear();
    }
}
