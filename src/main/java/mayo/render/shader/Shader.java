package mayo.render.shader;

import mayo.render.MatrixStack;
import mayo.utils.ColorUtils;
import mayo.utils.IOUtils;
import mayo.utils.Resource;
import org.joml.*;

import java.util.HashMap;
import java.util.Map;

import static mayo.Client.LOGGER;
import static org.lwjgl.opengl.GL30.*;

public class Shader {

    private static final Map<String, String> INCLUDE_CACHE = new HashMap<>();

    public static Shader activeShader;

    public final int ID;

    public Shader(Resource res) {
        this.ID = loadShader(res);
    }

    private static int loadShader(Resource res) {
        String src = IOUtils.readString(res);
        String[] split = src.split("#type ");
        //[0] = empty string
        //[1] = first type (frag/vertex)
        //[2] = second type (frag/vertex)
        if (split.length != 3 || (!split[1].startsWith("vertex") && !split[1].startsWith("fragment")) || (!split[2].startsWith("vertex") && !split[2].startsWith("fragment")))
            throw new RuntimeException("Invalid shader type");

        String vertexSource, fragmentSource;

        int i = split[1].startsWith("vertex") ? 1 : 2;
        vertexSource = split[i].substring("vertex".length());
        fragmentSource = split[i % 2 + 1].substring("fragment".length());

        //process includes
        String[] vInclude = vertexSource.split("#include ");
        if (vInclude.length > 1)
            vertexSource = processInclude(vInclude);

        String[] fInclude = fragmentSource.split("#include ");
        if (fInclude.length > 1)
            fragmentSource = processInclude(fInclude);

        //create shaders
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexSource);
        glCompileShader(vertexShader);
        checkCompileErrors(res, vertexShader);

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentSource);
        glCompileShader(fragmentShader);
        checkCompileErrors(res, fragmentShader);

        //create program
        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);
        checkProgramErrors(res, program);

        //delete shaders
        glDetachShader(program, vertexShader);
        glDetachShader(program, fragmentShader);

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        return program;
    }

    private static String processInclude(String[] vInclude) {
        StringBuilder finalShader = new StringBuilder();

        for (String s : vInclude) {
            //separate include from the shader
            int index = s.indexOf("\n");
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
            LOGGER.error("Error compiling shader: " + res);
            throw new RuntimeException(glGetShaderInfoLog(id, i));
        }
    }

    private static void checkProgramErrors(Resource res, int id) {
        int status = glGetProgrami(id, GL_LINK_STATUS);
        if (status == GL_FALSE) {
            int i = glGetProgrami(id, GL_INFO_LOG_LENGTH);
            LOGGER.error("Error linking shader program: " + res);
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
        return glGetUniformLocation(ID, name);
    }

    public void setBool(String name, boolean value) {
        glUniform1i(glGetUniformLocation(ID, name), value ? 1 : 0);
    }

    public void setInt(String name, int value) {
        glUniform1i(get(name), value);
    }

    public void setFloat(String name, float value) {
        glUniform1f(get(name), value);
    }

    public void setVec2(String name, float x, float y) {
        glUniform2f(get(name), x, y);
    }

    public void setVec2(String name, Vector2f vec) {
        setVec2(name, vec.x, vec.y);
    }

    public void setVec3(String name, float x, float y, float z) {
        glUniform3f(get(name), x, y, z);
    }

    public void setVec3(String name, Vector3f vec) {
        setVec3(name, vec.x, vec.y, vec.z);
    }

    public void setVec4(String name, float x, float y, float z, float w) {
        glUniform4f(get(name), x, y, z, w);
    }

    public void setVec4(String name, Vector4f vec) {
        setVec4(name, vec.x, vec.y, vec.z, vec.w);
    }

    public void setVec4(String name, Vector3f vec, float w) {
        setVec4(name, vec.x, vec.y, vec.z, w);
    }

    public void setMat3(String name, Matrix3f matrix3f) {
        glUniformMatrix3fv(get(name), false, matrix3f.get(new float[3 * 3]));
    }

    public void setMat4(String name, Matrix4f matrix4f) {
        glUniformMatrix4fv(get(name), false, matrix4f.get(new float[4 * 4]));
    }

    public void setIntArray(String name, int[] array) {
        glUniform1iv(get(name), array);
    }

    public void setColor(String name, int color) {
        this.setVec3(name, ColorUtils.intToRGB(color));
    }

    public void setColorRGBA(String name, int color) {
        this.setVec4(name, ColorUtils.intToRGBA(color));
    }

    // -- common functions -- //

    public void setup(Matrix4f proj, Matrix4f view) {
        applyProjectionMatrix(proj);
        applyViewMatrix(view);
        applyColor(-1);
    }

    public void applyColor(int color) {
        this.setColor("color", color);
    }

    public void applyColor(Vector3f rgb) {
        this.applyColor(rgb.x, rgb.y, rgb.z);
    }

    public void applyColor(float x, float y, float z) {
        this.setVec3("color", x, y, z);
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

    public void applyInvertedNormalMatrix(Matrix3f matrix) {
        this.setMat3("normalMat", matrix);
    }

    public void applyMatrixStack(MatrixStack matrices) {
        MatrixStack.Matrices mat = matrices.peek();
        applyModelMatrix(mat.pos());
        applyInvertedNormalMatrix(mat.normal().invert());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Shader s && s.ID == this.ID;
    }
}
