package cinnamon.render;

import cinnamon.model.material.Material;
import cinnamon.model.material.MaterialTexture;
import cinnamon.render.shader.Shader;
import cinnamon.render.texture.Texture;

public class MaterialApplier {

    private static final Texture
            WHITE_TEX = Texture.generateSolid(0xFFFFFFFF),
            BLACK_TEX = Texture.generateSolid(0xFF000000),
            NORMAL_TEX = Texture.generateSolid(0xFF8080FF);

    private static Material lastMaterial;

    public static void applyMaterial(Material material, int offset) {
        if (material == null || material == lastMaterial)
            return;

        Shader s = Shader.activeShader;
        int i = offset;

        bindTex(s, material.getAlbedo(), i++, "material.albedoTex", Texture.MISSING);
        bindTex(s, material.getHeight(), i++, "material.heightTex", BLACK_TEX);
        bindTex(s, material.getNormal(), i++, "material.normalTex", NORMAL_TEX);
        bindTex(s, material.getAO(), i++, "material.aoTex", WHITE_TEX);
        bindTex(s, material.getRoughness(), i++, "material.roughnessTex", WHITE_TEX);
        bindTex(s, material.getMetallic(), i++, "material.metallicTex", BLACK_TEX);
        bindTex(s, material.getEmissive(), i++, "material.emissiveTex", BLACK_TEX);

        s.setFloat("material.heightScale", material.getHeight() != null ? material.getHeightScale() : 0f);

        lastMaterial = material;
    }

    private static void bindTex(Shader s, MaterialTexture texture, int index, String name, Texture fallback) {
        s.setInt(name, index);
        Texture tex;

        if (texture == null || (tex = Texture.of(texture.texture(), texture.params())) == null) {
            if (fallback == null) {
                Texture.unbindTex(index);
                return;
            }

            tex = fallback;
        }

        tex.bind(index);
    }

    public static void cleanup() {
        if (lastMaterial == null)
            return;

        lastMaterial = null;
        Texture.unbindAll();
    }
}
