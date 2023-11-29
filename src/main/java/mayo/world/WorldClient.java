package mayo.world;

import mayo.Client;
import mayo.gui.Toast;
import mayo.gui.screens.DeathScreen;
import mayo.gui.screens.PauseScreen;
import mayo.input.Movement;
import mayo.model.GeometryHelper;
import mayo.networking.ServerConnection;
import mayo.networking.packet.Handshake;
import mayo.networking.packet.Login;
import mayo.networking.packet.Message;
import mayo.registry.LivingModelRegistry;
import mayo.render.Camera;
import mayo.render.MatrixStack;
import mayo.render.Window;
import mayo.render.batch.VertexConsumer;
import mayo.render.framebuffer.Blit;
import mayo.render.framebuffer.Framebuffer;
import mayo.render.shader.Shader;
import mayo.render.shader.Shaders;
import mayo.text.Text;
import mayo.utils.AABB;
import mayo.utils.Maths;
import mayo.world.chunk.Chunk;
import mayo.world.collisions.Hit;
import mayo.world.entity.Entity;
import mayo.world.entity.living.Player;
import mayo.world.items.Item;
import mayo.world.items.ItemRenderContext;
import mayo.world.items.weapons.Weapon;
import mayo.world.light.DirectionalLight;
import mayo.world.light.Light;
import mayo.world.light.Spotlight;
import mayo.world.particle.Particle;
import mayo.world.terrain.Terrain;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static mayo.networking.ClientConnection.connection;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

public class WorldClient extends World {

    public final Hud hud = new Hud();
    private final Movement movement = new Movement();

    private Client client;
    public Player player;

    private int cameraMode = 0;

    private boolean debugRendering, renderShadowMap;
    private boolean hideHUD;

    //lights
    protected final List<Light> lights = new ArrayList<>();
    private final DirectionalLight sunLight = new DirectionalLight();
    private final Spotlight flashlight = (Spotlight) new Spotlight().cutOff(25f, 45f).brightness(64);

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
        Toast.addToast(Text.of("WASD - move\nR - reload\nMouse - look around\nLeft Click - attack\nF3 - debug\nF5 - third person"), Client.getInstance().font);

        //lights
        addLight(sunLight);
        addLight(flashlight);

        //rip for-loop
        addLight(new Light().pos(-5.5f, 0.5f, 2f).color(0x000000));
        addLight(new Light().pos(-3.5f, 0.5f, 2f).color(0xFF0000));
        addLight(new Light().pos(-1.5f, 0.5f, 2f).color(0x00FF00));
        addLight(new Light().pos(0.5f, 0.5f, 2f).color(0x0000FF));
        addLight(new Light().pos(2.5f, 0.5f, 2f).color(0x00FFFF));
        addLight(new Light().pos(4.5f, 0.5f, 2f).color(0xFF00FF));
        addLight(new Light().pos(6.5f, 0.5f, 2f).color(0xFFFF00));
        addLight(new Light().pos(8.5f, 0.5f, 2f).color(0xFFFFFF));

        //create player
        respawn();

        runScheduledTicks();

