package cinnamon.world.world;

import cinnamon.Client;
import cinnamon.animation.Animation;
import cinnamon.gui.DebugScreen;
import cinnamon.gui.Toast;
import cinnamon.gui.screens.world.ChatScreen;
import cinnamon.gui.screens.world.PauseScreen;
import cinnamon.input.Interaction;
import cinnamon.input.Keybind;
import cinnamon.input.Movement;
import cinnamon.model.GeometryHelper;
import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.SkyBoxRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.WorldRenderer;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;
import cinnamon.text.Text;
import cinnamon.utils.AABB;
import cinnamon.utils.Colors;
import cinnamon.utils.Maths;
import cinnamon.vr.XrHandTransform;
import cinnamon.vr.XrManager;
import cinnamon.vr.XrRenderer;
import cinnamon.world.Hud;
import cinnamon.world.Sky;
import cinnamon.world.collisions.Hit;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.Spawner;
import cinnamon.world.entity.collectable.EffectBox;
import cinnamon.world.entity.collectable.HealthPack;
import cinnamon.world.entity.living.Dummy;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.entity.living.LocalPlayer;
import cinnamon.world.entity.living.Player;
import cinnamon.world.entity.misc.Firework;
import cinnamon.world.entity.misc.FireworkStar;
import cinnamon.world.entity.vehicle.Cart;
import cinnamon.world.entity.vehicle.ShoppingCart;
import cinnamon.world.items.BubbleGun;
import cinnamon.world.items.Flashlight;
import cinnamon.world.items.Item;
import cinnamon.world.items.ItemRenderContext;
import cinnamon.world.items.MagicWand;
import cinnamon.world.items.weapons.CoilGun;
import cinnamon.world.items.weapons.PotatoCannon;
import cinnamon.world.items.weapons.RiceGun;
import cinnamon.world.items.weapons.Weapon;
import cinnamon.world.light.DirectionalLight;
import cinnamon.world.light.Light;
import cinnamon.world.light.Spotlight;
import cinnamon.world.particle.Particle;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.worldgen.TerrainGenerator;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.lwjgl.glfw.GLFW.*;

public class WorldClient extends World {

    protected Hud hud = new Hud();
    protected Movement movement = new Movement();
    protected Interaction interaction = new Interaction();

    protected Client client;
    public LocalPlayer player;

    private int cameraMode = 0;

    private boolean hideHUD;

    //lights
    protected final List<Light> lights = new ArrayList<>();
    protected final Light sunLight = new DirectionalLight().pos(0.5f, 5f, 0.5f).intensity(1f).castsShadows(true);

    //skybox
    protected final Sky sky = new Sky();

    //counters
    protected int renderedEntitiesTemp, renderedTerrainTemp, expectedRenderedTerrainTemp, renderedParticlesTemp;
    protected int renderedEntities, renderedTerrain, expectedRenderedTerrain, renderedParticles, renderedLights, renderedShadows;

    @Override
    public void init() {
        //set client
        client = Client.getInstance();
        client.setScreen(null);
        client.world = this;

        //init hud
        hud.init();

        //tutorial toast
        Toast.addToast(Text.translated("world.keybinds_help")).length(200).type(Toast.ToastType.WORLD).style(Hud.HUD_STYLE);

        //sunlight
        addLight(sunLight);

        //create player
        respawn(true);

        //SERVER STUFF
        tempLoad();

        runScheduledTicks();

        //request world data
        //connection.sendTCP(new Login());
    }

