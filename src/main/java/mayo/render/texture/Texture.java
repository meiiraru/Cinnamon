package mayo.render.texture;

import mayo.utils.Resource;
import mayo.utils.TextureIO;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static mayo.Client.LOGGER;
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


    protected Texture(int id) {
        this(id, 1, 1);
    }

    protected Texture(int id, int hFrames, int vFrames) {
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
        try (TextureIO.ImageData image = TextureIO.load(res)) {
            return registerTexture(image.width, image.height, image.buffer);
        } catch (Exception e) {
            LOGGER.error("Failed to load texture \"" + res + "\"", e);
            return MISSING.ID;
        }
    }

    protected static int registerTexture(int width, int height, ByteBuffer buffer) {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        glBindTexture(GL_TEXTURE_2D, 0);
        return id;
    }

    private static Texture generateMissingTex() {
        //generate texture properties
        Resource res = new Resource("generated/missing.png");

        //w * h * rgba
        ByteBuffer pixels = MemoryUtil.memAlloc(16 * 16 * 4);

        //paint texture
        //  Pink Black
        //  Black Pink
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                //x + y * w * rgba
                int i = (x + y * 16) * 4;
                boolean b = (x < 8 && y < 8) || (y >= 8 && x >= 8);
                int col = b ? 0xFF << 24 : 0xFFAD72FF; //ABGR
                pixels.putInt(i, col);
            }
        }

        pixels.flip();

        //return a new texture
        int id = registerTexture(16, 16, pixels);
        return cacheTexture(res, new Texture(id));
    }

    //returns a 1x1 texture with a solid color
    public static Texture generateSolid(int ARGB) {
        ByteBuffer buffer = MemoryUtil.memAlloc(4);
        buffer.put((byte) (ARGB >> 16));
        buffer.put((byte) (ARGB >> 8));
        buffer.put((byte) ARGB);
        buffer.put((byte) (ARGB >> 24));
        buffer.flip();

        int id = registerTexture(1, 1, buffer);
        return new Texture(id);
    }

    public static void unbindTex(int index) {
        glActiveTexture(GL_TEXTURE0 + index);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public static void unbindAll() {
        unbindAll(MAX_TEXTURES);
    }

    public static void unbindAll(int max) {
        //inverted so the last active texture is GL_TEXTURE0
        for (int i = max - 1; i >= 0; i--)
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
