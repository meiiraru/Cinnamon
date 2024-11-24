package cinnamon.resource;

import cinnamon.gui.GUIStyle;
import cinnamon.gui.screens.MainMenu;
import cinnamon.model.MaterialManager;
import cinnamon.model.ModelManager;
import cinnamon.registry.EntityModelRegistry;
import cinnamon.registry.LivingModelRegistry;
import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.shader.PostProcess;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.CubeMap;
import cinnamon.render.texture.Texture;
import cinnamon.sound.Sound;
import cinnamon.sound.SoundManager;
import cinnamon.world.SkyBox;

import java.util.ArrayList;
import java.util.List;

import static cinnamon.Client.LOGGER;

public class ResourceManager {

    private static final List<Runnable>
            INIT_EVENTS = new ArrayList<>(),
            FREE_EVENTS = new ArrayList<>();

    public static void register() {
        LOGGER.info("Registering resource events");

        INIT_EVENTS.add(Shaders::loadAll);
        INIT_EVENTS.add(PostProcess::loadAllShaders);
        INIT_EVENTS.add(TerrainRegistry::loadAllModels);
        INIT_EVENTS.add(LivingModelRegistry::loadAllModels);
        INIT_EVENTS.add(EntityModelRegistry::loadAllModels);
        INIT_EVENTS.add(MaterialRegistry::loadAllMaterials);
        INIT_EVENTS.add(SkyBox.Type::loadAll);
        INIT_EVENTS.add(GUIStyle::init);
        INIT_EVENTS.add(MainMenu::initTextures);

        FREE_EVENTS.add(Texture::freeAll);
        FREE_EVENTS.add(CubeMap::freeAll);
        FREE_EVENTS.add(SoundManager::stopAll);
        FREE_EVENTS.add(Sound::freeAllSounds);
        FREE_EVENTS.add(Shader::freeCache);
        FREE_EVENTS.add(Shaders::freeAll);
        FREE_EVENTS.add(PostProcess::free);
        FREE_EVENTS.add(ModelManager::free);
        FREE_EVENTS.add(MaterialManager::free);
        FREE_EVENTS.add(SkyBox.Type::freeAll);
        FREE_EVENTS.add(VertexConsumer::freeBatches);
    }

    public static void init() {
        for (Runnable freeEvent : INIT_EVENTS) {
            freeEvent.run();
        }
    }

    public static void free() {
        for (Runnable freeEvent : FREE_EVENTS) {
            freeEvent.run();
        }
    }
}