    protected void tempLoad() {
        //load level
        int r = 32;
        TerrainGenerator.fill(this, -r, 0, -r, r, 0, r, MaterialRegistry.GRASS);

        //TerrainGenerator.fill(this, -r, 1, -r, r, 1, -r, MaterialRegistry.BRICK_WALL);
        //TerrainGenerator.fill(this, -r, 1, r, r, 1, r, MaterialRegistry.BRICK_WALL);
        //TerrainGenerator.fill(this, -r, 1, -r, -r, 1, r, MaterialRegistry.BRICK_WALL);
        //TerrainGenerator.fill(this, r, 1, -r, r, 1, r, MaterialRegistry.BRICK_WALL);

        //0, 0
        Terrain t = TerrainRegistry.BOX.getFactory().get();
        t.setMaterial(MaterialRegistry.COBBLESTONE);
        removeTerrain(new AABB().translate(0.5f, 0.5f, 0.5f));
        addTerrain(t);

        //menger sponge
        TerrainGenerator.generateMengerSponge(this, 2, -23, 1, -23);

        //playSound(new Resource("sounds/song.ogg"), SoundCategory.MUSIC, new Vector3f(0, 0, 0)).loop(true);

        //lights
        sunLight.castsShadows(false);

        //for (int i = 0; i < 5; i++)
        //    addLight(new PointLight().pos(-5.5f + i * 3f, 5f, 2.5f).color(Colors.randomRainbow().rgb));

        addLight(new Spotlight().pos(1f, 5f, -3.0f).color(0xFF0000));
        addLight(new Spotlight().pos(0.25f, 5f, -2.567f).color(0x00FF00));
        addLight(new Spotlight().pos(0.25f, 5f, -3.433f).color(0x0000FF));

        //entities
        Cart c = new Cart(UUID.randomUUID());
        c.setPos(10, 2, 10);
        this.addEntity(c);

        ShoppingCart s = new ShoppingCart(UUID.randomUUID());
        s.setPos(15, 2, 10);
        this.addEntity(s);

        ShoppingCart s2 = new ShoppingCart(UUID.randomUUID());
        s2.setPos(15, 2, 8);
        this.addEntity(s2);

        ShoppingCart s3 = new ShoppingCart(UUID.randomUUID());
        s3.setPos(15, 2, 6);
        this.addEntity(s3);

        Dummy d = new Dummy(UUID.randomUUID());
        d.setPos(-10, 2, 10);
        this.addEntity(d);

        Dummy d2 = new Dummy(UUID.randomUUID());
        d2.setPos(-15, 2, 10);
        this.addEntity(d2);

        Spawner<EffectBox> effectBox = new Spawner<>(UUID.randomUUID(), 100, () -> new EffectBox(UUID.randomUUID()));
        effectBox.setPos(-1.5f, 2f, 10f);
        this.addEntity(effectBox);

        Spawner<HealthPack> healthPack = new Spawner<>(UUID.randomUUID(), 100, () -> new HealthPack(UUID.randomUUID()));
        healthPack.setPos(2.5f, 2f, 10f);
        this.addEntity(healthPack);
    }

    @Override
    public void close() {
        //ServerConnection.close();
        client.disconnect();
    }

    @Override
    public void tick() {
        super.tick();

        //process input
        tickInput();

        //hud
        this.hud.tick();
    }

    public void render(MatrixStack matrices, float delta) {
        if (player.getWorld() == null)
            return;

        //set camera
        client.camera.useOrtho(false);
        client.camera.setEntity(player);
        client.camera.setup(cameraMode, delta);
        client.camera.updateFrustum();

        //prepare sun
        float deltaDayTime = (worldTime + delta) % 24000;
        sky.setSunAngle(Maths.map(worldTime + delta, 0, 24000, 0, 360));

        float intensity = (deltaDayTime < 1000) ? (deltaDayTime - 100) / 900f //sunrise
                : (deltaDayTime > 11000) ? 1f - (deltaDayTime - 11000) / 900f //sunset
                : 1f; //day
        sunLight.intensity(intensity);
        sunLight.direction(sky.getSunDirection());

        //render our stuff
        if (client.anaglyph3D) {
            boolean[] hasConsumerPass = {false};
            client.camera.anaglyph3D(matrices, -1f / 64f, -1f, () -> {
                //render world
                WorldRenderer.prepareWorld(client.camera);
                renderWorld(client.camera, matrices, delta);
                applyTempCounters();

                //world vertex consumer
                WorldRenderer.vertexConsumerPass();
                hasConsumerPass[0] = VertexConsumer.finishAllBatches(client.camera) > 0;

                //lights and shadows
                renderLights(client.camera, matrices, delta);
            }, () -> {
                //bake world
                WorldRenderer.bakeWorld(this, hasConsumerPass[0]);

                //render other stuff
                renderSky(matrices, delta);
                renderOutlines(matrices, delta);
                renderDebug(client.camera, matrices, delta);
            });
        } else {
            //render world
            WorldRenderer.prepareWorld(client.camera);
            renderWorld(client.camera, matrices, delta);
            applyTempCounters();

            //world vertex consumer
            WorldRenderer.vertexConsumerPass();
            boolean hasConsumerPass = VertexConsumer.finishAllBatches(client.camera) > 0;

            //lights and shadows
            renderLights(client.camera, matrices, delta);

            //bake world
            WorldRenderer.bakeWorld(this, hasConsumerPass);

            //render other stuff
            renderSky(matrices, delta);
            renderOutlines(matrices, delta);
            renderDebug(client.camera, matrices, delta);
        }

        //finish world rendering
        client.camera.useOrtho(true);
    }

