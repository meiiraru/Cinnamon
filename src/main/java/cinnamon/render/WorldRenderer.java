package cinnamon.render;

import cinnamon.Client;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.framebuffer.Blit;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.framebuffer.PBRDeferredFramebuffer;
import cinnamon.render.framebuffer.ShadowMapFramebuffer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.CubeMap;
import cinnamon.render.texture.Texture;
import cinnamon.settings.Settings;
import cinnamon.vr.XrManager;
import cinnamon.world.Sky;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.light.CookieLight;
import cinnamon.world.light.DirectionalLight;
import cinnamon.world.light.Light;
import cinnamon.world.light.PointLight;
import cinnamon.world.world.WorldClient;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;

public class WorldRenderer {

    public static final int renderDistance = 192;
    public static final int entityRenderDistance = 144;

    public static final PBRDeferredFramebuffer PBRFrameBuffer = new PBRDeferredFramebuffer();
    public static final Framebuffer lightingMultiPassBuffer = new Framebuffer(Framebuffer.COLOR_BUFFER);
    public static final Framebuffer shadowBuffer = new Framebuffer(Framebuffer.DEPTH_BUFFER);
    public static final ShadowMapFramebuffer cubeShadowBuffer = new ShadowMapFramebuffer();
    public static final Framebuffer outlineFramebuffer = new Framebuffer(Framebuffer.COLOR_BUFFER);

    private static boolean shadowRendering = false;
    private static boolean outlineRendering = false;

    private static int renderedEntities, renderedTerrain, renderedParticles, renderedLights, renderedShadows;

    private static final Vector3f cameraPos = new Vector3f();
    private static final Quaternionf cameraRot = new Quaternionf();
    private static Framebuffer targetBuffer;

    public static boolean
            renderSky = true,
            renderLights = true,
            renderShadows = true,
            renderOutlines = true,
            renderDebug = true;

    public static void renderWorld(WorldClient world, Camera camera, MatrixStack matrices, float delta) {
        //prepare for world rendering
        setupFramebuffer();

        Runnable[] renderFunc = {
                () -> {
                    if (XrManager.isInXR() && Client.getInstance().screen == null)
                        world.renderXrHands(camera, matrices);
                },
                () -> world.renderTerrain(camera, matrices, delta),
                () -> world.renderEntities(camera, matrices, delta),
                () -> world.renderParticles(camera, matrices, delta),
                () -> {
                    if (camera.getEntity() instanceof LivingEntity le)
                        world.renderItemExtra(le, matrices, delta);
                }
        };

        //3d anaglyph rendering
        if (Client.getInstance().anaglyph3D) {
            renderAsAnaglyph(world, camera, matrices, delta, renderFunc);
            return;
        }

        //prepare world renderer
        initGBuffer(camera);

        //render world
        renderFunc[0].run(); //xr hands
        renderedTerrain   = world.renderTerrain(camera, matrices, delta);
        renderedEntities  = world.renderEntities(camera, matrices, delta);
        renderedParticles = world.renderParticles(camera, matrices, delta);
        renderFunc[4].run(); //item extra

        //world vertex consumer
        VertexConsumer.finishAllBatches(camera);

        //render the world lights
        renderedLights = renderedShadows = 0;
        if (renderLights) {
            renderLights(world.getLights(camera), camera, () -> {
                for (Runnable r : renderFunc)
                    r.run();
            });
        }

        //bake world
        bakeDeferred(world.getSky());

        //render the sky
        if (renderSky)
            renderSky(world.getSky(), camera, matrices);

        //render outlines
        if (renderOutlines)
            renderOutlines(world.getOutlines(camera), camera, matrices, delta);

        //render debug stuff
        if (renderDebug)
            world.renderDebug(camera, matrices, delta);
    }

