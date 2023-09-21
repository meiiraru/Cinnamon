package mayo.world.entity;

import mayo.model.ModelManager;
import mayo.model.obj.Mesh;
import mayo.utils.Resource;
import mayo.world.World;
import org.joml.Vector3f;

public class Player extends LivingEntity {

    private static final Mesh PLAYER_MODEL = ModelManager.load(new Resource("models/player/player.obj"));
    private static final Vector3f DIMENSIONS = PLAYER_MODEL.getBBMax().sub(PLAYER_MODEL.getBBMin(), new Vector3f());
    private static final int MAX_HEALTH = 100;
    private static final int INVULNERABILITY_TIME = 20;

    private int invulnerability = 0;

    public Player(World world) {
        super(PLAYER_MODEL, world, DIMENSIONS, MAX_HEALTH);
    }

    @Override
    public void tick() {
        super.tick();

        if (invulnerability > 0)
            invulnerability--;
    }

    @Override
    public boolean shouldRenderText() {
        return false;
    }

    @Override
    public void damage(int amount) {
        if (invulnerability > 0)
            return;

        super.damage(amount);
        this.invulnerability = INVULNERABILITY_TIME;
    }
}