    protected void renderSky(MatrixStack matrices, float delta) {
        Shader s = Shaders.SKYBOX.getShader();
        s.use().setup(client.camera);
        sky.render(client.camera, matrices);
    }

    protected void renderOutlines(MatrixStack matrices, float delta) {
        List<Entity> entitiesToOutline = new ArrayList<>();
        for (Entity e : entities.values())
            if (e.shouldRender(client.camera) && e.shouldRenderOutline())
                entitiesToOutline.add(e);

        //no entities to outline
        if (entitiesToOutline.isEmpty())
            return;

        //prepare framebuffer
        Shader s = WorldRenderer.prepareOutlineBuffer(client.camera);

        //render entities
        for (Entity entity : entitiesToOutline) {
            s.applyColor(entity.getOutlineColor());
            entity.render(matrices, delta);
        }

        //finish rendering
        WorldRenderer.bakeOutlines(null);
        VertexConsumer.discardBatches();
    }

    protected void renderLights(Camera camera, MatrixStack matrices, float delta) {
        renderedLights = renderedShadows = 0;

        List<Light> lightsToRender = new ArrayList<>();
        for (Light l : lights)
            if (l.shouldRender(camera))
                lightsToRender.add(l);

        //no lights to render!
        if (lightsToRender.isEmpty())
            return;

        //set up the light framebuffer and shader
        Shader lightPassShader = WorldRenderer.prepareLightPass(camera);

        for (Light light : lightsToRender) {
            if (light.castsShadows()) {
                //prepare shadow buffer
                Shader shadow = WorldRenderer.prepareShadow(camera, light);

                //render the world
                renderWorld(camera, matrices, delta);
                VertexConsumer.finishAllBatches(shadow, camera);

                //finish shadow rendering (restore to the light framebuffer)
                WorldRenderer.bindShadow(lightPassShader);
                renderedShadows++;
            }

            //render light
            light.pushToShader(lightPassShader);
            WorldRenderer.renderQuad();
            renderedLights++;
        }

        WorldRenderer.bakeLights(camera);
    }

    protected void renderWorld(Camera camera, MatrixStack matrices, float delta) {
        if (XrManager.isInXR() && client.screen == null)
            renderXrHands(camera, matrices);

        //render terrain
        List<Terrain> query = terrainManager.queryCustom(camera::isInsideFrustum);
        renderedTerrainTemp = 0;
        expectedRenderedTerrainTemp = query.size();
        for (Terrain terrain : query) {
            if (terrain.shouldRender(camera)) {
                terrain.render(matrices, delta);
                renderedTerrainTemp++;
            }
        }

        //render entities
        renderedEntitiesTemp = 0;
        for (Entity entity : entities.values()) {
            if (entity.shouldRender(camera)) {
                entity.render(matrices, delta);
                renderedEntitiesTemp++;
            }
        }

        //render particles
        renderedParticlesTemp = 0;
        for (Particle particle : particles) {
            if (particle.shouldRender(camera)) {
                particle.render(matrices, delta);
                renderedParticlesTemp++;
            }
        }

        //render camera entity item effects
        Entity cameraEntity = camera.getEntity();
        if (cameraEntity instanceof LivingEntity le)
            renderItemExtra(le, matrices, delta);
    }

    protected void renderItemExtra(LivingEntity entity, MatrixStack matrices, float delta) {
        Item item = entity.getHoldingItem();
        if (item == null)
            return;

        item.worldRender(matrices, delta);
    }

