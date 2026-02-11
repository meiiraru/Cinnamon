package cinnamon.world.voxel;

import cinnamon.model.material.Material;
import cinnamon.model.material.MaterialTexture;
import cinnamon.registry.MaterialRegistry;
import cinnamon.render.texture.Texture;
import cinnamon.utils.TextureIO;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL12.glTexImage3D;
import static org.lwjgl.opengl.GL12.glTexSubImage3D;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_LOD_BIAS;
import static org.lwjgl.opengl.GL30.*;

/**
 * Builds and manages GL_TEXTURE_2D_ARRAY textures for all block types.
 * Each block type gets one layer in each array. The mesher writes the layer index per-face
 * so the shader can sample the correct texture with a single bind.
 * <p>
 * Builds arrays for: albedo, normal, roughness, and AO.
 * All textures are rescaled to a common resolution (ATLAS_SIZE x ATLAS_SIZE).
 * This eliminates per-material texture state changes — one bind per chunk instead of one per block type.
 */
public class BlockTextureArray {

    /** Resolution of each texture layer */
    public static final int ATLAS_SIZE = 256;

    /** The OpenGL texture array IDs */
    private static int albedoArrayId = 0;
    private static int normalArrayId = 0;
    private static int roughnessArrayId = 0;
    private static int aoArrayId = 0;

    /** Map from BlockType to its layer index in the texture arrays */
    private static final Map<BlockType, Integer> layerMap = new HashMap<>();

    /** Whether the texture arrays have been built */
    private static boolean initialized = false;

    /**
     * Build the texture arrays from all BlockType materials.
     * Must be called after MaterialRegistry.loadAllMaterials() and on the GL thread.
     */
    public static void init() {
        if (initialized) return;

        // Count non-air block types
        int layerCount = 0;
        for (BlockType type : BlockType.VALUES) {
            if (!type.isAir() && type.material != null) {
                layerMap.put(type, layerCount);
                layerCount++;
            }
        }

        if (layerCount == 0) return;

        // Create all 4 texture arrays
        albedoArrayId = createTextureArray(layerCount);
        normalArrayId = createTextureArray(layerCount);
        roughnessArrayId = createTextureArray(layerCount);
        aoArrayId = createTextureArray(layerCount);

        // Upload each block type's textures to the corresponding layer
        for (Map.Entry<BlockType, Integer> entry : layerMap.entrySet()) {
            BlockType type = entry.getKey();
            int layer = entry.getValue();
            Material mat = type.material != null ? type.material.material : null;

            // Albedo
            uploadLayerFromTexture(albedoArrayId, layer,
                    mat != null ? mat.getAlbedo() : null,
                    createFallbackColor(type));

            // Normal — default flat normal (128, 128, 255) = (0.5, 0.5, 1.0) normalized
            uploadLayerFromTexture(normalArrayId, layer,
                    mat != null ? mat.getNormal() : null,
                    createSolidColor((byte) 128, (byte) 128, (byte) -1, (byte) -1));

            // Roughness — default 0.85
            uploadLayerFromTexture(roughnessArrayId, layer,
                    mat != null ? mat.getRoughness() : null,
                    createSolidColor((byte) (int)(0.85f * 255), (byte) (int)(0.85f * 255), (byte) (int)(0.85f * 255), (byte) -1));

            // AO — default 1.0 (white)
            uploadLayerFromTexture(aoArrayId, layer,
                    mat != null ? mat.getAO() : null,
                    createSolidColor((byte) -1, (byte) -1, (byte) -1, (byte) -1));
        }

        // Set texture parameters and generate mipmaps for all arrays
        finalizeTextureArray(albedoArrayId, true);
        finalizeTextureArray(normalArrayId, false);
        finalizeTextureArray(roughnessArrayId, false);
        finalizeTextureArray(aoArrayId, false);

        initialized = true;
    }

