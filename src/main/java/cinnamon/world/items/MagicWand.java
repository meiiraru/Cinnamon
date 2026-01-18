package cinnamon.world.items;

import cinnamon.registry.ItemModelRegistry;
import cinnamon.render.MatrixStack;
import cinnamon.utils.AABB;
import cinnamon.utils.ColorUtils;
import cinnamon.utils.Maths;
import cinnamon.utils.Rotation;
import cinnamon.vr.XrManager;
import cinnamon.world.collisions.Hit;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.entity.living.LocalPlayer;
import cinnamon.world.particle.Particle;
import cinnamon.world.particle.SquareParticle;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.world.World;
import cinnamon.world.world.WorldClient;
import org.joml.Vector3f;

public class MagicWand extends Item {

    //properties :D
    private static final float DISTANCE = 3f;
    private static final float STEP = 1 / 4f;
    private static final int LIFETIME = 6000;
    private static final float ERASER_RANGE = 0.25f;

    private Vector3f lastPos;
    private boolean drawing;

    public MagicWand() {
        super(ItemModelRegistry.MAGIC_WAND.id, 1, 1, ItemModelRegistry.MAGIC_WAND.resource);
    }

    @Override
    public Item copy() {
        return new MagicWand();
    }

    @Override
    public void tick() {
        super.tick();

        if (getSource() == null)
            return;

        World sourceWorld = getSource().getWorld();
        if (sourceWorld == null || !sourceWorld.isClientside())
            return;

        if (isFiring()) {
            //draw line
            Vector3f pos = spawnPos(getSource());
            drawing |= drawLine(lastPos, pos, (WorldClient) sourceWorld, getScale(getSource()));
            //save pos
            lastPos.set(pos);
        }

        if (isUsing()) {
            //get area
            AABB aabb = new AABB().inflate(ERASER_RANGE * getScale(getSource())).translate(spawnPos(getSource()));

            //remove particles in range
            for (Particle particle : ((WorldClient) sourceWorld).getParticles(aabb)) {
                if (particle instanceof SquareParticle)
                    particle.remove();
            }
        }
    }

    @Override
    public void render(ItemRenderContext context, MatrixStack matrices, float delta) {
        boolean change = context == ItemRenderContext.FIRST_PERSON;

        if (change) {
            matrices.pushMatrix();
            matrices.rotate(Rotation.X.rotationDeg(50));
        }

        super.render(context, matrices, delta);

        if (change)
            matrices.popMatrix();
    }

    @Override
    public boolean fire() {
        if (!super.fire())
            return false;
        //calculate new pos
        if (getSource().getWorld().isClientside() && (!drawing || lastPos == null))
            lastPos = spawnPos(getSource());
        return true;
    }

    @Override
    public void stopFiring() {
        super.stopFiring();
        World sourceWorld = getSource().getWorld();
        if (sourceWorld == null || !sourceWorld.isClientside())
            return;

        //draw a point if not drawn anything
        if (!drawing && lastPos != null)
            drawParticle(lastPos, getColor(getSource().getWorld().getTime()), (WorldClient) sourceWorld, getScale(getSource()));
        drawing = false;
        lastPos = null;
    }

    private static float getScale(LivingEntity source) {
        if (source instanceof LocalPlayer && XrManager.isInXR())
            return 0.2f;
        return 1f;
    }

    private static Vector3f spawnPos(LivingEntity source) {
        if (source instanceof LocalPlayer && XrManager.isInXR())
            return source.getHandDir(false, 1f).mul(0.25f).add(source.getHandPos(false, 1f));

        Hit<Terrain> terrain = source.getLookingTerrain(DISTANCE);
        if (terrain != null)
            return terrain.pos();

        return source.getLookDir().mul(DISTANCE).add(source.getEyePos());
    }

    private static int getColor(long time) {
        return ColorUtils.rgbToInt(ColorUtils.hsvToRGB(new Vector3f(((time * 3) % 360) / 360f, 0.7f, 1))) + (0xFF << 24);
    }

    private static boolean drawLine(Vector3f a, Vector3f b, WorldClient world, float scale) {
        boolean ret = false;
        //rainbow color
        int color = getColor(world.getTime());
        //draw line
        float len = (int) (b.distance(a) * 1000) / 1000f;
        for (float i = 0; i < len; i += STEP * scale) {
            
            ret = true;
            //pos
            float d = i / len;
            Vector3f pos = Maths.lerp(a, b, d);
            //draw particle
            drawParticle(pos, color, world, scale);
        }
        return ret;
    }

    private static void drawParticle(Vector3f pos, int color, WorldClient world, float scale) {
        SquareParticle particle = new SquareParticle(LIFETIME, color);
        particle.setPos(pos);
        particle.setEmissive(true);
        particle.setScale(scale);
        world.addParticle(particle);
    }

    public Object getCountText() {
        return "";
    }
}
