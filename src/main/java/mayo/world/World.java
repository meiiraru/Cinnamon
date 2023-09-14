package mayo.world;

import mayo.Client;
import mayo.model.obj.Mesh2;
import mayo.parsers.ObjLoader;
import mayo.render.MatrixStack;
import mayo.render.batch.VertexConsumer;
import mayo.render.shader.Shader;
import mayo.render.shader.Shaders;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.Resource;
import mayo.utils.Rotation;
import mayo.utils.TextUtils;

public class World {

    //temp
    private Mesh2 mesh, mesh2, mesh3, mesh4;

    public void init() {
        mesh = ObjLoader.load(new Resource("models/teapot.obj")).bake();
        mesh2 = ObjLoader.load(new Resource("models/mesa/mesa01.obj")).bake();
        mesh3 = ObjLoader.load(new Resource("models/bunny.obj")).bake();
        mesh4 = ObjLoader.load(new Resource("models/cube/cube.obj")).bake();
    }

    public void tick() {
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
    }

    public void renderHUD(MatrixStack matrices, float delta) {
        //render text demo
        Text t = Text.empty().append(Text.of("Lorem ipsum").withStyle(
                Style.EMPTY
                        .backgroundColor(0xFF72ADFF)
                        .shadowColor(0xFFFF7272)
                        .background(true)
                        .shadow(true)
                        .bold(true)
        ));

        t.append(Text.of(" dolor sit amet.\nSit quae dignissimos non voluptates sunt").withStyle(
                Style.EMPTY
                        .color(0xFF72FFAD)
        ).append(Text.of("\nut temporibus commodi eum galisum").withStyle(
                Style.EMPTY
                        .backgroundColor(0xFFFF72AD)
                        .background(true)
                        .outlined(true)
        )));

        t.append(Text.of(" alias.").withStyle(
                Style.EMPTY
                        .bold(true)
                        .italic(true)
                        .underlined(true)
        ));

        t.append("\n\n");

        t.append(Text.of("Lorem ipsum dolor sit amet,\nconsectetur adipisicing elit.").withStyle(
                Style.EMPTY
                        .outlineColor(0xFF72ADFF)
                        .outlined(true)
                        .italic(true)
        ).append(Text.of("\nAb accusamus ad alias aperiam\n[...]").withStyle(
                Style.EMPTY
                        .backgroundColor(0xFF72FF72)
                        .color(0xFF202020)
                        .bold(true)
                        .background(true)
                        .italic(false)
        )));

        t.append(Text.of("\n\niii OBFUSCATED iii").withStyle(
                Style.EMPTY
                        .backgroundColor(0xFFAD72FF)
                        .background(true)
                        .obfuscated(true)
        ));

        Client c = Client.getInstance();

        matrices.push();
        matrices.translate(c.scaledWidth / 2f, c.scaledHeight - c.font.height(t), 0f);
        c.font.render(VertexConsumer.FONT, matrices.peek(), t, TextUtils.Alignment.CENTER);
        matrices.pop();
    }
}
