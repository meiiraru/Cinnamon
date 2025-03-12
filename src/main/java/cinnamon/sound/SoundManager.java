package cinnamon.sound;

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

import static cinnamon.Client.LOGGER;
import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL11.AL_LINEAR_DISTANCE_CLAMPED;
import static org.lwjgl.openal.AL11.alSpeedOfSound;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.ALC11.ALC_ALL_DEVICES_SPECIFIER;
import static org.lwjgl.openal.ALC11.ALC_DEFAULT_ALL_DEVICES_SPECIFIER;

public class SoundManager {

    private static final List<SoundInstance> sounds = new ArrayList<>();
    private static final List<String> devices = new ArrayList<>();
    private static String currentDevice = "";
    private static long context;
    private static long device;
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
        if (deviceName != null && devices.contains(deviceName)) {
            deviceToUse = deviceName;
            currentDevice = deviceName;
        } else {
            String defaultDevice = alcGetString(0, ALC_DEFAULT_ALL_DEVICES_SPECIFIER);
            deviceToUse = defaultDevice != null ? defaultDevice : newDevices.getFirst();
            currentDevice = "";
        }

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
        LOGGER.info("OpenAL version: %s", alcGetInteger(0, ALC_MAJOR_VERSION) + "." + alcGetInteger(0, ALC_MINOR_VERSION));
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
        LOGGER.debug("Playing sound: %s", resource.getPath());

        if (!initialized)
            return new SoundInstance(category);

        SoundSource source = new SoundSource(resource, category, position);
        sounds.add(source);
        source.play();
        return source;
    }

    public static void stopAll() {
        for (SoundInstance sound : sounds) {
            sound.stop();
            sound.free();
        }
        sounds.clear();
    }

    public static void pauseAll() {
        for (SoundInstance sound : sounds)
            sound.pause();
    }

    public static void resumeAll() {
        for (SoundInstance sound : sounds)
            sound.play();
    }

    public static int getSoundCount() {
        return sounds.size();
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

    public static boolean isInitialized() {
        return initialized;
    }
}
