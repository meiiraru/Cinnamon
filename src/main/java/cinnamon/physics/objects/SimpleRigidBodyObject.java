package cinnamon.physics.objects;

import cinnamon.math.AABB;
import cinnamon.math.Maths;
import cinnamon.model.GeometryHelper;
import cinnamon.physics.RenderableObject;
import cinnamon.physics.TickableObject;
import cinnamon.physics.WorldObject;
import cinnamon.physics.component.MeshComponent;
import cinnamon.physics.component.RigidBody;
import cinnamon.physics.component.Transform;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SimpleRigidBodyObject extends WorldObject implements TickableObject, RenderableObject {

    private final Transform
            oldTransform = new Transform(),
            transform = new Transform();
    private final RigidBody rigidBody; // may be dynamic body or static geom
    private MeshComponent mesh; // optional

    public SimpleRigidBodyObject(int id, RigidBody rigidBody) {
        super(id);
        this.rigidBody = rigidBody;
        addComponent(oldTransform);
        addComponent(transform);
        addComponent(rigidBody);
    }

    public SimpleRigidBodyObject mesh(MeshComponent mesh) {
        this.mesh = mesh;
        addComponent(mesh);
        return this;
    }

    public Transform getOldTransform() {
        return oldTransform;
    }

    public Transform getTransform() {
        return transform;
    }

    public RigidBody getRigidBody() {
        return rigidBody;
    }

    // Set initial pose before the first tick
    public SimpleRigidBodyObject setInitialPose(Vector3f pos, Quaternionf rot, Vector3f scale) {
        transform.getPosition().set(pos);
        transform.getRotation().set(rot);
        transform.getScale().set(scale);
        // push to ODE once so it starts from this pose
        rigidBody.applyTransform(transform);
        return this;
    }

    @Override
    public void preTick() {
        // Save old transform for interpolation
        transform.copyTo(oldTransform);

        // For kinematic bodies, drive ODE from Transform each frame
        if (rigidBody.getBody() != null && rigidBody.getBody().isKinematic())
            rigidBody.applyTransform(transform);
    }

    @Override
    public void tick() {
        // For dynamic bodies, pull Transform from ODE after simulation
        if (rigidBody.getBody() != null && !rigidBody.getBody().isKinematic())
            rigidBody.syncTransform(transform);
    }

    @Override
    public boolean render(MatrixStack matrices, Camera camera, float delta) {
        if (mesh == null)
            return false;

        matrices.pushMatrix();
        matrices.translate(Maths.lerp(oldTransform.getPosition(), transform.getPosition(), delta));
        matrices.rotate(new Quaternionf(oldTransform.getRotation()).slerp(transform.getRotation(), delta));
        matrices.translate(mesh.getOffset());
        // first scale mesh to its intended collider size
        matrices.scale(mesh.getScale());
        // then apply any object-level scaling
        matrices.scale(Maths.lerp(oldTransform.getScale(), transform.getScale(), delta));
        boolean canRender = shouldRender(matrices, camera);

        if (canRender)
            mesh.render(matrices, camera, delta);

        AABB bb = mesh.getAABB(new AABB());
        VertexConsumer.LINES.consume(GeometryHelper.box(matrices, bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ(), 0xFFFF0000));

        bb.applyMatrix(matrices.peek().pos());
        matrices.popMatrix();

        VertexConsumer.LINES.consume(GeometryHelper.box(matrices, bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ(), 0xFFFFFFFF));

        return canRender;
    }

    @Override
    public boolean shouldRender(MatrixStack matrices, Camera camera) {
        AABB bb = mesh.getAABB(new AABB());
        Matrix4f mat = matrices.peek().pos();

        for (int i = 0; i < 8; i++) {
            float x = (i & 1) == 0 ? bb.minX() : bb.maxX();
            float y = (i & 2) == 0 ? bb.minY() : bb.maxY();
            float z = (i & 4) == 0 ? bb.minZ() : bb.maxZ();

            float nx = Math.fma(mat.m00(), x, Math.fma(mat.m10(), y, Math.fma(mat.m20(), z, mat.m30())));
            float ny = Math.fma(mat.m01(), x, Math.fma(mat.m11(), y, Math.fma(mat.m21(), z, mat.m31())));
            float nz = Math.fma(mat.m02(), x, Math.fma(mat.m12(), y, Math.fma(mat.m22(), z, mat.m32())));

            if (camera.isInsideFrustum(nx, ny, nz))
                return true;
        }

        return false;
    }
}