    private static void renderAsAnaglyph(WorldClient world, Camera camera, MatrixStack matrices, float delta, Runnable[] renderFunc) {
        Runnable renderWorld = () -> {
            for (Runnable r : renderFunc)
                r.run();
        };

        camera.anaglyph3D(matrices, -1f / 64f, -1f, () -> {
            //render world
            initGBuffer(camera);
            renderFunc[0].run(); //xr hands
            renderedTerrain   = world.renderTerrain(camera, matrices, delta);
            renderedEntities  = world.renderEntities(camera, matrices, delta);
            renderedParticles = world.renderParticles(camera, matrices, delta);
            renderFunc[4].run(); //item extra

            //vertex consumer
            VertexConsumer.finishAllBatches(camera);

            //lights and shadows
            renderedLights = renderedShadows = 0;
            if (renderLights)
                renderLights(world.getLights(camera), camera, renderWorld);
        }, () -> {
            //bake world
            bakeDeferred(world.getSky());

            //render other stuff
            if (renderSky) renderSky(world.getSky(), camera, matrices);
            if (renderOutlines) renderOutlines(world.getOutlines(camera), camera, matrices, delta);
            if (renderDebug) world.renderDebug(camera, matrices, delta);
        });
    }

    public static void setupFramebuffer() {
        setupFramebuffer(Framebuffer.activeFramebuffer);
    }

    public static void setupFramebuffer(Framebuffer targetBuffer) {
        WorldRenderer.targetBuffer = targetBuffer;
    }

    public static void initGBuffer(Camera camera) {
        //setup pbr framebuffer
        PBRFrameBuffer.resizeTo(targetBuffer);
        PBRFrameBuffer.useClear();
        targetBuffer.blit(PBRFrameBuffer.id(), false, false, true);

        //setup gbuffer shader
        Shader s = Shaders.GBUFFER_WORLD_PBR.getShader().use();
        s.setup(camera);
        s.setVec3("camPos", camera.getPosition());
    }

    public static void renderLights(List<Light> lights, Camera camera, Runnable renderFunction) {
        if (lights.isEmpty())
            return;

        //init the light buffer
        initLightBuffer(camera);
        boolean hasShadows = renderShadows && Settings.shadowQuality.get() >= 0;

        //render the lights
        for (Light light : lights) {
            boolean shadow = hasShadows && light.castsShadows();
            if (shadow) {
                //init the shadow buffer
                shadowRendering = true;
                initShadowBuffer();

                //render the light shadow
                if (light.isDirectional())
                    renderDirectionalLightShadow(light, camera, renderFunction);
                else
                    renderLightShadowToCubeMap((PointLight) light, camera, renderFunction);

                shadowRendering = false;
                renderedShadows++;
            }

            //bake this light
            bakeLight(light, shadow);
            renderedLights++;
        }

        //reset light state
        resetLightState(camera);
    }

    public static void initLightBuffer(Camera camera) {
        lightingMultiPassBuffer.resizeTo(targetBuffer);
        lightingMultiPassBuffer.useClear();

        //backup camera
        cameraPos.set(camera.getPos());
        cameraRot.set(camera.getRot());

        //custom blending for lights
        glBlendFunc(GL_ONE, GL_ONE);
        glDisable(GL_CULL_FACE);
    }

    public static void initShadowBuffer() {
        //prepare the shadow buffer
        int w = (int) Math.pow(2, Settings.shadowQuality.get() + 9); //min is 512
        cubeShadowBuffer.resize(w, w);
        shadowBuffer.resize(w, w);
        shadowBuffer.adjustViewPort();
    }

    public static void renderDirectionalLightShadow(Light light, Camera camera, Runnable renderFunction) {
        //move the directional lights away from the camera
        Vector3f dir = light.getDirection();
        if (light instanceof DirectionalLight)
            light.pos(cameraPos.x + dir.x * -50f, cameraPos.y + dir.y * -50f, cameraPos.z + dir.z * -50f);

        //calculate light matrix
        light.calculateLightSpaceMatrix();
        Matrix4f lightSpaceMatrix = light.getLightSpaceMatrix();
        camera.updateFrustum(lightSpaceMatrix);

        //update camera
        Vector3f p = light.getPos();
        camera.setPos(p.x, p.y, p.z);
        camera.lookAt(p.x + dir.x, p.y + dir.y, p.z + dir.z);

        //render world
        shadowBuffer.useClear();
        Shaders.DEPTH.getShader().use().setMat4("lightSpaceMatrix", lightSpaceMatrix);
        renderFunction.run();

        //render vertex consumer
        Shader font = Shaders.FONT_DEPTH.getShader().use();
        font.setMat4("lightSpaceMatrix", lightSpaceMatrix);
        VertexConsumer.WORLD_FONT.finishBatch(font, camera);
        VertexConsumer.WORLD_FONT_EMISSIVE.finishBatch(font, camera);

        Shader main = Shaders.MAIN_DEPTH.getShader().use();
        main.setMat4("lightSpaceMatrix", lightSpaceMatrix);
        VertexConsumer.finishAllBatches(main, camera);
    }

