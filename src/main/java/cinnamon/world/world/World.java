package cinnamon.world.world;

import cinnamon.Client;
import cinnamon.utils.AABB;
import cinnamon.utils.Resource;
import cinnamon.world.DamageType;
import cinnamon.world.collisions.CollisionDetector;
import cinnamon.world.collisions.CollisionResult;
import cinnamon.world.collisions.Hit;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.worldgen.OctreeTerrain;
import cinnamon.world.worldgen.TerrainManager;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Predicate;

public abstract class World {

    protected static final Resource EXPLOSION_SOUND = new Resource("sounds/world/explosion.ogg");

    protected final Queue<Runnable> scheduledTicks = new LinkedList<>();

    protected final TerrainManager terrainManager = new OctreeTerrain(new AABB().inflate(16));
    protected final Map<UUID, Entity> entities = new HashMap<>();

    public float updateTime = 1f / Client.TPS;
    public float gravity = 0.98f * updateTime;
    public float bottomOfTheWorld = -512f;
    protected long worldTime = 1000;
    protected boolean isPaused;

    public abstract void init();

    public abstract void close();

    public void tick() {
        if (isPaused())
            return;

        worldTime++;

        //run scheduled ticks
        runScheduledTicks();

        //terrain
        terrainManager.tick();

        //entities
        for (Entity e : entities.values())
            e.preTick();

        for (Iterator<Map.Entry<UUID, Entity>> iterator = entities.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<UUID, Entity> entry = iterator.next();
            Entity e = entry.getValue();
            if (e.isRemoved()) {
                iterator.remove();
                entityRemoved(e.getUUID());
            } else {
                e.tick();
            }
        }
    }

    protected void runScheduledTicks() {
        Runnable toRun;
        while ((toRun = scheduledTicks.poll()) != null)
            toRun.run();
    }

    public void addEntity(Entity entity) {
        scheduledTicks.add(() -> {
            this.entities.put(entity.getUUID(), entity);
            entity.onAdded(this);
        });
    }

    public void addTerrain(Terrain terrain) {
        scheduledTicks.add(() -> terrain.onAdded(this));
        terrainManager.insert(terrain);
    }

    public void removeTerrain(Terrain terrain) {
        terrainManager.remove(terrain);
    }

    public void removeTerrain(AABB aabb) {
        terrainManager.remove(aabb);
    }

    public void entityRemoved(UUID uuid) {}

    public List<Entity> getEntities(AABB region) {
        List<Entity> list = new ArrayList<>();
        for (Entity entity : entities.values()) {
            if (region.intersects(entity.getAABB()))
                list.add(entity);
        }
        return list;
    }

    public List<AABB> getTerrainCollisions(AABB region) {
        List<AABB> list = new ArrayList<>();
        for (Terrain terrain : terrainManager.query(region))
            list.addAll(terrain.getPreciseAABB());
        return list;
    }

    public Entity getEntityByUUID(UUID uuid) {
        return entities.get(uuid);
    }

    public void explode(AABB explosionArea, float strength, Entity source, boolean invisible) {
        int damage = (int) (4 * strength);

        for (Entity entity : getEntities(explosionArea)) {
            if (entity == source || entity.isRemoved())
                continue;

            //damage entities
            entity.damage(source, DamageType.EXPLOSION, damage, false);

            //knock back
            if (entity instanceof PhysEntity e) {
                Vector3f dir = explosionArea.getCenter().sub(e.getAABB().getCenter(), new Vector3f()).normalize().mul(-1);
                e.knockback(dir, 0.5f * strength);
            }
        }
    }

    public Hit<Terrain> raycastTerrain(AABB area, Vector3f pos, Vector3f dirLen) {
        //prepare variables
        CollisionResult terrainColl = null;
        Terrain tempTerrain = null;

        //loop through terrain in area
        for (Terrain t : terrainManager.query(area)) {
            //loop through its groups AABBs
            for (AABB aabb : t.getPreciseAABB()) {
                //check for collision
                CollisionResult result = CollisionDetector.collisionRay(aabb, pos, dirLen);
                //store collision if it is closer than previous collision
                if (result != null && (terrainColl == null || result.near() < terrainColl.near())) {
                    terrainColl = result;
                    tempTerrain = t;
                }
            }
        }

        //no collisions
        if (terrainColl == null)
            return null;

        //return terrain collision data
        float d = terrainColl.near();
        return new Hit<>(terrainColl, tempTerrain, new Vector3f(pos).add(dirLen.x * d, dirLen.y * d, dirLen.z * d));
    }

    public Hit<Entity> raycastEntity(AABB area, Vector3f pos, Vector3f dirLen, Predicate<Entity> predicate) {
        //prepare variables
        CollisionResult entityColl = null;
        Entity tempEntity = null;

        //loop through entities in area
        for (Entity e : getEntities(area)) {
            //check for the predicate if the entity is valid
            if (predicate.test(e)) {
                //check for collision
                CollisionResult result = CollisionDetector.collisionRay(e.getAABB(), pos, dirLen);
                //store collision if it is closer than previous collision
                if (result != null && (entityColl == null || result.near() < entityColl.near())) {
                    entityColl = result;
                    tempEntity = e;
                }
            }
        }

        //no collisions
        if (entityColl == null)
            return null;

        //return entity collision data
        float d = entityColl.near();
        return new Hit<>(entityColl, tempEntity, new Vector3f(pos).add(dirLen.x * d, dirLen.y * d, dirLen.z * d));
    }

    public long getTime() {
        return worldTime;
    }

    public long getDay() {
        return worldTime / 24000L;
    }

    public float getTimeOfDayProgress() {
        return ((worldTime + 6000) % 24000) / 24000f;
    }

    public String getTimeOfTheDay() {
        float time = ((worldTime + 6000) % 24000) / 1000f;
        int timeHors = (int) time;
        int timeMinutes = (int) ((time - timeHors) * 60);
        return String.format("%02d:%02d", timeHors, timeMinutes);
    }

    public void setPaused(boolean pause) {
        this.isPaused = pause;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public boolean isClientside() {
        return false;
    }
}
