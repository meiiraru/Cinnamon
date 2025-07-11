package cinnamon.render.texture;

import cinnamon.utils.Resource;
import cinnamon.utils.TextureIO;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static cinnamon.events.Events.LOGGER;
import static org.lwjgl.opengl.GL13.*;

public class CubeMap extends Texture {

    private static final Map<Resource, CubeMap> CUBEMAP_MAP = new HashMap<>();
    public static final CubeMap MISSING_CUBEMAP = generateMissingMap();

    protected CubeMap(int id, int width, int height) {
        super(id, width, height);
    }

    public static CubeMap of(Resource res) {
        if (res == null)
            return MISSING_CUBEMAP;

        CubeMap saved = CUBEMAP_MAP.get(res);
        if (saved != null)
            return saved;

        Resource[] resources = new Resource[6];
        for (int i = 0; i < 6; i++)
            resources[i] = res.resolve(Face.values()[i].path);

        return cacheCubemap(res, loadCubemap(resources));
    }

    private static CubeMap cacheCubemap(Resource res, CubeMap texture) {
        CUBEMAP_MAP.put(res, texture);
        return texture;
    }

    protected static CubeMap loadCubemap(Resource[] resources) {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_CUBE_MAP, id);

        int width = 0, height = 0;

        for (int i = 0; i < 6; i++) {
            Resource res = resources[i];
            int target = Face.values()[i].GLTarget;

            try (TextureIO.ImageData image = TextureIO.load(res)) {
                glTexImage2D(target, 0, GL_RGBA, image.width, image.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image.buffer);
                width = Math.max(image.width, width);
                height = Math.max(image.height, height);
            } catch (Exception e) {
                LOGGER.error("Failed to load cubemap texture", e);
                glDeleteTextures(id);
                return MISSING_CUBEMAP;
            }
        }

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
        return new CubeMap(id, width, height);
    }

    private static CubeMap generateMissingMap() {
        int id = glGenTextures();
        Resource res = new Resource("generated/missing_cubemap");

        //grab missing tex data
        glBindTexture(GL_TEXTURE_2D, Texture.MISSING.getID());
        int width = Texture.MISSING.getWidth();
        int height = Texture.MISSING.getHeight();

        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);
        glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        buffer.flip();

        glBindTexture(GL_TEXTURE_2D, 0);

        //generate cubemap
        glBindTexture(GL_TEXTURE_CUBE_MAP, id);

        for (Face face : Face.values())
            glTexImage2D(face.GLTarget, 0, GL_RGBA, 16, 16, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        glBindTexture(GL_TEXTURE_CUBE_MAP, 0);

        return cacheCubemap(res, new CubeMap(id, width, height));
    }

    public static void freeAll() {
        for (CubeMap map : CUBEMAP_MAP.values())
            map.free();
        CUBEMAP_MAP.clear();
    }

    public static int bind(int id, int index) {
        glActiveTexture(GL_TEXTURE0 + index);
        glBindTexture(GL_TEXTURE_CUBE_MAP, id);
        return index;
    }

    public static void unbindTex(int index) {
        bind(0, index);
    }

    @Override
    public int bind(int index) {
        return bind(getID(), index);
    }

    public enum Face {
        RIGHT(GL_TEXTURE_CUBE_MAP_POSITIVE_X, new Vector3f(1f, 0f, 0f), new Vector3f(0f, -1f, 0f)),
        LEFT(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, new Vector3f(-1f, 0f, 0f), new Vector3f(0f, -1f, 0f)),
        TOP(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, new Vector3f(0f, 1f, 0f), new Vector3f(0f, 0f, 1f)),
        BOTTOM(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, new Vector3f(0f, -1f, 0f), new Vector3f(0f, 0f, -1f)),
        FRONT(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, new Vector3f(0f, 0f, 1f), new Vector3f(0f, -1f, 0f)),
        BACK(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, new Vector3f(0f, 0f, -1f), new Vector3f(0f, -1f, 0f));

        public final int GLTarget;
        public final String path;
        public final Matrix4f viewMatrix;

        Face(int textureTarget, Vector3f center, Vector3f up) {
            this.GLTarget = textureTarget;
            this.path = this.name().toLowerCase() + ".png";
            this.viewMatrix = new Matrix4f().lookAt(0f, 0f, 0f, center.x, center.y, center.z, up.x, up.y, up.z);
        }
    }
}
