package cinnamon.model.material;

import cinnamon.utils.Resource;

public class PBRMaterial extends Material {

    public static final float DEFAULT_HEIGHT = 0.1f;

    private Resource
        albedo,
        height,
        normal,
        roughness,
        metallic,
        ao, //ambient occlusion
        emissive;
    private float heightScale = DEFAULT_HEIGHT;

    public PBRMaterial(String name) {
        super(name);
    }

    public Resource getAlbedo() {
        return albedo;
    }

    public void setAlbedo(Resource albedo) {
        this.albedo = albedo;
    }

    public Resource getHeight() {
        return height;
    }

    public void setHeight(Resource height) {
        this.height = height;
    }

    public Resource getNormal() {
        return normal;
    }

    public void setNormal(Resource normal) {
        this.normal = normal;
    }

    public Resource getRoughness() {
        return roughness;
    }

    public void setRoughness(Resource roughness) {
        this.roughness = roughness;
    }

    public Resource getMetallic() {
        return metallic;
    }

    public void setMetallic(Resource metallic) {
        this.metallic = metallic;
    }

    public Resource getAO() {
        return ao;
    }

    public void setAO(Resource ao) {
        this.ao = ao;
    }

    public Resource getEmissive() {
        return emissive;
    }

    public void setEmissive(Resource emissive) {
        this.emissive = emissive;
    }

    public float getHeightScale() {
        return heightScale;
    }

    public void setHeightScale(float heightScale) {
        this.heightScale = heightScale;
    }
}
