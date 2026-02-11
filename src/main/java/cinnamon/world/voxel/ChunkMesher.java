package cinnamon.world.voxel;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates optimized chunk meshes using greedy meshing and hidden face culling.
 * <p>
 * Hidden face culling: only emits a face if the adjacent block is air/transparent.
 * Greedy meshing: merges adjacent coplanar faces of the same block type into larger quads,
 * dramatically reducing vertex/triangle count (5-10x on typical terrain).
 * <p>
 * Output vertex format per vertex: px, py, pz, u, v, nx, ny, nz, tx, ty, tz, texLayer (12 floats).
 * This matches the custom gbuffer_voxel shader layout with TBN normal mapping support.
 */
public final class ChunkMesher {

    // Vertex stride in floats: pos(3) + uv(2) + normal(3) + tangent(3) + texLayer(1) = 12
    public static final int VERTEX_FLOATS = 12;

    // 6 face directions: -X, +X, -Y, +Y, -Z, +Z
    private static final int[][] FACE_NORMALS = {
            {-1, 0, 0}, {1, 0, 0},
            {0, -1, 0}, {0, 1, 0},
            {0, 0, -1}, {0, 0, 1}
    };

    // For each face, the two axes that form the face plane (u-axis, v-axis)
    // and which axis is the normal axis
    // Format: [normalAxis, uAxis, vAxis]
    private static final int[][] FACE_AXES = {
            {0, 2, 1}, // -X: normal=X, u=Z, v=Y
            {0, 2, 1}, // +X: normal=X, u=Z, v=Y
            {1, 0, 2}, // -Y: normal=Y, u=X, v=Z
            {1, 0, 2}, // +Y: normal=Y, u=X, v=Z
            {2, 1, 0}, // -Z: normal=Z, u=Y, v=X (swapped so cross(du,dv) matches winding)
            {2, 1, 0}, // +Z: normal=Z, u=Y, v=X
    };

    // Tangent vectors per face — aligned with the texture U axis
    // For axis-aligned voxel faces these are deterministic from the face direction
    private static final float[][] FACE_TANGENTS = {
            {0, 0, 1},  // -X face: tangent along +Z (u-axis)
            {0, 0, 1},  // +X face: tangent along +Z
            {1, 0, 0},  // -Y face: tangent along +X
            {1, 0, 0},  // +Y face: tangent along +X
            {0, 1, 0},  // -Z face: tangent along +Y (u-axis)
            {0, 1, 0},  // +Z face: tangent along +Y
    };

    private ChunkMesher() {}

