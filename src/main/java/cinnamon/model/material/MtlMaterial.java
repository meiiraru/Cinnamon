package cinnamon.model.material;

import cinnamon.utils.Resource;
import org.joml.Vector3f;

public class MtlMaterial extends Material {

    private final Vector3f
            ambient = new Vector3f(1, 1, 1), //Ka
            diffuse = new Vector3f(1, 1, 1), //Kd
            specular = new Vector3f(1, 1, 1), //Ks
            filter = new Vector3f(); //Tf
    private float
            specularExponent = 1, //Ns
            refraction; //Ni
    private int
            illumination; //illum
    private Resource
            ambientTex, //map_Ka
            diffuseTex, //map_Kd
            spColorTex, //map_Ks
            spHighlightTex, //map_Ns
            emissiveTex, //map_Ke
            alphaTex, //map_d
            bumpTex, //map_bump
            displacementTex, //disp
            stencilDecalTex; //decal

    public MtlMaterial(String name) {
        super(name);
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

    public Resource getAmbientTex() {
        return ambientTex;
    }

    public void setAmbientTex(Resource ambientTex) {
        this.ambientTex = ambientTex;
    }

    public Resource getDiffuseTex() {
        return diffuseTex;
    }

    public void setDiffuseTex(Resource diffuseTex) {
        this.diffuseTex = diffuseTex;
    }

    public Resource getSpColorTex() {
        return spColorTex;
    }

    public void setSpColorTex(Resource spColorTex) {
        this.spColorTex = spColorTex;
    }

    public Resource getSpHighlightTex() {
        return spHighlightTex;
    }

    public void setSpHighlightTex(Resource spHighlightTex) {
        this.spHighlightTex = spHighlightTex;
    }

    public Resource getEmissiveTex() {
        return emissiveTex;
    }

    public void setEmissiveTex(Resource emissiveTex) {
        this.emissiveTex = emissiveTex;
    }

    public Resource getAlphaTex() {
        return alphaTex;
    }

    public void setAlphaTex(Resource alphaTex) {
        this.alphaTex = alphaTex;
    }

    public Resource getBumpTex() {
        return bumpTex;
    }

    public void setBumpTex(Resource bumpTex) {
        this.bumpTex = bumpTex;
    }

    public Resource getDisplacementTex() {
        return displacementTex;
    }

    public void setDisplacementTex(Resource displacementTex) {
        this.displacementTex = displacementTex;
    }

    public Resource getStencilDecalTex() {
        return stencilDecalTex;
    }

    public void setStencilDecalTex(Resource stencilDecalTex) {
        this.stencilDecalTex = stencilDecalTex;
    }
}
