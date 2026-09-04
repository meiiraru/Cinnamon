package cinnamon.world.world;

import cinnamon.Client;
import cinnamon.animation.Animation;
import cinnamon.gui.DebugScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.screens.world.ChatScreen;
import cinnamon.gui.screens.world.DeathScreen;
import cinnamon.gui.screens.world.PauseScreen;
import cinnamon.input.Controller;
import cinnamon.input.Keybind;
import cinnamon.math.Maths;
import cinnamon.math.collision.Hit;
import cinnamon.math.collision.shape.AABB;
import cinnamon.math.collision.shape.Sphere;
import cinnamon.model.GeometryHelper;
import cinnamon.registry.LivingModelRegistry;
import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.render.Camera;
import cinnamon.render.DebugRenderer;
import cinnamon.render.MatrixStack;
import cinnamon.render.WorldRenderer;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.shader.PostProcess;
import cinnamon.settings.Settings;
import cinnamon.sound.SoundCategory;
import cinnamon.sound.SoundInstance;
import cinnamon.sound.SoundManager;
import cinnamon.utils.Pair;
import cinnamon.utils.Resource;
import cinnamon.vr.XrManager;
import cinnamon.vr.XrRenderer;
import cinnamon.world.Abilities;
import cinnamon.world.Decal;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.entity.living.LocalPlayer;
import cinnamon.world.entity.living.Player;
import cinnamon.world.gui.Hud;
import cinnamon.world.gui.Overlay;
import cinnamon.world.items.Item;
import cinnamon.world.light.DirectionalLight;
import cinnamon.world.light.Light;
import cinnamon.world.particle.ExplosionParticle;
import cinnamon.world.particle.Particle;
import cinnamon.world.sky.DynamicSky;
import cinnamon.world.sky.Sky;
import cinnamon.world.sky.SkyColors;
import cinnamon.world.terrain.PrimitiveTerrain;
import cinnamon.world.terrain.Terrain;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;

public class WorldClient extends World {

    public Supplier<Screen>
            chatScreen = ChatScreen::new,
            pauseScreen = PauseScreen::new,
            deathScreen = DeathScreen::new;

    protected Hud hud = new Hud();
    protected Overlay overlay;

    protected Client client;
    public LocalPlayer player;

    protected int cameraMode = 0;
    protected boolean enableDebugKeys = false;

    //lights
    protected final List<Light> lights = new ArrayList<>();
    protected final Light sunlight = new DirectionalLight().pos(0.5f, 5f, 0.5f).intensity(1f);

    //particles and decals
    protected final List<Particle> particles = new ArrayList<>();
    protected final List<Decal> decals = new ArrayList<>();

    //skybox
    protected Sky sky = new DynamicSky();
    protected final SkyColors skyColors = new SkyColors();
    private final SkyColors.SkyProperties renderSkyProps = new SkyColors.SkyProperties();

    @Override
    public void init() {
        //set client
        client = Client.getInstance();
        client.setScreen(null);
        client.world = this;
        client.hideHUD = false;

        //init hud
        hud.init();

        //sky
        addLight(sunlight);
        setSkyColors();

        //create player
        respawn(true);

        //SERVER STUFF
        levelLoad();

        runScheduledTicks();

        //request world data
        //connection.sendTCP(new Login());
    }

    protected void levelLoad() {
        //simple platform
        Terrain platform = new PrimitiveTerrain(GeometryHelper.box(null, -15, -1, -15, 15, 0, 15, 0xFFFFFFFF));
        platform.setMaterial(MaterialRegistry.DEBUG.material);
        addTerrain(platform);
    }

    public void reconstructWorld() {
        scheduledTicks.add(() -> {
            //remove everything
            for (Entity e : new ArrayList<>(entities.values())) {
                if (e != player)
                    e.remove();
            }
            entities.clear();
            terrainManager.clear();
            lights.clear();
            particles.clear();
            decals.clear();

            //reload the level
            addLight(sunlight);
            addEntity(player);
            levelLoad();
        });
    }