    /**
     * Build a mesh for the given chunk using greedy meshing.
     * Separates opaque and water faces into two meshes.
     * @return MeshBuildResult containing vertex and index data, or null if chunk is empty
     */
    public static MeshBuildResult buildMesh(VoxelChunk chunk) {
        if (chunk.isEmpty()) return null;

        List<float[]> opaqueVertexList = new ArrayList<>();
        List<int[]> opaqueIndexList = new ArrayList<>();
        int opaqueVertexOffset = 0;

        List<float[]> waterVertexList = new ArrayList<>();
        List<int[]> waterIndexList = new ArrayList<>();
        int waterVertexOffset = 0;

        int size = VoxelChunk.SIZE;

        // Process each of the 6 face directions
        for (int face = 0; face < 6; face++) {
            int normalAxis = FACE_AXES[face][0];
            int uAxis = FACE_AXES[face][1];
            int vAxis = FACE_AXES[face][2];

            int[] normal = FACE_NORMALS[face];
            boolean positive = (face & 1) == 1; // odd indices are positive direction

            // For each slice along the normal axis
            for (int d = 0; d < size; d++) {
                // Build a 2D mask of which faces need to be drawn in this slice
                // mask[u][v] = blockType ordinal if face visible, 0 if not
                short[][] mask = new short[size][size];
                boolean hasFaces = false;

                for (int v = 0; v < size; v++) {
                    for (int u = 0; u < size; u++) {
                        // Map (d, u, v) back to (x, y, z) based on the face axes
                        int[] pos = new int[3];
                        pos[normalAxis] = d;
                        pos[uAxis] = u;
                        pos[vAxis] = v;

                        int x = pos[0], y = pos[1], z = pos[2];
                        BlockType block = chunk.getBlock(x, y, z);

                        if (block.isAir()) {
                            mask[u][v] = 0;
                            continue;
                        }

                        // Check the adjacent block in the face normal direction
                        int ax = x + normal[0];
                        int ay = y + normal[1];
                        int az = z + normal[2];
                        BlockType adjacent = chunk.getBlockOrNeighbor(ax, ay, az);

                        // Only emit face if adjacent block is air or transparent (and we're opaque)
                        if (adjacent.isAir() || (adjacent.transparent && block.isOpaque())) {
                            mask[u][v] = (short) block.ordinal();
                            hasFaces = true;
                        } else {
                            mask[u][v] = 0;
                        }
                    }
                }

                if (!hasFaces) continue;

                // Greedy meshing: merge adjacent same-type faces into larger quads
                for (int v = 0; v < size; v++) {
                    for (int u = 0; u < size; ) {
                        short type = mask[u][v];
                        if (type == 0) {
                            u++;
                            continue;
                        }

                        // Determine width: extend along u-axis
                        int width = 1;
                        while (u + width < size && mask[u + width][v] == type) {
                            width++;
                        }

                        // Determine height: extend along v-axis
                        int height = 1;
                        boolean canExtend = true;
                        while (v + height < size && canExtend) {
                            for (int wu = 0; wu < width; wu++) {
                                if (mask[u + wu][v + height] != type) {
                                    canExtend = false;
                                    break;
                                }
                            }
                            if (canExtend) height++;
                        }

                        // Clear the merged region from the mask
                        for (int hv = 0; hv < height; hv++) {
                            for (int wu = 0; wu < width; wu++) {
                                mask[u + wu][v + hv] = 0;
                            }
                        }

                        // Emit the merged quad
                        float texLayer = BlockTextureArray.getLayerIndex(BlockType.fromId(type));
                        boolean isWater = BlockType.fromId(type) == BlockType.WATER;

                        // Choose the target vertex/index lists based on block type
                        List<float[]> targetVertexList = isWater ? waterVertexList : opaqueVertexList;
                        List<int[]> targetIndexList = isWater ? waterIndexList : opaqueIndexList;
                        int targetOffset = isWater ? waterVertexOffset : opaqueVertexOffset;

                        // Calculate the 4 corners of the quad in world-local coordinates
                        float[] corner = new float[3];
                        corner[normalAxis] = positive ? d + 1 : d;
                        corner[uAxis] = u;
                        corner[vAxis] = v;

                        float[] du = new float[3]; // direction along u * width
                        du[uAxis] = width;

                        float[] dv = new float[3]; // direction along v * height
                        dv[vAxis] = height;

                        float nx = normal[0], ny = normal[1], nz = normal[2];
                        float tx = FACE_TANGENTS[face][0];
                        float ty = FACE_TANGENTS[face][1];
                        float tz = FACE_TANGENTS[face][2];

                        // 4 vertices of the quad
                        // v0 = corner
                        // v1 = corner + du
                        // v2 = corner + du + dv
                        // v3 = corner + dv
                        float[] v0 = {corner[0], corner[1], corner[2],
                                0, 0, nx, ny, nz, tx, ty, tz, texLayer};
                        float[] v1 = {corner[0] + du[0], corner[1] + du[1], corner[2] + du[2],
                                width, 0, nx, ny, nz, tx, ty, tz, texLayer};
                        float[] v2 = {corner[0] + du[0] + dv[0], corner[1] + du[1] + dv[1], corner[2] + du[2] + dv[2],
                                width, height, nx, ny, nz, tx, ty, tz, texLayer};
                        float[] v3 = {corner[0] + dv[0], corner[1] + dv[1], corner[2] + dv[2],
                                0, height, nx, ny, nz, tx, ty, tz, texLayer};

                        targetVertexList.add(v0);
                        targetVertexList.add(v1);
                        targetVertexList.add(v2);
                        targetVertexList.add(v3);

                        // Two triangles — CCW winding so geometric normal matches face direction
                        // Positive faces (+X,+Y,+Z): v0->v3->v2, v0->v2->v1
                        // Negative faces (-X,-Y,-Z): v0->v1->v2, v0->v2->v3
                        if (positive) {
                            targetIndexList.add(new int[]{targetOffset, targetOffset + 3, targetOffset + 2});
                            targetIndexList.add(new int[]{targetOffset, targetOffset + 2, targetOffset + 1});
                        } else {
                            targetIndexList.add(new int[]{targetOffset, targetOffset + 1, targetOffset + 2});
                            targetIndexList.add(new int[]{targetOffset, targetOffset + 2, targetOffset + 3});
                        }

                        if (isWater) {
                            waterVertexOffset += 4;
                        } else {
                            opaqueVertexOffset += 4;
                        }
                        u += width;
                    }
                }
            }
        }

        if (opaqueVertexList.isEmpty() && waterVertexList.isEmpty()) return null;

        // Flatten opaque mesh
        float[] opaqueVertices = null;
        int[] opaqueIndices = null;
        if (!opaqueVertexList.isEmpty()) {
            opaqueVertices = new float[opaqueVertexList.size() * VERTEX_FLOATS];
            for (int i = 0; i < opaqueVertexList.size(); i++) {
                System.arraycopy(opaqueVertexList.get(i), 0, opaqueVertices, i * VERTEX_FLOATS, VERTEX_FLOATS);
            }
            opaqueIndices = new int[opaqueIndexList.size() * 3];
            for (int i = 0; i < opaqueIndexList.size(); i++) {
                System.arraycopy(opaqueIndexList.get(i), 0, opaqueIndices, i * 3, 3);
            }
        }

        // Flatten water mesh
        float[] waterVertices = null;
        int[] waterIndices = null;
        if (!waterVertexList.isEmpty()) {
            waterVertices = new float[waterVertexList.size() * VERTEX_FLOATS];
            for (int i = 0; i < waterVertexList.size(); i++) {
                System.arraycopy(waterVertexList.get(i), 0, waterVertices, i * VERTEX_FLOATS, VERTEX_FLOATS);
            }
            waterIndices = new int[waterIndexList.size() * 3];
            for (int i = 0; i < waterIndexList.size(); i++) {
                System.arraycopy(waterIndexList.get(i), 0, waterIndices, i * 3, 3);
            }
        }

        return new MeshBuildResult(opaqueVertices, opaqueIndices, waterVertices, waterIndices);
    }

    /**
     * Result of mesh building — raw vertex and index data for opaque and water meshes.
     */
    public record MeshBuildResult(
            float[] opaqueVertices, int[] opaqueIndices,
            float[] waterVertices, int[] waterIndices
    ) {
        public boolean hasOpaque() {
            return opaqueVertices != null && opaqueVertices.length > 0;
        }
        public boolean hasWater() {
            return waterVertices != null && waterVertices.length > 0;
        }
        public int opaqueVertexCount() {
            return hasOpaque() ? opaqueVertices.length / VERTEX_FLOATS : 0;
        }
        public int waterVertexCount() {
            return hasWater() ? waterVertices.length / VERTEX_FLOATS : 0;
        }
    }
}