    /**
     * Create an empty GL_TEXTURE_2D_ARRAY with the given number of layers.
     */
    private static int createTextureArray(int layerCount) {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D_ARRAY, id);
        glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, GL_RGBA8,
                ATLAS_SIZE, ATLAS_SIZE, layerCount,
                0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
        return id;
    }

    /**
     * Upload a texture to a specific layer in a texture array.
     * Falls back to the provided default data if the material texture is null or fails to load.
     */
    private static void uploadLayerFromTexture(int arrayId, int layer,
                                                MaterialTexture matTex,
                                                ByteBuffer fallback) {
        ByteBuffer pixelData = null;
        if (matTex != null) {
            pixelData = loadTextureData(matTex);
        }

        glBindTexture(GL_TEXTURE_2D_ARRAY, arrayId);
        if (pixelData != null) {
            glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0,
                    0, 0, layer,
                    ATLAS_SIZE, ATLAS_SIZE, 1,
                    GL_RGBA, GL_UNSIGNED_BYTE, pixelData);
            MemoryUtil.memFree(pixelData);
        } else {
            glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0,
                    0, 0, layer,
                    ATLAS_SIZE, ATLAS_SIZE, 1,
                    GL_RGBA, GL_UNSIGNED_BYTE, fallback);
        }
        glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
        MemoryUtil.memFree(fallback);
    }

    /**
     * Set texture parameters and generate mipmaps for a texture array.
     * @param useNearestMag true for albedo (pixel-art look), false for PBR maps (smooth interpolation)
     */
    private static void finalizeTextureArray(int arrayId, boolean useNearestMag) {
        glBindTexture(GL_TEXTURE_2D_ARRAY, arrayId);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, useNearestMag ? GL_NEAREST : GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        glTexParameterf(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_LOD_BIAS, -0.4f);
        glGenerateMipmap(GL_TEXTURE_2D_ARRAY);
        glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
    }

    /**
     * Load and rescale a material texture to ATLAS_SIZE x ATLAS_SIZE.
     * Returns a newly allocated ByteBuffer (caller must free), or null on failure.
     */
    private static ByteBuffer loadTextureData(MaterialTexture matTex) {
        if (matTex == null || matTex.texture() == null) return null;
        try (TextureIO.ImageData image = TextureIO.load(matTex.texture())) {
            if (image.width == ATLAS_SIZE && image.height == ATLAS_SIZE) {
                ByteBuffer copy = MemoryUtil.memAlloc(ATLAS_SIZE * ATLAS_SIZE * 4);
                copy.put(image.buffer);
                copy.flip();
                return copy;
            } else {
                return rescaleImage(image.buffer, image.width, image.height, ATLAS_SIZE, ATLAS_SIZE);
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Nearest-neighbor rescale of RGBA image.
     */
    private static ByteBuffer rescaleImage(ByteBuffer src, int srcW, int srcH, int dstW, int dstH) {
        ByteBuffer dst = MemoryUtil.memAlloc(dstW * dstH * 4);
        for (int y = 0; y < dstH; y++) {
            int sy = y * srcH / dstH;
            for (int x = 0; x < dstW; x++) {
                int sx = x * srcW / dstW;
                int srcIdx = (sy * srcW + sx) * 4;
                dst.put(src.get(srcIdx));
                dst.put(src.get(srcIdx + 1));
                dst.put(src.get(srcIdx + 2));
                dst.put(src.get(srcIdx + 3));
            }
        }
        dst.flip();
        return dst;
    }

    /**
     * Create a solid-color fallback texture for a block type (used for albedo).
     */
    private static ByteBuffer createFallbackColor(BlockType type) {
        int hash = type.name().hashCode();
        byte r = (byte) ((hash >> 16) & 0xFF);
        byte g = (byte) ((hash >> 8) & 0xFF);
        byte b = (byte) (hash & 0xFF);
        return createSolidColor(r, g, b, (byte) 0xFF);
    }

    /**
     * Create a solid-color ATLAS_SIZE x ATLAS_SIZE RGBA texture.
     */
    private static ByteBuffer createSolidColor(byte r, byte g, byte b, byte a) {
        ByteBuffer buf = MemoryUtil.memAlloc(ATLAS_SIZE * ATLAS_SIZE * 4);
        for (int i = 0; i < ATLAS_SIZE * ATLAS_SIZE; i++) {
            buf.put(r);
            buf.put(g);
            buf.put(b);
            buf.put(a);
        }
        buf.flip();
        return buf;
    }

    /**
     * Get the texture array layer index for a block type.
     */
    public static float getLayerIndex(BlockType type) {
        Integer layer = layerMap.get(type);
        return layer != null ? layer : 0;
    }

    /**
     * Bind the albedo texture array to a texture unit.
     */
    public static int bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D_ARRAY, albedoArrayId);
        return unit;
    }

    /**
     * Bind the normal texture array to a texture unit.
     */
    public static int bindNormal(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D_ARRAY, normalArrayId);
        return unit;
    }

    /**
     * Bind the roughness texture array to a texture unit.
     */
    public static int bindRoughness(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D_ARRAY, roughnessArrayId);
        return unit;
    }

    /**
     * Bind the AO texture array to a texture unit.
     */
    public static int bindAO(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D_ARRAY, aoArrayId);
        return unit;
    }

    /**
     * Unbind a texture array from a texture unit.
     */
    public static void unbind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
    }

    /**
     * Free all texture arrays.
     */
    public static void free() {
        if (albedoArrayId != 0) {
            glDeleteTextures(albedoArrayId);
            albedoArrayId = 0;
        }
        if (normalArrayId != 0) {
            glDeleteTextures(normalArrayId);
            normalArrayId = 0;
        }
        if (roughnessArrayId != 0) {
            glDeleteTextures(roughnessArrayId);
            roughnessArrayId = 0;
        }
        if (aoArrayId != 0) {
            glDeleteTextures(aoArrayId);
            aoArrayId = 0;
        }
        layerMap.clear();
        initialized = false;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static int getAlbedoArrayId() {
        return albedoArrayId;
    }

    public static int getNormalArrayId() {
        return normalArrayId;
    }

    public static int getRoughnessArrayId() {
        return roughnessArrayId;
    }

    public static int getAOArrayId() {
        return aoArrayId;
    }
}
