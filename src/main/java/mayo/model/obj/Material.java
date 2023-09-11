package mayo.model.obj;

import mayo.render.Texture;
import org.joml.Vector3f;

public class Material {

    private final String name;

    private final Vector3f
            ambient = new Vector3f(), //Ka
            diffuse = new Vector3f(), //Kd
            specular = new Vector3f(), //Ks
            filter = new Vector3f(); //Tf
    private float
            specularExponent, //Ns
            refraction; //Ni
    private int
            illumination; //illum
    private Texture
            diffuseTex; //map_Kd

    public Material(String name) {
        this.name = name;
    }


    // -- rendering -- //


    public void use() {
        if (diffuseTex != null)
            diffuseTex.bind();
    }


    // -- getters and setters -- //


    public String getName() {
        return name;
    }

    public Vector3f getAmbientColor() {
        return ambient;
    }

    public Vector3f getDiffuseColor() {
        return diffuse;
    }

    public Vector3f getSpecularColor() {
        return specular;
    }

    public float getSpecularExponent() {
        return specularExponent;
    }

    public void setSpecularExponent(float f) {
        this.specularExponent = f;
    }

    public Vector3f getFilterColor() {
        return filter;
    }

    public float getRefractionIndex() {
        return refraction;
    }

    public void setRefractionIndex(float f) {
        this.refraction = f;
    }

    public int getIllumModel() {
        return illumination;
    }

    public void setIllumModel(int i) {
        this.illumination = i;
    }

    public Texture getDiffuseTex() {
        return diffuseTex;
    }

    public void setDiffuseTex(Texture tex) {
        this.diffuseTex = tex;
    }
}
