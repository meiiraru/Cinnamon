package cinnamon.gui.screens.extras;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Toast;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.types.Button;
import cinnamon.math.MatrixStack;
import cinnamon.model.StaticGeometry;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.shader.Shader;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Colors;
import cinnamon.utils.Resource;

import static cinnamon.Client.LOGGER;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.opengl.GL11.glDepthMask;

public class ShadertoyScreen extends ParentedScreen {

    public static final Resource
            SHADER_RES = new Resource("generated/shaders", "shadertoy.glsl"),

            RELOAD = new Resource("textures/gui/icons/reload.png"),
            EDIT   = new Resource("textures/gui/icons/edit.png"),
            EXIT   = new Resource("textures/gui/icons/close.png");

    public static final String shaderSrc = """
                #type vertex
                #include shaders/libs/blit.vsh
                
                #type fragment
                #version 330 core
                
                in vec2 texCoords;
                
                out vec4 fragColor;
                
                uniform vec2 iResolution;
                uniform float iTime;
                uniform float iTimeDelta;
                uniform float iFrameRate;
                uniform int iFrame;
                uniform vec4 iMouse;
                
                uniform float iGUIScale;
                
                %s
                
                void main() {
                    mainImage(fragColor, texCoords * iResolution);
                    fragColor.a = 1.0f;
                }
                """;
    protected Shader shader;

    protected int ticks, frames;
    protected float mouseX, mouseY, clickX, clickY;
    protected boolean mousePressed;

    public ShadertoyScreen(ParentedScreen parent) {
        super(parent);
    }

    @Override
    public void init() {
        super.init();

        //buttons
        ContainerGrid buttons = new ContainerGrid(width - 4, 4, 4, 999);
        buttons.setAlignment(Alignment.TOP_RIGHT);

        //reload button
        Button reloadButton = new Button(0, 0, 16, 16, null, _ -> reloadShader());
        reloadButton.setIcon(RELOAD);
        reloadButton.setTooltip(Text.translated("gui.shadertoy_screen.reload"));
        buttons.addWidget(reloadButton);

        //edit button
        Button editButton = new Button(0, 0, 16, 16, null, _ -> {});
        editButton.setActive(false);
        editButton.setIcon(EDIT);
        editButton.setTooltip(Text.translated("gui.shadertoy_screen.edit"));
        buttons.addWidget(editButton);

        //close button
        Button closeButton = new Button(0, 0, 16, 16, null, _ -> close());
        closeButton.setIcon(EXIT);
        closeButton.setTooltip(Text.translated("gui.close"));
        buttons.addWidget(closeButton);

        addWidget(buttons);

        //init shader
        if (shader == null)
            reloadShader();
    }

    @Override
    protected void addBackButton() {
        //super.addBackButton();
    }

    protected void reloadShader() {
        String shaderCode = """
                void mainImage(out vec4 fragColor, in vec2 fragCoord) {
                    vec2 uv = (fragCoord - 0.5f * iResolution) / iResolution.y;
                    vec3 col = vec3(0.0f);
                    for (int i = 0; i < 8; i++) {
                        float t = float(i) * 0.785f + iTime;
                        vec2 pos = vec2(cos(t), sin(t * 2.0f)) * 0.3f;
                        float glow = 0.005f / length(uv - pos);
                        vec3 tint = 0.5f + 0.5f * cos(float(i) * 0.8f + vec3(0.0f, 2.0f, 4.0f));
                        col += tint * glow;
                    }
                    fragColor = vec4(col, 1.0f);
                }
                """;

        //initialize shader
        Shader.free(SHADER_RES);
        try {
            this.shader = Shader.of(SHADER_RES, shaderSrc.formatted(shaderCode));
        } catch (Exception e) {
            this.shader = null;
            Toast.addToast(Text.translated("gui.shadertoy_screen.error")).type(Toast.ToastType.ERROR);
            LOGGER.error("Failed to compile shader: " + SHADER_RES, e);
        }

        //reset vars
        this.ticks = this.frames = 0;
        this.mouseX = this.mouseY = this.clickX = this.clickY = 0;
        this.mousePressed = false;
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
    }

    protected void applyUniforms(Shader shader, float delta) {
        shader.setVec2("iResolution", width, height);
        shader.setFloat("iTime", (ticks + delta) / 20f);
        shader.setFloat("iTimeDelta", delta / 20f);
        shader.setFloat("iFrameRate", client.fps);
        shader.setInt("iFrame", frames);
        shader.setVec4("iMouse", mouseX, mouseY, clickX, clickY);
        //custom
        shader.setFloat("iGUIScale", client.window.guiScale);
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta, int color1, int color2, float size) {
        if (this.shader == null) {
            super.renderBackground(matrices, delta, color1, color2, size);
            return;
        }

        Shader oldShader = Shader.activeShader;
        this.shader.use();
        applyUniforms(this.shader, delta);

        glDepthMask(false);
        StaticGeometry.QUAD.render();
        glDepthMask(true);

        frames++;

        if (oldShader != null)
            oldShader.use();
    }

    @Override
    protected void renderChildren(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.renderChildren(matrices, mouseX, mouseY, delta);

        //debug text
        Text debugText;
        if (shader == null)
            debugText = Text.translated("gui.shadertoy_screen.error")
                    .withStyle(Style.EMPTY.outlined(true).color(Colors.RED));
        else
            debugText = Text.of("%.2f".formatted((ticks + delta) / 20f))
                    .append("    " + client.fps + "fps")
                    .append("    " + width + " x " + height)
                    .withStyle(Style.EMPTY.outlined(true));
        debugText.render(VertexConsumer.MAIN, matrices, 4, 4);
    }

    @Override
    public boolean mousePress(int button, int action, int mods) {
        if (action == GLFW_PRESS) {
            mousePressed = true;

            mouseX = client.window.mouseX;
            mouseY = height - client.window.mouseY;

            clickX = mouseX;
            clickY = mouseY;
        } else if (action == GLFW_RELEASE) {
            mousePressed = false;
            clickX = -Math.abs(clickX);
            clickY = -Math.abs(clickY);
        }

        return super.mousePress(button, action, mods);
    }

    @Override
    public boolean mouseMove(int x, int y) {
        if (mousePressed) {
            mouseX = x;
            mouseY = height - y;
        }
        return super.mouseMove(x, y);
    }
}
