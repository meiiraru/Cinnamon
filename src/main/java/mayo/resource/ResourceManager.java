package mayo.resource;

import mayo.model.MaterialManager;
import mayo.model.ModelManager;
import mayo.registry.*;
import mayo.render.Texture;
import mayo.render.framebuffer.PostProcess;
import mayo.render.shader.Shader;
import mayo.render.shader.Shaders;
import mayo.sound.Sound;

import java.util.ArrayList;
import java.util.List;

public class ResourceManager {

    private static final List<Runnable>
            INIT_EVENTS = new ArrayList<>(),
            FREE_EVENTS = new ArrayList<>();

    public static void register() {
        INIT_EVENTS.add(Shaders::loadAll);
        INIT_EVENTS.add(PostProcess::loadAllShaders);
        INIT_EVENTS.add(TerrainRegistry::loadAllModels);
        INIT_EVENTS.add(LivingModelRegistry::loadAllModels);
        INIT_EVENTS.add(EntityModelRegistry::loadAllModels);
        INIT_EVENTS.add(ParticlesRegistry::loadAllTextures);
        INIT_EVENTS.add(MaterialRegistry::loadAllMaterials);

        FREE_EVENTS.add(Texture::freeAll);
        FREE_EVENTS.add(Sound::freeAllSounds);
        FREE_EVENTS.add(Shader::freeCache);
        FREE_EVENTS.add(Shaders::freeAll);
        FREE_EVENTS.add(PostProcess::free);
        FREE_EVENTS.add(ModelManager::free);
        FREE_EVENTS.add(MaterialManager::free);
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