        //request world data
        connection.sendTCP(new Login());
    }

    @Override
    public void close() {
        client.soundManager.stopAll();
        client.world = null;
        ServerConnection.close();
        client.disconnect();
    }

    @Override
    public void tick() {
        super.tick();

        //if the player is dead, show death screen
        if (player.isDead())
            client.setScreen(new DeathScreen());

        //process input
        this.movement.apply(player);
        processMouseInput();

        //hud
        this.hud.tick();
    }

    public void render(MatrixStack matrices, float delta) {
        Client c = Client.getInstance();

        //set camera
        c.camera.setup(player, cameraMode, delta);

        //render skybox
        Shaders.MODEL.getShader().use().setup(
                c.camera.getPerspectiveMatrix(),
                c.camera.getViewMatrix()
        );
        skyBox.setSunAngle(Maths.map(timeOfTheDay + delta, 0, 24000, 0, 360));
        skyBox.render(c.camera, matrices);
        sunLight.direction(skyBox.getSunDirection());

        //flashlight
        flashlight.pos(player.getEyePos(delta));
        flashlight.direction(player.getLookDir(delta));

        //render shadows
        renderShadows(c.camera, matrices, delta);

        //set world shader
        Shader s = Shaders.WORLD_MODEL.getShader().use();
        s.setup(c.camera.getPerspectiveMatrix(), c.camera.getViewMatrix());

        //apply lighting
        applyWorldUniforms(s);
        applyShadowUniforms(s);

        //render world
        renderWorld(c.camera.getEntity(), matrices, delta);

        //render debug
        if (debugRendering && !hideHUD) {
            renderHitboxes(c.camera, matrices, delta);
            renderHitResults(matrices);
        }

        //finish rendering
        VertexConsumer.finishAllBatches(c.camera.getPerspectiveMatrix(), c.camera.getViewMatrix());

        //debug shadows
        if (renderShadowMap) {
            renderShadowBuffer(c.window.width, c.window.height, 500);
        }
    }

    private void renderShadows(Camera camera, MatrixStack matrices, float delta) {
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

    private void renderWorld(Entity camEntity, MatrixStack matrices, float delta) {
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

    private void renderShadowBuffer(int width, int height, int size) {
        glViewport(width - size, height - size, size, size);
        Blit.copy(shadowBuffer, 0, Shaders.DEPTH_BLIT.getShader());
        glViewport(0, 0, width, height);
    }

    private void renderHitboxes(Camera camera, MatrixStack matrices, float delta) {
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

        for (Light l : getLights(area)) {
            Vector3f pos = l.getPos();
            if (cameraPos.distanceSquared(pos) <= 0.1f)
                continue;

            float r = 0.125f;
            int color = l.getColor() + (0xFF << 24);
            GeometryHelper.pushCube(VertexConsumer.MAIN, matrices, pos.x - r, pos.y - r, pos.z - r, pos.x + r, pos.y + r, pos.z + r, color);
        }
    }

    private void renderHitResults(MatrixStack matrices) {
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
        s.setVec3("camPos", Client.getInstance().camera.getPos());

        //fog
        s.setFloat("fogStart", Chunk.getFogStart(this));
        s.setFloat("fogEnd", Chunk.getFogEnd(this));
        s.setColor("fogColor", Chunk.fogColor);

        //lighting
        s.setColor("ambient", 0x202020);//Chunk.ambientLight);

        s.setInt("lightCount", lights.size());
        for (int i = 0; i < lights.size(); i++) {
            lights.get(i).pushToShader(s, i);
        }
    }

    public void applyShadowUniforms(Shader s) {
        s.setMat4("lightSpaceMatrix", lightSpaceMatrix);
        s.setInt("shadowMap", 3);
        s.setVec3("shadowDir", skyBox.getSunDirection());

        glActiveTexture(GL_TEXTURE3); //0-1-2 used by the material
        glBindTexture(GL_TEXTURE_2D, shadowBuffer.getDepthBuffer());

        glActiveTexture(GL_TEXTURE0);
    }

    public void addLight(Light light) {
        scheduledTicks.add(() -> this.lights.add(light));
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

        switch (button) {
            case GLFW_MOUSE_BUTTON_1 -> {
                if (!press) player.stopAttacking();
            }
            case GLFW_MOUSE_BUTTON_2 -> {
                if (!press) player.stopUsing();
            }
        }

        processMouseInput();
    }

    private void processMouseInput() {
        Window w = Client.getInstance().window;
        if (w.mouse1Press)
            player.attackAction();
        if (w.mouse2Press)
            player.useAction();
    }

    public void mouseMove(double x, double y) {
        movement.mouseMove(x, y);
    }

    public void scroll(double x, double y) {
        player.setSelectedItem(player.getInventory().getSelectedIndex() - (int) Math.signum(y));
    }

    public void keyPress(int key, int scancode, int action, int mods) {
        movement.keyPress(key, action);

        if (action == GLFW_RELEASE)
            return;

        if (key >= GLFW_KEY_1 && key <= GLFW_KEY_9)
            player.setSelectedItem(key - GLFW_KEY_1);

        switch (key) {
            case GLFW_KEY_R -> {
                Item i = player.getHoldingItem();
                if (i instanceof Weapon weapon && !weapon.isOnCooldown() && i.getCount() < i.getStackCount())
                    weapon.setOnCooldown();
            }
            case GLFW_KEY_ESCAPE -> Client.getInstance().setScreen(new PauseScreen());
            case GLFW_KEY_F1 -> this.hideHUD = !this.hideHUD;
            case GLFW_KEY_F3 -> this.debugRendering = !this.debugRendering;
            case GLFW_KEY_F4 -> this.renderShadowMap = !this.renderShadowMap;
            case GLFW_KEY_F5 -> this.cameraMode = (this.cameraMode + 1) % 3;
            case GLFW_KEY_F7 -> this.timeOfTheDay -= 100;
            case GLFW_KEY_F8 -> this.timeOfTheDay += 100;

            case GLFW_KEY_F9 -> connection.sendTCP(new Handshake());
            case GLFW_KEY_F10 -> connection.sendUDP(new Message().msg("meow"));
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

    public void respawn() {
        player = new Player(LivingModelRegistry.STRAWBERRY);
        this.addEntity(player);
    }
}