    @Override
    public void close() {
        SoundManager.stopAll(c -> c != SoundCategory.GUI && c != SoundCategory.MASTER);
        this.sky.free();
        this.hud.free();
        //ServerConnection.close();
    }

    @Override
    public void tick() {
        super.tick();

        if (!isPaused()) {
            //particles
            for (Iterator<Particle> iterator = particles.iterator(); iterator.hasNext(); ) {
                Particle p = iterator.next();
                if (p.isRemoved())
                    iterator.remove();
                else
                    p.tick();
            }

            //decals
            for (Iterator<Decal> iterator = decals.iterator(); iterator.hasNext(); ) {
                Decal d = iterator.next();
                if (d.isRemoved())
                    iterator.remove();
                else
                    d.tick();
            }
        }

        //process input
        tickInput();

        //hud
        this.hud.tick();
        if (this.overlay != null) {
            this.overlay.tick();
            if (client.screen != null || this.overlay.isClosed())
                this.closeOverlay();
        }
    }

    public void render(MatrixStack matrices, float delta) {
        if (player.getWorld() == null)
            return;

        float d = isPaused() ? 1f : delta;
        float dt = isPaused() ? 0f : client.timer.deltaTime;
        boolean xr = XrManager.isInXR();

        //set camera
        updateCamera(client.camera, player, cameraMode, d);

        //view bobbing
        if (!xr) WorldRenderer.viewBobbing(WorldRenderer.camera, dt);

        //finish camera setup
        WorldRenderer.camera.updateFrustum();
        SoundManager.updateSoundPosition(WorldRenderer.camera);

        //prepare sun
        updateSky(WorldRenderer.camera, d);

        //render our stuff
        WorldRenderer.renderWorld(this, matrices, d);

        //post render world
        postWorldRender(matrices, d);

        //render first-person hand
        if (!client.hideHUD && !isThirdPerson()) {
            if (!xr) glClear(GL_DEPTH_BUFFER_BIT); //top of world
            WorldRenderer.renderHoldingItems(WorldRenderer.camera, matrices, d);
        }

        //render hud
        if (!client.hideHUD) {
            matrices.pushMatrix();
            if (xr) XrRenderer.applyGUITransform(matrices);
            else glClear(GL_DEPTH_BUFFER_BIT); //top of hand

            renderHUD(matrices, delta);
            if (this.overlay != null)
                overlay.render(matrices, delta);
            matrices.popMatrix();
        }
    }

    protected void postWorldRender(MatrixStack matrices, float delta) {
        if (player.isDead())
            PostProcess.apply(PostProcess.GRAYSCALE);
    }

    protected void updateCamera(Camera sourceCamera, Entity camEntity, int cameraMode, float delta) {
        WorldRenderer.camera.copyFrom(sourceCamera, true);
        WorldRenderer.camera.useOrtho(false);
        WorldRenderer.camera.setEntity(camEntity);
        WorldRenderer.camera.setup(cameraMode, delta);
    }

    protected void updateSky(Camera camera, float delta) {
        sky.setSunAngle(Maths.map(worldTime + delta, 0, dayLength, 0, 360));

        Vector3f dir = sky.getSunDirection();
        Vector3f pos = camera.getPos();
        float dist = Camera.FAR_PLANE - 1f;
        sunlight.glareSize(dist);
        sunlight.direction(dir);
        sunlight.pos(pos.x - dir.x * dist, pos.y - dir.y * dist, pos.z - dir.z * dist);

        //apply light
        applySkyLights(getDayMinutes(delta));
    }

