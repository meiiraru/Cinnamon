package cinnamon.world;

import cinnamon.Client;
import cinnamon.sound.SoundCategory;
import cinnamon.sound.SoundInstance;
import cinnamon.utils.AABB;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import cinnamon.world.collisions.CollisionDetector;
import cinnamon.world.collisions.CollisionResult;
import cinnamon.world.collisions.Hit;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import cinnamon.world.particle.ExplosionParticle;
import cinnamon.world.particle.Particle;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.worldgen.Chunk;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;
import java.util.function.Predicate;

public abstract class World {

    protected static final Resource EXPLOSION_SOUND = new Resource("sounds/explosion.ogg");

    protected final Queue<Runnable> scheduledTicks = new LinkedList<>();

    protected final Map<Vector3i, Chunk> chunks = new HashMap<>();
    protected final Map<UUID, Entity> entities = new HashMap<>();
    protected final List<Particle> particles = new ArrayList<>();

    public final float updateTime = 1f / Client.TPS;
    public final float gravity = 0.98f * updateTime;

    public final int renderDistance = 5;
    protected int timeOfTheDay = 0;

    public abstract void init();

    public abstract void close();

    public void tick() {
        timeOfTheDay++;

        //run scheduled ticks
        runScheduledTicks();

        //terrain
        for (Chunk chunk : chunks.values())
            chunk.tick();

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

    public Vector3i getChunkGridPos(Vector3f worldPos) {
        return getChunkGridPos(worldPos.x, worldPos.y, worldPos.z);
    }

    public Vector3i getChunkGridPos(float x, float y, float z) {
        return new Vector3i(
                (int) Math.floor(x / Chunk.CHUNK_SIZE),
                (int) Math.floor(y / Chunk.CHUNK_SIZE),
                (int) Math.floor(z / Chunk.CHUNK_SIZE)
        );
    }

    public void addChunk(Chunk chunk) {
        this.chunks.put(chunk.getGridPos(), chunk);
        chunk.onAdded(this);
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

    public void setTerrain(Terrain terrain, Vector3i pos) {
        setTerrain(terrain, pos.x, pos.y, pos.z);
    }

    public void setTerrain(Terrain terrain, int x, int y, int z) {
        Vector3i cPos = getChunkGridPos(x, y, z);
        Vector3i tPos = new Vector3i(
                x - cPos.x * Chunk.CHUNK_SIZE,
                y - cPos.y * Chunk.CHUNK_SIZE,
                z - cPos.z * Chunk.CHUNK_SIZE
        );

        Chunk c = chunks.get(cPos);
        if (c == null) {
            c = new Chunk(cPos);
            addChunk(c);
        }

        c.setTerrain(terrain, tPos);
        if (terrain != null)
            scheduledTicks.add(() -> terrain.onAdded(this));
    }

    public SoundInstance playSound(Resource sound, SoundCategory category, Vector3f position) {
        return Client.getInstance().soundManager.playSound(sound, category, position);
    }

    public void entityRemoved(UUID uuid) {}

    public int entityCount() {
        return entities.size();
    }

    public int chunkCount() {
        return chunks.size();
    }

    public int particleCount() {
        return particles.size();
    }

    public Chunk getChunk(Vector3i pos) {
        return chunks.get(pos);
    }

    public Chunk getChunk(int x, int y, int z) {
        return getChunk(new Vector3i(x, y, z));
    }

    public List<Chunk> getChunks(AABB region) {
        List<Chunk> list = new ArrayList<>();

        Vector3i min = getChunkGridPos(region.getMin());
        Vector3i max = getChunkGridPos(region.getMax());

        for (int x = min.x; x <= max.x; x++) {
            for (int y = min.y; y <= max.y; y++) {
                for (int z = min.z; z <= max.z; z++) {
                    Chunk chunk = getChunk(x, y, z);
                    if (chunk != null)
                        list.add(chunk);
                }
            }
        }

        return list;
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

        for (Chunk chunk : getChunks(region)) {
            Vector3i pos = chunk.getGridPos();
            AABB aabb = new AABB(region).translate(-pos.x * Chunk.CHUNK_SIZE, -pos.y * Chunk.CHUNK_SIZE, -pos.z * Chunk.CHUNK_SIZE);
            list.addAll(chunk.getTerrainInArea(aabb));
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
        for (Terrain terrain : getTerrain(region))
            list.addAll(terrain.getGroupsAABB());
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
        playSound(EXPLOSION_SOUND, SoundCategory.ENTITY, pos).maxDistance(64f).volume(0.5f).pitch(Maths.range(0.8f, 1.2f));
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

    public boolean isClientside() {
        return false;
    }
}
