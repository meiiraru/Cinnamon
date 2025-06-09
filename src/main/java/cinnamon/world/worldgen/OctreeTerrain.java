package cinnamon.world.worldgen;

import cinnamon.utils.AABB;
import cinnamon.utils.Maths;
import cinnamon.world.terrain.Terrain;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class OctreeTerrain extends TerrainManager {

    private OctreeNode root;

    public OctreeTerrain(AABB initialBounds) {
        this.root = new OctreeNode(initialBounds);
    }

    @Override
    public void tick() {
        root.tick();
    }

    @Override
    public void insert(Terrain terrain) {
        if (terrain == null)
            return;

        AABB terrainBB = terrain.getAABB();
        while (!root.bounds.isInside(terrainBB))
            growRoot(terrainBB);

        root.insert(terrain);
    }

    @Override
    public void remove(AABB region) {
        root.clearRegion(region);
    }

    public boolean isEmpty() {
        return root.isEmpty();
    }

    @Override
    public void clear() {
        root.clear();
    }

    @Override
    public List<Terrain> query(AABB region) {
        List<Terrain> result = new ArrayList<>();
        root.query(region, result);
        return result;
    }

    @Override
    public List<Terrain> queryCustom(Predicate<AABB> aabbPredicate) {
        List<Terrain> result = new ArrayList<>();
        root.queryCustom(aabbPredicate, result);
        return result;
    }

    @Override
    public List<AABB> getBounds() {
        List<AABB> boundsList = new ArrayList<>();
        collectBounds(root, boundsList);
        return boundsList;
    }

    private void collectBounds(OctreeNode node, List<AABB> boundsList) {
        boundsList.add(node.bounds);
        if (node.children != null)
            for (OctreeNode child : node.children)
                collectBounds(child, boundsList);
    }

    private void growRoot(AABB newBounds) {
        Vector3f min = new Vector3f(
                Math.min(root.bounds.minX(), newBounds.minX()),
                Math.min(root.bounds.minY(), newBounds.minY()),
                Math.min(root.bounds.minZ(), newBounds.minZ())
        );
        Vector3f max = new Vector3f(
                Math.max(root.bounds.maxX(), newBounds.maxX()),
                Math.max(root.bounds.maxY(), newBounds.maxY()),
                Math.max(root.bounds.maxZ(), newBounds.maxZ())
        );

        //make cube bounds for the new root
        Vector3f size = max.sub(min);
        float half = Maths.nextPowerOfTwo(Maths.max(size) * 0.5f);

        Vector3f center = min.add(size.x * 0.5f, size.y * 0.5f, size.z * 0.5f);
        center.floor();

        AABB biggerBounds = new AABB(center, center).inflate(half);

        OctreeNode newRoot = new OctreeNode(biggerBounds);
        newRoot.moveFromNode(root); //move everything into new root
        this.root = newRoot;
    }

    private static class OctreeNode {
        private static final int MAX_CONTENTS = 8;

        private final AABB bounds;
        private final List<Terrain> contents = new ArrayList<>();
        private OctreeNode[] children = null;

        public OctreeNode(AABB bounds) {
            this.bounds = bounds;
        }

        public void tick() {
            for (Terrain terrain : contents)
                terrain.tick();

            if (children != null) {
                for (OctreeNode child : children)
                    child.tick();
            }
        }

        public void insert(Terrain terrain) {
            if (!bounds.intersects(terrain.getAABB()))
                return;

            if (children == null) {
                contents.add(terrain);
                if (contents.size() > MAX_CONTENTS) {
                    subdivide();
                    redistribute();
                }
            } else {
                boolean added = false;

                for (OctreeNode child : children) {
                    if (child.bounds.isInside(terrain.getAABB())) {
                        child.insert(terrain);
                        added = true;
                        break;
                    }
                }

                //it is too big or spans multiple children
                if (!added)
                    contents.add(terrain);
            }
        }

        public void clearRegion(AABB region) {
            if (!bounds.intersects(region))
                return;

            contents.removeIf(terrain -> terrain.getAABB().intersects(region));

            if (children != null)
                for (OctreeNode child : children)
                    child.clearRegion(region);

            //remove children if they are empty
            if (isChildEmpty())
                children = null;
        }

        public boolean isEmpty() {
            return contents.isEmpty() && isChildEmpty();
        }

        private boolean isChildEmpty() {
            if (children != null) {
                for (OctreeNode child : children) {
                    if (!child.isEmpty())
                        return false;
                }
            }

            return true;
        }

        public void clear() {
            contents.clear();

            if (children != null) {
                for (OctreeNode child : children)
                    child.clear();

                children = null;
            }
        }

        public void query(AABB region, List<Terrain> result) {
            if (!bounds.intersects(region))
                return;

            for (Terrain terrain : contents)
                if (terrain.getAABB().intersects(region))
                    result.add(terrain);

            if (children != null)
                for (OctreeNode child : children)
                    child.query(region, result);
        }

        public void queryCustom(Predicate<AABB> aabbPredicate, List<Terrain> result) {
            if (!aabbPredicate.test(bounds))
                return;

            result.addAll(contents);
            if (children != null)
                for (OctreeNode child : children)
                    child.queryCustom(aabbPredicate, result);
        }

        private void subdivide() {
            children = new OctreeNode[8];
            Vector3f center = bounds.getCenter();
            Vector3f size = bounds.getDimensions().mul(0.5f);

            //create 8 children using center +- size
            for (int i = 0; i < 8; i++) {
                //compute min/max based on bitmask
                float minX = center.x + ((i & 1) == 0 ? -size.x : 0);
                float minY = center.y + ((i & 2) == 0 ? -size.y : 0);
                float minZ = center.z + ((i & 4) == 0 ? -size.z : 0);
                children[i] = new OctreeNode(new AABB(minX, minY, minZ, minX + size.x, minY + size.y, minZ + size.z));
            }
        }

        private void redistribute() {
            List<Terrain> oldContents = new ArrayList<>(contents);
            contents.clear();
            for (Terrain terrain : oldContents)
                insert(terrain);
        }

        public void moveFromNode(OctreeNode node) {
            for (Terrain terrain : node.contents)
                insert(terrain);

            if (node.children != null)
                for (OctreeNode child : node.children)
                    moveFromNode(child);
        }
    }
}
