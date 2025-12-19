package cinnamon.render.shader;

import cinnamon.Cinnamon;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.texture.CubeMap;
import cinnamon.render.texture.Texture;
import cinnamon.render.texture.TextureArray;
import cinnamon.settings.ArgsOptions;
import cinnamon.utils.ColorUtils;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static cinnamon.events.Events.LOGGER;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;

public class Shader {

    private static final Map<String, String> INCLUDE_CACHE = new HashMap<>();

    public static Shader activeShader;

    public final int ID;

    public Shader(Resource res) {
        this.ID = loadShader(res);
    }

    private static int loadShader(Resource res) {
        LOGGER.debug("Loading shader \"%s\"", res);
        String src = IOUtils.readString(res);
        String[] split = src.split("#type ");

        //create shaders
        int vertexShader = readShader(res, split, Type.VERTEX);
        int geometryShader = readShader(res, split, Type.GEOMETRY); //optional
        int fragmentShader = readShader(res, split, Type.FRAGMENT);

        if (vertexShader == -1 || fragmentShader == -1)
            throw new RuntimeException("Error loading shader \"" + res + "\" - missing vertex or fragment shader");

        //create program
        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        if (geometryShader != -1) glAttachShader(program, geometryShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);
        checkProgramErrors(res, program);

        //delete shaders
        glDetachShader(program, vertexShader);
        glDetachShader(program, fragmentShader);

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        if (geometryShader != -1) {
            glDetachShader(program, geometryShader);
            glDeleteShader(geometryShader);
        }

        return program;
    }

    private static int readShader(Resource res, String[] split, Type type) {
        //find the correct string index for the shader type
        String typeName = type.name().toLowerCase();

        String src = null;
        for (String s : split) {
            if (s.startsWith(typeName)) {
                src = s.substring(typeName.length());
                break;
            }
        }

        if (src == null)
            return -1;

        //process includes
        String[] include = src.split("#include ");
        if (include.length > 1)
            src = processInclude(include);

        //apply opengl es compatibility extensions for geometry shader (if supported)
        if (type == Type.GEOMETRY)
            src = Type.fixGLESGeometryShader(src);

        //create shader
        int shader = glCreateShader(type.glBind);
        glShaderSource(shader, src);
        glCompileShader(shader);
        checkCompileErrors(res, shader);

        return shader;
    }

    private static String processInclude(String[] vInclude) {
        StringBuilder finalShader = new StringBuilder();

        for (String s : vInclude) {
            //separate include from the shader
            int index = s.indexOf("\n");
            if (index == -1)
                index = s.length();

            String toInclude = s.substring(0, index).trim();

            //check for cache and append to the final shader
            if (!toInclude.isBlank()) {
                String cache = INCLUDE_CACHE.computeIfAbsent(toInclude, string -> IOUtils.readString(new Resource(string)));
                finalShader.append(cache);
            }

            //append the shader itself
            finalShader.append(s.substring(index));
        }

        return finalShader.toString();
    }

    private static void checkCompileErrors(Resource res, int id) {
        int status = glGetShaderi(id, GL_COMPILE_STATUS);
        if (status == GL_FALSE) {
            int i = glGetShaderi(id, GL_INFO_LOG_LENGTH);
            LOGGER.fatal("Error compiling shader \"%s\"", res);
            throw new RuntimeException(glGetShaderInfoLog(id, i));
        }
    }

    private static void checkProgramErrors(Resource res, int id) {
        int status = glGetProgrami(id, GL_LINK_STATUS);
        if (status == GL_FALSE) {
            int i = glGetProgrami(id, GL_INFO_LOG_LENGTH);
            LOGGER.fatal("Error linking shader program \"%s\"", res);
            throw new RuntimeException(glGetProgramInfoLog(id, i));
        }
    }

    public static void freeCache() {
        INCLUDE_CACHE.clear();
    }

    public Shader use() {
        activeShader = this;
        glUseProgram(ID);
        return this;
    }

    public void free() {
        glDeleteProgram(ID);
    }

    private int get(String name) {
        if (activeShader != this)
            throw new RuntimeException("Shader must be bound before setting uniforms");
        return glGetUniformLocation(ID, name);
    }

