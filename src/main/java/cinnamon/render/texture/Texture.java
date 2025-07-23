package cinnamon.render.texture;

import cinnamon.utils.Resource;
import cinnamon.utils.TextureIO;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static cinnamon.events.Events.LOGGER;
import static cinnamon.render.texture.Texture.TextureParams.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

public class Texture {

    //a map containing all registered textures
    private static final Map<Resource, Texture> TEXTURE_MAP = new HashMap<>();

    //the missing texture
    public static final Texture MISSING = /*generateSolid(0xFFEDEDED);*/ generateMissingTex();

    public static final int MAX_TEXTURES = 16;

    private final int ID;
    private final int width, height;


    // -- texture loading -- //


    protected Texture(int id, int width, int height) {
        this.ID = id;
        this.width = width;
        this.height = height;
    }

    public static Texture of(Resource res, TextureParams... params) {
        if (res == null)
            return MISSING;

        //returns an already registered texture, if any
        Texture saved = TEXTURE_MAP.get(res);
        if (saved != null)
            return saved;

        //otherwise load a new texture and cache it
        return cacheTexture(res, loadTexture(res, TextureParams.bake(params)));
    }

    private static Texture cacheTexture(Resource res, Texture tex) {
        TEXTURE_MAP.put(res, tex);
        return tex;
    }

    private static Texture loadTexture(Resource res, int params) {
        try (TextureIO.ImageData image = TextureIO.load(res)) {
            return new Texture(registerTexture(image.width, image.height, image.buffer, params), image.width, image.height);
        } catch (Exception e) {
            LOGGER.error("Failed to load texture \"%s\"", res, e);
            return MISSING;
        }
    }

    protected static int registerTexture(int width, int height, ByteBuffer buffer, int params) {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, SMOOTH_SAMPLING.has(params) ? GL_LINEAR : GL_NEAREST);

        if (MIPMAP.has(params)) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, MIPMAP_SMOOTH.has(params) ? GL_LINEAR_MIPMAP_LINEAR : GL_NEAREST_MIPMAP_NEAREST);
            glGenerateMipmap(GL_TEXTURE_2D);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, SMOOTH_SAMPLING.has(params) ? GL_LINEAR : GL_NEAREST);
        }

        glBindTexture(GL_TEXTURE_2D, 0);
        return id;
    }

    private static Texture generateMissingTex() {
        int w = 2, h = 2;

        //w * h * rgba
        ByteBuffer pixels = MemoryUtil.memAlloc(w * h * 4);

        //paint texture
        //  Pink Black
        //  Black Pink
        int hw = w / 2, hh = h / 2;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                //x + y * w * rgba
                int i = (x + y * w) * 4;
                boolean b = (x < hw && y < hh) || (y >= hh && x >= hw);
                int col = b ? 0xFF << 24 : 0xFFAD72FF; //ABGR
                pixels.putInt(i, col);
            }
        }

        pixels.flip();

        //return a new texture
        return new Texture(registerTexture(w, h, pixels, MIPMAP_SMOOTH.id), w, h);
    }

    //returns a 1x1 texture with a solid color
    public static Texture generateSolid(int ARGB) {
        ByteBuffer buffer = MemoryUtil.memAlloc(4);
        buffer.put((byte) (ARGB >> 16));
        buffer.put((byte) (ARGB >> 8));
        buffer.put((byte) ARGB);
        buffer.put((byte) (ARGB >> 24));
        buffer.flip();

        int id = registerTexture(1, 1, buffer, 0);
        return new Texture(id, 1, 1);
    }

    public static int bind(int id, int index) {
        glActiveTexture(GL_TEXTURE0 + index);
        glBindTexture(GL_TEXTURE_2D, id);
        return index;
    }

    public static void unbindTex(int index) {
        bind(0, index);
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


    public int bind(int index) {
        return bind(this.ID, index);
    }

    public int getID() {
        return this.ID;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Texture t && t.ID == this.ID;
    }

    public enum TextureParams {
        SMOOTH_SAMPLING(0x1, "smooth"),
        MIPMAP(0x2, "mip", "mipmap"),
        MIPMAP_SMOOTH(0x6, "smooth_mip"); //0x2 (mipmap) + 0x4 (self)

        public final int id;
        public final String[] aliases;

        TextureParams(int id, String... aliases) {
            this.id = id;
            this.aliases = aliases;
        }

        public boolean has(int flags) {
            return (flags & this.id) != 0;
        }

        public static int bake(TextureParams... params) {
            int flags = 0;
            for (TextureParams param : params)
                flags |= param.id;
            return flags;
        }

        public static TextureParams getByAlias(String alias) {
            for (TextureParams param : values())
                for (String a : param.aliases)
                    if (a.equals(alias))
                        return param;

            return null;
        }
    }
}