    public void renderHand(Camera camera, MatrixStack matrices, float delta) {
        Item item;
        if (!(camera.getEntity() instanceof LivingEntity le) || (item = le.getHoldingItem()) == null)
            return;

        //setup rendering
        client.camera.useOrtho(false);

        Shader s = Shaders.WORLD_MODEL_PBR.getShader().use();
        s.setup(camera);
        applyWorldUniforms(s);
        sky.pushToShader(s, Texture.MAX_TEXTURES - 1);

        matrices.pushMatrix();

        //transforms
        if (XrManager.isInXR()) {
            //camera transforms
            matrices.translate(camera.getPos());
            matrices.rotate(camera.getRot());

            //xr transform
            XrHandTransform transform = XrRenderer.getHandTransform(1);
            matrices.translate(transform.pos());
            matrices.rotate(transform.rot());
            matrices.scale(0.35f);
        } else {
            //camera transforms
            matrices.translate(camera.getPosition());
            matrices.rotate(camera.getRotation());

            //screen transform
            matrices.translate(0.75f, -0.5f, -1);
        }

        //render item
        item.render(ItemRenderContext.FIRST_PERSON, matrices, delta);

        matrices.popMatrix();

        //finish rendering
        client.camera.useOrtho(true);
    }

    protected void renderXrHands(Camera camera, MatrixStack matrices) {
        matrices.pushMatrix();
        matrices.translate(camera.getPos());
        matrices.rotate(camera.getRot());
        XrRenderer.renderHands(matrices);
        matrices.popMatrix();
    }

    public void renderHUD(MatrixStack matrices, float delta) {
        this.hud.render(matrices, delta);
    }

    protected void renderDebug(Camera camera, MatrixStack matrices, float delta) {
        if (hudHidden())
            return;

        Entity cameraEntity = camera.getEntity();

        if (DebugScreen.isPlayerTab())
            renderHitResults(cameraEntity, matrices);

        if (DebugScreen.isWorldRelatedTab())
            renderHitboxes(camera, matrices, delta);

        if (player.getAbilities().canBuild())
            renderTargetedBlock(cameraEntity, matrices, delta);

        VertexConsumer.finishAllBatches(camera);
    }

    protected void renderHitboxes(Camera camera, MatrixStack matrices, float delta) {
        Entity cameraEntity = camera.getEntity();
        Vector3f cameraPos = camera.getPos();
        AABB area = new AABB();
        area.translate(cameraPos);
        area.inflate(8f);

        if (DebugScreen.getSelectedTab() == 3) {
            //lights
            for (Light light : lights)
                if (light.shouldRender(camera))
                    light.renderDebug(camera, matrices);

            //particles
            for (Particle p : getParticles(area))
                p.renderDebugHitbox(matrices, delta);
        }

        else if (DebugScreen.getSelectedTab() == 4) {
            //octree
            for (AABB aabb : terrainManager.getBounds()) {
                Vector3f min = aabb.getMin();
                Vector3f max = aabb.getMax();
                VertexConsumer.LINES.consume(GeometryHelper.cube(matrices, min.x, min.y, min.z, max.x, max.y, max.z, 0xFF00FF00));
            }

            //terrain
            for (Terrain t : terrainManager.query(area))
                t.renderDebugHitbox(matrices, delta);

            //placement terrain
            Hit<Terrain> hit = cameraEntity.getLookingTerrain(cameraEntity.getPickRange());
            if (hit != null) {
                Vector3f pos = new Vector3f(hit.pos()).floor();
                if (hit.get() != null && pos.equals(hit.get().getPos()))
                    pos.add(hit.collision().normal());

                VertexConsumer.LINES.consume(GeometryHelper.cube(matrices, pos.x, pos.y, pos.z, pos.x + 1, pos.y + 1, pos.z + 1, 0xFFFF0000));
            }
        }

        else if (DebugScreen.getSelectedTab() == 5) {
            //entities
            for (Entity e : getEntities(area)) {
                if (e != cameraEntity || isThirdPerson())
                    e.renderDebugHitbox(matrices, delta);
            }
        }
    }

    protected void renderHitResults(Entity cameraEntity, MatrixStack matrices) {
        float f = 0.025f;
        float r = cameraEntity.getPickRange();

        Hit<Terrain> terrain = cameraEntity.getLookingTerrain(r);
        if (terrain != null) {
            Vector3f pos = terrain.pos();
            VertexConsumer.MAIN.consume(GeometryHelper.cube(matrices, pos.x - f, pos.y - f, pos.z - f, pos.x + f, pos.y + f, pos.z + f, 0xFF00FFFF));
        }

        Hit<Entity> entity = cameraEntity.getLookingEntity(r);
        if (entity != null) {
            AABB aabb = entity.obj().getAABB();
            VertexConsumer.LINES.consume(GeometryHelper.cube(matrices, aabb.minX(), aabb.minY(), aabb.minZ(), aabb.maxX(), aabb.maxY(), aabb.maxZ(), 0xFFFFFF00));

            Vector3f pos = entity.pos();
            VertexConsumer.MAIN.consume(GeometryHelper.cube(matrices, pos.x - f, pos.y - f, pos.z - f, pos.x + f, pos.y + f, pos.z + f, 0xFF00FFFF));
        }
    }

