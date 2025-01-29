package cinnamon.world.world;

import cinnamon.Client;
import cinnamon.animation.Animation;
import cinnamon.gui.Toast;
import cinnamon.gui.screens.world.ChatScreen;
import cinnamon.gui.screens.world.DeathScreen;
import cinnamon.gui.screens.world.PauseScreen;
import cinnamon.input.Movement;
import cinnamon.model.GeometryHelper;
import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.WorldRenderer;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.framebuffer.Blit;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;
import cinnamon.text.Text;
import cinnamon.utils.AABB;
import cinnamon.utils.Direction;
import cinnamon.utils.Maths;
import cinnamon.world.Hud;
import cinnamon.world.SkyBox;
import cinnamon.world.collisions.Hit;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.Spawner;
import cinnamon.world.entity.collectable.EffectBox;
import cinnamon.world.entity.collectable.HealthPack;
import cinnamon.world.entity.living.Dummy;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.entity.living.LocalPlayer;
import cinnamon.world.entity.living.Player;
import cinnamon.world.entity.vehicle.Cart;
import cinnamon.world.items.*;
import cinnamon.world.items.weapons.CoilGun;
import cinnamon.world.items.weapons.PotatoCannon;
import cinnamon.world.items.weapons.RiceGun;
import cinnamon.world.items.weapons.Weapon;
import cinnamon.world.light.DirectionalLight;
import cinnamon.world.light.Light;
import cinnamon.world.particle.Particle;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.worldgen.Chunk;
import cinnamon.world.worldgen.TerrainGenerator;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glViewport;

public class WorldClient extends World {

    public final Hud hud = new Hud();
    private final Movement movement = new Movement();

    protected Client client;
    public LocalPlayer player;

    private int cameraMode = 0;

    private boolean hideHUD;

    //lights
    protected final List<Light> lights = new ArrayList<>();
    protected final DirectionalLight sunLight = new DirectionalLight();

    //skybox
    protected final SkyBox skyBox = new SkyBox();

    //shadows
    private final Framebuffer shadowBuffer = new Framebuffer(2048, 2048, Framebuffer.DEPTH_BUFFER);
    private final Matrix4f lightSpaceMatrix = new Matrix4f();
    private boolean renderShadowMap;

    //counters
    private int renderedEntities, renderedChunks, renderedTerrain, renderedParticles;

    //terrain
    private int selectedTerrain = TerrainRegistry.BOX.ordinal();
    private int selectedMaterial = MaterialRegistry.GRASS.ordinal();

    @Override
    public void init() {
        //set client
        client = Client.getInstance();
        client.setScreen(null);
        client.world = this;

        //init hud
        hud.init();

        //tutorial toast
        Toast.addToast(Text.of("WASD - move\nR - reload\nMouse - look around\nLeft Click - attack\nF3 - debug\nF5 - third person"), client.font).length(200).type(Toast.ToastType.WORLD);

        //sun light
        //addLight(sunLight);

        //create player
        respawn(true);

        //prepare renderer
        WorldRenderer.resize(client.window.width, client.window.height);

        //SERVER STUFF
        tempLoad();

        runScheduledTicks();

        //request world data
        //connection.sendTCP(new Login());
    }

    protected void tempLoad() {
        //load level
        int radius = 1;
        for (int i = -radius; i < radius; i++) {
            for (int j = -radius; j < radius; j++) {
                Chunk c = TerrainGenerator.generatePlain(i, 0, j);
                chunks.put(c.getGridPos(), c);
            }
        }

        //menger sponge
        TerrainGenerator.generateMengerSponge(this, 2, -23, 1, -23);

        //playSound(new Resource("sounds/song.ogg"), SoundCategory.MUSIC, new Vector3f(0, 0, 0)).loop(true);

        /*
        //rip for-loop
        addLight(new Light().pos(-5.5f, 0.5f, 2f).color(0x000000));
        addLight(new Light().pos(-3.5f, 0.5f, 2f).color(0xFF0000));
        addLight(new Light().pos(-1.5f, 0.5f, 2f).color(0x00FF00));
        addLight(new Light().pos(0.5f, 0.5f, 2f).color(0x0000FF));
        addLight(new Light().pos(2.5f, 0.5f, 2f).color(0x00FFFF));
        addLight(new Light().pos(4.5f, 0.5f, 2f).color(0xFF00FF));
        addLight(new Light().pos(6.5f, 0.5f, 2f).color(0xFFFF00));
        addLight(new Light().pos(8.5f, 0.5f, 2f).color(0xFFFFFF));
        */

        Cart c = new Cart(UUID.randomUUID());
        c.setPos(10, 2, 10);
        this.addEntity(c);

        Cart c2 = new Cart(UUID.randomUUID());
        c2.setPos(15, 2, 10);
        this.addEntity(c2);

        Dummy d = new Dummy(UUID.randomUUID());
        d.setPos(-10, 2, 10);
        this.addEntity(d);

        Dummy d2 = new Dummy(UUID.randomUUID());
        d2.setPos(-15, 2, 10);
        this.addEntity(d2);

        Spawner effectBox = new Spawner(UUID.randomUUID(), 100, () -> new EffectBox(UUID.randomUUID()));
        effectBox.setPos(-1.5f, 2f, 10f);
        this.addEntity(effectBox);

        Spawner healthPack = new Spawner(UUID.randomUUID(), 100, () -> new HealthPack(UUID.randomUUID()));
        healthPack.setPos(2.5f, 2f, 10f);
        this.addEntity(healthPack);
    }