    protected void applySkyLights(float dayMinutes) {
        SkyColors.SkyProperties props = skyColors.getPropertiesAtTime(dayMinutes, renderSkyProps);
        if (props == null)
            return;

        this.sky.sunColor = props.sunColor();
        this.sky.skyColor = props.skyColor();
        this.sky.fogColor = props.fogColor();
        this.sky.cloudsColor = props.cloudsColor();
        this.sky.ambientLight = props.ambientLight();
        this.sky.fogStart = props.fogStart();
        this.sky.fogEnd = props.fogEnd();
        this.sky.fogIntensity = props.fogIntensity();
        this.sky.sunIntensity = props.sunIntensity();
        this.sky.starsIntensity = props.starsIntensity();

        sunlight.color(props.sunlightColor());
        sunlight.intensity(Math.min(props.sunlightIntensity(), 1f));
        sunlight.shadowIntensity(props.sunlightShadowIntensity());
    }

    public int renderTerrain(Camera camera, MatrixStack matrices, float delta) {
        int count = 0;
        List<Terrain> query = terrainManager.queryCustom(camera::isInsideFrustum);
        for (Terrain terrain : query) {
            if (terrain.shouldRender(camera)) {
                terrain.render(camera, matrices, delta);
                count++;
            }
        }
        return count;
    }

    public int renderEntities(Camera camera, MatrixStack matrices, float delta) {
        int count = 0;
        for (Entity entity : entities.values()) {
            if (entity.shouldRender(camera)) {
                entity.render(camera, matrices, delta);
                count++;
            }
        }
        return count;
    }

    public int renderParticles(Camera camera, MatrixStack matrices, float delta) {
        int count = 0;
        for (Particle particle : particles) {
            if (particle.shouldRender(camera)) {
                particle.render(camera, matrices, delta);
                count++;
            }
        }
        return count;
    }

    public void renderExtras(Camera camera, MatrixStack matrices, float delta) {}

    public void renderWater(Camera camera, MatrixStack matrices, float delta) {}

    public void renderFire(Camera camera, MatrixStack matrices, float delta) {}

    public void renderTransparent(Camera camera, MatrixStack matrices, float delta) {
        List<Terrain> query = terrainManager.queryCustom(camera::isInsideFrustum);
        for (Terrain terrain : query) {
            if (terrain.shouldRender(camera))
                terrain.renderTransparent(camera, matrices, delta);
        }

        for (Entity entity : entities.values()) {
            if (entity.shouldRender(camera))
                entity.renderTransparent(camera, matrices, delta);
        }

        for (Particle particle : particles) {
            if (particle.shouldRender(camera))
                particle.renderTransparent(camera, matrices, delta);
        }
    }

    public void renderItemExtra(LivingEntity entity, MatrixStack matrices, float delta) {
        Item item = entity.getHoldingItem();
        if (item == null)
            return;

        item.worldRender(matrices, delta);
    }

    public void renderHUD(MatrixStack matrices, float delta) {
        this.hud.render(matrices, delta);
    }

    public void renderDebug(Camera camera, MatrixStack matrices, float delta) {
        if (client.hideHUD)
            return;

        Entity cameraEntity = camera.getEntity();

        if (DebugScreen.isTabOpen(DebugScreen.Tab.PLAYER))
            renderDebugPlayer(cameraEntity, matrices, delta);

        if (DebugScreen.isTabOpen(DebugScreen.Tab.WORLD, DebugScreen.Tab.TERRAIN, DebugScreen.Tab.ENTITIES))
            renderHitboxes(camera, matrices, delta);

        //sounds
        if (DebugScreen.isTabOpen(DebugScreen.Tab.WORLD, DebugScreen.Tab.SOUND)) {
            for (SoundInstance s : SoundManager.getSounds())
                DebugRenderer.renderSound(s, camera, matrices);
        }

        if (cameraEntity instanceof Player p && p.getAbilities().get(Abilities.Ability.CAN_BUILD) && !p.isDead())
            renderTargetedBlock(p, matrices, delta);

        VertexConsumer.finishAllBatches(camera);
    }

