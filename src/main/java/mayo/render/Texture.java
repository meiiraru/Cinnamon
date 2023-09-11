package mayo.render;

import mayo.utils.IOUtils;
import mayo.utils.Resource;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

public class Texture {

    //a map containing the registered ids of all textures
    private static final Map<String, Integer> ID_MAP = new HashMap<>();

    private final int ID, hFrames, vFrames;

    public Texture(Resource res) {
        this(res, 1, 1);
    }

    public Texture(Resource res, int hFrames, int vFrames) {
        this.ID = loadTexture(res);
        this.hFrames = hFrames;
        this.vFrames = vFrames;
    }

    private static int loadTexture(Resource res) {
        //returns id of already registered texture, if any
        Integer saved = ID_MAP.get(res.toString());
        if (saved != null)
            return saved;

        //read texture
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer imageBuffer = IOUtils.getResourceBuffer(res);
            ByteBuffer buffer = STBImage.stbi_load_from_memory(imageBuffer, w, h, channels, 4);
            if (buffer == null)
                throw new Exception("Failed to load image \"" + res + "\", " + STBImage.stbi_failure_reason());

            int width = w.get();
            int height = h.get();

            int id = glGenTextures();
            ID_MAP.put(res.toString(), id);
            glBindTexture(GL_TEXTURE_2D, id);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
            glGenerateMipmap(GL_TEXTURE_2D);

            STBImage.stbi_image_free(buffer);
            return id;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load texture \"" + res + "\"", e);
        }
    }

    public int gethFrames() {
        return hFrames;
    }

    public int getvFrames() {
        return vFrames;
    }

    public float getSpriteWidth() {
        return 1f / gethFrames();
    }

    public float getSpriteHeight() {
        return 1f / getvFrames();
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, this.ID);
    }

    public int getID() {
        return this.ID;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Texture t && t.ID == this.ID;
    }
}
