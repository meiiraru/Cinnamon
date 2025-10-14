package cinnamon.world.world;

import cinnamon.Client;
import cinnamon.sound.SoundCategory;
import cinnamon.sound.SoundInstance;
import cinnamon.sound.SoundManager;
import cinnamon.utils.AABB;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import cinnamon.world.DamageType;
import cinnamon.world.collisions.CollisionDetector;
import cinnamon.world.collisions.CollisionResult;
import cinnamon.world.collisions.Hit;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import cinnamon.world.particle.ExplosionParticle;
import cinnamon.world.particle.Particle;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.worldgen.OctreeTerrain;
import cinnamon.world.worldgen.TerrainManager;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Predicate;

public abstract class World {

    protected static final Resource EXPLOSION_SOUND = new Resource("sounds/world/explosion.ogg");

    protected final Queue<Runnable> scheduledTicks = new LinkedList<>();

    protected final TerrainManager terrainManager = new OctreeTerrain(new AABB().inflate(16));
    protected final Map<UUID, Entity> entities = new HashMap<>();
    protected final List<Particle> particles = new ArrayList<>();

    public final float updateTime = 1f / Client.TPS;
    public final float gravity = 0.98f * updateTime;
    public final float bottomOfTheWorld = -512f;
    protected int worldTime = 1000;

    public abstract void init();

    public abstract void close();

    public void tick() {
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
            e.tick();
            if (e.isRemoved()) {
                iterator.remove();
                entityRemoved(e.getUUID());
            }
        }

        //particles
        for (Iterator<Particle> iterator = particles.iterator(); iterator.hasNext(); ) {
            Particle p = iterator.next();
            p.tick();
            if (p.isRemoved())
                iterator.remove();
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

    public void addParticle(Particle particle) {
        scheduledTicks.add(() -> {
            this.particles.add(particle);
            particle.onAdded(this);
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

    public SoundInstance playSound(Resource sound, SoundCategory category, Vector3f position) {
        return SoundManager.playSound(sound, category, position);
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

    public List<Particle> getParticles(AABB region) {
        List<Particle> list = new ArrayList<>();
        for (Particle particle : this.particles) {
            if (region.intersects(particle.getAABB()))
                list.add(particle);
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

    public void explode(Vector3f pos, float range, float strength, Entity source, boolean invisible) {
        AABB explosionBB = new AABB().inflate(range).translate(pos);
        int damage = (int) (4 * strength);

        for (Entity entity : getEntities(explosionBB)) {
            if (entity == source || entity.isRemoved())
                continue;

            //damage entities
            entity.damage(source, DamageType.EXPLOSION, damage, false);

            //knock back
            if (entity instanceof PhysEntity e) {
                Vector3f dir = explosionBB.getCenter().sub(e.getAABB().getCenter(), new Vector3f()).normalize().mul(-1);
                e.knockback(dir, 0.5f * strength);
            }
        }

        if (invisible)
            return;

        //particles
        for (int i = 0; i < 30 * range; i++) {
            ExplosionParticle particle = new ExplosionParticle((int) (Math.random() * 10) + 15);
            particle.setPos(explosionBB.getRandomPoint());
            particle.setScale(5f);
            addParticle(particle);
        }

        //sound
        playSound(EXPLOSION_SOUND, SoundCategory.ENTITY, pos).maxDistance(64f).volume(0.5f).pitch(Maths.range(0.8f, 1.2f));
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

    public int getTime() {
        return worldTime;
    }

    public int getDay() {
        return worldTime / 24000;
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

    public boolean isClientside() {
        return false;
    }
}
