package cinnamon.render.model;

import cinnamon.model.Vertex;
import cinnamon.model.material.Material;
import cinnamon.render.MaterialApplier;
import cinnamon.render.MatrixStack;
import cinnamon.render.shader.Shader;
import cinnamon.render.texture.Texture;
import cinnamon.utils.AABB;

/**
 * Minimal cube face renderer with per-face masking.
 * Cube bounds: x,z in [-0.5,0.5], y in [0,1].
 * Face order: +X, -X, +Y, -Y, +Z, -Z.
 */
public final class CubeRenderer {

    private static final MeshData[] FACES = new MeshData[6];

    static {
        // build 6 faces as individual meshes
        // +X (right) normal (1,0,0)
        FACES[0] = face(
                0.5f, 0f, -0.5f,
                0.5f, 1f, -0.5f,
                0.5f, 1f,  0.5f,
                0.5f, 0f,  0.5f,
                1f, 0f, 0f
        );
        // -X (left) normal (-1,0,0)
        FACES[1] = face(
                -0.5f, 0f,  0.5f,
                -0.5f, 1f,  0.5f,
                -0.5f, 1f, -0.5f,
                -0.5f, 0f, -0.5f,
                -1f, 0f, 0f
        );
        // +Y (top) normal (0,1,0)
        FACES[2] = face(
                -0.5f, 1f, -0.5f,
                 0.5f, 1f, -0.5f,
                 0.5f, 1f,  0.5f,
                -0.5f, 1f,  0.5f,
                0f, 1f, 0f
        );
        // -Y (bottom) normal (0,-1,0)
        FACES[3] = face(
                -0.5f, 0f,  0.5f,
                 0.5f, 0f,  0.5f,
                 0.5f, 0f, -0.5f,
                -0.5f, 0f, -0.5f,
                0f, -1f, 0f
        );
        // +Z (front) normal (0,0,1)
        FACES[4] = face(
                0.5f, 0f, 0.5f,
                0.5f, 1f, 0.5f,
               -0.5f, 1f, 0.5f,
               -0.5f, 0f, 0.5f,
                0f, 0f, 1f
        );
        // -Z (back) normal (0,0,-1)
        FACES[5] = face(
               -0.5f, 0f, -0.5f,
               -0.5f, 1f, -0.5f,
                0.5f, 1f, -0.5f,
                0.5f, 0f, -0.5f,
                0f, 0f, -1f
        );
    }

    private static MeshData face(float x0, float y0, float z0,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float nx, float ny, float nz) {
        // two triangles (0,1,2) and (0,2,3)
        var v0 = Vertex.of(x0, y0, z0).uv(0, 0).normal(nx, ny, nz);
        var v1 = Vertex.of(x1, y1, z1).uv(1, 0).normal(nx, ny, nz);
        var v2 = Vertex.of(x2, y2, z2).uv(1, 1).normal(nx, ny, nz);
        var v3 = Vertex.of(x3, y3, z3).uv(0, 1).normal(nx, ny, nz);

        var list = java.util.List.of(
                v0, v1, v2,
                v0, v2, v3
        );
        AABB aabb = new AABB(-0.5f, 0f, -0.5f, 0.5f, 1f, 0.5f);
        return new MeshData(aabb, list, null);
    }

    public static void renderFaces(MatrixStack matrices, Material material, byte faceMask) {
        Shader.activeShader.applyMatrixStack(matrices);
        int texCount = MaterialApplier.applyMaterial(material, 0);
        for (int i = 0; i < 6; i++) {
            if (((faceMask >> i) & 1) != 0) continue; // hidden
            FACES[i].render();
        }
        Texture.unbindAll(texCount);
    }
}
