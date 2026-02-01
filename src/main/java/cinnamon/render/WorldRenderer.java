package cinnamon.render;

import cinnamon.Client;
import cinnamon.model.SimpleGeometry;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.framebuffer.PBRDeferredFramebuffer;
import cinnamon.render.shader.PostProcess;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;
import cinnamon.settings.Settings;
import cinnamon.utils.Rotation;
import cinnamon.vr.XrManager;
import cinnamon.vr.XrRenderer;
import cinnamon.world.Decal;
import cinnamon.world.Mask;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.items.ItemRenderContext;
import cinnamon.world.sky.Sky;
import cinnamon.world.world.WorldClient;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class WorldRenderer {

    //properties
    public static final Camera camera = new Camera();
    private static Framebuffer targetBuffer;
    public static final PBRDeferredFramebuffer PBRFrameBuffer = new PBRDeferredFramebuffer();
    public static final Framebuffer outputBuffer = new Framebuffer(Framebuffer.HDR_COLOR_BUFFER | Framebuffer.DEPTH_BUFFER | Framebuffer.STENCIL_BUFFER);
    public static final Framebuffer lastFrameFramebuffer = new Framebuffer(Framebuffer.HDR_COLOR_BUFFER | Framebuffer.DEPTH_BUFFER);

    //rendering info
    private static boolean outlineRendering = false;
    private static boolean heldItemRendering = false;
    private static int
            renderedEntities,
            renderedTerrain,
            renderedParticles,
            renderedLights,
            renderedShadows,
            renderedDecals;

    //render mask
    public static final Mask passMask = new Mask(-1);
    public static Mask activeMask = passMask;

    //view bobbing
    private static float bobPhase, bobX, bobY, bobPitch;

    //render properties
    public static int shadowRenderDistance = 192;
    public static int renderDistance = 192;
    public static int entityRenderDistance = 144;

    public static boolean
            renderWater    = true,
            renderSSAO     = true,
            renderSSR      = true,
            renderLights   = true,
            renderShadows  = true,
            renderSky      = true,
            renderBloom    = true,
            renderDebug    = true,
            renderOutlines = true,
            renderDecals   = true;

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
        renderedDecals    = renderDecals(world, camera);
        renderedEntities  = world.renderEntities(camera, matrices, delta);
        renderedParticles = world.renderParticles(camera, matrices, delta);
        renderFunc[4].run(); //item extra

        //world vertex consumer
        finishMaterials(camera);

        //water
        renderWater(world, camera, matrices, delta);

        //render ssao
        renderSSAO(camera);

        //render ssr
        renderSSR(camera);

        //render the world lights
        renderLights(world, camera, renderFunc);

        //bake world
        bakeDeferred(camera, world.getSky());

        //render the sky
        if (renderSky)
            world.getSky().render(camera, matrices);

        //apply bloom
        applyBloom();

        //render glares and lens flare
        renderLightsGlare(world, camera);

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
            renderedDecals    = renderDecals(world, camera);
            renderedEntities  = world.renderEntities(camera, matrices, delta);
            renderedParticles = world.renderParticles(camera, matrices, delta);
            renderFunc[4].run(); //item extra

            //vertex consumer
            finishMaterials(camera);

            //water
            renderWater(world, camera, matrices, delta);

            //effects
            renderSSAO(camera);
            renderSSR(camera);

            //lights
            renderLights(world, camera, renderFunc);
        }, () -> {
            //bake world
            bakeDeferred(camera, world.getSky());

            //post bake renderer
            if (renderSky) world.getSky().render(camera, matrices);
            applyBloom();
            renderLightsGlare(world, camera);
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

    public static void finishMaterials(Camera camera) {
        MaterialApplier.cleanup();
        VertexConsumer.finishAllBatches(camera);
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
        int i = 0;
        s.setTexture("gAlbedo",   PBRFrameBuffer.getAlbedo(),      i++);
        s.setTexture("gNormal",   PBRFrameBuffer.getNormal(),      i++);
        s.setTexture("gORM",      PBRFrameBuffer.getORM(),         i++);
        s.setTexture("gEmissive", PBRFrameBuffer.getEmissive(),    i++);
        s.setTexture("gDepth",    PBRFrameBuffer.getDepthBuffer(), i++);
        s.setTexture("lightTex",  LightRenderer.getTexture(),      i++);
        s.setTexture("ssaoTex",   SSAORenderer.getTexture(),       i++);
        s.setTexture("ssrTex",    SSRRenderer.getTexture(),        i++);

        s.setFloat("lightFactor", renderLights && renderedLights > 0 ? 1f : 0f);
        s.setFloat("ssaoFactor", renderSSAO && Settings.ssaoLevel.get() >= 0 ? 1f : 0f);
        s.setFloat("ssrFactor", renderSSR && Settings.ssrLevel.get() >= 0 ? 1f : 0f);

        //apply sky
        sky.applyUniforms(s, camera);
        sky.bind(s, i);

        //render to the output framebuffer the final scene
        //and blit the remaining depth and stencil to the main
        renderQuad();
        PBRFrameBuffer.blit(outputBuffer, false, true, true);

        //cleanup textures
        Texture.unbindAll(i);
        sky.unbind(i);
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

    public static void renderLights(WorldClient world, Camera camera, Runnable[] renderFunc) {
        renderedLights = renderedShadows = 0;
        if (renderLights) {
            LightRenderer.renderLights(PBRFrameBuffer, world.getLights(camera), camera, renderShadows, () -> {
                for (Runnable r : renderFunc)
                    r.run();
            });
            renderedLights = LightRenderer.getRenderedLightsCount();
            renderedShadows = LightRenderer.getRenderedShadowsCount();
        }
    }

    public static void renderLightsGlare(WorldClient world, Camera camera) {
        if (renderLights)
            LightRenderer.renderLightsGlare(outputBuffer, PBRFrameBuffer, world.getLights(camera), camera);
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

    public static int renderDecals(WorldClient world, Camera camera) {
        if (!renderDecals)
            return 0;

        List<Decal> decals = world.getDecals(camera);
        if (decals.isEmpty())
            return 0;

        //finish gbuffer materials
        finishMaterials(camera);

        //render decals
        DecalRenderer.renderDecals(PBRFrameBuffer, camera, decals);

        return decals.size();
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
        finishMaterials(camera);

        //apply ssao
        renderSSAO(camera);

        //apply lights
        int renderedLightsBak = renderedLights;
        int renderedShadowsBak = renderedShadows;
        renderedLights = renderedShadows = 0;
        LightRenderer.renderLights(PBRFrameBuffer, world.getLights(camera), camera, renderShadows, renderFunc);
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

    public static void viewBobbing(Camera camera, float deltaTime) {
        float strength = Settings.viewBobbingStrength.get();
        if (strength <= 0f)
            return;

        float speed = 0f;
        if (camera.getEntity() instanceof LivingEntity le && le.isOnGround()) {
            Vector3f impulse = le.getImpulse();
            speed = Math.sqrt(impulse.x * impulse.x + impulse.z * impulse.z);
        }

        if (speed > 0.1f) {
            float freq = 50f;
            bobPhase += (freq * speed) * deltaTime;
            if (bobPhase > Math.PI_TIMES_2_f)
                bobPhase -= Math.PI_TIMES_2_f;

            float amplitude = 0.02f * strength;
            float pitchAmplitude = 0.5f * strength;

            float targetX = Math.sin(bobPhase) * amplitude;
            float targetY = Math.abs(Math.cos(bobPhase)) * amplitude;
            float targetPitch = Math.abs(Math.cos(bobPhase)) * pitchAmplitude;

            float smoothing = 0.2f;

            bobX += (targetX - bobX) * smoothing;
            bobY += (targetY - bobY) * smoothing;
            bobPitch += (targetPitch - bobPitch) * smoothing;
        } else {
            float decay = 15f;
            bobX += -bobX * decay * deltaTime;
            bobY += -bobY * decay * deltaTime;
            bobPitch += -bobPitch * decay * deltaTime;
        }

        Vector3f camPos = camera.getPos();
        Quaternionf camRot = camera.getRot()
                .rotateX(Math.toRadians(bobPitch));

        Vector3f bob = new Vector3f(bobX, bobY, 0f).rotate(camRot);
        camera.setPos(camPos.x + bob.x, camPos.y + bob.y, camPos.z + bob.z);

        camera.setRot(camRot);
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
        renderOutlines =
        renderDecals   = true;
    }

    public static boolean isShadowRendering() {
        return LightRenderer.getShadowLight() != null;
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

    public static int getDecalsCount() {
        return renderedDecals;
    }
}
