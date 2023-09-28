package mayo.world;

import mayo.Client;
import mayo.gui.Toast;
import mayo.gui.screens.DeathScreen;
import mayo.gui.screens.PauseScreen;
import mayo.input.Movement;
import mayo.render.MatrixStack;
import mayo.render.shader.Shader;
import mayo.render.shader.Shaders;
import mayo.text.Text;
import mayo.utils.AABB;
import mayo.utils.ColorUtils;
import mayo.utils.Rotation;
import mayo.world.entity.Entity;
import mayo.world.entity.collectable.EffectBox;
import mayo.world.entity.collectable.HealthPack;
import mayo.world.entity.living.Enemy;
import mayo.world.entity.living.Player;
import mayo.world.items.Item;
import mayo.world.items.weapons.CoilGun;
import mayo.world.items.weapons.Firearm;
import mayo.world.items.weapons.PotatoCannon;
import mayo.world.particle.Particle;
import mayo.world.terrain.*;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class World {

    private final Hud hud = new Hud();
    private final List<Terrain> terrain = new ArrayList<>();
    private final List<Entity> entities = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();

    private final Movement movement = new Movement();
    public Player player;
    private int cameraMode = 0;
    private boolean attackPress, usePress;

    private boolean debugRendering;
    private boolean isPaused;

    public final float gravity = -0.98f * 0.03f;

    public void init() {
        //init hud
        hud.init();

        //add player
        respawn();

        //tutorial toast
        Toast.addToast(Text.of("WASD - move\nR - reload\nMouse - look around\nLeft Click - attack\nF3 - debug\nF5 - third person"), Client.getInstance().font);

        //temp
        Terrain grass = new Grass(this);
        addTerrain(grass);

        Terrain pillar = new Pillar(this);
        addTerrain(pillar);

        Terrain teapot = new Teapot(this);
        teapot.setPos(0, pillar.getDimensions().y, 0);
        addTerrain(teapot);

        Terrain torii = new ToriiGate(this);
        addTerrain(torii);

        Terrain pole = new LightPole(this);
        pole.setPos(0, 0, 5);
        addTerrain(pole);
    }

    public void tick() {
        if (isPaused)
            return;

        //terrain
        for (Terrain terrain : terrain)
            terrain.tick();

        //entities
        for (Entity entity : entities)
            entity.tick();

        //remove entities flagged to be removed
        entities.removeIf(entity -> {
            if (entity.isRemoved()) {
                entity.onRemove();
                return true;
            }
            return false;
        });

        //particles
        for (Particle particle : particles)
            particle.tick();

        //remove dead particles
        particles.removeIf(Particle::isRemoved);

        //if the player is dead, show death screen
        Client c = Client.getInstance();
        if (player.isDead())
            c.setScreen(new DeathScreen());

        //process input
        this.movement.apply(player);
        processMouseInput();

        //hud
        this.hud.tick();

        //temp
        //every 3 seconds, spawn a new enemy
        if (c.ticks % 60 == 0) {
            Enemy enemy = new Enemy(this);
            enemy.setPos((int) (Math.random() * 128) - 64, 0, (int) (Math.random() * 128) - 64);
            addEntity(enemy);
        }

        //every 15 seconds, spawn a new health pack and a mystery effect box
        if (c.ticks % 300 == 0) {
            HealthPack health = new HealthPack(this);
            health.setPos((int) (Math.random() * 128) - 64, 0, (int) (Math.random() * 128) - 64);
            addEntity(health);
            EffectBox box = new EffectBox(this);
            box.setPos((int) (Math.random() * 128) - 64, 0, (int) (Math.random() * 128) - 64);
            addEntity(box);
        }
    }

    public void render(MatrixStack matrices, float delta) {
        Client c = Client.getInstance();
        if (isPaused)
            delta = 1f;

        //set camera
        c.camera.setup(player, cameraMode, delta);

        //set shader
        Shader s = Shaders.MODEL.getShader().use();

        s.setProjectionMatrix(c.camera.getPerspectiveMatrix());
        s.setViewMatrix(c.camera.getViewMatrix());

        //apply lighting
        uploadLightUniforms(s);

        //render terrain
        for (Terrain terrain : terrain)
            terrain.render(matrices, delta);

        //render entities
        for (Entity entity : entities) {
            if (entity != player || isThirdPerson())
                entity.render(matrices, delta);
        }

        //render particles
        for (Particle particle : particles)
            particle.render(matrices, delta);
    }

    public void renderHand(MatrixStack matrices, float delta) {
        Item item = player.getHoldingItem();
        if (item == null)
            return;

        Client c = Client.getInstance();

        //set shader
        Shader s = Shaders.MODEL.getShader().use();

        s.setProjectionMatrix(c.camera.getPerspectiveMatrix());
        s.setViewMatrix(new Matrix4f());

        //render model
        matrices.push();

        matrices.scale(-1, 1, -1);
        matrices.translate(-0.75f, -0.5f, 1f);
        matrices.rotate(Rotation.Y.rotationDeg(170));

        item.render(matrices, delta);

        matrices.pop();
    }

    public void renderHUD(MatrixStack matrices, float delta) {
        hud.render(matrices, delta);
    }

    public void uploadLightUniforms(Shader s) {
        s.setVec3("ambientLight", ColorUtils.intToRGB(0x050511));
        s.setVec3("lightPos", 0f, 3f, 5f);
    }

    public void addTerrain(Terrain terrain) {
        this.terrain.add(terrain);
    }

    public void addEntity(Entity entity) {
        this.entities.add(entity);
        entity.onAdd();
    }

    public void addParticle(Particle particle) {
        if (particle.shouldRender())
            this.particles.add(particle);
    }

    public void mousePress(int button, int action, int mods) {
        boolean press = action == GLFW_PRESS;

        switch (button) {
            case GLFW_MOUSE_BUTTON_1 -> attackPress = press;
            case GLFW_MOUSE_BUTTON_2 -> usePress = press;
        }

        processMouseInput();
    }

    private void processMouseInput() {
        if (attackPress)
            player.attack();
        if (usePress)
            player.use();
    }

    public void mouseMove(double x, double y) {
        if (!isPaused)
            movement.mouseMove(x, y);
    }

    public void keyPress(int key, int scancode, int action, int mods) {
        if (isPaused)
            return;

        movement.keyPress(key, action);

        if (action != GLFW_PRESS)
            return;

        switch (key) {
            case GLFW_KEY_R -> {
                Item i = player.getHoldingItem();
                if (i instanceof Firearm firearm && !firearm.isOnCooldown() && i.getCount() < i.getStackCount())
                    firearm.setOnCooldown();
            }
            case GLFW_KEY_ESCAPE -> Client.getInstance().setScreen(new PauseScreen());
            case GLFW_KEY_F3 -> this.debugRendering = !this.debugRendering;
            case GLFW_KEY_F5 -> this.cameraMode = (this.cameraMode + 1) % 3;
        }
    }

    public int entityCount() {
        return entities.size();
    }

    public int terrainCount() {
        return terrain.size();
    }

    public int particleCount() {
        return particles.size();
    }

    public List<Entity> getEntities(AABB region) {
        List<Entity> list = new ArrayList<>();
        for (Entity entity : entities) {
            if (region.intersects(entity.getAABB()))
                list.add(entity);
        }
        return list;
    }

    public boolean isDebugRendering() {
        return this.debugRendering;
    }

    public void respawn() {
        entities.clear();
        player = new Player(this);
        player.setHoldingItem(new CoilGun(1, 5, 0));
        player.setHoldingItem(new PotatoCannon(3, 40, 40));
        player.setPos(0, 0, 2);
        addEntity(player);
    }

    public void setPaused(boolean pause) {
        this.isPaused = pause;
        this.movement.reset();
    }

    public boolean isThirdPerson() {
        return cameraMode > 0;
    }
}
