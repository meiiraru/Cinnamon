package mayo.render;

import mayo.utils.IOUtils;
import mayo.utils.Resource;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class Texture {

    //a map containing all registered textures
    private static final Map<Resource, Texture> TEXTURE_MAP = new HashMap<>();
    //the missing texture
    public static final Texture MISSING = generateMissingTex();
    public static final int MAX_TEXTURES = 16;

    private final int ID, uFrames, vFrames;


    // -- texture loading -- //


    private Texture(int id, int hFrames, int vFrames) {
        this.ID = id;
        this.uFrames = hFrames;
        this.vFrames = vFrames;
    }

    public static Texture of(Resource res) {
        return of(res, 1, 1);
    }

    public static Texture of(Resource res, int hFrames, int vFrames) {
        if (res == null)
            return MISSING;

        //returns an already registered texture, if any
        Texture saved = TEXTURE_MAP.get(res);
        if (saved != null)
            return saved;

        //otherwise load a new texture and cache it
        return cacheTexture(res, new Texture(loadTexture(res), hFrames, vFrames));
    }

    private static Texture cacheTexture(Resource res, Texture tex) {
        TEXTURE_MAP.put(res, tex);
        return tex;
    }

    private static int loadTexture(Resource res) {
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
            glBindTexture(GL_TEXTURE_2D, id);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

            STBImage.stbi_image_free(buffer);
            glBindTexture(GL_TEXTURE_2D, 0);

            return id;
        } catch (Exception e) {
            System.err.println("Failed to load texture \"" + res + "\"");
            e.printStackTrace();
            return MISSING.ID;
        }
    }

    private static Texture generateMissingTex() {
        //generate texture properties
        int id = glGenTextures();
        Resource res = new Resource("generated/missing.png");

        //w * h * rgba
        long pixels = MemoryUtil.nmemAlloc(16 * 16 * 4);

        //paint texture
        //  Pink Black
        //  Black Pink
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                //x + y * w * rgba
                long i = (x + y * 16) * 4;
                boolean b = (x < 8 && y < 8) || (y >= 8 && x >= 8);
                MemoryUtil.memPutInt(pixels + i, b ? 0xFFAD72FF : 0xFF << 24); //A B G R
            }
        }

        //open gl stuff
        glBindTexture(GL_TEXTURE_2D, id);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 16, 16, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

        glBindTexture(GL_TEXTURE_2D, 0);

        //return a new texture
        return cacheTexture(res, new Texture(id, 1, 1));
    }

    public static void unbindTex(int index) {
        glActiveTexture(GL_TEXTURE0 + index);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public static void unbindAll() {
        //inverted so the last active texture is GL_TEXTURE0
        for (int i = MAX_TEXTURES - 1; i >= 0; i--)
            unbindTex(i);
    }

    public static void freeAll() {
        for (Texture texture : TEXTURE_MAP.values())
            texture.free();
        TEXTURE_MAP.clear();
    }

    public void free() {
        glDeleteTextures(this.ID);
    }


    // -- getters -- //


    public int getuFrames() {
        return uFrames;
    }

    public int getvFrames() {
        return vFrames;
    }

    public float getSpriteWidth() {
        return 1f / getuFrames();
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
