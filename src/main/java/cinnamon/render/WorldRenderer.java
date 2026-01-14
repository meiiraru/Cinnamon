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
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.items.ItemRenderContext;
import cinnamon.world.light.CookieLight;
import cinnamon.world.light.Light;
import cinnamon.world.light.PointLight;
import cinnamon.world.sky.Sky;
import cinnamon.world.world.WorldClient;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class WorldRenderer {

    public static int shadowRenderDistance = 192;
    public static int renderDistance = 192;
    public static int entityRenderDistance = 144;

    public static final PBRDeferredFramebuffer PBRFrameBuffer = new PBRDeferredFramebuffer();
    public static final Framebuffer outputBuffer = new Framebuffer(Framebuffer.HDR_COLOR_BUFFER | Framebuffer.DEPTH_BUFFER | Framebuffer.STENCIL_BUFFER);
    public static final Framebuffer lightingMultiPassBuffer = new Framebuffer(Framebuffer.HDR_COLOR_BUFFER);
    public static final Framebuffer shadowBuffer = new Framebuffer(Framebuffer.DEPTH_BUFFER);
    public static final ShadowCubemapFramebuffer cubeShadowBuffer = new ShadowCubemapFramebuffer();
    public static final Framebuffer lastFrameFramebuffer = new Framebuffer(Framebuffer.HDR_COLOR_BUFFER | Framebuffer.DEPTH_BUFFER);

    public static final ShadowCascadeFramebuffer cascadeShadowBuffer = new ShadowCascadeFramebuffer(CascadedShadow.NUM_CASCADES);
    public static final CascadedShadow cascadedShadow = new CascadedShadow();

    public static Light shadowLight = null;
    private static boolean outlineRendering = false;
    private static boolean heldItemRendering = false;

    private static int renderedEntities, renderedTerrain, renderedParticles, renderedLights, renderedShadows;

    public static final Camera camera = new Camera();
    private static final Vector3f cameraPos = new Vector3f();
    private static final Quaternionf cameraRot = new Quaternionf();
    private static final Matrix4f pointLightMatrix = new Matrix4f();
    private static final Quaternionf pointLightRotation = new Quaternionf();
    private static final Matrix4f lightModelMatrix = new Matrix4f();
    private static Framebuffer targetBuffer;

    private static final Mask passMask = new Mask(-1);
    public static Mask activeMask = passMask;

    public static boolean
            renderWater    = true,
            renderSSAO     = true,
            renderSSR      = true,
            renderLights   = true,
            renderShadows  = true,
            renderSky      = true,
            renderBloom    = true,
            renderDebug    = true,
            renderOutlines = true;

    public static void renderWorld(WorldClient world, MatrixStack matrices, float delta) {
        //prepare for world rendering
        setupFramebuffer();
        Client client = Client.getInstance();

        Runnable[] renderFunc = {
                () -> {
                    if (XrManager.isInXR() && client.screen == null)
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
        if (client.anaglyph3D) {
            renderAsAnaglyph(world, matrices, delta, renderFunc);
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

        //water
        renderWater(world, camera, matrices, delta);

        //render ssao
        renderSSAO(camera);

        //render ssr
        renderSSR(camera);

        //render the world lights
        renderedLights = renderedShadows = 0;
        if (renderLights) {
            renderLights(world.getLights(camera), camera, () -> {
                for (Runnable r : renderFunc)
                    r.run();
            });
        }

        //bake world
        bakeDeferred(camera, world.getSky());

        //render the sky
        if (renderSky)
            world.getSky().render(camera, matrices);

        //apply bloom
        applyBloom();

        //render lens flare
        if (renderLights)
            renderLightsFlare(world.getLights(camera), camera, matrices);

        //render debug stuff
        if (renderDebug)
            world.renderDebug(camera, matrices, delta);

        //store current frame for later
        copyLastFrame(true, true);

        //render outlines
        renderOutlines(world, camera, matrices, delta);

        //bake output buffer to the target buffer
        bake();
    }

    private static void renderAsAnaglyph(WorldClient world, MatrixStack matrices, float delta, Runnable[] renderFunc) {
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
            MaterialApplier.cleanup();
            VertexConsumer.finishAllBatches(camera);

            //water
            renderWater(world, camera, matrices, delta);

            //effects
            renderSSAO(camera);
            renderSSR(camera);

            //lights
            renderedLights = renderedShadows = 0;
            if (renderLights)
                renderLights(world.getLights(camera), camera, renderWorld);
        }, () -> {
            //bake world
            bakeDeferred(camera, world.getSky());

            //post bake renderer
            if (renderSky) world.getSky().render(camera, matrices);
            applyBloom();
            if (renderLights) renderLightsFlare(world.getLights(camera), camera, matrices);
            if (renderDebug) world.renderDebug(camera, matrices, delta);
        });

        //finish render
        copyLastFrame(true, true);

        //render outlines
        renderOutlines(world, camera, matrices, delta);

        bake();
    }


    // -- framebuffer -- //


    public static void setupFramebuffer() {
        setupFramebuffer(Framebuffer.activeFramebuffer);
    }

    public static void setupFramebuffer(Framebuffer buffer) {
        targetBuffer = buffer;
    }

    public static void copyLastFrame(boolean color, boolean depth) {
        Framebuffer current = Framebuffer.activeFramebuffer;
        lastFrameFramebuffer.resizeTo(current);
        lastFrameFramebuffer.use();
        current.blit(lastFrameFramebuffer, color, depth, false);
        current.use();
    }

    public static void bake() {
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        outputBuffer.blit(targetBuffer);
        targetBuffer.use();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        resetFlags();
    }

    public static void bakeQuad() {
        //framebuffer
        targetBuffer.use();

        //shader
        Shader s = PostProcess.BLIT.getShader().use();
        s.setTexture("colorTex", outputBuffer.getColorBuffer(), 0);

        //render
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        renderQuad();

        //clean
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        Texture.unbindTex(0);
        resetFlags();
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
        targetBuffer.blit(PBRFrameBuffer, false, true, true);

        //setup gbuffer shader
        Shader s = Shaders.GBUFFER_WORLD_PBR.getShader().use();
        s.setup(camera);
        s.setVec3("camPos", camera.getPosition());
    }

    public static void bakeDeferred(Camera camera, Sky sky) {
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
        s.setTexture("lightTex",  lightingMultiPassBuffer.getColorBuffer(), 5);
        s.setTexture("ssaoTex",   SSAORenderer.getTexture(),       6);
        s.setTexture("ssrTex",    SSRRenderer.getTexture(),        7);

        s.setFloat("lightFactor", renderLights && renderedLights > 0 ? 1f : 0f);
        s.setFloat("ssaoFactor", renderSSAO && Settings.ssaoLevel.get() >= 0 ? 1f : 0f);
        s.setFloat("ssrFactor", renderSSR && Settings.ssrLevel.get() >= 0 ? 1f : 0f);

        //apply sky
        sky.applyUniforms(s, camera);
        sky.bind(s, 8);

        //render to the output framebuffer the final scene
        //and blit the remaining depth and stencil to the main
        renderQuad();
        PBRFrameBuffer.blit(outputBuffer, false, true, true);

        //cleanup textures
        Texture.unbindAll(8);
        sky.unbind(8);
    }


    // -- effects -- //


    public static void renderSSAO(Camera camera) {
        int ssaoLevel = Settings.ssaoLevel.get();
        if (renderSSAO && ssaoLevel >= 0) {
            SSAORenderer.renderSSAO(PBRFrameBuffer, camera, ssaoLevel, 0.5f);
            if (ssaoLevel > 0)
                SSAORenderer.blurSSAO();
        }
    }

    public static void renderSSR(Camera camera) {
        int ssrLevel = Settings.ssrLevel.get();
        if (renderSSR && ssrLevel >= 0)
            SSRRenderer.render(PBRFrameBuffer, lastFrameFramebuffer.getColorBuffer(), camera, ssrLevel);
    }

    public static void applyBloom() {
        float bloom = Settings.bloomStrength.get();
        if (renderBloom && bloom > 0f)
            BloomRenderer.applyBloom(outputBuffer, PBRFrameBuffer.getEmissive(), 1.5f, bloom);
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
            if (light.getIntensity() <= 0f)
                continue;

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
            bakeLight(camera, light, shadow);
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

        //init light pass
        Shader s = Shaders.LIGHT_PASS.getShader().use();
        s.setVec3("camPos", camera.getPosition());
        s.setup(camera);
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

        //update camera frustum for culling
        camera.updateFrustum(cascadedShadow.getCullingMatrix());

        //render world for each cascade
        cascadeShadowBuffer.useClear();

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

    public static void bakeLight(Camera camera, Light light, boolean hasShadow) {
        //since shadows have a custom viewport, we need to adjust its view too
        lightingMultiPassBuffer.use();
        lightingMultiPassBuffer.adjustViewPort();

        //bind the shadow map and gbuffer textures to the light shader
        Shader s = Shaders.LIGHT_PASS.getShader().use();
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

        s.setInt("cascadeCount", CascadedShadow.NUM_CASCADES);
        s.setFloatArray("cascadeDistances", cascadedShadow.getCascadeDistances());
        s.setMat4Array("cascadeMatrices", cascadedShadow.getCascadeMatrices());

        light.pushToShader(s);

        //render the light volume
        glEnable(GL_CULL_FACE);
        glCullFace(GL_FRONT);

        switch (light.getType()) {
            case 1 -> {
                //for point lights, render a sphere
                lightModelMatrix.identity();
                light.copyTransform(lightModelMatrix);
                s.setMat4("model", lightModelMatrix);

                SimpleGeometry.SPHERE.render();
            }
            case 3 -> {
                //for directional lights, render a full screen quad
                lightModelMatrix.identity()
                        .translate(cameraPos)
                        .rotate(cameraRot)
                        .translate(camera.getXrPos())
                        .rotate(camera.getXrRot())
                        .translate(0, 0, -Camera.NEAR_PLANE - 0.01f);
                light.copyTransform(lightModelMatrix);
                s.setMat4("model", lightModelMatrix);

                glCullFace(GL_BACK);
                SimpleGeometry.QUAD.render();
            }
            default -> {
                //render a cone for spot lights
                lightModelMatrix.identity();
                light.copyTransform(lightModelMatrix);
                s.setMat4("model", lightModelMatrix);

                SimpleGeometry.CONE.render();
            }
        }

        //reset state
        glCullFace(GL_BACK);
        glDisable(GL_CULL_FACE);

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

        float xrScalar = XrManager.isInXR() ? 0.35f : 1f;

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
            s.setFloat("glareSize", light.getGlareSize() * xrScalar);
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
                lensShader.setFloat("scale", xrScalar);
                SimpleGeometry.QUAD.render();
            }
        }

        //reset state
        glEnable(GL_DEPTH_TEST);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        Texture.unbindAll(2);
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

    public static void renderHoldingItems(Camera camera, MatrixStack matrices, float delta) {
        if (!(camera.getEntity() instanceof LivingEntity le) || le.getHoldingItem() == null)
            return;

        //set rendering flag
        heldItemRendering = true;
        renderSSR = false;

        WorldClient world = (WorldClient) le.getWorld();
        Runnable renderFunc = () -> le.renderHandItem(XrManager.isInXR() ? ItemRenderContext.XR : ItemRenderContext.FIRST_PERSON, matrices, delta);

        //setup gbuffer
        setupFramebuffer();
        initGBuffer(camera);

        //render item
        renderFunc.run();
        MaterialApplier.cleanup();
        VertexConsumer.finishAllBatches(camera);

        //apply ssao
        renderSSAO(camera);

        //apply lights
        int renderedLightsBak = renderedLights;
        int renderedShadowsBak = renderedShadows;
        renderedLights = renderedShadows = 0;
        renderLights(world.getLights(camera), camera, renderFunc);
        renderedLights = renderedLightsBak;
        renderedShadows = renderedShadowsBak;

        //bake item into deferred
        bakeDeferred(camera, world.getSky());

        //apply bloom
        applyBloom();

        //render as a quad
        bakeQuad();

        //reset flag
        heldItemRendering = false;
    }

    public static void renderWater(WorldClient world, Camera camera, MatrixStack matrices, float delta) {
        if (!renderWater)
            return;

        //glDepthMask(false);
        int tex = WaterRenderer.prepareWaterRenderer(camera, world.getTime() + delta);
        world.renderWater(camera, matrices, delta);
        Texture.unbindAll(tex);
        //glDepthMask(true);
    }

    public static void renderOutlines(WorldClient world, Camera camera, MatrixStack matrices, float delta) {
        if (!renderOutlines)
            return;

        List<Entity> entitiesToOutline = world.getOutlines(camera);
        if (entitiesToOutline.isEmpty())
            return;

        //init outline renderer
        outlineRendering = true;
        OutlineRenderer.prepareRenderer(outputBuffer, camera);

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

        //bake outlines to the target buffer
        OutlineRenderer.bakeOutlines(null);
        outlineRendering = false;
    }


    // -- checks -- //


    public static void resetFlags() {
        renderWater    =
        renderSSAO     =
        renderSSR      =
        renderLights   =
        renderShadows  =
        renderSky      =
        renderBloom    =
        renderDebug    =
        renderOutlines = true;
    }

    public static boolean isShadowRendering() {
        return shadowLight != null;
    }

    public static boolean isOutlineRendering() {
        return outlineRendering;
    }

    public static boolean isHeldItemRendering() {
        return heldItemRendering;
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