    public void setBool(String name, boolean value) {
        glUniform1i(glGetUniformLocation(ID, name), value ? 1 : 0);
    }

    public void setInt(String name, int value) {
        int i = get(name);
        if (i != -1) glUniform1i(i, value);
    }

    public void setFloat(String name, float value) {
        int i = get(name);
        if (i != -1) glUniform1f(i, value);
    }

    public void setVec2(String name, float x, float y) {
        int i = get(name);
        if (i != -1) glUniform2f(i, x, y);
    }

    public void setVec2(String name, Vector2f vec) {
        setVec2(name, vec.x, vec.y);
    }

    public void setVec3(String name, float x, float y, float z) {
        int i = get(name);
        if (i != -1) glUniform3f(i, x, y, z);
    }

    public void setVec3(String name, Vector3f vec) {
        setVec3(name, vec.x, vec.y, vec.z);
    }

    public void setVec4(String name, float x, float y, float z, float w) {
        int i = get(name);
        if (i != -1) glUniform4f(i, x, y, z, w);
    }

    public void setVec4(String name, Vector4f vec) {
        setVec4(name, vec.x, vec.y, vec.z, vec.w);
    }

    public void setVec4(String name, Vector3f vec, float w) {
        setVec4(name, vec.x, vec.y, vec.z, w);
    }

