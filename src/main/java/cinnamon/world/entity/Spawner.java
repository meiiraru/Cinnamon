package cinnamon.world.entity;

import cinnamon.Client;
import cinnamon.gui.DebugScreen;
import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.registry.EntityRegistry;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.Rotation;
import cinnamon.utils.UIHelper;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Spawner<E extends Entity> extends Entity {

    private final int delay;
    private final Supplier<E> entitySupplier;
    private final Predicate<E> respawnPredicate;

    private int time;
    private E entity;

    private boolean renderCooldown = true;

    public Spawner(UUID uuid, int delay, Supplier<E> entitySupplier) {
        this(uuid, delay, entitySupplier, Entity::isRemoved);
    }

    public Spawner(UUID uuid, int delay, Supplier<E> entitySupplier, Predicate<E> respawnPredicate) {
        super(uuid, null);
        this.delay = Math.max(delay, 1);
        this.entitySupplier = entitySupplier;
        this.respawnPredicate = respawnPredicate;
    }

    @Override
    public void tick() {
        super.tick();

        if (checkRespawn() && time-- <= 0) {
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
        if (entity == null && (renderCooldown || DebugScreen.isTabOpen(DebugScreen.Tab.ENTITIES)))
            renderCountdowns(matrices, delta);
    }

    public void renderCountdowns(MatrixStack matrices, float delta) {
        matrices.pushMatrix();
        matrices.translate(getPos(delta));
        matrices.scale(1f, -1f, 1f);
        matrices.rotate(Rotation.Y.rotationDeg(180f));
        Client.getInstance().camera.billboard(matrices);

        Vertex[] vertices = GeometryHelper.circle(matrices, 0, 0, 0.15f, 1, 16, 0x88000000);
        VertexConsumer.WORLD_MAIN.consume(vertices);

        matrices.translate(0, 0, UIHelper.getDepthOffset());

        vertices = GeometryHelper.circle(matrices, 0, 0, 0.15f * 0.85f, 1f - (time - delta + 1) / delay, 16, 0xAAFFFFFF);
        VertexConsumer.WORLD_MAIN.consume(vertices);

        matrices.popMatrix();
    }

    @Override
    protected void updateAABB() {
        this.aabb.set(getPos()).inflate(0.5f);
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.SPAWNER;
    }

    public void setRenderCooldown(boolean renderCooldown) {
        this.renderCooldown = renderCooldown;
    }

    protected boolean checkRespawn() {
        boolean predicate = entity == null || respawnPredicate.test(entity);
        if (predicate) entity = null;
        return predicate;
    }
}
