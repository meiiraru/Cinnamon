package cinnamon.render;

import cinnamon.model.material.Material;
import cinnamon.model.material.MtlMaterial;
import cinnamon.model.material.PBRMaterial;
import cinnamon.render.shader.Shader;
import cinnamon.render.texture.Texture;
import cinnamon.utils.Resource;

public class MaterialApplier {

    private static final Texture
            WHITE_TEX = Texture.generateSolid(0xFFFFFFFF),
            BLACK_TEX = Texture.generateSolid(0xFF000000),
            NORMAL_TEX = Texture.generateSolid(0xFF8080FF);

    public static int applyMaterial(Material material) {
        if (material == null)
            return -1;

        Shader s = Shader.activeShader;
        boolean smooth = material.isSmooth();
        boolean mip = material.isMipmap();

        if (material instanceof MtlMaterial phong) {
            s.setVec3("material.ambient", phong.getAmbientColor());
            s.setVec3("material.diffuse", phong.getDiffuseColor());
            s.setVec3("material.specular", phong.getSpecularColor());
            s.setFloat("material.shininess", phong.getSpecularExponent());

            bindTex(s, smooth, mip, phong.getDiffuseTex(), 0, "material.diffuseTex", Texture.MISSING);
            bindTex(s, smooth, mip, phong.getSpColorTex(), 1, "material.specularTex", BLACK_TEX);
            bindTex(s, smooth, mip, phong.getEmissiveTex(), 2, "material.emissiveTex", BLACK_TEX);

            return 3;
        } else if (material instanceof PBRMaterial pbr) {
            bindTex(s, smooth, mip, pbr.getAlbedo(), 0, "material.albedoTex", Texture.MISSING);
            bindTex(s, smooth, mip, pbr.getHeight(), 1, "material.heightTex", WHITE_TEX);
            bindTex(s, smooth, mip, pbr.getNormal(), 2, "material.normalTex", NORMAL_TEX);
            bindTex(s, smooth, mip, pbr.getRoughness(), 3, "material.roughnessTex", WHITE_TEX);
            bindTex(s, smooth, mip, pbr.getMetallic(), 4, "material.metallicTex", BLACK_TEX);
            bindTex(s, smooth, mip, pbr.getAO(), 5, "material.aoTex", WHITE_TEX);
            bindTex(s, smooth, mip, pbr.getEmissive(), 6, "material.emissiveTex", BLACK_TEX);

            s.setFloat("material.heightScale", pbr.getHeightScale());

            return 7;
        } else {
            return -1;
        }
    }

    private static void bindTex(Shader s, boolean smooth, boolean mipmap, Resource res, int index, String name, Texture fallback) {
        s.setInt(name, index);
        Texture tex;

        if (res == null || (tex = Texture.of(res, smooth, mipmap)) == null) {
            if (fallback == null) {
                Texture.unbindTex(index);
                return;
            }

            tex = fallback;
        }

        tex.bind(index);
    }
}
