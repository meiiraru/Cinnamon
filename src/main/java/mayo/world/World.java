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
import mayo.world.entity.Entity;
import mayo.world.entity.collectable.HealthPack;
import mayo.world.entity.living.Enemy;
import mayo.world.entity.living.Player;
import mayo.world.items.Item;
import mayo.world.items.weapons.Firearm;
import mayo.world.particle.Particle;
import mayo.world.terrain.*;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class World {

    private final Hud hud = new Hud();
    private final List<TerrainObject> terrainObjects = new ArrayList<>();
    private final List<Entity> entities = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();

    private final Movement movement = new Movement();
    public Player player;
    private int cameraMode = 0;

    private boolean debugRendering;
    private boolean isPaused;

    public void init() {
        //init hud
        hud.init();

        //add player
        respawn();

        //tutorial toast
        Toast.addToast(Text.of("WASD - move\nR - reload\nMouse - look around\nLeft Click - attack\nF3 - debug\nF5 - third person"), Client.getInstance().font);

        //temp
        TerrainObject grass = new GrassSquare(this);
        addTerrainObject(grass);

        TerrainObject pillar = new Pillar(this);
        addTerrainObject(pillar);

        TerrainObject teapot = new Teapot(this);
        teapot.setPos(0, pillar.getDimensions().y, 0);
        addTerrainObject(teapot);

        TerrainObject torii = new Torii(this);
        addTerrainObject(torii);
    }

    public void tick() {
        if (isPaused)
            return;

        //tick movement
        this.movement.apply(player);
        //hud
        this.hud.tick();

        //entities
        for (Entity entity : entities)
            entity.tick();

        //remove entities flagged to be removed
        entities.removeIf(Entity::isRemoved);

        //particles
        for (Particle particle : particles)
            particle.tick();

        //remove dead particles
        particles.removeIf(Particle::isRemoved);

        //if the player is dead, show death screen
        if (player.isDead())
            Client.getInstance().setScreen(new DeathScreen());

        //temp
        //every 3 seconds, spawn a new enemy
        if (Client.getInstance().ticks % 60 == 0) {
            Enemy enemy = new Enemy(this);
            enemy.setPos((int) (Math.random() * 128) - 64, 0, (int) (Math.random() * 128) - 64);
            addEntity(enemy);
        }

        //every 15 seconds, spawn a new health pack
        if (Client.getInstance().ticks % 300 == 0) {
            HealthPack health = new HealthPack(this);
            health.setPos((int) (Math.random() * 128) - 64, 0, (int) (Math.random() * 128) - 64);
            addEntity(health);
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
        for (TerrainObject object : terrainObjects)
            object.render(matrices, delta);

        //render entities
        for (Entity entity : entities) {
            if (entity != player || isThirdPerson())
                entity.render(matrices, delta);
        }

        //render particles
        for (Particle particle : particles)
            particle.render(matrices, delta);
    }

    public void renderHUD(MatrixStack matrices, float delta) {
        hud.render(matrices, delta);
    }

    public void uploadLightUniforms(Shader s) {
        s.setVec3("ambientLight", ColorUtils.intToRGB(0x050511));
        s.setVec3("lightPos", 0f, 3f, 2f);
    }

    public void addTerrainObject(TerrainObject object) {
        this.terrainObjects.add(object);
    }

    public void addEntity(Entity entity) {
        this.entities.add(entity);
    }

    public void addParticle(Particle particle) {
        this.particles.add(particle);
    }

    public void mousePress(int button, int action, int mods) {
        if (action != GLFW_PRESS)
            return;

        switch (button) {
            case GLFW_MOUSE_BUTTON_1 -> player.attack();
            case GLFW_MOUSE_BUTTON_2 -> player.use();
        }
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
        return terrainObjects.size();
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
        player.setHoldingItem(new Firearm("The Gun", 16, 20, 3));
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