    @Override
    public void close() {
        //ServerConnection.close();
        shadowBuffer.free();
        client.disconnect();
    }

    @Override
    public void tick() {
        super.tick();

        //if the player is dead, show death screen
        if (player.isDead() && client.screen == null)
            client.setScreen(new DeathScreen());

        //process input
        this.movement.tick(player);
        processMouseInput();

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
        skyBox.setSunAngle(Maths.map(timeOfTheDay + delta, 0, 24000, 0, 360));
        sunLight.direction(skyBox.getSunDirection());

        //render shadows
        //renderShadows(client.camera, matrices, delta);

        //render skybox
        renderSky(matrices, delta);

        if (client.anaglyph3D) {
            client.camera.anaglyph3D(matrices, -1f / 64f, -1f, () -> {
                WorldRenderer.prepare(client.camera);
                renderWorld(client.camera, matrices, delta);
            }, () -> {
                WorldRenderer.finish(this);
                VertexConsumer.finishAllBatches(client.camera);
                renderOutlines(matrices, delta);
            });
        } else {
            //render world
            WorldRenderer.prepare(client.camera);
            renderWorld(client.camera, matrices, delta);
            WorldRenderer.finish(this);
            VertexConsumer.finishAllBatches(client.camera);
            renderOutlines(matrices, delta);
        }

        //finish world rendering
        client.camera.useOrtho(true);

        //debug shadows
        if (renderShadowMap) {
            int size = client.window.height / 4;
            renderShadowBuffer(client.window.width - size, client.window.height - size, size);
        }
    }

    protected void renderSky(MatrixStack matrices, float delta) {
        Shader s = Shaders.SKYBOX.getShader();
        s.use().setup(client.camera);
        skyBox.render(client.camera, matrices);
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
            s.applyColor(Math.abs(entity.getUUID().hashCode()));
            entity.render(matrices, delta);
        }

