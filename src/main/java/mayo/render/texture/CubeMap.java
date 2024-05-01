package mayo.render.texture;

import mayo.utils.IOUtils;
import mayo.utils.Resource;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL13.*;

public class CubeMap extends Texture {

    private static final Map<Resource, CubeMap> CUBEMAP_MAP = new HashMap<>();
    public static final CubeMap MISSING_CUBEMAP = generateMissingMap();

    protected CubeMap(int id) {
        super(id);
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

        return cacheCubemap(res, new CubeMap(loadCubemap(resources)));
    }

    private static CubeMap cacheCubemap(Resource res, CubeMap texture) {
        CUBEMAP_MAP.put(res, texture);
        return texture;
    }

    protected static int loadCubemap(Resource[] resources) {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_CUBE_MAP, id);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            for (int i = 0; i < 6; i++) {
                Resource res = resources[i];
                int target = Face.values()[i].GLTarget;

                ByteBuffer imageBuffer = IOUtils.getResourceBuffer(res);
                ByteBuffer buffer = STBImage.stbi_load_from_memory(imageBuffer, w, h, channels, 4);
                if (buffer == null)
                    throw new Exception("Failed to load image \"" + res + "\", " + STBImage.stbi_failure_reason());

                glTexImage2D(target, 0, GL_RGBA, w.get(), h.get(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

                STBImage.stbi_image_free(buffer);
                w.clear();
                h.clear();
                channels.clear();
            }
        } catch (Exception e) {
            System.err.println("Failed to load cubemap texture");
            e.printStackTrace();

            glDeleteTextures(id);
            return MISSING_CUBEMAP.getID();
        }

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
        return id;
    }

    private static CubeMap generateMissingMap() {
        int id = glGenTextures();
        Resource res = new Resource("generated/missing_cubemap");

        glBindTexture(GL_TEXTURE_CUBE_MAP, id);

        for (Face face : Face.values())
            glTexImage2D(face.GLTarget, 0, GL_RGBA, 16, 16, 0, GL_RGBA, GL_UNSIGNED_BYTE, MISSING_DATA);

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        glBindTexture(GL_TEXTURE_CUBE_MAP, 0);

        return cacheCubemap(res, new CubeMap(id));
    }

    public static void freeAll() {
        for (CubeMap map : CUBEMAP_MAP.values())
            map.free();
        CUBEMAP_MAP.clear();
    }

    public static void unbindTex(int index) {
        glActiveTexture(GL_TEXTURE0 + index);
        glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
    }

    @Override
    public void bind() {
        glBindTexture(GL_TEXTURE_CUBE_MAP, getID());
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
