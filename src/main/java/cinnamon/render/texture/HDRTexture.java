package cinnamon.render.texture;

import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static cinnamon.Client.LOGGER;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.GL_RGB16F;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.stbi_image_free;

public class HDRTexture extends Texture {

    protected HDRTexture(int id) {
        super(id);
    }

    public static HDRTexture of(Resource resource) {
        return of(resource, false, false);
    }

    public static HDRTexture of(Resource resource, boolean smooth, boolean mipmap) {
        return new HDRTexture(loadTexture(resource, smooth, mipmap));
    }

    private static int loadTexture(Resource res, boolean smooth, boolean mipmap) {
        //read texture
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer imageBuffer = IOUtils.getResourceBuffer(res);

            STBImage.stbi_set_flip_vertically_on_load(true);
            FloatBuffer buffer = STBImage.stbi_loadf_from_memory(imageBuffer, w, h, channels, 0);
            STBImage.stbi_set_flip_vertically_on_load(false);
            if (buffer == null)
                throw new Exception("Failed to load HDR image \"" + res + "\", " + STBImage.stbi_failure_reason());

            return registerTexture(w.get(), h.get(), buffer, smooth, mipmap);
        } catch (Exception e) {
            LOGGER.error("Failed to load HDR texture \"{}\"", res, e);
            return MISSING.getID();
        }
    }

    protected static int registerTexture(int width, int height, FloatBuffer buffer, boolean smooth, boolean mipmap) {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB16F, width, height, 0, GL_RGB, GL_FLOAT, buffer);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, smooth ? GL_LINEAR : GL_NEAREST);
        if (mipmap) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, smooth ? GL_LINEAR_MIPMAP_LINEAR : GL_NEAREST_MIPMAP_NEAREST);
            glGenerateMipmap(GL_TEXTURE_2D);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, smooth ? GL_LINEAR : GL_NEAREST);
        }

        stbi_image_free(buffer);
        glBindTexture(GL_TEXTURE_2D, 0);

        return id;
    }
}
