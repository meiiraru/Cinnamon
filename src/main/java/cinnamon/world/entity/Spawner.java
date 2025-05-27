package cinnamon.world.entity;

import cinnamon.Client;
import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.registry.EntityRegistry;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.AABB;
import cinnamon.utils.Rotation;
import cinnamon.utils.UIHelper;

import java.util.UUID;
import java.util.function.Supplier;

public class Spawner extends Entity {

    private final int delay;
    private final Supplier<Entity> entitySupplier;

    private int time;
    private Entity entity;

    public Spawner(UUID uuid, int delay, Supplier<Entity> entitySupplier) {
        super(uuid, null);
        this.delay = delay;
        this.entitySupplier = entitySupplier;
    }

    @Override
    public void tick() {
        super.tick();

        if ((entity == null || entity.isRemoved()) && time-- <= 0) {
            time = delay;
            entity = entitySupplier.get();
            entity.setPos(getPos());
            entity.setRot(getRot());
            getWorld().addEntity(entity);
        }
    }

    @Override
    protected void renderModel(MatrixStack matrices, float delta) {
        //no model to render
    }

    @Override
    protected void renderFeatures(MatrixStack matrices, float delta) {
        super.renderFeatures(matrices, delta);

        if (entity != null && !entity.isRemoved())
            return;

        matrices.pushMatrix();
        matrices.scale(1f, -1f, 1f);
        matrices.rotate(Rotation.Y.rotationDeg(180f));
        Client.getInstance().camera.billboard(matrices);

        Vertex[] vertices = GeometryHelper.circle(matrices, 0, 0, 0.15f, 1, 16, 0x88000000);
        VertexConsumer.MAIN.consume(vertices);

        matrices.translate(0, 0, UIHelper.getDepthOffset());

        vertices = GeometryHelper.circle(matrices, 0, 0, 0.15f * 0.85f, 1f - (float) time / delay, 16, 0xAAFFFFFF);
        VertexConsumer.MAIN.consume(vertices);

        matrices.popMatrix();
    }

    @Override
    protected void updateAABB() {
        this.aabb = new AABB().translate(getPos()).inflate(0.5f);
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.SPAWNER;
    }
}
