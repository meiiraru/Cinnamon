package cinnamon.physics.component;

import cinnamon.math.AABB;
import cinnamon.model.ModelManager;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.model.ModelRenderer;
import cinnamon.utils.Resource;
import org.joml.Math;
import org.joml.Vector3f;

public class MeshComponent extends Component {

    private final ModelRenderer model;
    private final Vector3f offset = new Vector3f();
    private final Vector3f scale = new Vector3f(1, 1, 1);

    public MeshComponent(Resource mesh) {
        this.model = ModelManager.load(mesh);
    }

    public MeshComponent(Resource mesh, Vector3f offset) {
        this.model = ModelManager.load(mesh);
        if (offset != null) this.offset.set(offset);
    }

    /** Center the mesh on origin and scale it to the given size (local space). */
    public MeshComponent fitToSize(float sx, float sy, float sz, boolean center) {
        AABB aabb = model.getAABB();
        Vector3f min = aabb.getMin();
        Vector3f max = aabb.getMax();
        float dx = Math.max(1e-6f, max.x - min.x);
        float dy = Math.max(1e-6f, max.y - min.y);
        float dz = Math.max(1e-6f, max.z - min.z);
        if (center) {
            offset.set(-(min.x + max.x) * 0.5f, -(min.y + max.y) * 0.5f, -(min.z + max.z) * 0.5f);
        }
        scale.set(sx / dx, sy / dy, sz / dz);
        return this;
    }

    public void render(MatrixStack matrices, Camera camera, float delta) {
        model.render(matrices);
    }

    public AABB getAABB(AABB out) {
        out.set(model.getAABB());
        return out;
    }

    public Vector3f getOffset() {
        return offset;
    }

    public Vector3f getScale() {
        return scale;
    }

    @Override
    public ComponentType getType() {
        return ComponentType.MESH;
    }
}