    protected void renderHitboxes(Camera camera, MatrixStack matrices, float delta) {
        Entity cameraEntity = camera.getEntity();
        Vector3f cameraPos = camera.getPos();
        AABB area = new AABB();
        area.translate(cameraPos);
        area.inflate(8f);

        if (DebugScreen.isTabOpen(DebugScreen.Tab.WORLD)) {
            //lights
            for (Light light : lights)
                if (light.shouldRender(camera))
                    DebugRenderer.renderLight(light, camera, matrices);

            //particles
            for (Particle p : getParticles(area))
                p.renderDebugHitbox(matrices, delta);

            //decals
            for (Decal d : decals)
                d.renderDebugHitbox(matrices, delta);
        }

        if (DebugScreen.isTabOpen(DebugScreen.Tab.TERRAIN)) {
            //octree
            //for (AABB aabb : terrainManager.getBounds())
            //    DebugRenderer.renderAABB(matrices, aabb, 0xFF00FF00);

            //terrain
            for (Terrain t : terrainManager.query(area))
                t.renderDebugHitbox(matrices, delta);

            //placement terrain
            Pair<Hit, Terrain> hit = cameraEntity.getLookingTerrain(cameraEntity.getPickRange());
            if (hit != null) {
                Vector3f pos = new Vector3f(hit.first().position()).floor();
                Terrain terrain = hit.second();
                if (terrain != null && pos.equals(terrain.getTransform().getPos()))
                    pos.add(hit.first().normal());

                DebugRenderer.renderAABB(matrices, new AABB(pos, pos).expand(1f, 1f, 1f), 0xFFFF0000);
            }
        }

        if (DebugScreen.isTabOpen(DebugScreen.Tab.ENTITIES)) {
            //entities
            for (Entity e : getEntities(area)) {
                if (e != cameraEntity || isThirdPerson())
                    e.renderDebugHitbox(matrices, delta);
            }
        }
    }

    protected static void renderDebugPlayer(Entity cameraEntity, MatrixStack matrices, float delta) {
        float f = 0.03f;
        float r = cameraEntity.getPickRange();

        Pair<Hit, Terrain> terrain = cameraEntity.getLookingTerrain(r);
        if (terrain != null) {
            Vector3f pos = terrain.first().position();
            DebugRenderer.renderPoint(matrices, pos, f, 0xFF00FFFF);
        }

        Pair<Hit, Entity> entity = cameraEntity.getLookingEntity(r);
        if (entity != null) {
            Entity ent = entity.second();
            AABB aabb = ent.getAABB();
            DebugRenderer.renderAABB(matrices, aabb, 0xFFFFFF00);

            Vector3f pos = entity.first().position();
            DebugRenderer.renderPoint(matrices, pos, f, 0xFF00FFFF);
        }

        //draw hands debug
        if (cameraEntity instanceof Player player) {
            for (int i = 0; i < 2; i++) {
                boolean left = i == 0;
                Vector3f pos = player.getHandPos(left, delta);
                Vector3f dir = player.getHandDir(left, delta);
                Vector3f aimDir = player.getAimDir(left, delta, 20f);

                DebugRenderer.renderPoint(matrices, pos, f, 0xFF00FFFF);

                matrices.pushMatrix().translate(pos);
                DebugRenderer.renderArrow(matrices, dir, 0.25f, 0xFFFF00FF);
                DebugRenderer.renderArrow(matrices, aimDir, 0.25f, 0xFFFFFF00);
                matrices.popMatrix();
            }
        }
    }

    protected static void renderTargetedBlock(Player player, MatrixStack matrices, float delta) {
        float range = player.getPickRange();
        Pair<Hit, Entity> entity; Pair<Hit, Terrain> terrain;
        if (XrManager.isInXR()) {
            entity = player.raycastHandEntity(range);
            terrain = player.raycastHandTerrain(range);
        } else {
            entity = player.getLookingEntity(range);
            terrain = player.getLookingTerrain(range);
        }

        if (terrain == null || (entity != null && entity.first().tNear() < terrain.first().tNear()) || !terrain.second().isSelectable(player))
            return;

        int alpha = (int) Math.lerp(0x32, 0xFF, (Math.sin((Client.getInstance().ticks + delta) * 0.15f) + 1f) * 0.5f);
        terrain.second().renderTargeted(matrices, delta, 0xFFFFFF + (alpha << 24));
    }