    public static void renderLightShadowToCubeMap(PointLight light, Camera camera, Runnable renderFunction) {
        //calculate light matrix
        light.calculateLightSpaceMatrix();
        Matrix4f lightSpaceMatrix = light.getLightSpaceMatrix();
        Vector3f pos = light.getPos();
        float farPlane = light.getFalloffEnd();

        //setup the shaders
        Shader s = Shaders.POINT_DEPTH.getShader().use();
        s.setVec3("lightPos", pos);
        s.setFloat("farPlane", farPlane);

        Shader sh = Shaders.POINT_MAIN_DEPTH.getShader().use();
        sh.setVec3("lightPos", pos);
        sh.setFloat("farPlane", farPlane);

        //render the scene for each cube map face
        cubeShadowBuffer.use();
        for (CubeMap.Face face : CubeMap.Face.values()) {
            //bind the face
            cubeShadowBuffer.bindCubemap(face.GLTarget);

            //calculate look at matrix
            Vector3f dir = face.direction;
            Vector3f up = face.up;
            Matrix4f look = new Matrix4f().lookAt(pos.x, pos.y, pos.z, pos.x + dir.x, pos.y + dir.y, pos.z + dir.z, up.x, up.y, up.z);
            lightSpaceMatrix.mul(look, look);

            //update the camera
            camera.setPos(pos.x, pos.y, pos.z);
            camera.setRot(new Quaternionf().lookAlong(dir, up));
            camera.updateFrustum(look);

            //render the world
            s.use();
            s.setMat4("lightSpaceMatrix", look);
            renderFunction.run();

            //render vertex consumer
            sh.use();
            sh.setMat4("lightSpaceMatrix", look);
            VertexConsumer.finishAllBatches(sh, camera);
        }
    }

    public static void bakeLight(Light light, boolean hasShadow) {
        //since shadows have a custom viewport, we need to adjust its view too
        lightingMultiPassBuffer.use();
        lightingMultiPassBuffer.adjustViewPort();

        //bind the shadow map and gbuffer textures to the light shader
        Shader s = Shaders.LIGHTING_PASS.getShader().use();
        s.setTexture("gAlbedo",   PBRFrameBuffer.getTexture(0), 0);
        s.setTexture("gPosition", PBRFrameBuffer.getTexture(1), 1);
        s.setTexture("gNormal",   PBRFrameBuffer.getTexture(2), 2);
        s.setTexture("gORM",      PBRFrameBuffer.getTexture(3), 3);

        int i = 4;
        if (hasShadow) {
            s.setTexture("shadowMap", shadowBuffer.getDepthBuffer(), i++); //4
            s.setCubeMap("shadowCubeMap", cubeShadowBuffer.getCubemap(), 6);
        }

        //set up the camera position
        s.setVec3("camPos", cameraPos.x, cameraPos.y, cameraPos.z);

        //set up the light properties
        if (!hasShadow)
            light.calculateLightSpaceMatrix();
        if (light instanceof CookieLight cookie)
            s.setTexture("cookieMap", Texture.of(cookie.getTexture()), i++); //5

        s.setBool("light.castsShadows", hasShadow);
        light.pushToShader(s);

        //then render the light volume
        Blit.renderQuad();

        //unbind textures
        Texture.unbindAll(i);
        if (hasShadow)
            CubeMap.unbindTex(6);
    }

