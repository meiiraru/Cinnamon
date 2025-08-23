package cinnamon.events;

import cinnamon.gui.GUIStyle;
import cinnamon.gui.screens.MainMenu;
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
import cinnamon.sound.Sound;
import cinnamon.sound.SoundManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static cinnamon.events.EventType.RESOURCE_FREE;
import static cinnamon.events.EventType.RESOURCE_INIT;

public class Events {

    public static final Logger LOGGER = new Logger(Logger.ROOT_NAMESPACE + "/resource");

    //event map
    private final Map<EventType, List<Consumer<Object[]>>> eventMap = new HashMap<>(EventType.values().length, 1f);
    {
        for (EventType value : EventType.values())
            eventMap.put(value, new ArrayList<>());
    }

    public void registerEvent(EventType type, Consumer<Object[]> event) {
        eventMap.get(type).add(event);
    }

    public void runEvents(EventType type, Object... args) {
        for (Consumer<Object[]> consumer : eventMap.get(type))
            consumer.accept(args);
    }

    public void registerClientEvents() {
        LOGGER.info("Registering client resource events");

        registerEvent(RESOURCE_INIT, o -> SoundManager.swapDevice(SoundManager.getCurrentDevice()));
        registerEvent(RESOURCE_INIT, o -> Shaders.loadAll());
        registerEvent(RESOURCE_INIT, o -> PostProcess.loadAllShaders());
        registerEvent(RESOURCE_INIT, o -> MaterialRegistry.loadAllMaterials());
        registerEvent(RESOURCE_INIT, o -> MainMenu.initTextures());
        registerEvent(RESOURCE_INIT, o -> LangManager.init());

        registerEvent(RESOURCE_FREE, o -> Texture.freeAll());
        registerEvent(RESOURCE_FREE, o -> AnimatedTexture.freeAll());
        registerEvent(RESOURCE_FREE, o -> CubeMap.freeAll());
        registerEvent(RESOURCE_FREE, o -> SoundManager.stopAll());
        registerEvent(RESOURCE_FREE, o -> Sound.freeAllSounds());
        registerEvent(RESOURCE_FREE, o -> Shader.freeCache());
        registerEvent(RESOURCE_FREE, o -> Shaders.freeAll());
        registerEvent(RESOURCE_FREE, o -> PostProcess.free());
        registerEvent(RESOURCE_FREE, o -> ModelManager.free());
        registerEvent(RESOURCE_FREE, o -> MaterialManager.free());
        registerEvent(RESOURCE_FREE, o -> SkyBox.freeAll());
        registerEvent(RESOURCE_FREE, o -> VertexConsumer.freeBatches());
        registerEvent(RESOURCE_FREE, o -> GUIStyle.free());
        registerEvent(RESOURCE_FREE, o -> Font.freeAll());
    }
}