    public Sky getSky() {
        return sky;
    }

    public void addLight(Light light) {
        scheduledTicks.add(() -> {
            this.lights.add(light);
            light.onAdded(this);
        });
    }

    public void removeLight(Light light) {
        scheduledTicks.add(() -> this.lights.remove(light));
    }

    public List<Light> getLights(Camera camera) {
        List<Light> lightsToRender = new ArrayList<>();
        for (Light l : lights)
            if (l.shouldRender(camera))
                lightsToRender.add(l);
        return lightsToRender;
    }

    public void addParticle(Particle particle) {
        scheduledTicks.add(() -> {
            this.particles.add(particle);
            particle.onAdded(this);
        });
    }

    public void addDecal(Decal decal) {
        scheduledTicks.add(() -> {
            this.decals.add(decal);
            decal.onAdded(this);
        });
    }

    public List<Entity> getOutlines(Camera camera) {
        List<Entity> entitiesToOutline = new ArrayList<>();
        for (Entity e : entities.values())
            if (e.shouldRender(camera) && e.shouldRenderOutline())
                entitiesToOutline.add(e);
        return entitiesToOutline;
    }

    public List<Light> getLights(AABB region) {
        List<Light> list = new ArrayList<>();
        for (Light light : this.lights) {
            if (region.intersects(light.getAABB()))
                list.add(light);
        }
        return list;
    }

    public List<Particle> getParticles(AABB region) {
        List<Particle> list = new ArrayList<>();
        for (Particle particle : this.particles) {
            if (region.intersects(particle.getAABB()))
                list.add(particle);
        }
        return list;
    }

    public List<Decal> getDecals(Camera camera) {
        List<Decal> decalsToRender = new ArrayList<>();
        for (Decal d : decals)
            if (d.shouldRender(camera))
                decalsToRender.add(d);
        return decalsToRender;
    }

    public SoundInstance playSound(Resource sound, SoundCategory category, Vector3f position) {
        return SoundManager.playSound(sound, category, position);
    }

    @Override
    public void explode(Sphere explosionArea, float strength, Entity source, boolean invisible) {
        super.explode(explosionArea, strength, source, invisible);

        if (invisible)
            return;

        //particles
        float volume = explosionArea.getVolume();
        for (int i = 0; i < volume; i++) {
            ExplosionParticle particle = new ExplosionParticle((int) (Math.random() * 10) + 15);
            particle.setPos(explosionArea.getRandomPoint(new Vector3f()));
            particle.setScale(Maths.range(3f, 7f));
            addParticle(particle);
        }

        //sound
        playSound(EXPLOSION_SOUND, SoundCategory.ENTITY, explosionArea.getCenter()).maxDistance(64f).volume(0.5f).pitch(Maths.range(0.8f, 1.2f));
    }

    protected void tickInput() {
        Entity e = player;
        while (e != null) {
            e.getController().tick();
            e = e.isRiding() ? e.getRidingEntity() : null;
        }
        Controller.clearTick();
        Keybind.flush();
    }

    public void mousePress(int button, int action, int mods) {
        if (overlay != null && overlay.stealsMouse() && overlay.mousePress(button, action, mods))
            return;

        if (!client.window.isMouseLocked())
            return;

        Keybind.mousePress(button, action, mods);
    }

    public void mouseMove(double x, double y) {
        if (!client.window.isMouseLocked() || XrManager.isInXR())
            return;
        Controller.mouseMove(x, y);
    }

    public void mouseScroll(double x, double y) {
        if (client.window.isMouseLocked())
            Controller.mouseScroll(x, y);
    }

