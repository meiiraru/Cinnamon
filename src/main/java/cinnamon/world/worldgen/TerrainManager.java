package cinnamon.world.worldgen;

import cinnamon.math.collision.AABB;
import cinnamon.world.terrain.Terrain;

import java.util.List;
import java.util.function.Predicate;

public abstract class TerrainManager {

    public abstract void tick();

    public abstract boolean insert(Terrain terrain);
    public abstract int remove(AABB region);
    public abstract boolean remove(Terrain terrain);
    public abstract void clear();

    public abstract List<Terrain> query(AABB region);
    public abstract List<Terrain> queryCustom(Predicate<AABB> aabbPredicate);
    public abstract List<AABB> getBounds();
}
