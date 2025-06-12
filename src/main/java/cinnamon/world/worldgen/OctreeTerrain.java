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
        //tick the root node and all its children
        root.tick();
    }

    @Override
    public void insert(Terrain terrain) {
        //nothing to insert
        if (terrain == null)
            return;

        //if the octree is smaller than the terrain general bounds, grow the root node
        AABB terrainBB = terrain.getAABB();
        while (!root.bounds.isInside(terrainBB))
            growRoot(terrainBB);

        //insert the terrain into the octree
        root.insert(terrain);
    }

    @Override
    public void remove(AABB region) {
        //remove all terrains that intersect with the given region (loosely)
        root.clearRegion(region);
    }

    @Override
    public void remove(Terrain terrain) {
        //remove the terrain from the octree
        root.removeElement(terrain);
    }

    public boolean isEmpty() {
        //check if the root node is empty
        return root.isEmpty();
    }

    @Override
    public void clear() {
        //clear the octree by clearing the root node
        root.clear();
    }

    @Override
    public List<Terrain> query(AABB region) {
        //collect all the terrains that intersect with the given region (loosely)
        List<Terrain> result = new ArrayList<>();
        root.query(region, result);
        return result;
    }

    @Override
    public List<Terrain> queryCustom(Predicate<AABB> aabbPredicate) {
        //collect all the terrains that match the custom predicate of the node bounds
        List<Terrain> result = new ArrayList<>();
        root.queryCustom(aabbPredicate, result);
        return result;
    }

    @Override
    public List<AABB> getBounds() {
        //collect all the bounds from the octree
        List<AABB> boundsList = new ArrayList<>();
        collectBounds(root, boundsList);
        return boundsList;
    }

    private void collectBounds(OctreeNode node, List<AABB> boundsList) {
        //add the boundaries to the list
        boundsList.add(node.bounds);
        if (node.children != null)
            for (OctreeNode child : node.children)
                collectBounds(child, boundsList);
    }

    private void growRoot(AABB newBounds) {
        //grab the new min/max bounds
        Vector3f min = root.bounds.getMin().min(newBounds.getMin());
        Vector3f max = root.bounds.getMax().max(newBounds.getMax());

        //calculate the next power of two from the bounds size
        Vector3f size = max.sub(min);
        float half = Maths.nextPowerOfTwo(Maths.max(size) * 0.5f);

        //create a new AABB that is centered around the old center and has expanded the new size
        Vector3f center = root.bounds.getCenter();
        AABB biggerBounds = new AABB(center, center).inflate(half);

        //create a new root node with the bigger bounds
        //and move the old contents into it
        OctreeNode newRoot = new OctreeNode(biggerBounds);
        newRoot.moveFromNode(root);
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
            //tick terrain
            for (Terrain terrain : contents)
                terrain.tick();

            //tick children
            if (children != null) {
                for (OctreeNode child : children)
                    child.tick();
            }
        }

        public void insert(Terrain terrain) {
            //not on this bounds, skip
            if (!bounds.intersects(terrain.getAABB()))
                return;

            //if we have no children, we can add the terrain directly
            if (children == null) {
                contents.add(terrain);
                //however, if we have too many contents, we subdivide
                if (contents.size() > MAX_CONTENTS) {
                    subdivide();
                    redistribute();
                }
            } else {
                boolean added = false;

                //try to insert into a children if it fits
                for (OctreeNode child : children) {
                    if (child.bounds.isInside(terrain.getAABB())) {
                        child.insert(terrain);
                        added = true;
                        break;
                    }
                }

                //it did not fit, it is too big or spans multiple children
                if (!added)
                    contents.add(terrain);
            }
        }

        public void removeElement(Terrain terrain) {
            if (contents.remove(terrain))
                return;

            if (children != null)
                for (OctreeNode child : children)
                    child.removeElement(terrain);

            //remove children if they are empty
            if (isChildEmpty())
                children = null;
        }

        public void clearRegion(AABB region) {
            //not in region, skip
            if (!bounds.intersects(region))
                return;

            //remove all terrain that intersects with the region
            contents.removeIf(terrain -> terrain.getAABB().intersects(region));

            //including children
            if (children != null)
                for (OctreeNode child : children)
                    child.clearRegion(region);

            //remove children if they are empty
            if (isChildEmpty())
                children = null;
        }

        public boolean isEmpty() {
            //no contents and children all empty
            return contents.isEmpty() && isChildEmpty();
        }

        private boolean isChildEmpty() {
            //we have a children, so check them
            if (children != null) {
                for (OctreeNode child : children) {
                    if (!child.isEmpty())
                        return false;
                }
            }

            //nothing on here!
            return true;
        }

        public void clear() {
            //wipe contents
            contents.clear();

            //wipe children
            if (children != null) {
                for (OctreeNode child : children)
                    child.clear();
                children = null;
            }
        }

        public void query(AABB region, List<Terrain> result) {
            //failed the bounds check, skip
            if (!bounds.intersects(region))
                return;

            //try adding all terrain from this node
            //and check for the terrain bounds
            for (Terrain terrain : contents)
                if (terrain.getAABB().intersects(region))
                    result.add(terrain);

            //add from children
            if (children != null)
                for (OctreeNode child : children)
                    child.query(region, result);
        }

        public void queryCustom(Predicate<AABB> aabbPredicate, List<Terrain> result) {
            //failed the predicate, skip
            if (!aabbPredicate.test(bounds))
                return;

            //success, add all
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
            //readd the contents of this node into itself
            List<Terrain> oldContents = new ArrayList<>(contents);
            contents.clear();
            for (Terrain terrain : oldContents)
                insert(terrain);
        }

        public void moveFromNode(OctreeNode node) {
            //take all contents from the given node and insert them into this node
            for (Terrain terrain : node.contents)
                insert(terrain);

            //including the children
            if (node.children != null)
                for (OctreeNode child : node.children)
                    moveFromNode(child);
        }
    }
}
