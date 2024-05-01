package mayo.render;

import mayo.model.obj.material.Material;
import mayo.model.obj.material.MtlMaterial;
import mayo.model.obj.material.PBRMaterial;
import mayo.render.shader.Shader;
import mayo.render.texture.Texture;
import mayo.utils.Resource;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class MaterialApplier {

    private static final Texture
            WHITE_TEX = Texture.generateSolid(0xFFFFFFFF),
            BLACK_TEX = Texture.generateSolid(0xFF000000),
            NORMAL_TEX = Texture.generateSolid(0xFF8080FF);

    public static int applyMaterial(Material material) {
        if (material == null)
            return -1;

        Shader s = Shader.activeShader;

        if (material instanceof MtlMaterial phong) {
            s.setVec3("material.ambient", phong.getAmbientColor());
            s.setVec3("material.diffuse", phong.getDiffuseColor());
            s.setVec3("material.specular", phong.getSpecularColor());
            s.setFloat("material.shininess", phong.getSpecularExponent());

            bindTex(s, phong.getDiffuseTex(), 0, "material.diffuseTex", WHITE_TEX);
            bindTex(s, phong.getSpColorTex(), 1, "material.specularTex", BLACK_TEX);
            bindTex(s, phong.getEmissiveTex(), 2, "material.emissiveTex", BLACK_TEX);

            return 3;
        } else if (material instanceof PBRMaterial pbr) {
            bindTex(s, pbr.getAlbedo(), 0, "material.albedoTex", WHITE_TEX);
            bindTex(s, pbr.getHeight(), 1, "material.heightTex", WHITE_TEX);
            bindTex(s, pbr.getNormal(), 2, "material.normalTex", NORMAL_TEX);
            bindTex(s, pbr.getRoughness(), 3, "material.roughnessTex", BLACK_TEX);
            bindTex(s, pbr.getMetallic(), 4, "material.metallicTex", BLACK_TEX);
            bindTex(s, pbr.getAO(), 5, "material.aoTex", WHITE_TEX);
            bindTex(s, pbr.getEmissive(), 6, "material.emissiveTex", BLACK_TEX);

            s.setFloat("material.heightScale", pbr.getHeightScale());

            return 7;
        } else {
            return -1;
        }
    }

    private static void bindTex(Shader s, Resource res, int index, String name, Texture fallback) {
        s.setInt(name, index);
        Texture tex;

        if (res == null || (tex = Texture.of(res)) == null) {
            if (fallback == null) {
                Texture.unbindTex(index);
                return;
            }

            tex = fallback;
        }

        glActiveTexture(GL_TEXTURE0 + index);
        tex.bind();
    }
}
