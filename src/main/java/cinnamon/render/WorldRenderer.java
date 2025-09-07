package cinnamon.render;

import cinnamon.Client;
import cinnamon.model.SimpleGeometry;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.framebuffer.PBRDeferredFramebuffer;
import cinnamon.render.framebuffer.ShadowMapFramebuffer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.CubeMap;
import cinnamon.render.texture.Texture;
import cinnamon.settings.Settings;
import cinnamon.utils.Rotation;
import cinnamon.vr.XrManager;
import cinnamon.vr.XrRenderer;
import cinnamon.world.Mask;
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
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;

public class WorldRenderer {

    public static int renderDistance = 192;
    public static int entityRenderDistance = 144;

    public static final PBRDeferredFramebuffer PBRFrameBuffer = new PBRDeferredFramebuffer();
    public static final Framebuffer outputBuffer = new Framebuffer(Framebuffer.HDR_COLOR_BUFFER | Framebuffer.DEPTH_BUFFER | Framebuffer.STENCIL_BUFFER);
    public static final Framebuffer lightingMultiPassBuffer = new Framebuffer(Framebuffer.HDR_COLOR_BUFFER);
    public static final Framebuffer shadowBuffer = new Framebuffer(Framebuffer.DEPTH_BUFFER);
    public static final ShadowMapFramebuffer cubeShadowBuffer = new ShadowMapFramebuffer();
    public static final Framebuffer outlineFramebuffer = new Framebuffer(Framebuffer.COLOR_BUFFER);

    public static Light shadowLight = null;
    private static boolean outlineRendering = false;

    private static int renderedEntities, renderedTerrain, renderedParticles, renderedLights, renderedShadows;

    private static final Vector3f cameraPos = new Vector3f();
    private static final Quaternionf cameraRot = new Quaternionf();
    private static Framebuffer targetBuffer;

    private static final Mask passMask = new Mask(-1);
    public static Mask activeMask = passMask;

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
                        renderXrHands(camera, matrices, delta);
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

        //apply bloom
        float bloom = Settings.bloomStrength.get();
        if (bloom > 0f)
            BloomRenderer.applyBloom(outputBuffer, PBRFrameBuffer.getTexture(4), 1.5f, bloom);

        //bake output buffer to the target buffer
        outputBuffer.blit(targetBuffer.id());
        targetBuffer.use();

        //render debug stuff
        if (renderDebug)
            world.renderDebug(camera, matrices, delta);

        //render outlines
        if (renderOutlines)
            renderOutlines(world.getOutlines(camera), camera, matrices, delta);
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

