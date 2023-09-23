package mayo.world;

import mayo.Client;
import mayo.gui.Toast;
import mayo.gui.screens.DeathScreen;
import mayo.gui.screens.PauseScreen;
import mayo.input.Movement;
import mayo.model.ModelManager;
import mayo.model.obj.Mesh;
import mayo.render.MatrixStack;
import mayo.render.shader.Shader;
import mayo.render.shader.Shaders;
import mayo.text.Text;
import mayo.utils.AABB;
import mayo.utils.Resource;
import mayo.world.entity.Entity;
import mayo.world.entity.living.Enemy;
import mayo.world.entity.living.Player;
import mayo.world.items.weapons.Firearm;
import mayo.world.objects.Pillar;
import mayo.world.objects.Teapot;
import mayo.world.particle.Particle;
import mayo.world.particle.TextParticle;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class World {

    private final Hud hud = new Hud();
    private final List<WorldObject> objects = new ArrayList<>();
    private final List<Entity> entities = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();

    private final Mesh terrain = ModelManager.load(new Resource("models/terrain/terrain.obj"));

    private final Movement movement = new Movement();
    public Player player;
    private boolean thirdPerson;

    private boolean debugRendering;

    public void init() {
        //init hud
        hud.init();

        //add player
        respawn();

        //tutorial toast
        Toast.addToast(Text.of("WASD - move\nMouse - look around\nLeft Click - attack\nF3 - debug\nF5 - third person"), Client.getInstance().font);

        //temp
        Pillar pillar = new Pillar(new Vector3f(0, 0, 0));
        addObject(pillar);
        Teapot teapot = new Teapot(new Vector3f(0, pillar.getDimensions().y, 0));
        addObject(teapot);
    }

    public void tick() {
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

        //every 3 seconds, spawn a new enemy
        if (Client.getInstance().ticks % 60 == 0) {
            Enemy enemy = new Enemy(this);
            enemy.setPos((int) (Math.random() * 128) - 64, 0, (int) (Math.random() * 128) - 64);
            addEntity(enemy);
        }
    }

    public void render(MatrixStack matrices, float delta) {
        Client c = Client.getInstance();

        //set camera
        c.camera.setup(player, thirdPerson, delta);

        //set shader
        Shader s = Shaders.MODEL.getShader();
        s.use();
        s.setProjectionMatrix(c.camera.getPerspectiveMatrix());
        s.setViewMatrix(c.camera.getViewMatrix());

        //render terrain
        s.setModelMatrix(matrices.peek());
        terrain.render();

        //render objects
        for (WorldObject object : objects)
            object.render(matrices, delta);

        //render entities
        for (Entity entity : entities) {
            if (entity != player || thirdPerson)
                entity.render(matrices, delta);
        }

        //render particles
        for (Particle particle : particles)
            particle.render(matrices, delta);
    }

    public void renderHUD(MatrixStack matrices, float delta) {
        hud.render(matrices, delta);
    }

    public void addObject(WorldObject obj) {
        this.objects.add(obj);
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
        movement.mouseMove(x, y);
    }

    public void keyPress(int key, int scancode, int action, int mods) {
        movement.keyPress(key, action);

        if (action != GLFW_PRESS)
            return;

        switch (key) {
            case GLFW_KEY_ESCAPE -> Client.getInstance().setScreen(new PauseScreen());
            case GLFW_KEY_F3 -> this.debugRendering = !this.debugRendering;
            case GLFW_KEY_F5 -> this.thirdPerson = !this.thirdPerson;
        }
    }

    public void resetMovement() {
        movement.reset();
    }

    public int entityCount() {
        return entities.size();
    }

    public int objectCount() {
        return objects.size();
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
        player.setHoldingItem(new Firearm("The Gun", 8, 20));
        player.setPos(0, 0, 2);
        addEntity(player);
    }
}
