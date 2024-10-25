package cinnamon.world.entity.terrain;

import cinnamon.Client;
import cinnamon.render.MatrixStack;
import cinnamon.render.shader.Shader;
import cinnamon.utils.ColorUtils;
import cinnamon.utils.Resource;
import org.joml.Vector3f;

import java.util.UUID;

public class DiscoFloor extends TerrainEntity {

    private static final Resource MODEL = new Resource("models/entities/terrain/disco_floor/floor.obj");

    public DiscoFloor(UUID uuid) {
        super(uuid, MODEL);
    }

    @Override
    protected void renderModel(MatrixStack matrices, float delta) {
        Shader.activeShader.applyColor(ColorUtils.hsvToRGB(new Vector3f((Client.getInstance().ticks + delta) % 255 / 255f, 1f, 1f)));

        //render
        super.renderModel(matrices, delta);

        //reset
        Shader.activeShader.applyColor(0xFFFFFF);
    }
}
