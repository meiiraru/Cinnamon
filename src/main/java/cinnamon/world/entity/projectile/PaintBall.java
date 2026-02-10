package cinnamon.world.entity.projectile;

import cinnamon.registry.EntityModelRegistry;
import cinnamon.registry.EntityRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.shader.Shader;
import cinnamon.utils.ColorUtils;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import cinnamon.world.Decal;
import cinnamon.world.collisions.CollisionResolver;
import cinnamon.world.collisions.CollisionResult;
import cinnamon.world.world.WorldClient;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.UUID;

public class PaintBall extends Projectile {

    public static final Resource DECAL_RESOURCE = new Resource("textures/misc/paint_splatter.png");

    public static final int DAMAGE = 0;
    public static final int LIFETIME = 15;
    public static final float SPEED = 5f;

    private final int color;

    public PaintBall(UUID uuid, UUID owner, int color) {
        super(uuid, EntityModelRegistry.PAINT_BALL.resource, DAMAGE, LIFETIME, SPEED, 0f, owner);
        this.color = color;
    }

    @Override
    protected void renderModel(Camera camera, MatrixStack matrices, float delta) {
        Shader.activeShader.applyColorRGBA(color);
        super.renderModel(camera, matrices, delta);
        Shader.activeShader.applyColorRGBA(0xFFFFFFFF);
    }

    @Override
    protected void motionFallout() {
        //no fallout
    }

    @Override
    protected void resolveCollision(CollisionResult collision, Vector3f totalMove) {
        CollisionResolver.stick(collision, getMotion(), totalMove);

        if (!getWorld().isClientside()) {
            remove();
            return;
        }

        Decal decal = new Decal(300, DECAL_RESOURCE);
        decal.getTransform().setColor(ColorUtils.argbIntToRGBA(color));

        Vector3f pos = getPos();
        decal.getTransform().setPos(pos.x + totalMove.x, pos.y + totalMove.y, pos.z + totalMove.z);

        decal.getTransform().setScale(0.2f);

        decal.getTransform().setRot(Maths.dirToQuat(collision.normal()).rotateZ((float) Math.random() * Math.PI_TIMES_2_f));

        ((WorldClient) getWorld()).addDecal(decal);
        remove();
    }

    @Override
    protected void confetti() {
        //no confetti
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.PAINT_BALL;
    }
}
