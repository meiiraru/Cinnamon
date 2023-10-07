package mayo.world.items;

import mayo.Client;
import mayo.model.ModelManager;
import mayo.render.MatrixStack;
import mayo.render.Model;
import mayo.utils.*;
import mayo.world.World;
import mayo.world.entity.Entity;
import mayo.world.particle.Particle;
import mayo.world.particle.SquareParticle;
import org.joml.Vector3f;

public class MagicWand extends Item {

    private static final String ID = "Magic Wand";
    private static final Model MODEL = ModelManager.load(new Resource("models/items/magic_wand/magic_wand.obj"));

    //properties :D
    private static final float DISTANCE = 3f;
    private static final float STEP = 1 / 4f;
    private static final int LIFETIME = Integer.MAX_VALUE;
    private static final float ERASER_RANGE = 0.25f;

    private Vector3f lastPos;

    public MagicWand(int stackCount) {
        super(ID, stackCount, MODEL);
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

        //catch
        if (lastPos == null)
            lastPos = pos;

        //draw line
        drawLine(lastPos, pos, source.getWorld());

        //save pos
        lastPos.set(pos);
    }

    @Override
    public void stopAttacking() {
        super.stopAttacking();

        //clear last pos
        lastPos = null;
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
        return source.getLookDir().mul(DISTANCE).add(source.getEyePos());
    }

    private static void drawLine(Vector3f a, Vector3f b, World world) {
        //rainbow color
        int color = ColorUtils.rgbToInt(ColorUtils.hsvToRGB(new Vector3f(((Client.getInstance().ticks * 3) % 360) / 360f, 0.7f, 1))) + (0xFF << 24);
        //draw line
        float len = b.sub(a, new Vector3f()).length();
        for (float i = 0; i < len; i += STEP) {
            //delta
            float d = i / len;
            //spawn particle
            SquareParticle particle = new SquareParticle(LIFETIME, color);
            particle.setPos(Maths.lerp(a, b, d));
            world.addParticle(particle);
        }
    }
}
