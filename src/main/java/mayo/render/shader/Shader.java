package mayo.render.shader;

import mayo.utils.ColorUtils;
import mayo.utils.IOUtils;
import mayo.utils.Resource;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL30.*;

public class Shader {

    public static Shader activeShader;

    public final int ID;

    public Shader(Resource res) {
        this.ID = loadShader(res);
    }

    private static int loadShader(Resource res) {
        String src, vertexSource, fragmentSource;
        try {
            InputStream resource = IOUtils.getResource(res);
            if (resource == null)
                throw new RuntimeException("Resource not found: " + res);

            src = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String[] split = src.split("#type ");
        //[0] = empty string
        //[1] = first type (frag/vertex)
        //[2] = second type (frag/vertex)
        if (split.length != 3 || (!split[1].startsWith("vertex") && !split[1].startsWith("fragment")) || (!split[2].startsWith("vertex") && !split[2].startsWith("fragment")))
            throw new RuntimeException("Invalid shader type");

        int i = split[1].startsWith("vertex") ? 1 : 2;
        vertexSource = split[i].substring("vertex".length());
        fragmentSource = split[i % 2 + 1].substring("fragment".length());

        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexSource);
        glCompileShader(vertexShader);
        checkCompileErrors(vertexShader);

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentSource);
        glCompileShader(fragmentShader);
        checkCompileErrors(fragmentShader);

        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);
        checkProgramErrors(program);

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        return program;
    }

    private static void checkCompileErrors(int id) {
        int status = glGetShaderi(id, GL_COMPILE_STATUS);
        if (status == GL_FALSE) {
            int i = glGetShaderi(id, GL_INFO_LOG_LENGTH);
            throw new RuntimeException(glGetShaderInfoLog(id, i));
        }
    }

    private static void checkProgramErrors(int id) {
        int status = glGetProgrami(id, GL_LINK_STATUS);
        if (status == GL_FALSE) {
            int i = glGetProgrami(id, GL_INFO_LOG_LENGTH);
            throw new RuntimeException(glGetProgramInfoLog(id, i));
        }
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

    public void setMat4(String name, Matrix4f matrix4f) {
        glUniformMatrix4fv(get(name), false, matrix4f.get(new float[4 * 4]));
    }

    public void setIntArray(String name, int[] array) {
        glUniform1iv(get(name), array);
    }

    // -- common functions -- //

    public void setColor(int color) {
        this.setColor(ColorUtils.intToRGB(color));
    }

    public void setColor(Vector3f rgb) {
        this.setColor(rgb.x, rgb.y, rgb.z);
    }

    public void setColor(float x, float y, float z) {
        this.setVec3("color", x, y, z);
    }

    public void setProjectionMatrix(Matrix4f matrix) {
        this.setMat4("projection", matrix);
    }

    public void setViewMatrix(Matrix4f matrix) {
        this.setMat4("view", matrix);
    }

    public void setModelMatrix(Matrix4f matrix) {
        this.setMat4("model", matrix);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Shader s && s.ID == this.ID;
    }
}
