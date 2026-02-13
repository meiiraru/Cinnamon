package cinnamon.render;

import cinnamon.model.SimpleGeometry;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.framebuffer.PBRDeferredFramebuffer;
import cinnamon.render.framebuffer.ShadowCascadeFramebuffer;
import cinnamon.render.framebuffer.ShadowCubemapFramebuffer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.CubeMap;
import cinnamon.render.texture.Texture;
import cinnamon.render.texture.TextureArray;
import cinnamon.settings.Settings;
import cinnamon.vr.XrManager;
import cinnamon.world.light.CookieLight;
import cinnamon.world.light.DirectionalLight;
import cinnamon.world.light.Light;
import cinnamon.world.light.PointLight;
import cinnamon.world.light.Spotlight;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class LightRenderer {

    public static final Framebuffer lightingMultiPassBuffer = new Framebuffer(Framebuffer.HDR_COLOR_BUFFER);
    public static final Framebuffer shadowBuffer = new Framebuffer(Framebuffer.DEPTH_BUFFER);
    public static final ShadowCubemapFramebuffer cubeShadowBuffer = new ShadowCubemapFramebuffer();

    public static final ShadowCascadeFramebuffer cascadeShadowBuffer = new ShadowCascadeFramebuffer(CascadedShadow.NUM_CASCADES);
    public static final CascadedShadow cascadedShadow = new CascadedShadow();

    private static Light shadowLight = null;

    private static final Vector3f cameraPos = new Vector3f();
    private static final Quaternionf cameraRot = new Quaternionf();
    private static final Matrix4f pointLightMatrix = new Matrix4f();
    private static final Quaternionf pointLightRotation = new Quaternionf();
    private static final Matrix4f lightModelMatrix = new Matrix4f();

    private static int renderedLights, renderedShadows;

    public static void renderLights(PBRDeferredFramebuffer gBuffer, List<Light> lights, Camera camera, boolean renderShadows, Runnable renderFunction) {
        renderedLights = renderedShadows = 0;

        if (lights.isEmpty())
            return;

        //init the light buffer
        initLightBuffer(gBuffer, camera);
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
                    case POINT -> renderLightShadowToCubeMap((PointLight) light, camera, renderFunction);
                    case DIRECTIONAL -> renderDirectionalLightShadow(light, camera, renderFunction);
                    default -> renderSpotLightShadow(light, camera, renderFunction);
                }

                renderedShadows++;
            }

            //bake this light
            bakeLight(gBuffer, camera, light, shadow);
            renderedLights++;
        }

        //reset light state
        resetLightState(camera);
    }

    public static void renderLightsGlare(Framebuffer target, PBRDeferredFramebuffer gBuffer, List<Light> lights, Camera camera) {
        if (lights.isEmpty())
            return;

        boolean lensFlare = Settings.lensFlare.get();
        List<DirectionalLight> directionalLights = new ArrayList<>();
        List<Spotlight> spotLights = new ArrayList<>();

        //prepare the flare buffer
        target.use();
        glDisable(GL_DEPTH_TEST);
        glBlendFunc(GL_ONE, GL_ONE);

        //set up the flare shader
        Shader s = Shaders.LIGHT_GLARE.getShader().use();
        s.setup(camera);
        s.setTexture("gDepth", gBuffer.getDepthBuffer(), 0);

        float aspectRatio = (float) target.getWidth() / target.getHeight();
        float texelX = 1f / target.getWidth(), texelY = 1f / target.getHeight();
        s.setFloat("aspectRatio", aspectRatio);
        s.setVec2("sampleRadius", texelX, texelY);

        float xrScalar = XrManager.isInXR() ? 0.35f : 1f;

        //render the glares
        for (Light light : lights) {
            float intensity = light.getGlareIntensity();
            if (intensity <= 0f)
                continue;

            Light.Type type = light.getType();

            if (lensFlare && type == Light.Type.DIRECTIONAL)
                directionalLights.add((DirectionalLight) light);

            if (type == Light.Type.SPOT || type == Light.Type.COOKIE)
                spotLights.add((Spotlight) light);

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
            lensShader.setTexture("gDepth", gBuffer.getDepthBuffer(), 0);

            for (DirectionalLight light : directionalLights) {
                lensShader.applyColor(light.getColor());
                lensShader.setVec3("direction", light.getDirection());
                lensShader.setFloat("intensity", light.getGlareIntensity());
                lensShader.setFloat("scale", xrScalar);
                SimpleGeometry.QUAD.render();
            }
        }

        //re-enable depth test for spotlight beams - but disable writes
        glEnable(GL_DEPTH_TEST);
        glDepthMask(false);

        //render the beams
        if (!spotLights.isEmpty()) {
            Shader beamShader = Shaders.SPOTLIGHT_BEAM.getShader().use();
            beamShader.setup(camera);
            beamShader.setVec3("camPos", camera.getPosition());

            for (Spotlight spotlight : spotLights) {
                float beamStrength = spotlight.getBeamStrength();
                if (beamStrength <= 0f)
                    continue;

                float h = spotlight.getFalloffEnd();
                float r = h * Math.tan(Math.toRadians(spotlight.getInnerAngle()));

                beamShader.setVec3("lightPos", spotlight.getPos());
                beamShader.setVec3("lightDir", spotlight.getDirection());
                beamShader.setFloat("height", h);
                beamShader.setFloat("radius", r);

                beamShader.setFloat("beamIntensity", beamStrength);
                beamShader.setColor("color", spotlight.getColor());

                SimpleGeometry.TRIANGLE.render();
            }
        }

        //reset state
        glDepthMask(true);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        Texture.unbindAll(2);
    }


    // buffer //


    private static void initLightBuffer(Framebuffer target, Camera camera) {
        lightingMultiPassBuffer.resizeTo(target);
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

    private static void resetLightState(Camera camera) {
        //reset gl
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_CULL_FACE);

        //restore camera
        camera.setPos(cameraPos.x, cameraPos.y, cameraPos.z);
        camera.setRot(cameraRot);
        camera.updateFrustum();
    }


    // shadows //


    private static void initShadowBuffer() {
        //prepare the shadow buffer
        int w = 256 << Settings.shadowQuality.get();
        cascadeShadowBuffer.resize(w, w);
        cubeShadowBuffer.resize(w, w);
        shadowBuffer.resize(w, w);
        shadowBuffer.adjustViewPort();
    }

    private static void renderDirectionalLightShadow(Light light, Camera camera, Runnable renderFunction) {
        //set rendering state
        shadowLight = light;
        WorldRenderer.activeMask = light.getShadowMask();

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
        WorldRenderer.activeMask = WorldRenderer.passMask;
    }

    private static void renderSpotLightShadow(Light light, Camera camera, Runnable renderFunction) {
        //set rendering state
        shadowLight = light;
        WorldRenderer.activeMask = light.getShadowMask();

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
        WorldRenderer.activeMask = WorldRenderer.passMask;
    }

    private static void renderLightShadowToCubeMap(PointLight light, Camera camera, Runnable renderFunction) {
        //set rendering state
        shadowLight = light;
        WorldRenderer.activeMask = light.getShadowMask();

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
        WorldRenderer.activeMask = WorldRenderer.passMask;
    }

    private static void bakeLight(PBRDeferredFramebuffer gBuffer, Camera camera, Light light, boolean hasShadow) {
        //since shadows have a custom viewport, we need to adjust its view too
        lightingMultiPassBuffer.use();
        lightingMultiPassBuffer.adjustViewPort();

        //bind the shadow map and gbuffer textures to the light shader
        Shader s = Shaders.LIGHT_PASS.getShader().use();
        s.setTexture("gAlbedo", gBuffer.getAlbedo(),      0);
        s.setTexture("gNormal", gBuffer.getNormal(),      1);
        s.setTexture("gORM",    gBuffer.getORM(),         2);
        s.setTexture("gDepth",  gBuffer.getDepthBuffer(), 3);

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
            case POINT -> {
                //for point lights, render a sphere
                lightModelMatrix.identity();
                light.copyTransform(lightModelMatrix);
                s.setMat4("model", lightModelMatrix);

                SimpleGeometry.SPHERE.render();
            }
            case DIRECTIONAL -> {
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
                //render a cone for spot and cookie lights
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


    // getters //


    public static int getTexture() {
        return lightingMultiPassBuffer.getColorBuffer();
    }

    public static int getRenderedLightsCount() {
        return renderedLights;
    }

    public static int getRenderedShadowsCount() {
        return renderedShadows;
    }

    public static Light getShadowLight() {
        return shadowLight;
    }
}
