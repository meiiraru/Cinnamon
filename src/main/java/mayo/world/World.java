package mayo.world;

import mayo.Client;
import mayo.model.obj.Mesh;
import mayo.parsers.ObjLoader;
import mayo.render.MatrixStack;
import mayo.render.shader.Shader;
import mayo.render.shader.Shaders;
import mayo.utils.Resource;
import mayo.utils.Rotation;

public class World {

    private final Hud hud = new Hud();

    //temp
    private Mesh mesh, mesh2, mesh3, mesh4, mesh5, mesh6;

    public void init() {
        mesh = ObjLoader.load(new Resource("models/teapot.obj")).bake();
        mesh2 = ObjLoader.load(new Resource("models/mesa/mesa01.obj")).bake();
        mesh3 = ObjLoader.load(new Resource("models/bunny.obj")).bake();
        mesh4 = ObjLoader.load(new Resource("models/cube/cube.obj")).bake();
        mesh5 = ObjLoader.load(new Resource("models/suzanne.obj")).bake();
        mesh6 = ObjLoader.load(new Resource("models/bullet/bullet.obj")).bake();
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

        //render mesh 1
        matrices.push();
        matrices.translate(0, -mesh.getBBMin().y + (mesh2.getBBMax().y - mesh2.getBBMin().y), 0);
        matrices.scale(0.5f);
        s.setModelMatrix(matrices.peek());
        mesh.render();
        matrices.pop();

        //render mesh 2
        s.setModelMatrix(matrices.peek());
        mesh2.render();

        //render mesh 3
        matrices.push();
        matrices.translate(-3f, (mesh2.getBBMax().y - mesh2.getBBMin().y) - 1f, -4f);
        matrices.rotate(Rotation.Y.rotationDeg(c.ticks + delta));
        matrices.scale(30f);
        s.setModelMatrix(matrices.peek());
        mesh3.render();
        matrices.pop();

        //render mesh 4
        matrices.push();
        matrices.scale(2f);
        matrices.translate(0, -mesh4.getBBMin().y, 0);
        s.setModelMatrix(matrices.peek());
        mesh4.render();
        matrices.pop();

        //render mesh 5
        matrices.push();
        matrices.translate(3f, (mesh2.getBBMax().y - mesh2.getBBMin().y) + 0.3f, 3f);
        matrices.rotate(Rotation.Y.rotationDeg(30f));
        matrices.rotate(Rotation.X.rotationDeg(-50f));
        s.setModelMatrix(matrices.peek());
        mesh5.render();
        matrices.pop();

        //render mesh 6
        s.setModelMatrix(matrices.peek());
        mesh6.render();
    }

    public void renderHUD(MatrixStack matrices, float delta) {
        hud.render(matrices, delta);
    }
}
