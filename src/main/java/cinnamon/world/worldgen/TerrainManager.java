package cinnamon.world.worldgen;

import cinnamon.utils.AABB;
import cinnamon.world.terrain.Terrain;

import java.util.List;
import java.util.function.Predicate;

public abstract class TerrainManager {

    public abstract void tick();

    public abstract void insert(Terrain terrain);
    public abstract void remove(AABB region);
    public abstract void clear();

    public abstract List<Terrain> query(AABB region);
    public abstract List<Terrain> queryCustom(Predicate<AABB> aabbPredicate);
    public abstract List<AABB> getBounds();
}
