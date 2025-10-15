package cinnamon.sound;

import cinnamon.logger.Logger;
import cinnamon.render.Camera;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import org.joml.Vector3f;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.openal.ALUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL11.AL_LINEAR_DISTANCE_CLAMPED;
import static org.lwjgl.openal.AL11.alSpeedOfSound;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.ALC11.ALC_ALL_DEVICES_SPECIFIER;
import static org.lwjgl.openal.ALC11.ALC_DEFAULT_ALL_DEVICES_SPECIFIER;

public class SoundManager {

    private static final Logger LOGGER = new Logger(Logger.ROOT_NAMESPACE + "/sound");
    public static final int MAX_SOUND_INSTANCES = 256;

    private static final List<SoundInstance> sounds = new ArrayList<>();
    private static final List<String> devices = new ArrayList<>();
    private static String currentDevice = "";
    private static boolean useDefaultDevice;
    private static long context;
    private static long device;
    private static String ALVersion = "Unknown";
    private static boolean initialized;

    public static void init(String deviceName) {
        if (initialized)
            return;

        LOGGER.info("Initializing OpenAL sound engine...");

        devices.clear();
        List<String> newDevices = ALUtil.getStringList(0, ALC_ALL_DEVICES_SPECIFIER);

        if (newDevices == null || newDevices.isEmpty()) {
            LOGGER.warn("No devices found! Disabling all sounds...");
            return;
        }

        devices.addAll(newDevices);

        //list all OpenAL devices
        LOGGER.debug("Available OpenAL devices:");
        for (String device : devices)
            LOGGER.debug(device);

        //initialize audio device
        String deviceToUse;
        useDefaultDevice = deviceName == null || !devices.contains(deviceName);
        if (useDefaultDevice) {
            String defaultDevice = alcGetString(0, ALC_DEFAULT_ALL_DEVICES_SPECIFIER);
            deviceToUse = defaultDevice != null ? defaultDevice : newDevices.getFirst();
        } else {
            deviceToUse = deviceName;
        }

        currentDevice = deviceToUse;
        device = alcOpenDevice(deviceToUse);

        try {
            checkALCError(device);
        } catch (Exception e) {
            LOGGER.error("Failed to initialize sound engine!", e);
            LOGGER.warn("Disabling all sounds...");
            return;
        }

        //setup context
        int[] attributes = {0};
        context = alcCreateContext(device, attributes);
        alcMakeContextCurrent(context);

        //init capabilities
        ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
        ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);

        //set up properties
        if (alCapabilities.OpenAL11)
            alDistanceModel(AL_LINEAR_DISTANCE_CLAMPED);

        alSpeedOfSound(1f);
        initialized = true;
        ALVersion = alcGetInteger(0, ALC_MAJOR_VERSION) + "." + alcGetInteger(0, ALC_MINOR_VERSION);
        LOGGER.info("OpenAL version: %s", ALVersion);
        LOGGER.info("OpenAL device: %s", deviceToUse);
    }

    public static void free() {
        if (!initialized)
            return;

        stopAll();
        alcDestroyContext(context);
        alcCloseDevice(device);
        Sound.freeAllSounds();
        initialized = false;
    }

    static void checkALError() {
        int err = alGetError();
        if (err != AL_NO_ERROR)
            throw new RuntimeException("(" + err + ") " + alGetString(err));
    }

    private static void checkALCError(long device) {
        int err = alcGetError(device);
        if (err != ALC_NO_ERROR)
            throw new RuntimeException("(" + err + ") " + alcGetString(device, err));
    }

    public static void tick(Camera camera) {
        if (!initialized)
            return;

        checkALError();

        //free stopped sounds
        for (SoundInstance sound : sounds)
            if (sound.isStopped())
                sound.free();

        //remove sounds
        sounds.removeIf(SoundInstance::isRemoved);

        //setup listener properties
        Vector3f pos = camera.getPos();
        Vector3f forward = camera.getForwards();
        Vector3f up = camera.getUp();

        if (Maths.isNaN(pos) || Maths.isNaN(forward) || Maths.isNaN(up))
            throw new RuntimeException("Camera properties contains a NaN value!");

        alListener3f(AL_POSITION, pos.x, pos.y, pos.z);
        alListenerfv(AL_ORIENTATION, new float[]{
                forward.x, forward.y, forward.z,
                up.x, up.y, up.z
        });
    }

    public static SoundInstance playSound(Resource resource, SoundCategory category) {
        return playSound(resource, category, null);
    }

    public static SoundInstance playSound(Resource resource, SoundCategory category, Vector3f position) {
        if (sounds.size() >= MAX_SOUND_INSTANCES) {
            LOGGER.debug("Reached %s sound instances! Skipping sound \"%s\"", MAX_SOUND_INSTANCES, resource);
            return new SoundInstance(category);
        }

        if (!initialized) {
            LOGGER.debug("Sound engine is not initialized! Skipping sound \"%s\"", resource);
            return new SoundInstance(category);
        }

        LOGGER.debug("Playing sound \"%s\"", resource);

        SoundSource source = new SoundSource(resource, category, position);
        sounds.add(source);
        source.play();
        return source;
    }

    public static void stopAll() {
        stopAll(category -> true);
    }

    public static void stopAll(Predicate<SoundCategory> categoryPredicate) {
        for (SoundInstance sound : sounds) {
            if (categoryPredicate.test(sound.getCategory())) {
                sound.stop();
                sound.free();
            }
        }
        sounds.removeIf(SoundInstance::isRemoved);
    }

    public static void pauseAll() {
        pauseAll(category -> true);
    }

    public static void pauseAll(Predicate<SoundCategory> categoryPredicate) {
        for (SoundInstance sound : sounds) {
            if (categoryPredicate.test(sound.getCategory()))
                sound.pause();
        }
    }

    public static void resumeAll() {
        resumeAll(category -> true);
    }

    public static void resumeAll(Predicate<SoundCategory> categoryPredicate) {
        for (SoundInstance sound : sounds) {
            if (categoryPredicate.test(sound.getCategory()))
                sound.play();
        }
    }

    public static int getSoundCount() {
        return getSoundCount(category -> true);
    }

    public static int getSoundCount(Predicate<SoundCategory> categoryPredicate) {
        int count = 0;
        for (SoundInstance sound : sounds) {
            if (categoryPredicate.test(sound.getCategory()))
                count++;
        }
        return count;
    }

    public static void updateVolumes(SoundCategory category) {
        //update the sounds volume
        for (SoundInstance sound : sounds) {
            if (category == SoundCategory.MASTER || sound.getCategory() == category)
                sound.volume(sound.getVolume());
        }
    }

    public static void swapDevice(String deviceName) {
        free();
        init(deviceName);
    }

    public static List<String> getDevices() {
        return devices;
    }

    public static String getCurrentDevice() {
        return currentDevice;
    }

    public static boolean isUsingDefaultDevice() {
        return useDefaultDevice;
    }

    public static String getALVersion() {
        return ALVersion;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
