package cinnamon.model.material;

public class Material {

    public static final float
            DEFAULT_HEIGHT = 0.1f,
            DEFAULT_ALPHA_CUTOUT = 0.5f;

    private final String name;

    private MaterialTexture
            albedo,    //map_Kd / albedo / diffuse
            height,    //map_disp / bump / height
            normal,    //map_Bump / norm / normal / map_Kn
            ao,        //map_ao / map_AO / ao / ambient_occlusion
            roughness, //map_Pr / roughness
            metallic,  //map_Pm / metallic
            emissive;  //map_Ke / emissive

    private float
            heightScale = DEFAULT_HEIGHT, //map_disp -dm
            alphaCutout = DEFAULT_ALPHA_CUTOUT; //d / alpha_cutout

    public Material(String name) {
        this.name = name;
    }

    public MaterialTexture getAlbedo() {
        return albedo;
    }

    public void setAlbedo(MaterialTexture albedo) {
        this.albedo = albedo;
    }

    public MaterialTexture getHeight() {
        return height;
    }

    public void setHeight(MaterialTexture height) {
        this.height = height;
    }

    public MaterialTexture getNormal() {
        return normal;
    }

    public void setNormal(MaterialTexture normal) {
        this.normal = normal;
    }

    public MaterialTexture getAO() {
        return ao;
    }

    public void setAO(MaterialTexture ao) {
        this.ao = ao;
    }

    public MaterialTexture getRoughness() {
        return roughness;
    }

    public void setRoughness(MaterialTexture roughness) {
        this.roughness = roughness;
    }

    public MaterialTexture getMetallic() {
        return metallic;
    }

    public void setMetallic(MaterialTexture metallic) {
        this.metallic = metallic;
    }

    public MaterialTexture getEmissive() {
        return emissive;
    }

    public void setEmissive(MaterialTexture emissive) {
        this.emissive = emissive;
    }

    public float getHeightScale() {
        return heightScale;
    }

    public void setHeightScale(float heightScale) {
        this.heightScale = heightScale;
    }

    public float getAlphaCutout() {
        return alphaCutout;
    }

    public void setAlphaCutout(float alphaCutout) {
        this.alphaCutout = alphaCutout;
    }

    public String getName() {
        return name;
    }
}
