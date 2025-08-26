package cinnamon.render;

import cinnamon.model.SimpleGeometry;
import cinnamon.render.framebuffer.Blit;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.framebuffer.PBRDeferredFramebuffer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;
import cinnamon.settings.Settings;
import cinnamon.world.light.DirectionalLight;
import cinnamon.world.light.Light;
import cinnamon.world.world.WorldClient;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Stack;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;

public class WorldRenderer {

    public static final int renderDistance = 192;
    public static final int entityRenderDistance = 144;

    public static final PBRDeferredFramebuffer PBRFrameBuffer = new PBRDeferredFramebuffer(1, 1);
    public static final Framebuffer outlineFramebuffer = new Framebuffer(1, 1, Framebuffer.COLOR_BUFFER);
    public static final Framebuffer vertexConsumerFramebuffer = new Framebuffer(1, 1, Framebuffer.COLOR_BUFFER | Framebuffer.DEPTH_BUFFER);
    public static final Framebuffer lightingMultiPassBuffer = new Framebuffer(1, 1, Framebuffer.HDR_COLOR_BUFFER);
    private static final Framebuffer shadowBuffer = new Framebuffer(1, 1, Framebuffer.DEPTH_BUFFER);

    private static boolean outlineRendering = false;
    private static boolean shadowRendering = false;

    private static final Vector3f cameraPos = new Vector3f();
    private static final Quaternionf cameraRot = new Quaternionf();
    private static final Stack<Framebuffer> bufferStack = new Stack<>();

    //public static final Material TERRAIN_MATERIAL = MaterialManager.load(new Resource("textures/terrain/terrain.pbr"), "terrain");

    public static void prepareWorld(Camera camera) {
        Framebuffer prevFB = bufferStack.push(Framebuffer.activeFramebuffer);
        PBRFrameBuffer.useClear();
        PBRFrameBuffer.resizeTo(prevFB);
        prevFB.blit(PBRFrameBuffer.id(), false, false, true);
        Shader s = Shaders.GBUFFER_WORLD_PBR.getShader().use();
        s.setup(camera);
        s.setVec3("camPos", camera.getPosition());
    }

    public static void bakeWorld(WorldClient world, boolean hasConsumerPass) {
        Framebuffer prevFB = bufferStack.pop().use();
        Shader s = Shaders.DEFERRED_WORLD_PBR.getShader().use();

        //world uniforms
        world.applyWorldUniforms(s);
        world.getSky().pushToShader(s, Texture.MAX_TEXTURES - 1);

        //gbuffer textures
        int i = 0;
        s.setInt("gPosition", i++);
        s.setInt("gAlbedo", i++);
        s.setInt("gORM", i++);
        s.setInt("gNormal", i++);
        s.setInt("gEmissive", i++);
        PBRFrameBuffer.bindTextures();

        //lights
        if (world.getRenderedLights() > 0)
            s.setTexture("lightTex", lightingMultiPassBuffer.getColorBuffer(), i++);

        //render and blit to main framebuffer
        renderQuad();
        PBRFrameBuffer.blit(prevFB.id(), false, true, true);

        //cleanup textures
        Texture.unbindAll(i);

        //render vertex consumer stuff
        if (!hasConsumerPass)
            return;

        Shader blit = Shaders.BLIT_COLOR_DEPTH.getShader().use();
        blit.setTexture("colorTexA", prevFB.getColorBuffer(), 0);
        blit.setTexture("depthTexA", prevFB.getDepthBuffer(), 1);
        blit.setTexture("colorTexB", vertexConsumerFramebuffer.getColorBuffer(), 2);
        blit.setTexture("depthTexB", vertexConsumerFramebuffer.getDepthBuffer(), 3);

        //render quad
        SimpleGeometry.QUAD.render();

        //free textures
        Texture.unbindAll(4);
    }

    public static void vertexConsumerPass() {
        vertexConsumerFramebuffer.resizeTo(Framebuffer.activeFramebuffer);
        vertexConsumerFramebuffer.useClear();
        vertexConsumerFramebuffer.adjustViewPort();
    }

    public static void renderQuad() {
        Blit.renderQuad();
    }