    public void keyPress(int key, int scancode, int action, int mods) {
        if (key == GLFW_KEY_LEFT_ALT) {
            if (action == GLFW_PRESS) {
                client.window.unlockMouse();
            } else if (action == GLFW_RELEASE) {
                client.window.lockMouse();
                resetInput();
            }
        }

        Keybind.keyPress(key, scancode, action, mods);

        if (action == GLFW_RELEASE)
            return;

        boolean shift = (mods & GLFW_MOD_SHIFT) != 0;

        switch (key) {
            case GLFW_KEY_ESCAPE -> pause();
            case GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> {
                Screen chat = chatScreen.get();
                if (chat != null)
                    client.setScreen(chat);
            }
            case GLFW_KEY_F5 -> this.cameraMode = (this.cameraMode + 1) % 3;
            case GLFW_KEY_F7 -> {if (enableDebugKeys) this.worldTime = Math.max(this.worldTime - 100, 0);}
            case GLFW_KEY_F8 -> {if (enableDebugKeys) this.worldTime += 100;}

            case GLFW_KEY_COMMA -> player.setSelectedTerrain((player.getSelectedTerrain() + 1) % (TerrainRegistry.values().length - 1));
            case GLFW_KEY_PERIOD -> player.setSelectedMaterial(Maths.modulo((player.getSelectedMaterial() + (shift ? -1 : 1)), MaterialRegistry.values().length));

            //case GLFW_KEY_F9 -> connection.sendTCP(new Handshake());
            //case GLFW_KEY_F10 -> connection.sendUDP(new Message().msg("meow"));
        }
    }

    public void onWindowResize(int width, int height) {
        resetInput();
    }

    public void xrButtonPress(int button, boolean pressed, int hand) {
        if (client.window.isMouseLocked())
            Keybind.xrButtonPress(button, pressed, hand);
    }

    public void xrTriggerPress(int button, float value, int hand, float lastValue) {
        if (client.window.isMouseLocked())
            Keybind.xrTriggerPress(button, value, hand, lastValue);
    }

    public void xrJoystickMove(float x, float y, int hand, float lastX, float lastY) {}

    public void joystickButtonPress(int button, boolean pressed, int joystick) {}
    public void joystickAxisMove(int axis, float value, int joystick, float lastValue) {}
    public void joystickHatMove(int hat, byte hatState, int joystick, byte lastValue) {}

    public void gamepadButtonPress(int button, boolean pressed, int joystick) {
        if (client.window.isMouseLocked())
            Keybind.gamepadButtonPress(button, pressed, joystick);
    }

    public void gamepadAxisMove(int axis, float value, int joystick, float lastValue) {
        if (client.window.isMouseLocked())
            Keybind.gamepadAxisMove(axis, value, joystick, lastValue);
    }

    public void resetInput() {
        resetInput(true, true);
    }

    public void resetInput(boolean mouse, boolean keys) {
        Controller.reset();
        Keybind.releaseAll(mouse, keys);
    }

    public int getCameraMode() {
        return cameraMode;
    }

    public boolean isThirdPerson() {
        return cameraMode > 0;
    }

    @Override
    public void setPaused(boolean pause) {
        super.setPaused(pause);
        if (pause) {
            SoundManager.pauseAll(c -> c != SoundCategory.GUI && c != SoundCategory.MASTER);
            Animation.pauseAll();
        } else {
            SoundManager.resumePaused();
            Animation.resumePaused();
        }
    }

