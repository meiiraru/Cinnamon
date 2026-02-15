package cinnamon.render;

import cinnamon.Client;
import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.texture.Texture;
import cinnamon.sound.SoundInstance;
import cinnamon.utils.AABB;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import cinnamon.utils.Rotation;
import cinnamon.world.light.Light;
import cinnamon.world.light.PointLight;
import cinnamon.world.light.Spotlight;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class DebugRenderer {

    public static final Resource
            SPEAKER = new Resource("textures/environment/debug/speaker.png"),
            LAMP = new Resource("textures/environment/debug/lamp.png"),
            LAMP_OVERLAY = new Resource("textures/environment/debug/lamp_overlay.png");

    public static void renderPoint(MatrixStack matrices, Vector3f pos, float radius, int color) {
        VertexConsumer.MAIN.consume(GeometryHelper.sphere(matrices, pos.x, pos.y, pos.z, radius, 8, color));
    }

    public static void renderAABB(MatrixStack matrices, AABB aabb, int color) {
        VertexConsumer.LINES.consume(GeometryHelper.box(matrices, aabb.minX(), aabb.minY(), aabb.minZ(), aabb.maxX(), aabb.maxY(), aabb.maxZ(), color));
    }

    public static void renderArrow(MatrixStack matrices, Vector3f dir, float len, int color) {
        renderArrow(matrices, dir.x, dir.y, dir.z, len, color);
    }

    public static void renderArrow(MatrixStack matrices, float dirX, float dirY, float dirZ, float len, int color) {
        matrices.pushMatrix();
        matrices.rotate(Maths.dirToQuat(dirX, dirY, dirZ));
        VertexConsumer.LINES.consume(GeometryHelper.line(matrices, 0f, 0f, 0f, 0f, 0f, len, 0.001f, color));

        matrices.translate(0f, 0f, len);
        matrices.rotate(Rotation.X.rotationDeg(90f));
        VertexConsumer.LINES.consume(GeometryHelper.cone(matrices, 0f, 0f, 0f, 0.05f, 0.025f, 5, 1f, false, color));
        matrices.popMatrix();
    }

    public static void renderTextureOnScreen(MatrixStack matrices, int texture, boolean overlay) {
        Client c = Client.getInstance();

        //debug quad
        float w = c.window.getGUIWidth() * (overlay ? 1f : 0.3f);
        float h = overlay ? c.window.getGUIHeight() : w;
        float x = overlay ? 0 : c.window.getGUIWidth() - w - 4;
        float y = overlay ? 0 : 4;

        if (!overlay) {
            int texWidth = Texture.getWidth(texture);
            int texHeight = Texture.getHeight(texture);
            float aspect = (float) texWidth / (float) texHeight;
            h = w / aspect;
        }

        VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices, x, y, w, h, 0, 1, 1, -1, 1, 1), texture);
        VertexConsumer.MAIN.finishBatch(c.camera);
    }

    public static void renderFrustum(MatrixStack matrices, Matrix4f viewProjMatrix, int color) {
        float s = 0.1f;
        Vector3f[] corners = Frustum.calculateCorners(viewProjMatrix);

        //near/far
        for (int i = 0; i < 2; i++) {
            int j = i * 4;
            int[] order = {j, j + 1, j + 3, j + 2};

            for (int k = 0; k < 4; k++) {
                Vector3f
                        a = corners[order[k]],
                        b = corners[order[(k + 1) % 4]];

                VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, a.x, a.y, a.z, b.x, b.y, b.z, s, color));
            }
        }

        //sides
        for (int i = 0; i < 4; i++) {
            Vector3f
                    near = corners[i],
                    far = corners[i + 4];

            VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, near.x, near.y, near.z, far.x, far.y, far.z, s, color));
        }
    }

    public static void renderLight(Light light, Camera camera, MatrixStack matrices) {
        Vector3f pos = light.getPos();
        if (camera.getPos().distanceSquared(pos) <= 0.1f)
            return;

        matrices.pushMatrix();
        matrices.translate(pos);

        int c = light.getColor() | 0xFF000000;
        Light.Type type = light.getType();
        if (type != Light.Type.POINT) //point lights have no direction
            renderArrow(matrices, light.getDirection(), 0.5f, c);

        camera.billboard(matrices);
        matrices.rotate(Rotation.Z.rotationDeg(180f));

        Vertex[] v = GeometryHelper.quad(matrices, -0.25f, -0.25f, 0.5f, 0.5f);
        VertexConsumer.MAIN.consume(v, LAMP);

        for (Vertex vertex : v)
            vertex.color(c);
        VertexConsumer.MAIN.consume(v, LAMP_OVERLAY);

        matrices.popMatrix();

        //if ((type == Light.Type.SPOT || type == Light.Type.COOKIE) && light.castsShadows())
        //    renderFrustum(matrices, light.getLightSpaceMatrix(), c);

        renderLightMesh(light, matrices);
    }

    public static void renderLightMesh(Light light, MatrixStack matrices) {
        Vector3f pos = light.getPos();
        int color = light.getColor();

        switch (light.getType()) {
            case POINT -> {
                PointLight point = (PointLight) light;
                matrices.pushMatrix();
                matrices.translate(pos);
                matrices.scale(point.getFalloffEnd());
                VertexConsumer.LINES.consume(GeometryHelper.sphere(matrices, 0, 0, 0, 1f, 12, color | 0xFF000000));

                matrices.popMatrix();
            }
            case SPOT, COOKIE -> {
                Spotlight spot = (Spotlight) light;
                float height = spot.getFalloffEnd();
                float radius = height * Math.tan(Math.toRadians(spot.getOuterAngle()));

                matrices.pushMatrix();

                matrices
                        .translate(pos)
                        .rotate(Maths.dirToQuat(light.getDirection()))
                        .rotate(Rotation.X.rotation(-Math.PI_OVER_2_f))
                        .scale(radius, height, radius);
                VertexConsumer.LINES.consume(GeometryHelper.cone(matrices, 0, -1, 0, 1f, 1f, 12, color | 0xFF000000));

                matrices.popMatrix();
            }
        }
    }

    public static void renderSound(SoundInstance s, Camera camera, MatrixStack matrices) {
        Vector3f pos = s.getPos();
        if (pos.distance(camera.getPosition()) > s.getMaxDistance())
            return;

        matrices.pushMatrix();
        matrices.translate(pos);

        camera.billboard(matrices);
        matrices.rotate(Rotation.Z.rotationDeg(180f));

        VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices, -0.25f, -0.25f, 0.5f, 0.5f), SPEAKER);

        matrices.popMatrix();
    }
}
