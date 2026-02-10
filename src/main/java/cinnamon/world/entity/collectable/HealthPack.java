package cinnamon.world.entity.collectable;

import cinnamon.registry.EntityModelRegistry;
import cinnamon.registry.EntityRegistry;
import cinnamon.utils.AABB;
import cinnamon.world.entity.PhysEntity;
import cinnamon.world.entity.living.Player;
import cinnamon.world.particle.SteamParticle;
import cinnamon.world.world.WorldClient;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.UUID;

public class HealthPack extends Collectable {

    private static final int HEAL = 10;
    private static final float SMOKE_CHANCE = 0.05f;

    public HealthPack(UUID uuid) {
        super(uuid, EntityModelRegistry.HEALTH_PACK.resource);
    }

    @Override
    public void tick() {
        super.tick();

        if (getWorld().isClientside() && Math.random() < SMOKE_CHANCE) {
            SteamParticle p = new SteamParticle((int) (Math.random() * 5) + 15, 0xFFDDDDDD);

            AABB aabb = new AABB(this.aabb);
            aabb.inflate(-0.3f, 0, -0.3f);
            Vector3f pos = aabb.getRandomPoint();
            pos.y = this.pos.y + aabb.getHeight();

            p.setPos(pos);
            p.setScale(2f);

            ((WorldClient) getWorld()).addParticle(p);
        }
    }

    @Override
    protected boolean onPickUp(PhysEntity entity) {
        return entity instanceof Player p && p.heal(HEAL);
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.HEALTH_PACK;
    }
}
