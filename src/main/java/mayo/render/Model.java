package mayo.render;

import mayo.model.obj.Mesh;

public abstract class Model {

    private final Mesh mesh;

    public Model(Mesh mesh) {
        this.mesh = mesh;
    }

    public abstract void render();

    public Mesh getMesh() {
        return mesh;
    }
}
