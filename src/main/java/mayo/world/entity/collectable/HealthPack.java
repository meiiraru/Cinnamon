package mayo.world.entity.collectable;

import mayo.registry.EntityModelRegistry;
import mayo.registry.EntityRegistry;
import mayo.utils.AABB;
import mayo.world.entity.Entity;
import mayo.world.entity.living.Player;
import mayo.world.particle.SteamParticle;
import org.joml.Vector3f;

import java.util.UUID;

public class HealthPack extends Collectable {

    private static final int HEAL = 10;
    private static final float SMOKE_CHANCE = 0.05f;

    public HealthPack(UUID uuid) {
        super(uuid, EntityModelRegistry.HEALTH_PACK.model);
    }

    @Override
    public void tick() {
        super.tick();

        if (Math.random() < SMOKE_CHANCE) {
            SteamParticle p = new SteamParticle((int) (Math.random() * 5) + 15, 0xFFDDDDDD);

            AABB aabb = new AABB(this.aabb);
            aabb.inflate(-0.3f, 0, -0.3f);
            Vector3f pos = aabb.getRandomPoint();
            pos.y = this.pos.y + aabb.getHeight();

            p.setPos(pos);
            p.setScale(2f);
            p.setEmissive(true);

            world.addParticle(p);
        }
    }

    @Override
    protected boolean onPickUp(Entity entity) {
        return entity instanceof Player p && p.heal(HEAL);
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.HEALTH_PACK;
    }
}
