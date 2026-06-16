package cinnamon.utils;

import cinnamon.logger.Logger;
import cinnamon.vr.XrManager;
import org.lwjgl.glfw.GLFW;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

public class CrashHandler {

    private static final AtomicBoolean HAS_CRASHED = new AtomicBoolean(false);

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
                writer.println("---- CINNAMON ENGINE CRASH REPORT ----");
                writer.println("Time: " + new Date());
                writer.println("Thread: " + thread.getName());
                writer.println("OS: " + System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")");
                writer.println("Java: " + System.getProperty("java.version"));

                Runtime runtime = Runtime.getRuntime();
                writer.println("Memory Used: " + ((runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024) + "MB");
                writer.println("Memory Max: " + (runtime.maxMemory() / 1024 / 1024) + "MB");
                writer.println("--------------------------------------\n");

                writer.println("Stacktrace:");
                throwable.printStackTrace(writer);

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