    public static Shader prepareOutlineBuffer(Camera camera) {
        outlineRendering = true;
        Framebuffer prevFB = bufferStack.push(Framebuffer.activeFramebuffer);
        outlineFramebuffer.useClear();
        outlineFramebuffer.resizeTo(prevFB);
        Shader s = Shaders.MODEL_PASS.getShader().use();
        s.setup(camera);
        return s;
    }

    public static void bakeOutlines(Consumer<Shader> outlineConsumer) {
        //prepare outline
        bufferStack.pop().use();
        Shader outline = Shaders.OUTLINE.getShader().use();
        outline.setVec2("textelSize", 1f / outlineFramebuffer.getWidth(), 1f / outlineFramebuffer.getHeight());
        outline.setTexture("outlineTex", outlineFramebuffer.getColorBuffer(), 0);
        outline.setFloat("radius", 4f);
        if (outlineConsumer != null)
            outlineConsumer.accept(outline);

        //render outline
        renderQuad();

        //cleanup
        Texture.unbindAll(1);
        outlineRendering = false;
    }

    public static boolean isRenderingOutlines() {
        return outlineRendering;
    }

    public static Shader prepareLightPass(Camera camera) {
        Framebuffer prevFB = bufferStack.push(Framebuffer.activeFramebuffer);
        lightingMultiPassBuffer.useClear();
        lightingMultiPassBuffer.resizeTo(prevFB);

        //backup camera
        cameraPos.set(camera.getPos());
        cameraRot.set(camera.getRot());

        //set up the camera pos
        Shader s = Shaders.LIGHTING_PASS.getShader().use();
        s.setVec3("camPos", camera.getPosition());

        //apply g-buffer textures
        s.setInt("gPosition", 0);
        s.setInt("gAlbedo", 1);
        s.setInt("gORM", 2);
        s.setInt("gNormal", 3);
        PBRFrameBuffer.bindTextures();

        //custom blending for lights
        glBlendFunc(GL_ONE, GL_ONE);
        glDisable(GL_CULL_FACE);

        return s;
    }

    public static void bakeLights(Camera camera) {
        //restore gl flags
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_CULL_FACE);

        //unbind textures
        Texture.unbindAll(5);

        //use previous framebuffer
        bufferStack.pop().use();

        //restore camera
        camera.setPos(cameraPos.x, cameraPos.y, cameraPos.z);
        camera.setRot(cameraRot);
        camera.updateFrustum();
    }

    public static Shader prepareShadow(Camera camera, Light light) {
        shadowRendering = true;

        //prepare the shadow buffer
        shadowBuffer.useClear();
        int w = (int) Math.pow(2, Settings.shadowQuality.get() + 9); //min is 512
        shadowBuffer.resize(w, w);
        shadowBuffer.adjustViewPort();

        //move the directional lights away from the camera
        Vector3f dir = light.getDirection();
        if (light instanceof DirectionalLight)
            light.pos(cameraPos.x + dir.x * -50f, cameraPos.y + dir.y * -50f, cameraPos.z + dir.z * -50f);

        //calculate light matrix
        light.calculateLightSpaceMatrix();
        camera.updateFrustum(light.getLightSpaceMatrix());

        //update camera
        Vector3f p = light.getPos();
        camera.setPos(p.x, p.y, p.z);
        camera.lookAt(p.x + dir.x, p.y + dir.y, p.z + dir.z);

        //apply the light matrix to the depth shader
        Shader s = Shaders.MAIN_DEPTH.getShader().use();
        s.setMat4("lightSpaceMatrix", light.getLightSpaceMatrix());

        Shader sh = Shaders.DEPTH.getShader().use();
        sh.setMat4("lightSpaceMatrix", light.getLightSpaceMatrix());

        return s;
    }

    public static void bindShadow(Shader lightShader) {
        //bind the shadow map to the light shader
        lightShader.use();

        //bind textures
        PBRFrameBuffer.bindTextures();
        lightShader.setTexture("shadowMap", shadowBuffer.getDepthBuffer(), 4);

        //since shadows have a custom viewport, we need to reset it too
        lightingMultiPassBuffer.use();
        lightingMultiPassBuffer.adjustViewPort();

        shadowRendering = false;
    }

    public static boolean isRenderingShadows() {
        return shadowRendering;
    }
}