    public static void resetLightState(Camera camera) {
        //reset gl
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_CULL_FACE);

        //restore camera
        camera.setPos(cameraPos.x, cameraPos.y, cameraPos.z);
        camera.setRot(cameraRot);
        camera.updateFrustum();
    }

    public static void bakeDeferred(Sky sky) {
        //world uniforms
        targetBuffer.use();
        Shader s = Shaders.DEFERRED_WORLD_PBR.getShader().use();
        setSkyUniforms(s);
        sky.pushToShader(s, Texture.MAX_TEXTURES - 1);

        //apply gbuffer textures and the lightmap
        s.setTexture("gAlbedo",   PBRFrameBuffer.getTexture(0), 0);
        s.setTexture("gPosition", PBRFrameBuffer.getTexture(1), 1);
        s.setTexture("gNormal",   PBRFrameBuffer.getTexture(2), 2);
        s.setTexture("gORM",      PBRFrameBuffer.getTexture(3), 3);
        s.setTexture("gEmissive", PBRFrameBuffer.getTexture(4), 4);

        int i = 5;
        if (renderedLights > 0)
            s.setTexture("lightTex",  lightingMultiPassBuffer.getColorBuffer(), i++);

        //render and blit to main framebuffer
        Blit.renderQuad();
        PBRFrameBuffer.blit(targetBuffer.id(), false, true, true);

        //cleanup textures
        Texture.unbindAll(i);
    }

    public static void setSkyUniforms(Shader shader) {
        //camera
        shader.setVec3("camPos", Client.getInstance().camera.getPosition());

        //fog
        shader.setFloat("fogStart", renderDistance * Sky.fogDensity);
        shader.setFloat("fogEnd", renderDistance);
        shader.setColor("fogColor", Sky.fogColor);
    }

    public static void renderSky(Sky sky, Camera camera, MatrixStack matrices) {
        Shaders.SKYBOX.getShader().use().setup(camera);
        sky.render(camera, matrices);
    }

    public static void renderOutlines(List<Entity> entitiesToOutline, Camera camera, MatrixStack matrices, float delta) {
        if (entitiesToOutline.isEmpty())
            return;

        //prepare outline
        Shader s = initOutlineBatch(camera);

        //render entities
        for (Entity entity : entitiesToOutline) {
            s.applyColor(entity.getOutlineColor());
            entity.render(matrices, delta);
        }
        VertexConsumer.discardBatches();

        //apply outlines to the main buffer
        bakeOutlines(null);
    }

    public static Shader initOutlineBatch(Camera camera) {
        outlineRendering = true;
        outlineFramebuffer.resizeTo(targetBuffer);
        outlineFramebuffer.useClear();

        Shader s = Shaders.MODEL_PASS.getShader().use();
        s.setup(camera);

        return s;
    }

    public static void bakeOutlines(Consumer<Shader> shaderConsumer) {
        //prepare framebuffer
        targetBuffer.use();

        //prepare shader
        Shader s = Shaders.OUTLINE.getShader().use();
        s.setVec2("textelSize", 1f / outlineFramebuffer.getWidth(), 1f / outlineFramebuffer.getHeight());
        s.setTexture("outlineTex", outlineFramebuffer.getColorBuffer(), 0);
        s.setFloat("radius", 4f);

        if (shaderConsumer != null)
            shaderConsumer.accept(s);

        //render outline
        Blit.renderQuad();

        //cleanup
        Texture.unbindTex(0);
        outlineRendering = false;
    }


    // -- checks -- //


    public static boolean isShadowRendering() {
        return shadowRendering;
    }

    public static boolean isOutlineRendering() {
        return outlineRendering;
    }

    public static int getRenderedTerrain() {
        return renderedTerrain;
    }

    public static int getRenderedEntities() {
        return renderedEntities;
    }

    public static int getRenderedParticles() {
        return renderedParticles;
    }

    public static int getLightsCount() {
        return renderedLights;
    }

    public static int getShadowsCount() {
        return renderedShadows;
    }
}
