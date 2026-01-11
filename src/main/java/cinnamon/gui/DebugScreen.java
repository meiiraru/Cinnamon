package cinnamon.gui;

import cinnamon.Client;
import cinnamon.gui.screens.MainMenu;
import cinnamon.logger.Level;
import cinnamon.logger.LogOutput;
import cinnamon.logger.LoggerConfig;
import cinnamon.model.GeometryHelper;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.WorldRenderer;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.shader.PostProcess;
import cinnamon.render.texture.Texture;
import cinnamon.settings.Settings;
import cinnamon.sound.SoundCategory;
import cinnamon.sound.SoundManager;
import cinnamon.text.Formatting;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.*;
import cinnamon.vr.XrManager;
import cinnamon.world.Abilities;
import cinnamon.world.WorldObject;
import cinnamon.world.collisions.Hit;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.Player;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.world.WorldClient;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static cinnamon.Client.LOGGER;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class DebugScreen {

    private static final Resource STYLE_PATH = new Resource("data/gui_styles/debug.json");
    private static final Style STYLE = Style.EMPTY.background(true).shadow(true).guiStyle(STYLE_PATH);

    private static final CircularQueue<String> LOG = new CircularQueue<>(20);
    public static final LogOutput LOG_OUTPUT = new LogOutput() {
        @Override
        public void write(Level level, String message, Throwable throwable) {
            switch (level) {
                case TRACE, DEBUG -> message = "&7" + message;
                case INFO -> message = "&f" + message;
                case WARN -> message = "&e" + message;
                case ERROR -> message = "&c" + message;
                case FATAL -> message = "&d" + message;
            }
            if (throwable != null) {
                message += "\t" + throwable.getMessage();
            }
            message = message.replaceAll("\r?\n$", "").replaceAll("\t", "  ") + "&r";
            Collections.addAll(LOG, message.split("\n", 0));
        }
    };
    static {
        LOG_OUTPUT.setLevel(Level.DEBUG);
        LOG_OUTPUT.setFormatting("[%1$tT] [%2$s/%3$s] (%4$s) %5$s\n");
    }

    private static final List<Tab> selectedTabs = new ArrayList<>(Tab.values().length);
    static {
        selectedTabs.add(Tab.SYSTEM);
    }

    private static int hoveredTab;
    private static boolean active;
    private static boolean f3Pressed, f3Voided;

    public static void render(MatrixStack matrices, float delta) {
        Client c = Client.getInstance();

        //renderDebugTexture(matrices, WaterRenderer.getNoiseTexture(), false);

        boolean fpsOnly = !active && Settings.showFPS.get() && (c.world == null || !c.hideHUD);
        if (!fpsOnly && !active)
            return;

        matrices.pushMatrix();
        matrices.translate(0f, 0f, 20f);

        if (fpsOnly) {
            String vsync = Settings.vsync.get() ? " (vsync)" : "";
            Text.of(c.fps + " fps @ " + c.ms + " ms" + vsync).withStyle(STYLE).render(VertexConsumer.MAIN, matrices, 4, 4);
            matrices.popMatrix();
            return;
        }

        renderDebugCrosshair(matrices, c);
        renderHeader(matrices, c);
        renderTabs(matrices, c);
        renderContent(matrices, c);

        matrices.popMatrix();
    }

    public static boolean keyPress(int key, int scancode, int action, int mods) {
        if (action == GLFW_PRESS) {
            if (key == GLFW_KEY_F3) {
                f3Pressed = true;
                return true;
            } else if (!f3Pressed) {
                return false;
            }

            switch (key) {
                case GLFW_KEY_X -> XrManager.init();
                case GLFW_KEY_L -> LoggerConfig.debugLogLevels(LOGGER);
                case GLFW_KEY_R -> {
                    Client c = Client.getInstance();
                    c.queueTick(() -> {
                        if (c.screen != null)
                            c.screen.rebuild();
                    });
                }
                case GLFW_KEY_T -> Client.getInstance().reloadAssets();
                case GLFW_KEY_Q -> {
                    Client c = Client.getInstance();
                    c.disconnect();
                    c.queueTick(() -> c.setScreen(new MainMenu()));
                }
                default -> {return false;}
            }

            f3Voided = true;
            return true;
        }

        else if (action == GLFW_RELEASE && key == GLFW_KEY_F3) {
            boolean wasPressed = f3Pressed;
            f3Pressed = false;

            if (wasPressed && !f3Voided) {
                active = !active;
                return true;
            }
            f3Voided = false;
        }

        return false;
    }

    public static boolean mousePress(int button, int action, int mods) {
        if (action == GLFW_PRESS && button == GLFW_MOUSE_BUTTON_1 && hoveredTab != -1) {
            click();
            return true;
        }
        return false;
    }

    public static boolean xrTriggerPress(int button, float value, int hand, float lastValue) {
        if (button == 0 && lastValue < 0.5f && value >= 0.5f && hoveredTab != -1) {
            click();
            return true;
        }
        return false;
    }

    private static void click() {
        if (hoveredTab < 0) { //close button -2
            active = false;
        } else {
            Tab tab = Tab.values()[hoveredTab];
            if (selectedTabs.contains(tab))
                selectedTabs.remove(tab);
            else
                selectedTabs.addFirst(tab);
        }
    }

    private static void renderDebugCrosshair(MatrixStack matrices, Client c) {
        matrices.pushMatrix();
        matrices.translate(c.window.getGUIWidth() / 2f, c.window.getGUIHeight() / 2f, 0);
        if (c.world != null)
            matrices.scale(1, -1, 1);

        float len = 10;
        Camera camera = c.world == null ? c.camera : WorldRenderer.camera;
        matrices.translate(0, 0, len);
        matrices.peek().pos().rotate(camera.getRot().invert(new Quaternionf()));

        VertexConsumer.MAIN.consume(GeometryHelper.box(matrices, 1, 0, 0, len, 1, 1, 0xFFFF0000));
        VertexConsumer.MAIN.consume(GeometryHelper.box(matrices, 0, 1, 0, 1, len, 1, 0xFF00FF00));
        VertexConsumer.MAIN.consume(GeometryHelper.box(matrices, 0, 0, 1, 1, 1, len, 0xFF0000FF));

        matrices.popMatrix();
    }

    private static void renderHeader(MatrixStack matrices, Client c) {
        Text text = TextUtils.parseColorFormatting(
                Text.of("Cinnamon v&e%s&r\n&e%s&r fps @ &e%s&r ms"
                        .formatted(Version.CLIENT_VERSION, c.fps, c.ms))
                        .withStyle(STYLE.background(false)));

        int bg = GUIStyle.of(STYLE_PATH).getInt("background_color");
        float x = 4;
        float y = 4;
        float w = TextUtils.getWidth(text);
        float h = TextUtils.getHeight(text);

        VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, x, y, x + w, y + h, bg));

        matrices.pushMatrix();
        matrices.translate(0, 0, UIHelper.getDepthOffset());
        text.render(VertexConsumer.MAIN, matrices, x, y);
        matrices.popMatrix();
    }

    private static void renderTabs(MatrixStack matrices, Client c) {
        int spacing = 16;
        float x = 8;
        float y = 8 + 20;

        int mouseX = c.window.mouseX;
        int mouseY = c.window.mouseY;
        boolean hasMouse = !c.window.isMouseLocked();

        hoveredTab = -1;
        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            Text tabText = Text.of(tabs[i].name);
            float yy = y + i * spacing;
            if (hoveredTab == -1 && hasMouse &&
                    mouseX >= x && mouseX <= x + TextUtils.getWidth(tabText) &&
                    mouseY >= yy && mouseY <= yy + TextUtils.getHeight(tabText)) {
                hoveredTab = i;
            }

            Style s = STYLE.shadow(true);
            if (i == hoveredTab) s = s.backgroundColor(0xAAFFFFFF);
            if (selectedTabs.contains(tabs[i])) s = s.formatted(Formatting.YELLOW);
            tabText.withStyle(s).render(VertexConsumer.MAIN, matrices, x, yy);
        }

        //special case for the "close" button
        Style s = STYLE.outlined(true).formatted(Formatting.RED);
        Text close = Text.of(" x ");
        int xx = 100;
        if (hoveredTab == -1 && hasMouse &&
                mouseX >= xx && mouseX <= xx + TextUtils.getWidth(close) &&
                mouseY >= 8 && mouseY <= 8 + TextUtils.getHeight(close)) {
            hoveredTab = -2;
            s = s.backgroundColor(0xAAFFFFFF);
        }
        close.withStyle(s).render(VertexConsumer.MAIN, matrices, xx, 8);
    }

    private static void renderContent(MatrixStack matrices, Client c) {
        if (selectedTabs.isEmpty())
            return;

        float x = 8 + 50;
        float y = 8 + 20;

        for (Tab selectedTab : selectedTabs) {
            String result = selectedTab.function.apply(c);
            if (result == null)
                continue;

            Text text = TextUtils
                    .parseColorFormatting(Text.of(result))
                    .withStyle(STYLE.background(false));

            int bg = GUIStyle.of(STYLE_PATH).getInt("background_color");
            float w = TextUtils.getWidth(text);
            float h = TextUtils.getHeight(text);

            VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, x - 4, y - 4, x + w + 4, y + h + 4, bg));

            matrices.pushMatrix();
            matrices.translate(0, 0, UIHelper.getDepthOffset());
            text.render(VertexConsumer.MAIN, matrices, x, y);
            matrices.popMatrix();

            y += h + 8 + 4; //height + border + spacing
        }
    }

    public static void renderDebugTexture(MatrixStack matrices, int texture, boolean overlay) {
        Client c = Client.getInstance();

        //debug quad
        float w = c.window.getGUIWidth() * (overlay ? 1f : 0.3f);
        float h = overlay ? c.window.getGUIHeight() : w;
        float x = overlay ? 0 : c.window.getGUIWidth() - w - 4;
        float y = overlay ? 0 : 4;

        if (!overlay) {
            int texWidth = Texture.getWidth(texture);
            int texHeight = Texture.getHeight(texture);
            float aspect = (float) texWidth / (float) texHeight;
            h = w / aspect;
        }

        VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices, x, y, w, h, 0, 1, 1, -1, 1, 1), texture);
        VertexConsumer.MAIN.finishBatch(c.camera);
    }

    private static String getTargetedObjString(Hit<? extends WorldObject> hit, float range) {
        if (hit == null)
            return "---";

        Vector3f pos = hit.obj().getPos();
        Vector3f hPos = hit.pos();
        Vector3f normal = hit.collision().normal();
        float distance = range * hit.collision().near();
        String type = (hit.obj() instanceof Entity) ? "entity" : (hit.obj() instanceof Terrain) ? "terrain" : "unknown";
        String typeEnum = hit.obj().getType().name().toLowerCase();
        String extra = (hit.obj() instanceof Entity e) ? "\n" + e.getUUID() : (hit.obj() instanceof Terrain t) ? "\nrotation &e" + (int) t.getRotationAngle() + "&r" : "";
        return String.format("""
                x &c%.3f&r y &a%.3f&r z &b%.3f&r
                hit pos x &c%.3f&r y &a%.3f&r z &b%.3f&r
                hit normal x &c%.3f&r y &a%.3f&r z &b%.3f&r
                hit distance &e%.3fm&r
                type &e%s&r:&e%s&r%s""",

                pos.x, pos.y, pos.z,
                hPos.x, hPos.y, hPos.z,
                normal.x, normal.y, normal.z,
                distance,
                type, typeEnum,
                extra
        );
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isTabOpen(Tab... tabs) {
        if (!active)
            return false;
        for (Tab tab : tabs)
            if (selectedTabs.contains(tab))
                return true;
        return false;
    }

    public enum Tab {
        SYSTEM(c -> {
            long millis = System.currentTimeMillis();

            Runtime r = Runtime.getRuntime();
            long max = r.maxMemory();
            long total = r.totalMemory();
            long free = r.freeMemory();
            long used = total - free;

            return String.format("""
                    [&bjava&r]
                    java version &e%s&r
                    mem &e%s&r%% &e%s&r/&e%s&r
                    allocated &e%s&r%% &e%s&r

                    [&bproperties&r]
                    date &e%tT %tF&r
                    OS &e%s&r
                    %s
                    LWJGL &e%s&r
                    OpenGL &e%s&r""",

                    System.getProperty("java.version"),
                    used * 100 / max, Maths.prettyByteSize(used), Maths.prettyByteSize(max),
                    total * 100 / max, Maths.prettyByteSize(total),

                    millis, millis,
                    System.getProperty("os.name"),
                    glGetString(GL_RENDERER),
                    org.lwjgl.Version.getVersion(),
                    glGetString(GL_VERSION)
            );
        }),
        RENDER(c -> {
            Window w = c.window;
            PostProcess post = c.postProcess == -1 ? null : PostProcess.EFFECTS[c.postProcess];
            Camera camera = c.world == null ? c.camera : WorldRenderer.camera;

            Quaternionf crot = camera.getRotation();
            Vector3f cpos = camera.getPosition();
            Vector3f forwards = camera.getForwards();
            Vector3f up = camera.getUp();
            float yaw = Maths.getYaw(crot);
            String face = Direction.fromRotation(yaw).name;

            return String.format("""
                    [&bwindow&r]
                    &e%s&r x &e%s&r gui scale &e%s&r fullscreen &e%s&r
                    vsync &e%s&r FPS limit &e%s&r

                    [&beffects&r]
                    post process &e%s&r
                    3D anaglyph &e%s&r XR &e%s&r
                    
                    [&bcamera&r]
                    x &c%.3f&r y &a%.3f&r z &b%.3f&r
                    x &e%.3f&r y &e%.3f&r z &e%.3f&r w &e%.3f&r
                    forwards x &c%.3f&r y &a%.3f&r z &b%.3f&r
                    up x &c%.3f&r y &a%.3f&r z &b%.3f&r
                    facing &e%s&r""",

                    w.width, w.height, w.guiScale, w.isFullscreen() ? "on" : "off",
                    Settings.vsync.get() ? "on" : "off", Settings.fpsLimit.get() <= 0 ? "unlimited" : Settings.fpsLimit.get() + " fps",

                    post == null ? "none" : post.name(),
                    c.anaglyph3D ? "on" : "off", XrManager.isInXR() ? "on" : "off",

                    cpos.x, cpos.y, cpos.z,
                    crot.x, crot.y, crot.z, crot.w,
                    forwards.x, forwards.y, forwards.z,
                    up.x, up.y, up.z,

                    face
            );
        }),
        SOUND(c -> {
            if (!SoundManager.isInitialized())
                return "&cSound system not initialized&r";

            StringBuilder buffer = new StringBuilder();
            for (SoundCategory category : SoundCategory.values()) {
                int vol = (int) (category.getVolume() * 100f);
                int soundCount = SoundManager.getSoundCount(cat -> cat == category);
                buffer.append(String.format("%s: &e%d%%&r / &e%s&r\n", category.name(), vol, soundCount));
            }

            return String.format("""
                    [&bcategories&r]
                    &e%s&r total sounds

                    %s
                    [&bdevice&r]
                    %s
                    OpenAL &e%s&r""",
                    SoundManager.getSoundCount(),
                    buffer,
                    SoundManager.getCurrentDevice().replaceFirst("^OpenAL Soft on ", ""),
                    SoundManager.getALVersion()
            );
        }),
        PLAYER(c -> {
            WorldClient w = c.world;
            if (w == null)
                return "&cNo world loaded&r";

            Player p = w.player;
            Abilities abilities = p.getAbilities();

            Vector3f epos = p.getPos();
            Vector2f erot = p.getRot();
            Vector3f emot = p.getMotion();

            float range = p.getPickRange();
            String object = getTargetedObjString(p.getLookingObject(range), range);

            return String.format("""
                    [&bentity&r]
                    &e%s&r
                    %s
                    x &c%.3f&r y &a%.3f&r z &b%.3f&r
                    pitch &e%.3f&r yaw &e%.3f&r
                    motion &c%.3f &a%.3f &b%.3f&r
                    onground &e%s&r
                    noclip &e%s&r god mode &e%s&r
                    can fly &e%s&r can build &e%s&r

                    [&btargeted object&r]
                    %s""",

                    p.getName(), p.getUUID(),
                    epos.x, epos.y, epos.z,
                    erot.x, erot.y,
                    emot.x, emot.y, emot.z,

                    p.isOnGround() ? "yes" : "no",
                    abilities.noclip() ? "on" : "off",
                    abilities.godMode() ? "on" : "off",
                    abilities.canFly() ? "on" : "off",
                    abilities.canBuild() ? "on" : "off",

                    object
            );
        }),
        WORLD(c -> {
            WorldClient w = c.world;
            if (w == null)
                return "&cNo world loaded&r";

            String camera = switch (w.getCameraMode()) {
                case 0 -> "First Person";
                case 1 -> "Third Person (back)";
                case 2 -> "Third Person (front)";
                default -> "unknown";
            };

            return String.format("""
                    time &e%s&r (&e%s&r)
                    day &e%s&r
                    camera &e%s&r
                    &e%s&r light sources
                    &e%s&r shadow casters
                    &e%s&r particles""",

                    w.getTime(), w.getTimeOfTheDay(),
                    w.getDay(),
                    camera,
                    WorldRenderer.getLightsCount(),
                    WorldRenderer.getShadowsCount(),
                    WorldRenderer.getRenderedParticles()
            );
        }),
        TERRAIN(c -> {
            WorldClient w = c.world;
            if (w == null)
                return "&cNo world loaded&r";

            return String.format("""
                    &e%s&r terrain""",
                    WorldRenderer.getRenderedTerrain()
            );
        }),
        ENTITIES(c -> {
            WorldClient w = c.world;
            if (w == null)
                return "&cNo world loaded&r";

            return String.format("""
                    &e%s&r entities""",
                    WorldRenderer.getRenderedEntities()
            );
        }),
        LOGGER(c -> {
            StringBuilder sb = new StringBuilder();
            for (String log : LOG) {
                if (!sb.isEmpty())
                    sb.append('\n');
                sb.append(log);
            }
            return sb.toString();
        });

        public final String name;
        private final Function<Client, String> function;

        Tab(Function<Client, String> function) {
            this.name = this.name().charAt(0) + this.name().substring(1).toLowerCase();
            this.function = function;
        }
    }
}
