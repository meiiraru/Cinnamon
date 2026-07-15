package cinnamon.world;

import cinnamon.Client;
import cinnamon.math.Maths;
import cinnamon.math.Rotation;
import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.render.MatrixStack;
import cinnamon.render.WorldRenderer;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Resource;
import cinnamon.world.entity.Entity;
import org.joml.Math;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Marker {

    public static final Resource MARKER_TEX = new Resource("textures/gui/hud/marker.png");
    public static final Resource MARKER_SND = new Resource("sounds/ui/marker.ogg");

    private final float x, y, z;
    private final Entity target;
    private final int color;
    private final Text message;
    private int lifetime;

    public Marker(Vector3f pos, Text message, int lifetime, int color) {
        this(pos.x, pos.y, pos.z, message, lifetime, color);
    }

    public Marker(float x, float y, float z, Text message, int lifetime, int color) {
        this(x, y, z, message, null, lifetime, color);
    }

    public Marker(Entity target, Text message, int lifetime, int color) {
        this(0f, 0f, 0f, message, target, lifetime, color);
    }

    protected Marker(float x, float y, float z, Text message, Entity target, int lifetime, int color) {
        this.x = x; this.y = y; this.z = z;
        this.target = target;
        this.color = color;
        this.message = message;
        this.lifetime = lifetime;
    }

    public void tick() {
        if (lifetime > 0)
            lifetime--;
    }

    public void render(MatrixStack matrices, float delta) {
        float x, y, z;
        if (target != null) {
            Vector3f pos = target.getEyePos(delta);
            x = pos.x; y = pos.y; z = pos.z;
        } else {
            x = this.x; y = this.y; z = this.z;
        }

        Vector4f screenPos = WorldRenderer.camera.worldToScreenSpace(x, y, z);

        //get the screen dimensions and center
        float screenWidth  = Client.getInstance().window.getGUIWidth();
        float screenHeight = Client.getInstance().window.getGUIHeight();
        float centerX = screenWidth  / 2f;
        float centerY = screenHeight / 2f;
        float padding = 8;

        float ndcX  = screenPos.x();
        float ndcY  = screenPos.y();
        float clipW = screenPos.z();
        float dist  = screenPos.w();

        float screenX = (ndcX * 0.5f + 0.5f) * screenWidth;
        float screenY = (1f - (ndcY * 0.5f + 0.5f)) * screenHeight;
        float rotationAngle = 0f;
        float uOffset = dist > 100f ? 16f : 0f;

        //check if the marker is off-screen or behind the camera
        boolean isBehind = clipW < 0;
        boolean isOffScreen = isBehind || screenX < padding || screenX > screenWidth - padding || screenY < padding || screenY > screenHeight - padding;

        //if were offscreen
        if (isOffScreen) {
            //uv offset for arrow instead of the full marker
            uOffset = 32f;

            //find the direction from the center of the screen to the marker position
            float dirX, dirY;
            if (!isBehind) {
                dirX = screenX - centerX;
                dirY = screenY - centerY;
            } else {
                dirX = -(screenX - centerX);
                dirY = -(screenY - centerY);

                //look down when looking perfectly away from the marker
                if (dirX == 0 && dirY == 0)
                    dirY = 1f;
            }

            //normalize the dir vector
            float length = Math.sqrt(dirX * dirX + dirY * dirY);
            if (length > 0) {
                dirX /= length;
                dirY /= length;
            }

            //rotation angle for the arrow to point towards the marker
            rotationAngle = Math.atan2(dirY, dirX) - Math.PI_OVER_2_f;

            //find the minimum scale from the marker position to the edge of the screen
            float maxEdgeX = centerX - padding;
            float maxEdgeY = centerY - padding;

            float scaleX = (dirX != 0) ? Math.abs(maxEdgeX / dirX) : Float.MAX_VALUE;
            float scaleY = (dirY != 0) ? Math.abs(maxEdgeY / dirY) : Float.MAX_VALUE;
            float finalScale = Math.min(scaleX, scaleY);

            //project the marker position onto the edge of the screen
            screenX = centerX + dirX * finalScale;
            screenY = centerY + dirY * finalScale;
        } else {
            //clamp the marker position to stay within the screen bounds
            screenX = Maths.clamp(screenX, padding, screenWidth  - padding);
            screenY = Maths.clamp(screenY, padding, screenHeight - padding);
        }

        matrices.pushMatrix();
        matrices.translate(screenX, screenY, 0f);

        //rotate when off-screen
        if (isOffScreen)
            matrices.rotate(Rotation.Z.rotation(rotationAngle));

        //draw marker
        Vertex[] vertices = GeometryHelper.quad(matrices,
                -8f, -8f,
                16f, 16f,
                uOffset, 0f,
                16f, 16f,
                48, 16
        );
        for (Vertex vertex : vertices)
            vertex.color(color);
        VertexConsumer.MAIN.consume(vertices, MARKER_TEX);

        matrices.popMatrix();

        //check if were looking at the marker
        float lookingPadding = 32f;
        boolean isLookingAt = !isOffScreen && screenX > centerX - lookingPadding && screenX < centerX + lookingPadding && screenY > centerY - lookingPadding && screenY < centerY + lookingPadding;

        //draw distance text
        if (isLookingAt) {
            Style style = Style.EMPTY.color(color).background(true).outlined(true).guiSkin(Hud.SKIN);

            Text.of(String.format("%.1fm", dist))
                    .withStyle(style)
                    .render(VertexConsumer.MAIN, matrices, screenX, screenY + 8 + 4, Alignment.TOP_CENTER);

            if (message != null)
                Text.empty()
                        .withStyle(style)
                        .append(message)
                        .render(VertexConsumer.MAIN, matrices, screenX, screenY - 8 - 4, Alignment.BOTTOM_CENTER);
        }
    }

    public boolean isRemoved() {
        return lifetime == 0 || (target != null && target.isRemoved());
    }
}
