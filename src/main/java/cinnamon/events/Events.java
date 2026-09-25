package cinnamon.events;

import cinnamon.gui.GUISkin;
import cinnamon.lang.LangManager;
import cinnamon.logger.Logger;
import cinnamon.model.MaterialManager;
import cinnamon.model.ModelManager;
import cinnamon.registry.MaterialRegistry;
import cinnamon.render.Font;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.shader.PostProcess;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.AnimatedTexture;
import cinnamon.render.texture.CubeMap;
import cinnamon.render.texture.SkyBox;
import cinnamon.render.texture.Texture;
import cinnamon.settings.Settings;
import cinnamon.sound.Sound;
import cinnamon.sound.SoundManager;

public class Events {

    public static final Logger LOGGER = new Logger(Logger.ROOT_NAMESPACE + "/resource");

    public static void registerClientEvents() {
        LOGGER.info("Registering client resource events");

        CoreEvents.RESOURCE_INIT.register(() -> {
            SoundManager.swapDevice(Settings.soundDevice.get());
            Shaders.loadAll();
            PostProcess.loadAllShaders();
            MaterialRegistry.loadAllMaterials();
            LangManager.init();
            GUISkin.init();
        });

        CoreEvents.RESOURCE_FREE.register(() -> {
            Texture.freeAll();
            AnimatedTexture.freeAll();
            CubeMap.freeAll();
            SoundManager.stopAll();
            Sound.freeAllSounds();
            Shader.freeCache();
            Shaders.freeAll();
            PostProcess.free();
            ModelManager.free();
            MaterialManager.free();
            SkyBox.freeAll();
            VertexConsumer.freeBatches();
            GUISkin.free();
            Font.freeAll();
        });
    }
}
