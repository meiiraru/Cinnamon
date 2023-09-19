package mayo.world;

import mayo.Client;
import mayo.world.items.weapons.Firearm;
import mayo.model.ModelManager;
import mayo.model.obj.Mesh;
import mayo.render.Camera;
import mayo.render.MatrixStack;
import mayo.render.shader.Shader;
import mayo.render.shader.Shaders;
import mayo.utils.AABB;
import mayo.utils.Resource;
import mayo.world.entity.Player;
import mayo.world.objects.Pillar;
import mayo.world.objects.Teapot;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_2;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public class World {

    private final Hud hud = new Hud();
    private final List<WorldObject> objects = new ArrayList<>();

    private final Mesh terrain = ModelManager.load(new Resource("models/terrain/terrain.obj"));
    public Player player;

    public void init() {
        player = new Player(this, new AABB(0.875f, 1.8f, 0.875f));
        player.setHoldingItem(new Firearm("Gun", 8));

        Pillar pillar = new Pillar(new Vector3f(0, 0, 3));
        addObject(pillar);
        Teapot teapot = new Teapot(new Vector3f(0, pillar.getDimensions().y, 3));
        addObject(teapot);
    }

    public void tick() {
        this.hud.tick();
    }

    public void render(MatrixStack matrices, float delta) {
        Client c = Client.getInstance();

        Shader s = Shaders.MODEL.getShader();
        s.use();
        s.setProjectionMatrix(c.camera.getPerspectiveMatrix());
        s.setViewMatrix(c.camera.getViewMatrix(delta));

        //render terrain
        s.setModelMatrix(matrices.peek());
        terrain.render();

        //render objects
        for (WorldObject object : objects)
            object.render(s, matrices, delta);
    }

    public void renderHUD(MatrixStack matrices, float delta) {
        hud.render(matrices, delta);
    }

    public void addObject(WorldObject obj) {
        this.objects.add(obj);
    }

    public void mousePress(int button, int action, int mods) {
        if (action != GLFW_PRESS)
            return;

        if (button == GLFW_MOUSE_BUTTON_2) {
            Camera c = Client.getInstance().camera;
            Vector3f vec = c.getForwards().mul(5f);
            addObject(new Teapot(c.getPos().add(vec)));
        }
    }

    public void keyPress(int key, int scancode, int action, int mods) {
        //do nothing for now
    }
}