    protected void renderTargetedBlock(Entity cameraEntity, MatrixStack matrices, float delta) {
        Hit<Entity> entity = cameraEntity.getLookingEntity(cameraEntity.getPickRange());
        Hit<Terrain> terrain = cameraEntity.getLookingTerrain(cameraEntity.getPickRange());
        if (terrain == null || (entity != null && entity.collision().near() < terrain.collision().near()) || !terrain.obj().isSelectable(cameraEntity))
            return;

        int alpha = (int) Maths.lerp(0x32, 0xFF, ((float) Math.sin((client.ticks + delta) * 0.15f) + 1f) * 0.5f);

        for (AABB aabb : terrain.obj().getPreciseAABB())
            VertexConsumer.LINES.consume(GeometryHelper.cube(matrices, aabb.minX(), aabb.minY(), aabb.minZ(), aabb.maxX(), aabb.maxY(), aabb.maxZ(), 0xFFFFFF + (alpha << 24)));
    }

    public void applyWorldUniforms(Shader s) {
        //camera
        s.setVec3("camPos", client.camera.getPosition());

        //fog
        float fogDensity = 0.5f;
        s.setFloat("fogStart", WorldRenderer.renderDistance * fogDensity);
        s.setFloat("fogEnd", WorldRenderer.renderDistance);
        s.setColor("fogColor", Sky.fogColor);

        //lighting
        s.setColor("ambient", 0xFFFFFF);
    }

    public Sky getSky() {
        return sky;
    }

    public void addLight(Light light) {
        scheduledTicks.add(() -> this.lights.add(light));
    }

    public void removeLight(Light light) {
        scheduledTicks.add(() -> this.lights.remove(light));
    }

    public int lightCount() {
        return lights.size();
    }

    public List<Light> getLights(AABB region) {
        List<Light> list = new ArrayList<>();
        for (Light light : this.lights) {
            if (region.isInside(light.getPos()))
                list.add(light);
        }
        return list;
    }

    public int getRenderedTerrain() {
        return renderedTerrain;
    }

    public int getExpectedRenderedTerrain() {
        return expectedRenderedTerrain;
    }

    public int getRenderedEntities() {
        return renderedEntities;
    }

    public int getRenderedParticles() {
        return renderedParticles;
    }

    public int getRenderedLights() {
        return renderedLights;
    }

    public int getRenderedShadows() {
        return renderedShadows;
    }

    protected void applyTempCounters() {
        this.renderedTerrain = this.renderedTerrainTemp;
        this.expectedRenderedTerrain = this.expectedRenderedTerrainTemp;
        this.renderedEntities = this.renderedEntitiesTemp;
        this.renderedParticles = this.renderedParticlesTemp;
    }

    protected void tickInput() {
        if (!client.window.isMouseLocked())
            return;

        this.movement.tick(player);
        this.interaction.tick(player);
    }

    public void mousePress(int button, int action, int mods) {
        Keybind.mousePress(button, action, mods);
        this.interaction.tick(player);
    }

    public void mouseMove(double x, double y) {
        if (!XrManager.isInXR())
            movement.mouseMove(x, y);
    }

    public void scroll(double x, double y) {
        this.interaction.scrollItem((int) Math.signum(-y));
    }