            //apply bloom
            float bloom = Settings.bloomStrength.get();
            if (bloom > 0f)
                BloomRenderer.applyBloom(targetBuffer, PBRFrameBuffer.getTexture(4), 1.5f, bloom);
        });
    }


    // -- framebuffer -- //


    public static void setupFramebuffer() {
        setupFramebuffer(Framebuffer.activeFramebuffer);
    }

    public static void setupFramebuffer(Framebuffer targetBuffer) {
        WorldRenderer.targetBuffer = targetBuffer;
    }

    public static void renderQuad() {
        glDisable(GL_DEPTH_TEST);
        SimpleGeometry.QUAD.render();
        glEnable(GL_DEPTH_TEST);
    }


    // -- deferred rendering -- //


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

    public static void bakeDeferred(Sky sky) {
        //world uniforms
        outputBuffer.resizeTo(targetBuffer);
        outputBuffer.useClear();
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

        //render to the output framebuffer the final scene
        //and blit the remaining depth and stencil to the main
        renderQuad();
        PBRFrameBuffer.blit(outputBuffer.id(), false, true, true);
        outputBuffer.use();

        //cleanup textures
        Texture.unbindAll(i);
    }


    // -- lights -- //


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
                initShadowBuffer();

                //render the light shadow
                if (light.getType() != 1) //only point lights use cube maps
                    renderDirectionalLightShadow(light, camera, renderFunction);
                else
                    renderLightShadowToCubeMap((PointLight) light, camera, renderFunction);

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
        //set rendering state
        shadowLight = light;
        activeMask = light.getShadowMask();

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
        Shader main = Shaders.MAIN_DEPTH.getShader().use();
        main.setMat4("lightSpaceMatrix", lightSpaceMatrix);
        VertexConsumer.finishAllBatches(main, camera);

        //reset state
        shadowLight = null;
        activeMask = passMask;
    }

    public static void renderLightShadowToCubeMap(PointLight light, Camera camera, Runnable renderFunction) {
        //set rendering state
        shadowLight = light;
        activeMask = light.getShadowMask();

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

        //reset state
        shadowLight = null;
        activeMask = passMask;
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
        renderQuad();

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


    // -- sky -- //


    public static void setSkyUniforms(Shader shader) {
        //camera
        shader.setVec3("camPos", Client.getInstance().camera.getPosition());

        //fog
        Sky sky = Client.getInstance().world.getSky();
        shader.setFloat("fogStart", sky.fogStart);
        shader.setFloat("fogEnd", sky.fogEnd);
        shader.setColor("fogColor", sky.fogColor);
    }

    public static void renderSky(Sky sky, Camera camera, MatrixStack matrices) {
        Shaders.SKYBOX.getShader().use().setup(camera);
        sky.render(camera, matrices);
    }


    // -- outlines -- //


    public static void renderOutlines(List<Entity> entitiesToOutline, Camera camera, MatrixStack matrices, float delta) {
        if (entitiesToOutline.isEmpty())
            return;

        //prepare outline
        initOutlineBatch(camera);
        Shader main = Shaders.MAIN_PASS.getShader();
        Shader model = Shaders.MODEL_PASS.getShader();

        //render entities
        for (Entity entity : entitiesToOutline) {
            int color = entity.getOutlineColor();
            model.use().applyColorRGBA(color);
            entity.render(matrices, delta);

            //finish vertex consumers here because color
            main.use().applyColorRGBA(color);
            VertexConsumer.finishAllBatches(main, camera);
        }

        //apply outlines to the main buffer
        bakeOutlines(null);
    }

    public static void initOutlineBatch(Camera camera) {
        outlineRendering = true;
        outlineFramebuffer.resizeTo(targetBuffer);
        outlineFramebuffer.useClear();

        Shaders.MAIN_PASS.getShader().use().setup(camera);
        Shaders.MODEL_PASS.getShader().use().setup(camera);
    }

    public static void bakeOutlines(Consumer<Shader> shaderConsumer) {
        //prepare framebuffer
        targetBuffer.use();

        //prepare shader
        Shader s = Shaders.OUTLINE.getShader().use();
        s.setVec2("texelSize", 1f / outlineFramebuffer.getWidth(), 1f / outlineFramebuffer.getHeight());
        s.setTexture("outlineTex", outlineFramebuffer.getColorBuffer(), 0);
        s.setFloat("radius", 4f);

        if (shaderConsumer != null)
            shaderConsumer.accept(s);

        //render outline
        renderQuad();

        //cleanup
        Texture.unbindTex(0);
        outlineRendering = false;
    }


    // -- other -- //


    public static void renderXrHands(Camera camera, MatrixStack matrices, float delta) {
        matrices.pushMatrix();
        matrices.translate(camera.getEntity().getEyePos(delta));
        Vector2f rot = camera.getEntity().getRot(delta);
        matrices.rotate(Rotation.Y.rotationDeg(-rot.y));
        matrices.rotate(Rotation.X.rotationDeg(rot.x));
        XrRenderer.renderHands(matrices);
        matrices.popMatrix();
    }


    // -- checks -- //


    public static boolean isShadowRendering() {
        return shadowLight != null;
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
