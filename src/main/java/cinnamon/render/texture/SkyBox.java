package cinnamon.render.texture;

import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import cinnamon.utils.TextureIO;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static cinnamon.events.Events.LOGGER;
import static cinnamon.render.texture.Texture.TextureParams.SMOOTH_SAMPLING;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.GL_RGB16F;
import static org.lwjgl.stb.STBImage.stbi_image_free;

public class SkyBox {

    private static final Map<Resource, SkyBox> SKYBOX_MAP = new HashMap<>();
    public static final SkyBox MISSING = new SkyBox(CubeMap.MISSING_CUBEMAP, CubeMap.MISSING_CUBEMAP, CubeMap.MISSING_CUBEMAP);
    protected static final Texture LUT_MAP = IBLMap.brdfLUT(512);

    protected final CubeMap texture, irradiance, prefilter;

    protected SkyBox(CubeMap texture, CubeMap irradiance, CubeMap prefilter) {
        this.texture = texture;
        this.irradiance = irradiance;
        this.prefilter = prefilter;
    }

    public static SkyBox of(Resource resource) {
        if (resource == null)
            return MISSING;

        SkyBox saved = SKYBOX_MAP.get(resource);
        if (saved != null)
            return saved;

        return cacheSkybox(resource, loadSkybox(resource));
    }

    private static SkyBox cacheSkybox(Resource resource, SkyBox skybox) {
        SKYBOX_MAP.put(resource, skybox);
        return skybox;
    }

    private static SkyBox loadSkybox(Resource resource) {
        LOGGER.debug("Loading skybox \"%s\"", resource);

        CubeMap texture;
        boolean hdr = testHdr(resource);
        if (hdr) {
            boolean isTrulyHdr = resource.getExtension().equalsIgnoreCase("hdr");
            Texture hdrTex = HDRTexture.of(resource, isTrulyHdr, SMOOTH_SAMPLING);
            texture = IBLMap.hdrToCubemap(hdrTex, isTrulyHdr);
            hdrTex.free();
        } else {
            texture = CubeMap.of(resource);
        }
        CubeMap irradiance = IBLMap.generateIrradianceMap(texture);
        CubeMap prefilter = IBLMap.generatePrefilterMap(texture);
        return new SkyBox(texture, irradiance, prefilter);
    }

    private static boolean testHdr(Resource resource) {
        for (int i = 0; i < 6; i++)
            if (IOUtils.hasResource(resource.resolve(CubeMap.Face.values()[i].path)))
                return false;
        return true;
    }

    public static void freeAll() {
        for (SkyBox skyBox : SKYBOX_MAP.values())
            skyBox.free();
        SKYBOX_MAP.clear();
    }

    public void free() {
        texture.free();
        irradiance.free();
        prefilter.free();
    }

    public void bind(int index) {
        texture.bind(index);
    }

    public void bindIBL(int index) {
        irradiance.bind(index);
        prefilter.bind(index + 1);
        LUT_MAP.bind(index + 2);
    }

    private static class HDRTexture extends Texture {

        protected HDRTexture(int id, int width, int height) {
            super(id, width, height);
        }

        public static HDRTexture of(Resource resource, boolean hdr, TextureParams... params) {
            return loadTexture(resource, hdr, TextureParams.bake(params));
        }

        private static HDRTexture loadTexture(Resource res, boolean hdr, int params) {
            //read texture
            try (MemoryStack stack = MemoryStack.stackPush()) {
                if (!hdr) {
                    try (TextureIO.ImageData data = TextureIO.load(res, true, 4)) {
                        return new HDRTexture(registerTexture(data.width, data.height, data.buffer, params, false), data.width, data.height);
                    }
                }

                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);

                ByteBuffer imageBuffer = IOUtils.getResourceBuffer(res);

                STBImage.stbi_set_flip_vertically_on_load(true);
                FloatBuffer buffer = STBImage.stbi_loadf_from_memory(imageBuffer, w, h, channels, 0);
                STBImage.stbi_set_flip_vertically_on_load(false);
                if (buffer == null)
                    throw new Exception("Failed to load image \"" + res + "\", " + STBImage.stbi_failure_reason());

                int texture = registerTexture(w.get(0), h.get(0), buffer, params, true);
                stbi_image_free(buffer);
                return new HDRTexture(texture, w.get(0), h.get(0));
            } catch (Exception e) {
                LOGGER.error("Failed to load texture \"%s\"", res, e);
                return new HDRTexture(MISSING.getID(), MISSING.getWidth(), MISSING.getHeight());
            }
        }

        protected static int registerTexture(int width, int height, Buffer buffer, int params, boolean isHdr) {
            int id = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, id);

            if (isHdr) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB16F, width, height, 0, GL_RGB, GL_FLOAT, (FloatBuffer) buffer);
            } else {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) buffer);
            }

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            applyParams(params);

            glBindTexture(GL_TEXTURE_2D, 0);
            return id;
        }
    }
}
