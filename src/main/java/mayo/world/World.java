package mayo.world;

import mayo.Client;
import mayo.input.Movement;
import mayo.model.ModelManager;
import mayo.model.obj.Mesh;
import mayo.render.MatrixStack;
import mayo.render.shader.Shader;
import mayo.render.shader.Shaders;
import mayo.utils.Resource;
import mayo.world.entity.Enemy;
import mayo.world.entity.Entity;
import mayo.world.entity.Player;
import mayo.world.items.weapons.Firearm;
import mayo.world.objects.Pillar;
import mayo.world.objects.Teapot;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public class World {

    private final Hud hud = new Hud();
    private final List<WorldObject> objects = new ArrayList<>();
    private final List<Entity> entities = new ArrayList<>();

    private final Mesh terrain = ModelManager.load(new Resource("models/terrain/terrain.obj"));

    private final Movement movement = new Movement();
    public Player player;

    public void init() {
        player = new Player(this);
        player.setHoldingItem(new Firearm("The Gun", 8));
        player.setPos(-2, 0, 2);
        addEntity(player);

        Enemy enemy = new Enemy(this);
        enemy.setPos(2, 0, 2);
        addEntity(enemy);

        Pillar pillar = new Pillar(new Vector3f(0, 0, 0));
        addObject(pillar);
        Teapot teapot = new Teapot(new Vector3f(0, pillar.getDimensions().y, 0));
        addObject(teapot);
    }

    public void tick() {
        this.movement.apply(player);
        this.hud.tick();
        entities.get(1).move(0, 0, 0.1f);
    }

    public void render(MatrixStack matrices, float delta) {
        Client c = Client.getInstance();

        //set camera
        c.camera.setup(player, true, delta);

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
        for (Entity entity : entities)
            entity.render(matrices, delta);

        Vector3f pos = player.getEyePos(delta);
        //Vector3f dest = pos.add(player.getLookDir().mul(5f), new Vector3f());
        //entities.get(1).setPos(dest.x, dest.y, dest.z);

        entities.get(1).lookAt(pos.x, pos.y, pos.z);
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

    public void mousePress(int button, int action, int mods) {
        if (action != GLFW_PRESS)
            return;
    }

    public void mouseMove(double x, double y) {
        movement.mouseMove(x, y);
    }

    public void keyPress(int key, int scancode, int action, int mods) {
        movement.keyPress(key, action);
    }

    public void resetMovement() {
        movement.firstMouse = true;
    }

    public int entityCount() {
        return entities.size();
    }

    public int objectCount() {
        return objects.size();
    }
}
