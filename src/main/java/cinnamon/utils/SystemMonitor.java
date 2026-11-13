package cinnamon.utils;

import org.lwjgl.Version;

import java.lang.management.ManagementFactory;
import java.util.concurrent.CompletableFuture;

public final class SystemMonitor {

    //static hardware info
    public static final String
            OS_NAME    = System.getProperty("os.name"),
            OS_VERSION = System.getProperty("os.version"),
            OS_ARCH    = System.getProperty("os.arch"),

            JAVA_VERSION = System.getProperty("java.version"),
            JAVA_VENDOR  = System.getProperty("java.vendor"),
            JAVA_VM_NAME    = System.getProperty("java.vm.name"),
            JAVA_VM_VERSION = System.getProperty("java.vm.version"),
            JAVA_RUNTIME_NAME    = System.getProperty("java.runtime.name"),
            JAVA_RUNTIME_VERSION = System.getProperty("java.runtime.version"),
            JVM_FLAGS = String.join(" ", ManagementFactory.getRuntimeMXBean().getInputArguments()),

            LWJGL_VERSION = Version.getVersion();

    public static final int
            CPUS = Runtime.getRuntime().availableProcessors();

    //dynamic metrics
    public static volatile long millis;
    public static volatile long memMax;
    public static volatile long memTotal;
    public static volatile long memFree;
    public static volatile long memUsed;
    public static volatile long memUsedPercent;
    public static volatile long memAllocatedPercent;

    private static CompletableFuture<Void> currentTask = CompletableFuture.completedFuture(null);

    private SystemMonitor() {}

    public static void updateAsync() {
        if (currentTask == null || currentTask.isDone())
            currentTask = CompletableFuture.runAsync(SystemMonitor::update);
    }

    public static void update() {
        millis = System.currentTimeMillis();

        Runtime r = Runtime.getRuntime();
        memMax = r.maxMemory();
        memTotal = r.totalMemory();
        memFree = r.freeMemory();
        memUsed = memTotal - memFree;

        memUsedPercent = memUsed * 100 / memMax;
        memAllocatedPercent = memTotal * 100 / memMax;
    }
}