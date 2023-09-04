package mayo.render;

import mayo.utils.IOUtils;
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

    public Texture(String namespace, String name) {
        this(namespace, name, 1, 1);
    }

    public Texture(String namespace, String name, int hFrames, int vFrames) {
        this.ID = loadTexture(namespace, name);
        this.hFrames = hFrames;
        this.vFrames = vFrames;
    }

    private static int loadTexture(String namespace, String texture) {
        //returns id of already registered texture, if any
        Integer saved = ID_MAP.get(texture);
        if (saved != null)
            return saved;

        //read texture
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer imageBuffer = IOUtils.getResourceBuffer(namespace, "textures/" + texture + ".png");
            ByteBuffer buffer = STBImage.stbi_load_from_memory(imageBuffer, w, h, channels, 4);
            if (buffer == null)
                throw new Exception("Failed to load image \"" + texture + "\", " + STBImage.stbi_failure_reason());

            int width = w.get();
            int height = h.get();

            int id = glGenTextures();
            ID_MAP.put(texture, id);
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
            throw new RuntimeException("Failed to load texture \"" + texture + "\"", e);
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