    protected void setSkyColors() {
        //sunrise
        skyColors.addProperty(6*60, new SkyColors.SkyProperties()
                        .sunColor(0xFF4400).skyColor(0x859090).ambientLight(0x101020).fogColor(0xE57E4B).cloudsColor(0x7F7F7F)
                        .fogStart(64f).fogEnd(80f).sunIntensity(1f).fogIntensity(1f).starsIntensity(0.5f)
                        .sunlightColor(0xFF4400).sunlightIntensity(3f).sunlightShadowIntensity(0f)
        );
        //day start
        skyColors.addProperty(7*60, new SkyColors.SkyProperties()
                .sunColor(0xFFEEDD).skyColor(0x446FD0).ambientLight(0xBBCCDD).fogColor(0xBFD3DE).cloudsColor(0xB0D0FF)
                .fogStart(96f).fogEnd(192f).sunIntensity(0f).fogIntensity(1f).starsIntensity(0.05f)
                .sunlightColor(0xFFEEDD).sunlightIntensity(5f).sunlightShadowIntensity(1f)
        );
        //day end
        skyColors.addProperty(17*60, new SkyColors.SkyProperties()
                .sunColor(0xFFEEDD).skyColor(0x446FD0).ambientLight(0xBBCCDD).fogColor(0xBFD3DE).cloudsColor(0xB0D0FF)
                .fogStart(96f).fogEnd(192f).sunIntensity(0f).fogIntensity(1f).starsIntensity(0.05f)
                .sunlightColor(0xFFEEDD).sunlightIntensity(5f).sunlightShadowIntensity(1f)
        );
        //sunset
        skyColors.addProperty(18*60, new SkyColors.SkyProperties()
                .sunColor(0xFF4400).skyColor(0x8844DD).ambientLight(0x101020).fogColor(0xFF72AD).cloudsColor(0x90507F)
                .fogStart(64f).fogEnd(80f).sunIntensity(1f).fogIntensity(1f).starsIntensity(0.5f)
                .sunlightColor(0xFF4400).sunlightIntensity(3f).sunlightShadowIntensity(0f)
        );
        //night start
        skyColors.addProperty(19*60, new SkyColors.SkyProperties()
                .sunColor(0x07070F).skyColor(0x0C0C18).ambientLight(0x101020).fogColor(0x0C0C18).cloudsColor(0x0A0A14)
                .fogStart(64f).fogEnd(80f).sunIntensity(0f).fogIntensity(1f).starsIntensity(5f)
                .sunlightColor(0x07070F).sunlightIntensity(0.1f).sunlightShadowIntensity(0f)
        );
        //night end
        skyColors.addProperty(5*60, new SkyColors.SkyProperties()
                .sunColor(0x07070F).skyColor(0x0C0C18).ambientLight(0x101020).fogColor(0x0C0C18).cloudsColor(0x0A0A14)
                .fogStart(64f).fogEnd(80f).sunIntensity(0f).fogIntensity(1f).starsIntensity(5f)
                .sunlightColor(0x07070F).sunlightIntensity(0.1f).sunlightShadowIntensity(0f)
        );
    }

    public void respawn(boolean init) {
        String playerName = Settings.playerName.get();
        player = new LocalPlayer(playerName.isBlank() ? "Player" : playerName, LivingModelRegistry.valueOf(Settings.playerModel.get()));
        player.setPos(0.5f, 0f, 0.5f);
        player.getAbilities().set(Abilities.Ability.CAN_FLY, true);
        this.addEntity(player);

        Animation anim = player.getAnimation("blink");
        if (anim != null)
            anim.setLoop(Animation.Loop.LOOP).play();

        //if (!init)
        //    connection.sendTCP(new Respawn());
    }

    public void pause() {
        setPaused(true);
        if (DebugScreen.pauseOnLostFocus) {
            Screen pause = pauseScreen.get();
            if (pause != null)
                client.setScreen(pause);
        }
    }

    @Override
    public boolean isClientside() {
        return true;
    }

    public void openOverlay(Overlay overlay) {
        if (this.overlay != null)
            this.overlay.close();

        this.overlay = overlay;
        if (overlay != null) {
            if (overlay.stealsMouse()) {
                client.window.unlockMouse();
                resetInput(true, false);
            }
            overlay.open();
        }
    }

    public void closeOverlay() {
        if (this.overlay == null)
            return;

        this.overlay.close();
        if (this.overlay.stealsMouse()) {
            client.window.lockMouse();
            resetInput(true, false);
        }
        this.overlay = null;
    }
}
