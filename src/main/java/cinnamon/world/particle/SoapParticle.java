package cinnamon.world.particle;

import cinnamon.Client;
import cinnamon.model.Vertex;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.ColorUtils;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class SoapParticle extends BubbleParticle {

    public SoapParticle(int lifetime) {
        super(lifetime, 0xFFFFFFFF);
    }

    @Override
    protected void drawParticle(float delta, VertexConsumer consumer, Vertex[] vertices) {
        Vector3f pos = getPos(delta);
        Vector4f screenSpace = Client.getInstance().camera.worldToScreenSpace(pos.x, pos.y, pos.z);
        vertices[3].color(ColorUtils.lerpRGBColorThroughHSV(0xFF0000, 0xFF0000, screenSpace.x * 0.5f + 0.5f, true) + (0xFF << 24));
        consumer.consume(vertices, texture.getResource());
    }
}
