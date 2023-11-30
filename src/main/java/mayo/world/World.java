package mayo.world;

import mayo.Client;
import mayo.sound.SoundCategory;
import mayo.sound.SoundSource;
import mayo.utils.AABB;
import mayo.utils.Resource;
import mayo.world.collisions.CollisionDetector;
import mayo.world.collisions.CollisionResult;
import mayo.world.collisions.Hit;
import mayo.world.entity.Entity;
import mayo.world.entity.PhysEntity;
import mayo.world.particle.ExplosionParticle;
import mayo.world.particle.Particle;
import mayo.world.terrain.Terrain;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Predicate;

public abstract class World {

    protected static final Resource EXPLOSION_SOUND = new Resource("sounds/explosion.ogg");

    protected final Queue<Runnable> scheduledTicks = new LinkedList<>();

    protected final List<Terrain> terrain = new ArrayList<>();
    protected final Map<UUID, Entity> entities = new HashMap<>();
    protected final List<Particle> particles = new ArrayList<>();

    public final float updateTime = 0.05f; // 1/20
    public final float gravity = 0.98f * updateTime;

    public final float renderDistance = 3;
    protected int timeOfTheDay = 0;

    public abstract void init();

    public abstract void close();

    public void tick() {
        timeOfTheDay++;

        //run scheduled ticks
        runScheduledTicks();

        //terrain
        for (Terrain terrain : terrain)
            terrain.tick();

        //entities
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

    public void addTerrain(Terrain terrain) {
        scheduledTicks.add(() -> {
            this.terrain.add(terrain);
            terrain.onAdded(this);
        });
    }

    public void addEntity(Entity entity) {
        scheduledTicks.add(() -> {
            this.entities.put(entity.getUUID(), entity);
            entity.onAdded(this);
        });
    }

    public void addParticle(Particle particle) {
        if (particle.shouldRender())
            scheduledTicks.add(() -> {
                this.particles.add(particle);
                particle.onAdded(this);
            });
    }

    public SoundSource playSound(Resource sound, SoundCategory category, Vector3f position) {
        return Client.getInstance().soundManager.playSound(sound, category, position);
    }

    public void entityRemoved(UUID uuid) {}

    public int entityCount() {
        return entities.size();
    }

    public int terrainCount() {
        return terrain.size();
    }

    public int particleCount() {
        return particles.size();
    }

    public List<Entity> getEntities(AABB region) {
        List<Entity> list = new ArrayList<>();
        for (Entity entity : entities.values()) {
            if (region.intersects(entity.getAABB()))
                list.add(entity);
        }
        return list;
    }

    public List<Terrain> getTerrain(AABB region) {
        List<Terrain> list = new ArrayList<>();
        for (Terrain terrain : this.terrain) {
            if (region.intersects(terrain.getAABB()))
                list.add(terrain);
        }
        return list;
    }

    public List<Particle> getParticles(AABB region) {
        List<Particle> list = new ArrayList<>();
        for (Particle particle : this.particles) {
            if (region.isInside(particle.getPos()))
                list.add(particle);
        }
        return list;
    }

    public List<AABB> getTerrainCollisions(AABB region) {
        List<AABB> list = new ArrayList<>();
        for (Terrain terrain : this.terrain) {
            if (region.intersects(terrain.getAABB()))
                list.addAll(terrain.getGroupsAABB());
        }
        return list;
    }

    public Entity getEntityByUUID(UUID uuid) {
        return entities.get(uuid);
    }

    public void explode(Vector3f pos, float range, float strength, Entity source) {
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

        //particles
        for (int i = 0; i < 30 * range; i++) {
            ExplosionParticle particle = new ExplosionParticle((int) (Math.random() * 10) + 15);
            particle.setPos(explosionBB.getRandomPoint());
            particle.setScale(5f);
            addParticle(particle);
        }

        //sound
        playSound(EXPLOSION_SOUND, SoundCategory.ENTITY, pos).maxDistance(64f).volume(0.5f);
    }

    public Hit<Terrain> raycastTerrain(AABB area, Vector3f pos, Vector3f dirLen) {
        //prepare variables
        CollisionResult terrainColl = null;
        Terrain tempTerrain = null;

        //loop through terrain in area
        for (Terrain t : getTerrain(area)) {
            //loop through its groups AABBs
            for (AABB aabb : t.getGroupsAABB()) {
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
        return timeOfTheDay;
    }
}
