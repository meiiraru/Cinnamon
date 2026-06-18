package cinnamon.utils;

import cinnamon.Cinnamon;
import cinnamon.Client;
import cinnamon.logger.Logger;
import cinnamon.render.Camera;
import cinnamon.render.WorldRenderer;
import cinnamon.settings.Settings;
import cinnamon.sound.SoundManager;
import cinnamon.vr.XrManager;
import cinnamon.world.entity.living.Player;
import cinnamon.world.world.WorldClient;
import org.lwjgl.glfw.GLFW;

import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

public class CrashHandler {

    private static final AtomicBoolean HAS_CRASHED = new AtomicBoolean(false);

    private static final String[] sillyMessages = {
            "oh noes!", "oopsie", "embarrassing...", "well this is awkward", "whoops", "uhhh...", "yikes", "something went wrong",
            "rip in pieces", "the cake is a lie", "this is fine", "guess we broke it", "try turning it off and on again", "send help",
            "not again...", "that was expected", "goodbye world", "i'm afraid i can't do that", "i have no idea what i'm doing", "stop"
    };

    /**
     * initializes the global Uncaught Exception Handler
     */
    public static void init() {
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler::handleCrash);
    }

    /**
     * handles the exception, dumps the system state to a file, and forcefully terminates the JVM
     */
    public static void handleCrash(Thread thread, Throwable throwable) {
        //prevent recursive crashes or simultaneous thread crashes
        if (!HAS_CRASHED.compareAndSet(false, true))
            return;

        try {
            //log the crash to console and logger before doing anything risky
            System.err.println("Crash detected in thread: " + thread.getName());
            Logger.getRootLogger().fatal("Engine crashed in thread: " + thread.getName(), throwable);

            //generate the crash report file
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String fileName = "crash-" + sdf.format(new Date()) + ".txt";
            Path path = IOUtils.parseNonDuplicatePath(IOUtils.ROOT_FOLDER.resolve("crash/" + fileName));
            IOUtils.createOrGetPath(path);

            //write the stacktrace directly to the file
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(path))) {
                writer.println("---- Cinnamon Crash Report ----");
                writer.println("* " + sillyMessages[Math.abs(fileName.hashCode()) % sillyMessages.length] + " *");
                writer.println();

                writer.println("Cinnamon " + Version.CLIENT_VERSION);
                writer.println();

                writer.println("-- Stacktrace --");
                throwable.printStackTrace(writer);
                writer.println();

                writer.println("-- System Details --");
                writer.println("\tTime: "      + new Date());
                writer.println("\tThread: "    + thread.getName());
                writer.println("\tOS: "        + System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ") version " + System.getProperty("os.version"));
                writer.println("\tJava: "      + System.getProperty("java.version") + ", " + System.getProperty("java.vendor"));
                writer.println("\tJava VM: "   + System.getProperty("java.vm.name") + " (" + System.getProperty("java.vm.version") + ")");
                writer.println("\tJVM: "       + System.getProperty("java.runtime.name") + " (" + System.getProperty("java.runtime.version") + ")");
                writer.println("\tJVM Flags: " + String.join(" ", ManagementFactory.getRuntimeMXBean().getInputArguments()));

                Runtime runtime = Runtime.getRuntime();
                writer.println("\tVM Memory Used: "      + ((runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024) + "MB");
                writer.println("\tVM Memory Allocated: " + (runtime.totalMemory() / 1024 / 1024) + "MB");
                writer.println("\tVM Memory Max: "       + (runtime.maxMemory() / 1024 / 1024) + "MB");

                writer.println("\tCPUs: "   + runtime.availableProcessors());
                writer.println("\tGPU: "    + Cinnamon.GPU_DETAILS);
                writer.println("\tOpenGL: " + Cinnamon.OPENGL_VERSION);
                writer.println("\tLWJGL: "  + org.lwjgl.Version.getVersion());
                writer.println();

                writer.println("-- Cinnamon --");
                writer.println("\tVersion: " + Version.CLIENT_VERSION);

                Client client = Client.getInstance();
                if (client.isInitialized()) {
                    //render
                    writer.println("\tWindow: "      + client.window.width + "x" + client.window.height);
                    writer.println("\tGUI scale: "   + client.window.guiScale + "/" + client.window.maxGuiScale);
                    writer.println("\tFullscreen: "  + client.window.isFullscreen());
                    writer.println("\tVSync: "       + Settings.vsync.get());
                    writer.println("\tFPS limit: "   + Settings.fpsLimit.get());
                    writer.println("\tXR: "          + XrManager.isInXR());
                    writer.println("\t3D Anaglyph: " + client.anaglyph3D);

                    //sound
                    writer.println("\tSound Count: "    + SoundManager.getSoundCount());
                    writer.println("\tSound Device: "   + SoundManager.getCurrentDevice());
                    writer.println("\tOpenAL Version: " + SoundManager.getALVersion());
                    writer.println();

                    //world
                    writer.println("-- Loaded World --");
                    if (client.world != null) {
                        WorldClient world = client.world;

                        //world and player
                        writer.println("\tWorld Time: " + world.getTime());
                        if (world.player != null) {
                            Player player = world.player;
                            writer.println("\tPlayer Name: " + player.getName());
                            writer.println("\tPlayer UUID: " + player.getUUID());
                        } else {
                            writer.println("\tNo player entity");
                        }

                        //render
                        writer.println("\tEntities Rendered: "  + WorldRenderer.getRenderedEntities());
                        writer.println("\tTerrain Rendered: "   + WorldRenderer.getRenderedTerrain());
                        writer.println("\tParticles Rendered: " + WorldRenderer.getRenderedParticles());
                        writer.println("\tDecals Rendered: "    + WorldRenderer.getDecalsCount());
                        writer.println("\tLights Rendered: "    + WorldRenderer.getLightsCount());
                        writer.println("\tShadow Casters: "     + WorldRenderer.getShadowsCount());

                        //camera
                        Camera camera = WorldRenderer.camera;
                        writer.println("\tCamera Mode: "         + world.getCameraMode());
                        writer.println("\tCamera FOV: "          + camera.getFov());
                        writer.println("\tCamera Aspect Ratio: " + camera.getAspectRatio());
                        writer.println("\tCamera Position: "     + camera.getPos());
                        writer.println("\tCamera Rotation: "     + camera.getRot());
                        writer.println("\tCamera UP: "           + camera.getUp());
                        writer.println("\tCamera Forward: "      + camera.getForwards());
                    } else {
                        writer.println("\tNo world loaded");
                    }
                } else {
                    writer.println("\tClient not initialized");
                }

                writer.flush();
            }

            System.err.println("Crash report saved to: " + path.toAbsolutePath());
        } catch (Throwable fallbackError) {
            //fallback if writing the file fails
            System.err.println("Failed to write crash report!");
            fallbackError.printStackTrace();
        } finally {
            //attempt clean termination of low-level libraries
            try {
                XrManager.close();
                GLFW.glfwTerminate();
            } catch (Throwable ignored) {
                //ignore errors during native termination as the current state is untrusty
            }

            //forcefully terminate the JVM
            Runtime.getRuntime().halt(1);
        }
    }
}