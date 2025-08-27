package cinnamon.world.entity.terrain;

import cinnamon.Client;
import cinnamon.registry.TerrainEntityRegistry;
import cinnamon.render.MatrixStack;
import cinnamon.render.WorldRenderer;
import cinnamon.render.shader.Shader;
import cinnamon.utils.ColorUtils;
import org.joml.Vector3f;

import java.util.UUID;

public class DiscoFloor extends TerrainEntity {

    public DiscoFloor(UUID uuid) {
        super(uuid, TerrainEntityRegistry.DISCO_FLOOR.resource);
    }

    @Override
    protected void renderModel(MatrixStack matrices, float delta) {
        if (!WorldRenderer.isOutlineRendering())
            Shader.activeShader.applyColor(ColorUtils.hsvToRGB(new Vector3f((Client.getInstance().ticks + delta) % 255 / 255f, 1f, 1f)));

        //render
        super.renderModel(matrices, delta);

        //reset
        Shader.activeShader.applyColor(0xFFFFFF);
    }
}