    public void keyPress(int key, int scancode, int action, int mods) {
        if (key == GLFW_KEY_LEFT_ALT) {
            if (action == GLFW_PRESS) {
                client.window.unlockMouse();
            } else if (action == GLFW_RELEASE) {
                client.window.lockMouse();
                resetMovement();
            }
        }

        Keybind.keyPress(key, scancode, action, mods);

        if (action == GLFW_RELEASE)
            return;

        if (key >= GLFW_KEY_1 && key <= GLFW_KEY_9) {
            int i = key - GLFW_KEY_1;
            player.setSelectedItem(i);
            //connection.sendUDP(new SelectItem().index(i));
        }

        boolean shift = (mods & GLFW_MOD_SHIFT) != 0;

        switch (key) {
            case GLFW_KEY_N -> player.getAbilities().noclip(!player.getAbilities().noclip());
            case GLFW_KEY_R -> {
                Item i = player.getHoldingItem();
                if (i instanceof Weapon weapon)
                    weapon.reload();
            }
            case GLFW_KEY_ESCAPE -> client.setScreen(new PauseScreen());
            case GLFW_KEY_ENTER -> client.setScreen(new ChatScreen());
            case GLFW_KEY_F1 -> this.hideHUD = !this.hideHUD;
            case GLFW_KEY_F5 -> this.cameraMode = (this.cameraMode + 1) % 3;
            case GLFW_KEY_F7 -> this.worldTime -= 100;
            case GLFW_KEY_F8 -> this.worldTime += 100;

            case GLFW_KEY_COMMA -> player.setSelectedTerrain((player.getSelectedTerrain() + 1) % (TerrainRegistry.values().length));
            case GLFW_KEY_PERIOD -> player.setSelectedMaterial(Maths.modulo((player.getSelectedMaterial() + (shift ? -1 : 1)), MaterialRegistry.values().length));
            case GLFW_KEY_SLASH -> sky.setSkyBox(Maths.randomArr(SkyBoxRegistry.values()).resource);

            case GLFW_KEY_Z -> {
                Firework f = new Firework(UUID.randomUUID(), (int) Maths.range(30, 60), Maths.spread(new Vector3f(0, 1f, 0), 30, 30).mul(2f),
                        new FireworkStar(
                                new Integer[]{0xFFa19f7f, 0xFFcfa959, 0xFF9b8136, 0xFF908264, 0xFFebc789, 0xFFb39b5b},
                                null,
                                true, true,
                                FireworkStar.Shape.BALL
                        ),
                        new FireworkStar(
                                new Integer[]{Colors.WHITE.rgba},
                                null,
                                false, true,
                                FireworkStar.Shape.STAR
                        )
                );
                f.setPos(0, 1.5f, 0);
                addEntity(f);
            }

            //case GLFW_KEY_F9 -> connection.sendTCP(new Handshake());
            //case GLFW_KEY_F10 -> connection.sendUDP(new Message().msg("meow"));
        }
    }

    public void onWindowResize(int width, int height) {
        resetMovement();
    }

    public void xrButtonPress(int button, boolean pressed, int hand) {
        if (pressed && button == 1) {
            client.setScreen(new PauseScreen());
            return;
        }

        movement.xrButtonPress(button, pressed, hand);
    }

    public void xrTriggerPress(int button, float value, int hand, float lastValue) {
        interaction.xrTriggerPress(button, value, hand, lastValue);
    }

    public void xrJoystickMove(float x, float y, int hand, float lastX, float lastY) {
        movement.xrJoystickMove(x, y, hand, lastX, lastY);
    }

    public void resetMovement() {
        this.movement.reset();
        this.interaction.reset();
        Keybind.releaseAll();
    }

    public int getCameraMode() {
        return cameraMode;
    }

    public boolean isThirdPerson() {
        return cameraMode > 0;
    }

    public boolean hudHidden() {
        return hideHUD;
    }

    public void respawn(boolean init) {
        player = new LocalPlayer();
        player.setPos(0.5f, init ? 0f : 100f, 0.5f);
        player.getAbilities().godMode(true).canFly(true);
        givePlayerItems(player);
        this.addEntity(player);

        Animation anim = player.getAnimation("blink");
        if (anim != null)
            anim.setLoop(Animation.Loop.LOOP).play();

        //if (!init)
        //    connection.sendTCP(new Respawn());
    }

    public void givePlayerItems(Player player) {
        player.giveItem(new CoilGun(30, 3, 100));
        player.giveItem(new PotatoCannon(3, 60, 200));
        player.giveItem(new RiceGun(8, 40, 150));
        player.giveItem(new BubbleGun());
        player.getInventory().setItem(player.getInventory().getFreeIndex() + 1, new Flashlight(0xFFFFCC));
        player.getInventory().setItem(player.getInventory().getSize() - 1, new MagicWand());
    }

    @Override
    public boolean isClientside() {
        return true;
    }
}