        //finish rendering
        WorldRenderer.finishOutlines();
        VertexConsumer.clearBatches();
    }

    protected void renderShadows(Camera camera, MatrixStack matrices, float delta) {
        //prepare matrix
        float r = Chunk.CHUNK_SIZE * 2 * 0.5f;
        Matrix4f lightProjection = new Matrix4f().ortho(-r, r, -r, r, -r, r);

        //setup camera
        Vector3f dir = skyBox.getSunDirection();
        Vector3f pos = new Vector3f(camera.getPos());
        Vector2f rot = new Vector2f(camera.getRot());

        camera.setPos(pos.x + dir.x, pos.y + dir.y, pos.z + dir.z);
        camera.lookAt(pos.x, pos.y, pos.z);

        //finish matrix
        lightSpaceMatrix.set(lightProjection).mul(camera.getViewMatrix());
        camera.updateFrustum(lightSpaceMatrix);

        //shader
        Shader s = Shaders.DEPTH.getShader().use();
        s.setMat4("lightSpaceMatrix", lightSpaceMatrix);

        //framebuffer
        shadowBuffer.useClear();
        shadowBuffer.adjustViewPort();

        //render the world
        renderWorld(camera, matrices, delta);
        //render camera entity when in first person
        if (!isThirdPerson() && camera.getEntity() != null)
            camera.getEntity().render(matrices, delta);

        //finish rendering
        matrices.push();
        matrices.identity();
        s.applyMatrixStack(matrices);
        VertexConsumer.finishAllBatches(s);
        matrices.pop();

        //restore to default framebuffer
        Framebuffer.DEFAULT_FRAMEBUFFER.use();
        Framebuffer.DEFAULT_FRAMEBUFFER.adjustViewPort();

        //restore camera
        camera.setPos(pos.x, pos.y, pos.z);
        camera.setRot(rot.x, rot.y);
        camera.updateFrustum();
    }

    protected List<Chunk> getChunksToRender(Camera camera) {
        List<Chunk> list = new ArrayList<>();

        Vector3i startingPoint = getChunkGridPos(camera.getPos());
        for (int x = -renderDistance; x <= renderDistance; x++) {
            for (int y = -renderDistance; y <= renderDistance; y++) {
                for (int z = -renderDistance; z <= renderDistance; z++) {
                    Vector3i pos = new Vector3i(startingPoint).add(x, y, z);
                    Chunk c = chunks.get(pos);
                    if (c != null)
                        list.add(c);
                }
            }
        }

        return list;
    }

    protected void renderWorld(Camera camera, MatrixStack matrices, float delta) {
        //render terrain
        renderedChunks = 0;
        renderedTerrain = 0;
        for (Chunk chunk : getChunksToRender(camera)) {
            if (chunk.shouldRender(camera)) {
                renderedTerrain += chunk.render(camera, matrices, delta);
                renderedChunks++;
            }
        }

        //render entities
        renderedEntities = 0;
        for (Entity entity : entities.values()) {
            if (entity.shouldRender(camera)) {
                entity.render(matrices, delta);
                renderedEntities++;
            }
        }

        //render particles
        renderedParticles = 0;
        for (Particle particle : particles) {
            if (particle.shouldRender(camera)) {
                particle.render(matrices, delta);
                renderedParticles++;
            }
        }

        //render camera entity item effects
        Entity cameraEntity = camera.getEntity();
        if (cameraEntity instanceof LivingEntity le)
            renderItemExtra(le, matrices, delta);

        //render debug
        if (!hideHUD) {
            if (client.debug) {
                renderHitboxes(camera, matrices, delta);
                renderHitResults(cameraEntity, matrices);
            }

            renderTargetedBlock(cameraEntity, matrices, delta);
        }
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
        skyBox.pushToShader(s, Texture.MAX_TEXTURES - 1);

        matrices.push();

        //camera transforms
        matrices.translate(camera.getPos());
        matrices.rotate(camera.getRotation());

        //screen transform
        matrices.translate(0.75f, -0.5f, -1);

        //render item
        item.render(ItemRenderContext.FIRST_PERSON, matrices, delta);

        matrices.pop();

        //finish rendering
        client.camera.useOrtho(true);
    }

    protected void renderShadowBuffer(int x, int y, int size) {
        glViewport(x, y, size, size);
        Blit.copy(shadowBuffer, Framebuffer.DEFAULT_FRAMEBUFFER.id(), Shaders.DEPTH_BLIT.getShader(), Blit.DEPTH_UNIFORM);
        Framebuffer.DEFAULT_FRAMEBUFFER.adjustViewPort();
    }

    protected void renderHitboxes(Camera camera, MatrixStack matrices, float delta) {
        Vector3f cameraPos = camera.getPos();
        AABB area = new AABB();
        area.translate(cameraPos);
        area.inflate(8f);

        for (Terrain t : getTerrain(area))
            t.renderDebugHitbox(matrices, delta);

        Entity cameraEntity = camera.getEntity();
        for (Entity e : getEntities(area)) {
            if (e != cameraEntity || isThirdPerson())
                e.renderDebugHitbox(matrices, delta);
        }

        for (Particle p : getParticles(area))
            p.renderDebugHitbox(matrices, delta);

        renderLights(area, cameraPos, matrices);
    }

    protected void renderLights(AABB area, Vector3f cameraPos, MatrixStack matrices) {
        for (Light l : getLights(area)) {
            if (l instanceof DirectionalLight)
                continue;

            Vector3f pos = l.getPos();
            if (cameraPos.distanceSquared(pos) <= 0.1f)
                continue;

            float r = 0.125f;
            int color = l.getColor() + (0xFF << 24);
            VertexConsumer.MAIN.consume(GeometryHelper.cube(matrices, pos.x - r, pos.y - r, pos.z - r, pos.x + r, pos.y + r, pos.z + r, color));
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
        if (terrain == null || (entity != null && entity.collision().near() < terrain.collision().near()))
            return;

        int alpha = (int) Maths.lerp(0x32, 0xFF, ((float) Math.sin((client.ticks + delta) * 0.15f) + 1f) * 0.5f);

        for (AABB aabb : terrain.obj().getPreciseAABB())
            VertexConsumer.LINES.consume(GeometryHelper.cube(matrices, aabb.minX(), aabb.minY(), aabb.minZ(), aabb.maxX(), aabb.maxY(), aabb.maxZ(), 0xFFFFFF + (alpha << 24)));
    }

    public void applyWorldUniforms(Shader s) {
        //camera
        s.setVec3("camPos", client.camera.getPos());

        //fog
        s.setFloat("fogStart", Chunk.getFogStart(this));
        s.setFloat("fogEnd", Chunk.getFogEnd(this));
        s.setColor("fogColor", Chunk.fogColor);

        //lighting
        s.setColor("ambient", 0xFFFFFF);//Chunk.ambientLight);

        s.setInt("lightCount", lights.size());
        for (int i = 0; i < lights.size(); i++)
            lights.get(i).pushToShader(s, i);
    }

    public void applyShadowUniforms(Shader s) {
        s.setMat4("lightSpaceMatrix", lightSpaceMatrix);
        s.setVec3("shadowDir", skyBox.getSunDirection());
        s.setTexture("shadowMap", shadowBuffer.getDepthBuffer(), Texture.MAX_TEXTURES - 1);
    }

    public SkyBox getSkyBox() {
        return skyBox;
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

    public int getRenderedChunks() {
        return renderedChunks;
    }

    public int getRenderedTerrain() {
        return renderedTerrain;
    }

    public int getRenderedEntities() {
        return renderedEntities;
    }

    public int getRenderedParticles() {
        return renderedParticles;
    }

    public void mousePress(int button, int action, int mods) {
        boolean press = action == GLFW_PRESS;
        boolean release = action == GLFW_RELEASE;

        //ClientEntityAction mouseAction = new ClientEntityAction();
        //boolean used = false;

        switch (button) {
            case GLFW_MOUSE_BUTTON_1 -> {
                if (release) {
                    player.stopAttacking();
                    //mouseAction.attack(false);
                    //used = true;
                } else if (press && player.getHoldingItem() == null) {
                    Hit<Terrain> terrain = player.getLookingTerrain(player.getPickRange());
                    if (terrain != null) {
                        Vector3f pos = terrain.obj().getPos();
                        setTerrain(null, (int) pos.x, (int) pos.y, (int) pos.z);
                    }
                }
            }
            case GLFW_MOUSE_BUTTON_2 -> {
                if (release) {
                    player.stopUsing();
                    //mouseAction.use(false);
                    //used = true;
                } else if (press && player.getHoldingItem() == null) {
                    Hit<Entity> entity = player.getLookingEntity(player.getPickRange());
                    Hit<Terrain> terrain = player.getLookingTerrain(player.getPickRange());
                    if (terrain != null && (entity == null || terrain.collision().near() < entity.collision().near())) {
                        Vector3f dir = terrain.collision().normal();
                        Vector3f tpos = new Vector3f(terrain.obj().getPos()).add(dir);

                        AABB entities = new AABB().translate(tpos).expand(1f, 1f, 1f);
                        if (getEntities(entities).isEmpty()) {
                            Terrain t = TerrainRegistry.values()[selectedTerrain].getFactory().get();
                            t.setMaterial(MaterialRegistry.values()[selectedMaterial]);
                            setTerrain(t, (int) tpos.x, (int) tpos.y, (int) tpos.z);
                            t.setRotation(Direction.fromRotation(player.getRot().y).invRotation);
                        }
                    }
                }
            }
            case GLFW_MOUSE_BUTTON_3 -> {
                if (press) {
                    Hit<Terrain> terrain = player.getLookingTerrain(player.getPickRange());
                    if (terrain != null) {
                        selectedTerrain = terrain.obj().getType().ordinal();
                        MaterialRegistry material = terrain.obj().getMaterial();
                        if (material != null) selectedMaterial = material.ordinal();
                    }
                }
            }
        }

        //if (used) connection.sendUDP(mouseAction);

        processMouseInput();
    }

    private void processMouseInput() {
        Window w = client.window;
        if (!w.isMouseLocked())
            return;

        //ClientEntityAction action = new ClientEntityAction();
        //boolean used = false;

        if (w.mouse1Press) {
            player.attackAction();
            //action.attack(true);
            //used = true;
        }
        if (w.mouse2Press) {
            player.useAction();
            //action.use(true);
            //used = true;
        }

        //if (used) connection.sendUDP(action);
    }

    public void mouseMove(double x, double y) {
        movement.mouseMove(x, y);
    }

    public void scroll(double x, double y) {
        int i = player.getInventory().getSelectedIndex() - (int) Math.signum(y);
        player.setSelectedItem(i);
        //connection.sendUDP(new SelectItem().index(i));
    }

    public void keyPress(int key, int scancode, int action, int mods) {
        movement.keyPress(key, action);

        if (action == GLFW_RELEASE)
            return;

        if (key >= GLFW_KEY_1 && key <= GLFW_KEY_9) {
            int i = key - GLFW_KEY_1;
            player.setSelectedItem(i);
            //connection.sendUDP(new SelectItem().index(i));
        }

        boolean shift = (mods & GLFW_MOD_SHIFT) != 0;

        switch (key) {
            case GLFW_KEY_R -> {
                Item i = player.getHoldingItem();
                if (i instanceof Weapon weapon && !weapon.isOnCooldown() && i.getCount() < i.getStackCount())
                    weapon.setOnCooldown();
            }
            case GLFW_KEY_ESCAPE -> client.setScreen(new PauseScreen());
            case GLFW_KEY_ENTER -> client.setScreen(new ChatScreen());
            case GLFW_KEY_F1 -> this.hideHUD = !this.hideHUD;
            case GLFW_KEY_F4 -> this.renderShadowMap = !this.renderShadowMap;
            case GLFW_KEY_F5 -> this.cameraMode = (this.cameraMode + 1) % 3;
            case GLFW_KEY_F7 -> this.timeOfTheDay -= 100;
            case GLFW_KEY_F8 -> this.timeOfTheDay += 100;

            case GLFW_KEY_SLASH -> {
                skyBox.type = SkyBox.Type.values()[(skyBox.type.ordinal() + 1) % SkyBox.Type.values().length];
                Toast.addToast(Text.of(skyBox.type.name()), client.font).type(Toast.ToastType.WORLD);
            }
            case GLFW_KEY_COMMA -> selectedTerrain = (selectedTerrain + 1) % (TerrainRegistry.values().length);
            case GLFW_KEY_PERIOD -> selectedMaterial = Maths.modulo((selectedMaterial + (shift ? -1 : 1)), MaterialRegistry.values().length);

            //case GLFW_KEY_F9 -> connection.sendTCP(new Handshake());
            //case GLFW_KEY_F10 -> connection.sendUDP(new Message().msg("meow"));
        }
    }

    public void onWindowResize(int width, int height) {
        resetMovement();
        WorldRenderer.resize(width, height);
    }

    public void resetMovement() {
        this.movement.reset();
    }

    public int getCameraMode() {
        return cameraMode;
    }

    public boolean isThirdPerson() {
        return cameraMode > 0;
    }

    public boolean hideHUD() {
        return hideHUD;
    }

    public int getSelectedTerrain() {
        return selectedTerrain;
    }

    public int getSelectedMaterial() {
        return selectedMaterial;
    }

    public void respawn(boolean init) {
        player = new LocalPlayer();
        player.setPos(0.5f, init ? 0f : 100f, 0.5f);
        givePlayerItems(player);
        this.addEntity(player);

        Animation anim = player.getAnimation("blink");
        if (anim != null)
            anim.setLoop(Animation.Loop.LOOP).play();

        //if (!init)
        //    connection.sendTCP(new Respawn());
    }

    public void givePlayerItems(Player player) {
        player.giveItem(new CoilGun(1, 5, 0));
        player.giveItem(new PotatoCannon(3, 40, 30));
        player.giveItem(new RiceGun(8, 80, 60));
        player.giveItem(new BubbleGun(1));
        player.getInventory().setItem(player.getInventory().getFreeIndex() + 1, new Flashlight(1, 0xFFFFCC));
        player.getInventory().setItem(player.getInventory().getSize() - 1, new MagicWand(1));
    }

    @Override
    public boolean isClientside() {
        return true;
    }
}
