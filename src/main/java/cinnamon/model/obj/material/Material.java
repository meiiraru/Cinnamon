package cinnamon.model.obj.material;

public abstract class Material {

    private final String name;

    private boolean smooth, mipmap;

    public Material(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isSmooth() {
        return smooth;
    }

    public void setSmooth(boolean smooth) {
        this.smooth = smooth;
    }

    public boolean isMipmap() {
        return mipmap;
    }

    public void setMipmap(boolean mipmap) {
        this.mipmap = mipmap;
    }
}
