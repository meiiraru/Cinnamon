package mayo.world;

import mayo.Client;
import mayo.gui.Toast;
import mayo.gui.screens.DeathScreen;
import mayo.gui.screens.PauseScreen;
import mayo.input.Movement;
import mayo.model.GeometryHelper;
import mayo.model.ModelManager;
import mayo.registry.MaterialRegistry;
import mayo.render.*;
import mayo.render.batch.VertexConsumer;
import mayo.render.framebuffer.Blit;
import mayo.render.framebuffer.Framebuffer;
import mayo.render.shader.Shader;
import mayo.render.shader.Shaders;
import mayo.text.Text;
import mayo.utils.AABB;
import mayo.utils.Maths;
import mayo.utils.Resource;
import mayo.world.chunk.Chunk;
import mayo.world.collisions.Hit;
import mayo.world.entity.Entity;
import mayo.world.entity.living.LivingEntity;
import mayo.world.entity.living.LocalPlayer;
import mayo.world.entity.living.Player;
import mayo.world.entity.vehicle.Cart;
import mayo.world.items.Flashlight;
import mayo.world.items.Item;
import mayo.world.items.ItemRenderContext;
import mayo.world.items.MagicWand;
import mayo.world.items.weapons.Weapon;
import mayo.world.light.DirectionalLight;
import mayo.world.light.Light;
import mayo.world.particle.Particle;
import mayo.world.terrain.Terrain;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class WorldClient extends World {

    public final Hud hud = new Hud();
    private final Movement movement = new Movement();

    protected Client client;
    public LocalPlayer player;

    private int cameraMode = 0;

    private boolean debugRendering, renderShadowMap;
    private boolean hideHUD;

    //lights
    protected final List<Light> lights = new ArrayList<>();
    protected final DirectionalLight sunLight = new DirectionalLight();

    //skybox
    private final SkyBox skyBox = new SkyBox();

    //shadows
    private final Framebuffer shadowBuffer = new Framebuffer(2048, 2048, Framebuffer.DEPTH_BUFFER);
    private final Matrix4f lightSpaceMatrix = new Matrix4f();

    @Override
    public void init() {
        //set client
        client = Client.getInstance();
        client.setScreen(null);
        client.world = this;

        //init hud
        hud.init();

        //tutorial toast
        Toast.addToast(Text.of("WASD - move\nR - reload\nMouse - look around\nLeft Click - attack\nF3 - debug\nF5 - third person"), client.font, 200);

        //lights
        //rip for-loop
        addLight(sunLight);
        addLight(new Light().pos(-5.5f, 0.5f, 2f).color(0x000000));
        addLight(new Light().pos(-3.5f, 0.5f, 2f).color(0xFF0000));
        addLight(new Light().pos(-1.5f, 0.5f, 2f).color(0x00FF00));
        addLight(new Light().pos(0.5f, 0.5f, 2f).color(0x0000FF));
        addLight(new Light().pos(2.5f, 0.5f, 2f).color(0x00FFFF));
        addLight(new Light().pos(4.5f, 0.5f, 2f).color(0xFF00FF));
        addLight(new Light().pos(6.5f, 0.5f, 2f).color(0xFFFF00));
        addLight(new Light().pos(8.5f, 0.5f, 2f).color(0xFFFFFF));

        //playSound(new Resource("sounds/song.ogg"), SoundCategory.MUSIC, new Vector3f(0, 0, 0)).loop(true);

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
        LevelLoad.load(this, new Resource("data/levels/level0.json"));

        Cart c = new Cart(UUID.randomUUID());
        c.setPos(10, 2, 10);
        this.addEntity(c);

        Cart c2 = new Cart(UUID.randomUUID());
        c2.setPos(15, 2, 10);
        this.addEntity(c2);
    }

    @Override
    public void close() {
        //ServerConnection.close();
        //client.disconnect();
    }

    @Override
    public void tick() {
        super.tick();

        //if the player is dead, show death screen
        if (player.isDead())
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
        client.camera.setup(player, cameraMode, delta);

        //render skybox
        renderSky(matrices, delta);

        //render shadows
        renderShadows(client.camera, matrices, delta);

        //set world shader
        Shader s = Shaders.WORLD_MODEL.getShader().use();
        s.setup(client.camera.getPerspectiveMatrix(), client.camera.getViewMatrix());

        //apply lighting
        applyWorldUniforms(s);
        applyShadowUniforms(s);

        Entity cameraEntity = client.camera.getEntity();

        //render world
        renderWorld(cameraEntity, matrices, delta);

        //render local player item effects
        renderItemExtra(cameraEntity, matrices, delta);

        //render debug
        if (debugRendering && !hideHUD) {
            renderHitboxes(client.camera, matrices, delta);
            renderHitResults(matrices);
        }

        //finish rendering
        VertexConsumer.finishAllBatches(client.camera.getPerspectiveMatrix(), client.camera.getViewMatrix());

        //debug shadows
        if (renderShadowMap)
            renderShadowBuffer(client.window.width, client.window.height, 500);

        // -- PBR TEMP -- //

        pbr.setOverrideMaterial(currentMaterial.material);
        pbr2.setOverrideMaterial(currentMaterial.material);

        Shader sh = Shaders.WORLD_MODEL_PBR.getShader().use();
        sh.setup(client.camera.getPerspectiveMatrix(), client.camera.getViewMatrix());

        applyWorldUniforms(sh);
        applyShadowUniforms(sh);

        matrices.push();

        matrices.translate(-3, 1, 0);
        sh.applyMatrixStack(matrices);
        pbr.render();

        matrices.translate(-2, 0, 0);
        sh.applyMatrixStack(matrices);
        pbr2.render();

        matrices.pop();

        // -- END PBR TEMP -- //
    }

    private final Model
            pbr = new OpenGLModel(ModelManager.load(new Resource("models/terrain/sphere/sphere.obj")).getMesh()),
            pbr2 = new OpenGLModel(ModelManager.load(new Resource("models/terrain/box/box.obj")).getMesh());
    private MaterialRegistry currentMaterial = MaterialRegistry.BRICK_WALL;
    private int materialIndex = currentMaterial.ordinal();
    private int ambientLight = 0x888888;

    private void changeMaterial(int index) {
        materialIndex = index;
        MaterialRegistry[] values = MaterialRegistry.values();
        currentMaterial = values[(int) Maths.modulo(index, values.length)];

        Toast.addToast(Text.of(currentMaterial.name()), client.font);
    }

    protected void renderSky(MatrixStack matrices, float delta) {
        Shaders.MODEL.getShader().use().setup(
                client.camera.getPerspectiveMatrix(),
                client.camera.getViewMatrix()
        );
        skyBox.setSunAngle(Maths.map(timeOfTheDay + delta, 0, 24000, 0, 360));
        skyBox.render(client.camera, matrices);
        sunLight.direction(skyBox.getSunDirection());
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

        //shader
        Shader s = Shaders.DEPTH.getShader().use();
        s.setMat4("lightSpaceMatrix", lightSpaceMatrix);

        //framebuffer
        Framebuffer prev = Framebuffer.activeFramebuffer;
        shadowBuffer.use();
        shadowBuffer.clear();

        //null so the camera entity shadows renders even in first person
        renderWorld(null, matrices, delta);

        matrices.push();
        matrices.identity();
        s.applyMatrixStack(matrices);
        VertexConsumer.finishAllBatches(s);
        matrices.pop();

        if (prev != null) prev.use();
        else Framebuffer.useDefault();

        //restore camera
        camera.setPos(pos.x, pos.y, pos.z);
        camera.setRot(rot.x, rot.y);
    }

    protected void renderWorld(Entity camEntity, MatrixStack matrices, float delta) {
        //render terrain
        for (Terrain terrain : terrain)
            terrain.render(matrices, delta);

        //render entities
        for (Entity entity : entities.values()) {
            if (camEntity != entity || isThirdPerson())
                entity.render(matrices, delta);
        }

        //render particles
        for (Particle particle : particles)
            particle.render(matrices, delta);
    }

    protected void renderItemExtra(Entity entity, MatrixStack matrices, float delta) {
        if (!(entity instanceof LivingEntity le))
            return;

        Item item = le.getHoldingItem();
        if (item == null)
            return;

        item.worldRender(matrices, delta);
    }

    public void renderHand(Camera camera, MatrixStack matrices, float delta) {
        Item item = player.getHoldingItem();
        if (item == null)
            return;

        //set world shader
        Shader s = Shaders.WORLD_MODEL.getShader().use();
        s.setup(camera.getPerspectiveMatrix(), camera.getViewMatrix());

        //apply lighting
        applyWorldUniforms(s);

        matrices.push();

        //camera transforms
        matrices.translate(camera.getPos());
        matrices.rotate(camera.getRotation());

        //screen transform
        matrices.translate(0.75f, -0.5f, -1);

        //render item
        item.render(ItemRenderContext.FIRST_PERSON, matrices, delta);

        matrices.pop();
    }

    protected void renderShadowBuffer(int width, int height, int size) {
        glViewport(width - size, height - size, size, size);
        Blit.copy(shadowBuffer, 0, Shaders.DEPTH_BLIT.getShader());
        glViewport(0, 0, width, height);
    }

    protected void renderHitboxes(Camera camera, MatrixStack matrices, float delta) {
        Vector3f cameraPos = camera.getPos();
        AABB area = new AABB();
        area.translate(player.getPos());
        area.inflate(8f);

        for (Terrain t : getTerrain(area))
            t.renderDebugHitbox(matrices, delta);

        for (Entity e : getEntities(area)) {
            if (e != player || isThirdPerson())
                e.renderDebugHitbox(matrices, delta);
        }

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
            GeometryHelper.pushCube(VertexConsumer.MAIN, matrices, pos.x - r, pos.y - r, pos.z - r, pos.x + r, pos.y + r, pos.z + r, color);
        }
    }

    protected void renderHitResults(MatrixStack matrices) {
        float f = 0.025f;
        float r = player.getPickRange();

        Hit<Terrain> terrain = player.getLookingTerrain(r);
        if (terrain != null) {
            for (AABB aabb : terrain.obj().getGroupsAABB())
                GeometryHelper.pushCube(VertexConsumer.LINES, matrices, aabb.minX(), aabb.minY(), aabb.minZ(), aabb.maxX(), aabb.maxY(), aabb.maxZ(), 0xFFFFFF00);

            Vector3f pos = terrain.pos();
            GeometryHelper.pushCube(VertexConsumer.MAIN, matrices, pos.x - f, pos.y - f, pos.z - f, pos.x + f, pos.y + f, pos.z + f, 0xFF00FFFF);
        }

        Hit<Entity> entity = player.getLookingEntity(r);
        if (entity != null) {
            AABB aabb = entity.obj().getAABB();
            GeometryHelper.pushCube(VertexConsumer.LINES, matrices, aabb.minX(), aabb.minY(), aabb.minZ(), aabb.maxX(), aabb.maxY(), aabb.maxZ(), 0xFFFFFF00);

            Vector3f pos = entity.pos();
            GeometryHelper.pushCube(VertexConsumer.MAIN, matrices, pos.x - f, pos.y - f, pos.z - f, pos.x + f, pos.y + f, pos.z + f, 0xFF00FFFF);
        }
    }

    public void applyWorldUniforms(Shader s) {
        //camera
        s.setVec3("camPos", client.camera.getPos());

        //fog
        s.setFloat("fogStart", Chunk.getFogStart(this));
        s.setFloat("fogEnd", Chunk.getFogEnd(this));
        s.setColor("fogColor", Chunk.fogColor);

        //lighting
        s.setColor("ambient", ambientLight);//Chunk.ambientLight);

        s.setInt("lightCount", lights.size());
        for (int i = 0; i < lights.size(); i++) {
            lights.get(i).pushToShader(s, i);
        }
    }

    public void applyShadowUniforms(Shader s) {
        int id = Texture.MAX_TEXTURES - 1;

        s.setMat4("lightSpaceMatrix", lightSpaceMatrix);
        s.setInt("shadowMap", id);
        s.setVec3("shadowDir", skyBox.getSunDirection());

        glActiveTexture(GL_TEXTURE0 + id); //use last available texture
        glBindTexture(GL_TEXTURE_2D, shadowBuffer.getDepthBuffer());

        glActiveTexture(GL_TEXTURE0);
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

    public void mousePress(int button, int action, int mods) {
        boolean press = action != GLFW_RELEASE;

        //ClientEntityAction mouseAction = new ClientEntityAction();
        //boolean used = false;

        switch (button) {
            case GLFW_MOUSE_BUTTON_1 -> {
                if (!press) {
                    player.stopAttacking();
                    //mouseAction.attack(false);
                    //used = true;
                }
            }
            case GLFW_MOUSE_BUTTON_2 -> {
                if (!press) {
                    player.stopUsing();
                    //mouseAction.use(false);
                    //used = true;
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

        switch (key) {
            case GLFW_KEY_R -> {
                Item i = player.getHoldingItem();
                if (i instanceof Weapon weapon && !weapon.isOnCooldown() && i.getCount() < i.getStackCount())
                    weapon.setOnCooldown();
            }
            case GLFW_KEY_ESCAPE -> client.setScreen(new PauseScreen());
            case GLFW_KEY_F1 -> this.hideHUD = !this.hideHUD;
            case GLFW_KEY_F3 -> this.debugRendering = !this.debugRendering;
            case GLFW_KEY_F4 -> this.renderShadowMap = !this.renderShadowMap;
            case GLFW_KEY_F5 -> this.cameraMode = (this.cameraMode + 1) % 3;
            case GLFW_KEY_F7 -> this.timeOfTheDay -= 100;
            case GLFW_KEY_F8 -> this.timeOfTheDay += 100;

            case GLFW_KEY_PERIOD -> changeMaterial(materialIndex + 1);
            case GLFW_KEY_COMMA -> changeMaterial(materialIndex - 1);
            case GLFW_KEY_KP_ADD -> ambientLight = Math.min(0xFFFFFF, ambientLight + 0x111111);
            case GLFW_KEY_KP_SUBTRACT -> ambientLight = Math.max(0x000000, ambientLight - 0x111111);

            //case GLFW_KEY_F9 -> connection.sendTCP(new Handshake());
            //case GLFW_KEY_F10 -> connection.sendUDP(new Message().msg("meow"));
        }
    }

    public boolean isDebugRendering() {
        return this.debugRendering;
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

    public void respawn(boolean init) {
        player = new LocalPlayer();
        givePlayerItems(player);
        this.addEntity(player);

        //if (!init)
        //    connection.sendTCP(new Respawn());
    }

    public static void givePlayerItems(Player player) {
        //player.giveItem(new CoilGun(1, 5, 0));
        //player.giveItem(new PotatoCannon(3, 40, 30));
        //player.giveItem(new RiceGun(8, 80, 60));
        //player.getInventory().setItem(player.getInventory().getFreeIndex() + 1, new CurveMaker(1, 0, 5));
        player.getInventory().setItem(1, new Flashlight(1, 0xFFFFCC));
        player.getInventory().setItem(player.getInventory().getSize() - 1, new MagicWand(1));
    }

    @Override
    public boolean isClientside() {
        return true;
    }
}
