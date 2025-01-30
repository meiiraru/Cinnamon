package cinnamon.events;

import cinnamon.gui.GUIStyle;
import cinnamon.gui.Screen;
import cinnamon.gui.screens.MainMenu;
import cinnamon.model.MaterialManager;
import cinnamon.model.ModelManager;
import cinnamon.registry.MaterialRegistry;
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
import java.util.Map;
import java.util.function.Supplier;

import static cinnamon.Client.LOGGER;
import static cinnamon.events.EventType.RESOURCE_FREE;
import static cinnamon.events.EventType.RESOURCE_INIT;

public class Events {

    private Supplier<Screen> mainScreen = MainMenu::new;

    //event map
    private final Map<EventType, List<Runnable>> eventMap = Map.of(
            //tick events
            EventType.TICK_BEFORE_WORLD, new ArrayList<>(),
            EventType.TICK_BEFORE_GUI, new ArrayList<>(),
            EventType.TICK_END, new ArrayList<>(),

            //render events
            EventType.RENDER_BEFORE_WORLD, new ArrayList<>(),
            EventType.RENDER_BEFORE_GUI, new ArrayList<>(),
            EventType.RENDER_END, new ArrayList<>(),

            //resource events
            EventType.RESOURCE_INIT, new ArrayList<>(),
            EventType.RESOURCE_FREE, new ArrayList<>()
    );

    public void setMainScreen(Supplier<Screen> mainScreen) {
        this.mainScreen = mainScreen;
    }

    public Supplier<Screen> getMainScreen() {
        return mainScreen;
    }

    public void registerEvent(EventType type, Runnable event) {
        eventMap.get(type).add(event);
    }

    public void runEvents(EventType type) {
        for (Runnable runnable : eventMap.get(type))
            runnable.run();
    }

    public void registerClientEvents() {
        LOGGER.info("Registering client resource events");

        registerEvent(RESOURCE_INIT, Shaders::loadAll);
        registerEvent(RESOURCE_INIT, PostProcess::loadAllShaders);
        registerEvent(RESOURCE_INIT, MaterialRegistry::loadAllMaterials);
        registerEvent(RESOURCE_INIT, SkyBox.Type::loadAll);
        registerEvent(RESOURCE_INIT, GUIStyle::init);
        registerEvent(RESOURCE_INIT, MainMenu::initTextures);

        registerEvent(RESOURCE_FREE, Texture::freeAll);
        registerEvent(RESOURCE_FREE, CubeMap::freeAll);
        registerEvent(RESOURCE_FREE, SoundManager::stopAll);
        registerEvent(RESOURCE_FREE, Sound::freeAllSounds);
        registerEvent(RESOURCE_FREE, Shader::freeCache);
        registerEvent(RESOURCE_FREE, Shaders::freeAll);
        registerEvent(RESOURCE_FREE, PostProcess::free);
        registerEvent(RESOURCE_FREE, ModelManager::free);
        registerEvent(RESOURCE_FREE, MaterialManager::free);
        registerEvent(RESOURCE_FREE, SkyBox.Type::freeAll);
        registerEvent(RESOURCE_FREE, VertexConsumer::freeBatches);
    }
}