    public void setMat3(String name, Matrix3f matrix3f) {
        int i = get(name);
        if (i == -1)
            return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(9);
            matrix3f.get(buffer);
            glUniformMatrix3fv(i, false, buffer);
        }
    }

    public void setMat3(String name, float... values) {
        if (values.length != 9)
            throw new IllegalArgumentException("Matrix 3x3 needs 9 values");

        int i = get(name);
        if (i == -1)
            return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.floats(values);
            glUniformMatrix3fv(i, false, buffer);
        }
    }

    public void setMat4(String name, Matrix4f matrix4f) {
        int i = get(name);
        if (i == -1)
            return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            matrix4f.get(buffer);
            glUniformMatrix4fv(i, false, buffer);
        }
    }

    public void setMat4(String name, float... values) {
        if (values.length != 16)
            throw new IllegalArgumentException("Matrix 4x4 needs 16 values");

        int i = get(name);
        if (i == -1)
            return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.floats(values);
            glUniformMatrix4fv(i, false, buffer);
        }
    }

    public void setIntArray(String name, int... array) {
        int i = get(name);
        if (i == -1)
            return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buffer = stack.ints(array);
            glUniform1iv(i, buffer);
        }
    }

    public void setFloatArray(String name, float... array) {
        int i = get(name);
        if (i == -1)
            return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.floats(array);
            glUniform1fv(i, buffer);
        }
    }

    public void setMat3Array(String name, Matrix3f... matrices) {
        int i = get(name);
        if (i == -1)
            return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(matrices.length * 9);
            for (int j = 0; j < matrices.length; j++)
                matrices[j].get(j * 9, buffer);
            glUniformMatrix3fv(i, false, buffer);
        }
    }

    public void setMat4Array(String name, Matrix4f... matrices) {
        int i = get(name);
        if (i == -1)
            return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(matrices.length * 16);
            for (int j = 0; j < matrices.length; j++)
                matrices[j].get(j * 16, buffer);
            glUniformMatrix4fv(i, false, buffer);
        }
    }

    public void setColor(String name, int color) {
        this.setVec3(name, ColorUtils.intToRGB(color));
    }

    public void setColorRGBA(String name, int colorARGB) {
        this.setVec4(name, ColorUtils.argbIntToRGBA(colorARGB));
    }

    public void setTexture(String name, Texture texture, int index) {
        this.setInt(name, texture.bind(index));
    }

    public void setTexture(String name, int texture, int index) {
        this.setInt(name, Texture.bind(texture, index));
    }

    public void setCubeMap(String name, int cubemap, int index) {
        this.setInt(name, CubeMap.bind(cubemap, index));
    }

    public void setTextureArray(String name, int texture, int index) {
        this.setInt(name, TextureArray.bind(texture, index));
    }

    // -- common functions -- //

    public void setup(Matrix4f proj, Matrix4f view) {
        applyProjectionMatrix(proj);
        applyViewMatrix(view);
    }

    public void setup(Camera camera) {
        setup(camera.getProjectionMatrix(), camera.getViewMatrix());
    }

    public void setupInverse(Camera camera) {
        applyInverseProjectionMatrix(camera.getProjectionMatrix().invert(new Matrix4f()));
        applyInverseViewMatrix(camera.getViewMatrix().invert(new Matrix4f()));
    }

    public void applyColor(int color) {
        this.setColor("color", color);
    }

    public void applyColorRGBA(int colorARGB) {
        this.setColorRGBA("color", colorARGB);
    }

    public void applyColor(Vector3f rgb) {
        this.applyColor(rgb.x, rgb.y, rgb.z);
    }

    public void applyColorRGBA(Vector3f rgb) {
        this.applyColor(rgb.x, rgb.y, rgb.z, 1f);
    }

    public void applyColor(Vector4f rgba) {
        this.applyColor(rgba.x, rgba.y, rgba.z, rgba.w);
    }

    public void applyColor(float r, float g, float b) {
        this.setVec3("color", r, g, b);
    }

    public void applyColor(float r, float g, float b, float a) {
        this.setVec4("color", r, g, b, a);
    }

    public void applyProjectionMatrix(Matrix4f matrix) {
        this.setMat4("projection", matrix);
    }

    public void applyViewMatrix(Matrix4f matrix) {
        this.setMat4("view", matrix);
    }

    public void applyModelMatrix(Matrix4f matrix) {
        this.setMat4("model", matrix);
    }

    public void applyNormalMatrix(Matrix3f matrix) {
        this.setMat3("normalMat", matrix);
    }

    public void applyInverseProjectionMatrix(Matrix4f matrix) {
        this.setMat4("invProjection", matrix);
    }

    public void applyInverseViewMatrix(Matrix4f matrix) {
        this.setMat4("invView", matrix);
    }

    public void applyInverseModelMatrix(Matrix4f matrix) {
        this.setMat4("invModel", matrix);
    }

    public void applyMatrixStack(MatrixStack matrices) {
        MatrixStack.Matrices mat = matrices.peek();
        applyModelMatrix(mat.pos());
        applyNormalMatrix(mat.normal());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Shader s && s.ID == this.ID;
    }

    private enum Type {
        VERTEX(GL_VERTEX_SHADER),
        GEOMETRY(GL_GEOMETRY_SHADER),
        FRAGMENT(GL_FRAGMENT_SHADER);

        public final int glBind;

        Type(int bind) {
            this.glBind = bind;
        }

        private static String fixGLESGeometryShader(String shaderSrc) {
            //only apply if were using opengl es
            if (!ArgsOptions.EXPERIMENTAL_OPENGL_ES.getAsBool())
                return shaderSrc;

            //check if extensions are supported
            if (!Cinnamon.OPENGL_EXTENSIONS.contains("GL_EXT_geometry_shader") || !Cinnamon.OPENGL_EXTENSIONS.contains("GL_OES_geometry_shader")) {
                LOGGER.warn("Could not enable OpenGL ES compatibility extensions to geometry shader");
                return shaderSrc;
            }

            LOGGER.debug("Enabling OpenGL ES compatibility extensions to geometry shader");

            //enable the GL_EXT_geometry_shader and GL_OES_geometry_shader extensions
            final String ext = """
                    #extension GL_EXT_geometry_shader : enable
                    #extension GL_OES_geometry_shader : enable
                    """;

            //add extensions after version declaration
            String[] versionSplit = shaderSrc.split("#version ", 2);
            if (versionSplit.length > 1) {
                int index = versionSplit[1].indexOf("\n");
                if (index != -1) {
                    shaderSrc = "#version " + versionSplit[1].substring(0, index) + "\n" + ext + versionSplit[1].substring(index + 1);
                } else {
                    shaderSrc = "#version " + versionSplit[1] + "\n" + ext;
                }
            } else {
                shaderSrc = ext + shaderSrc;
            }

            return shaderSrc;
        }
    }
}
