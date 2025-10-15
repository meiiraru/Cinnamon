package cinnamon.render;

import cinnamon.Client;
import cinnamon.model.SimpleGeometry;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.framebuffer.PBRDeferredFramebuffer;
import cinnamon.render.framebuffer.ShadowCascadeFramebuffer;
import cinnamon.render.framebuffer.ShadowCubemapFramebuffer;
import cinnamon.render.shader.PostProcess;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.CubeMap;
import cinnamon.render.texture.Texture;
import cinnamon.render.texture.TextureArray;
import cinnamon.settings.Settings;
import cinnamon.utils.Rotation;
import cinnamon.vr.XrManager;
import cinnamon.vr.XrRenderer;
import cinnamon.world.Mask;
import cinnamon.world.Sky;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.light.CookieLight;
import cinnamon.world.light.Light;
import cinnamon.world.light.PointLight;
import cinnamon.world.world.WorldClient;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
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
    public static final ShadowCubemapFramebuffer cubeShadowBuffer = new ShadowCubemapFramebuffer();
    public static final Framebuffer outlineFramebuffer = new Framebuffer(Framebuffer.COLOR_BUFFER);

    public static final ShadowCascadeFramebuffer cascadeShadowBuffer = new ShadowCascadeFramebuffer(CascadedShadow.getNumCascades());
    public static final CascadedShadow cascadedShadow = new CascadedShadow();

    public static Light shadowLight = null;
    private static boolean outlineRendering = false;

    private static int renderedEntities, renderedTerrain, renderedParticles, renderedLights, renderedShadows;

    private static final Vector3f cameraPos = new Vector3f();
    private static final Quaternionf cameraRot = new Quaternionf();
    private static final Matrix4f pointLightMatrix = new Matrix4f();
    private static final Quaternionf pointLightRotation = new Quaternionf();
    private static Framebuffer targetBuffer;

    private static final Mask passMask = new Mask(-1);
    public static Mask activeMask = passMask;

    public static boolean
            renderSky = true,
            renderLights = true,
            renderShadows = true,
            renderOutlines = true,
            renderDebug = true;
    public static int debugGBuffer = -1;

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
        MaterialApplier.cleanup();
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
        bakeDeferred(camera, world.getSky(), renderedLights > 0);

        //debug gbuffer
        if (debugGBuffer >= 0)
            debugRenderGBuffer(debugGBuffer);

        //render the sky
        if (renderSky)
            renderSky(world.getSky(), camera, matrices);

        //apply bloom
        float bloom = Settings.bloomStrength.get();
        if (debugGBuffer < 0 && bloom > 0f)
            BloomRenderer.applyBloom(outputBuffer, PBRFrameBuffer.getEmissive(), 0.8f, bloom);

        //render lens flare
        if (renderLights)
            renderLightsFlare(world.getLights(camera), camera, matrices);

        //bake output buffer to the target buffer
        bake();

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
            bakeDeferred(camera, world.getSky(), renderedLights > 0);
            if (debugGBuffer >= 0) debugRenderGBuffer(debugGBuffer);

            //render other stuff
            if (renderSky) renderSky(world.getSky(), camera, matrices);
            if (renderOutlines) renderOutlines(world.getOutlines(camera), camera, matrices, delta);
            if (renderDebug) world.renderDebug(camera, matrices, delta);

            //bloom
            float bloom = Settings.bloomStrength.get();
            if (debugGBuffer < 0 && bloom > 0f)
                BloomRenderer.applyBloom(targetBuffer, PBRFrameBuffer.getEmissive(), 0.8f, bloom);

            //lens flare
            if (renderLights)
                renderLightsFlare(world.getLights(camera), camera, matrices);
        });
    }


    // -- framebuffer -- //


    public static void setupFramebuffer() {
        setupFramebuffer(Framebuffer.activeFramebuffer);
    }

    public static void setupFramebuffer(Framebuffer targetBuffer) {
        WorldRenderer.targetBuffer = targetBuffer;
    }

    public static void bake() {
        outputBuffer.blit(targetBuffer.id());
        targetBuffer.use();
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
        targetBuffer.blit(PBRFrameBuffer.id(), false, true, true);

        //setup gbuffer shader
        Shader s = Shaders.GBUFFER_WORLD_PBR.getShader().use();
        s.setup(camera);
        s.setVec3("camPos", camera.getPosition());
    }

    public static void bakeDeferred(Camera camera, Sky sky, boolean hasLights) {
        //world uniforms
        outputBuffer.resizeTo(targetBuffer);
        outputBuffer.useClear();
        Shader s = Shaders.DEFERRED_WORLD_PBR.getShader().use();

        //camera
        s.setupInverse(camera);

        //apply gbuffer textures and the lightmap
        s.setTexture("gAlbedo",   PBRFrameBuffer.getAlbedo(),      0);
        s.setTexture("gNormal",   PBRFrameBuffer.getNormal(),      1);
        s.setTexture("gORM",      PBRFrameBuffer.getORM(),         2);
        s.setTexture("gEmissive", PBRFrameBuffer.getEmissive(),    3);
        s.setTexture("gDepth",    PBRFrameBuffer.getDepthBuffer(), 4);
        s.setTexture("lightTex",  hasLights ? lightingMultiPassBuffer.getColorBuffer() : 0, 5);

        //apply sky
        setSkyUniforms(s, camera, sky);
        sky.bind(s, 6);

        //render to the output framebuffer the final scene
        //and blit the remaining depth and stencil to the main
        renderQuad();
        PBRFrameBuffer.blit(outputBuffer.id(), false, true, true);
        outputBuffer.use();

        //cleanup textures
        Texture.unbindAll(6);
        sky.unbind(6);
    }

    public static void debugRenderGBuffer(int index) {
        outputBuffer.resizeTo(targetBuffer);
        outputBuffer.useClear();

        Shader s = PostProcess.BLIT.getShader().use();
        s.setTexture("colorTex", PBRFrameBuffer.getTexture(index), 0);

        renderQuad();

        PBRFrameBuffer.blit(outputBuffer.id(), false, true, true);
        outputBuffer.use();
        Texture.unbindTex(0);
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
                switch (light.getType()) {
                    case 1 -> renderLightShadowToCubeMap((PointLight) light, camera, renderFunction);
                    case 3 -> renderDirectionalLightShadow(light, camera, renderFunction);
                    default -> renderSpotLightShadow(light, camera, renderFunction);
                }

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

        //set the light shader camera uniforms
        Shader s = Shaders.LIGHTING_PASS.getShader().use();
        s.setVec3("camPos", camera.getPosition());
        s.applyViewMatrix(camera.getViewMatrix());
        s.setupInverse(camera);

        //custom blending for lights
        glBlendFunc(GL_ONE, GL_ONE);
        glDisable(GL_CULL_FACE);
    }

    public static void initShadowBuffer() {
        //prepare the shadow buffer
        int w = 256 << Settings.shadowQuality.get();
        cascadeShadowBuffer.resize(w, w);
        cubeShadowBuffer.resize(w, w);
        shadowBuffer.resize(w, w);
        shadowBuffer.adjustViewPort();
    }

    public static void renderDirectionalLightShadow(Light light, Camera camera, Runnable renderFunction) {
        //set rendering state
        shadowLight = light;
        activeMask = light.getShadowMask();

        //restore the camera because the light matrices requires the camera view
        camera.setPos(cameraPos.x, cameraPos.y, cameraPos.z);
        camera.setRot(cameraRot);

        //calculate light space matrices for each cascade
        Vector3f dir = light.getDirection();
        cascadedShadow.calculateCascadeMatrices(camera, dir);
        Matrix4f[] cascadeMatrices = cascadedShadow.getCascadeMatrices();

        //update camera
        camera.lookAt(cameraPos.x + dir.x, cameraPos.y + dir.y, cameraPos.z + dir.z);

        //render world for each cascade
        cascadeShadowBuffer.useClear();

        //update camera frustum for culling
        camera.updateFrustum(cascadedShadow.getCullingMatrix());

        //prepare shader
        Shader s = Shaders.DEPTH_DIR.getShader().use();
        s.setMat4Array("cascadeMatrices", cascadeMatrices);

        //render world
        renderFunction.run();
        MaterialApplier.cleanup();

        //render vertex consumer
        cascadeShadowBuffer.use();
        Shader main = Shaders.MAIN_DEPTH_DIR.getShader().use();
        main.setMat4Array("cascadeMatrices", cascadeMatrices);
        VertexConsumer.finishAllBatches(main, camera);

        //reset state
        shadowLight = null;
        activeMask = passMask;
    }

    public static void renderSpotLightShadow(Light light, Camera camera, Runnable renderFunction) {
        //set rendering state
        shadowLight = light;
        activeMask = light.getShadowMask();

        //calculate light matrix
        light.calculateLightSpaceMatrix();
        Matrix4f lightSpaceMatrix = light.getLightSpaceMatrix();
        camera.updateFrustum(lightSpaceMatrix);

        //update camera
        Vector3f p = light.getPos();
        Vector3f dir = light.getDirection();
        camera.setPos(p.x, p.y, p.z);
        camera.lookAt(p.x + dir.x, p.y + dir.y, p.z + dir.z);

        //render world
        shadowBuffer.useClear();
        Shaders.DEPTH.getShader().use().setMat4("lightSpaceMatrix", lightSpaceMatrix);
        renderFunction.run();
        MaterialApplier.cleanup();

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
            pointLightMatrix.setLookAt(pos.x, pos.y, pos.z, pos.x + dir.x, pos.y + dir.y, pos.z + dir.z, up.x, up.y, up.z);
            lightSpaceMatrix.mul(pointLightMatrix, pointLightMatrix);

            //update the camera
            camera.setPos(pos.x, pos.y, pos.z);
            camera.setRot(pointLightRotation.identity().lookAlong(dir, up));
            camera.updateFrustum(pointLightMatrix);

            //render the world
            s.use();
            s.setMat4("lightSpaceMatrix", pointLightMatrix);
            renderFunction.run();
            MaterialApplier.cleanup();

            //render vertex consumer
            sh.use();
            sh.setMat4("lightSpaceMatrix", pointLightMatrix);
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
        s.setTexture("gAlbedo", PBRFrameBuffer.getAlbedo(),      0);
        s.setTexture("gNormal", PBRFrameBuffer.getNormal(),      1);
        s.setTexture("gORM",    PBRFrameBuffer.getORM(),         2);
        s.setTexture("gDepth",  PBRFrameBuffer.getDepthBuffer(), 3);

        s.setTexture("shadowMap", hasShadow ? shadowBuffer.getDepthBuffer() : 0, 4);
        s.setTexture("cookieMap", light instanceof CookieLight cookie ? Texture.of(cookie.getCookieTexture()).getID() : 0, 5);
        s.setCubeMap("shadowCubeMap", hasShadow ? cubeShadowBuffer.getCubemap() : 0, 6);
        s.setTextureArray("shadowCascadeMap", hasShadow ? cascadeShadowBuffer.getDepthTextureArray() : 0, 7);

        //set up the light properties
        if (!hasShadow) light.calculateLightSpaceMatrix();
        s.setBool("light.castsShadows", hasShadow);

        s.setInt("cascadeCount", CascadedShadow.getNumCascades());
        s.setFloatArray("cascadeDistances", cascadedShadow.getCascadeDistances());
        s.setMat4Array("cascadeMatrices", cascadedShadow.getCascadeMatrices());

        light.pushToShader(s);

        //then render the light volume
        renderQuad();

        //unbind textures
        Texture.unbindAll(6);
        CubeMap.unbindTex(6);
        TextureArray.unbindTex(7);
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

    public static void renderLightsFlare(List<Light> lights, Camera camera, MatrixStack matrices) {
        if (lights.isEmpty())
            return;

        boolean lensFlare = Settings.lensFlare.get();
        List<Light> directionalLights = new ArrayList<>();

        //prepare the flare buffer
        outputBuffer.use();
        glDisable(GL_DEPTH_TEST);
        glBlendFunc(GL_ONE, GL_ONE);

        //set up the flare shader
        Shader s = Shaders.LIGHT_GLARE.getShader().use();
        s.setup(camera);
        s.setTexture("gDepth", PBRFrameBuffer.getDepthBuffer(), 0);

        float aspectRatio = (float) outputBuffer.getWidth() / outputBuffer.getHeight();
        float texelX = 1f / outputBuffer.getWidth(), texelY = 1f / outputBuffer.getHeight();
        s.setFloat("aspectRatio", aspectRatio);
        s.setVec2("sampleRadius", texelX, texelY);

        //render the flares
        for (Light light : lights) {
            float intensity = light.getGlareIntensity();
            if (intensity <= 0f)
                continue;

            if (lensFlare && light.getType() == 3)
                directionalLights.add(light);

            s.applyColor(light.getColor());
            s.setFloat("intensity", intensity);
            s.setVec3("lightPosition", light.getPos());
            s.setFloat("glareSize", light.getGlareSize());
            s.setTexture("textureSampler", Texture.of(light.getGlareTexture()), 1);

            SimpleGeometry.QUAD.render();
        }

        //render lens flare for directional lights
        if (!directionalLights.isEmpty()) {
            Shader lensShader = Shaders.LENS_FLARE.getShader().use();
            lensShader.setup(camera);
            lensShader.setVec3("camPos", camera.getPosition());
            lensShader.setFloat("aspectRatio", aspectRatio);
            lensShader.setVec2("sampleRadius", texelX, texelY);
            lensShader.setTexture("gDepth", PBRFrameBuffer.getDepthBuffer(), 0);

            for (Light light : directionalLights) {
                lensShader.applyColor(light.getColor());
                lensShader.setVec3("direction", light.getDirection());
                lensShader.setFloat("intensity", light.getGlareIntensity());
                SimpleGeometry.QUAD.render();
            }
        }

        //reset state
        glEnable(GL_DEPTH_TEST);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        Texture.unbindAll(2);
    }


    // -- sky -- //


    public static void setSkyUniforms(Shader shader, Camera camera, Sky sky) {
        //camera
        shader.setVec3("camPos", camera.getPosition());

        //fog
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
            entity.render(camera, matrices, delta);
            MaterialApplier.cleanup();

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
