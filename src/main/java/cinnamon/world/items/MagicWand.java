package cinnamon.world.items;

import cinnamon.registry.ItemModelRegistry;
import cinnamon.render.MatrixStack;
import cinnamon.utils.AABB;
import cinnamon.utils.ColorUtils;
import cinnamon.utils.Maths;
import cinnamon.utils.Rotation;
import cinnamon.world.collisions.Hit;
import cinnamon.world.entity.Entity;
import cinnamon.world.particle.Particle;
import cinnamon.world.particle.SquareParticle;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.world.World;
import org.joml.Vector3f;

public class MagicWand extends Item {

    //properties :D
    private static final float DISTANCE = 3f;
    private static final float STEP = 1 / 4f;
    private static final int LIFETIME = 6000;
    private static final float ERASER_RANGE = 0.5f;

    private Vector3f lastPos;
    private boolean drawing;

    public MagicWand(int stackCount) {
        super(ItemModelRegistry.MAGIC_WAND.id, stackCount, ItemModelRegistry.MAGIC_WAND.resource);
    }

    @Override
    public void render(ItemRenderContext context, MatrixStack matrices, float delta) {
        boolean change = context == ItemRenderContext.FIRST_PERSON;

        if (change) {
            matrices.push();
            matrices.rotate(Rotation.X.rotationDeg(50));
        }

        super.render(context, matrices, delta);

        if (change)
            matrices.pop();
    }

    @Override
    public void attack(Entity source) {
        super.attack(source);

        //calculate new pos
        Vector3f pos = spawnPos(source);

        //first use
        if (lastPos == null) {
            lastPos = pos;
            return;
        }

        //draw line
        drawing |= drawLine(lastPos, pos, source.getWorld());
        //save pos
        lastPos.set(pos);
    }

    @Override
    public void stopAttacking(Entity source) {
        super.stopAttacking(source);

        //draw a point if it was not drawing
        if (!drawing && lastPos != null)
            drawParticle(lastPos, getColor(source.getWorld().getTime()), source.getWorld());

        //reset
        lastPos = null;
        drawing = false;
    }

    @Override
    public void use(Entity source) {
        super.use(source);

        //get area
        AABB aabb = new AABB().inflate(ERASER_RANGE).translate(spawnPos(source));

        //remove particles in range
        for (Particle particle : source.getWorld().getParticles(aabb)) {
            if (particle instanceof SquareParticle)
                particle.remove();
        }
    }

    private static Vector3f spawnPos(Entity source) {
        Hit<Terrain> terrain = source.getLookingTerrain(DISTANCE);
        if (terrain != null)
            return terrain.pos();

        return source.getLookDir().mul(DISTANCE).add(source.getEyePos());
    }

    private static int getColor(int time) {
        return ColorUtils.rgbToInt(ColorUtils.hsvToRGB(new Vector3f(((time * 3) % 360) / 360f, 0.7f, 1))) + (0xFF << 24);
    }

    private static boolean drawLine(Vector3f a, Vector3f b, World world) {
        boolean ret = false;
        //rainbow color
        int color = getColor(world.getTime());
        //draw line
        float len = (int) (b.distance(a) * 1000) / 1000f;
        for (float i = 0; i < len; i += STEP) {
            ret = true;
            //pos
            float d = i / len;
            Vector3f pos = Maths.lerp(a, b, d);
            //draw particle
            drawParticle(pos, color, world);
        }
        return ret;
    }

    private static void drawParticle(Vector3f pos, int color, World world) {
        SquareParticle particle = new SquareParticle(LIFETIME, color);
        particle.setPos(pos);
        particle.setEmissive(true);
        world.addParticle(particle);
    }
}
