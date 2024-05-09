package mayo.sound;

import mayo.render.Camera;
import mayo.utils.Maths;
import mayo.utils.Resource;
import org.joml.Vector3f;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;

import java.util.ArrayList;
import java.util.List;

import static mayo.Client.LOGGER;
import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL11.AL_LINEAR_DISTANCE_CLAMPED;
import static org.lwjgl.openal.ALC10.*;

public class SoundManager {

    private final List<SoundInstance> sounds = new ArrayList<>();
    private long context;
    private long device;
    private boolean initalized;

    public void init() {
        //initialize audio device
        String defaultDevice = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
        device = alcOpenDevice(defaultDevice);

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

        initalized = true;
    }

    public void free() {
        alcDestroyContext(context);
        alcCloseDevice(device);
        Sound.freeAllSounds();
    }

    static void checkALError() {
        int err = alGetError();
        if (err != AL_NO_ERROR)
            throw new RuntimeException("(" + err + ")" + alGetString(err));
    }

    private static void checkALCError(long device) {
        int err = alcGetError(device);
        if (err != ALC_NO_ERROR)
            throw new RuntimeException("(" + err + ")" + alcGetString(device, err));
    }

    public void tick(Camera camera) {
        if (!initalized)
            return;

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
            return;

        alListener3f(AL_POSITION, pos.x, pos.y, pos.z);
        alListenerfv(AL_ORIENTATION, new float[]{
                forward.x, forward.y, forward.z,
                up.x, up.y, up.z
        });
    }

    public SoundInstance playSound(Resource resource, SoundCategory category) {
        return this.playSound(resource, category, null);
    }

    public SoundInstance playSound(Resource resource, SoundCategory category, Vector3f position) {
        if (!initalized)
            return new SoundInstance(category);

        SoundSource source = new SoundSource(resource, category, position);
        sounds.add(source);
        source.play();
        return source;
    }

    public void stopAll() {
        for (SoundInstance sound : sounds)
            sound.stop();
    }

    public void pauseAll() {
        for (SoundInstance sound : sounds)
            sound.pause();
    }

    public void resumeAll() {
        for (SoundInstance sound : sounds)
            sound.play();
    }

    public int getSoundCount() {
        return sounds.size();
    }

    public void updateVolumes(SoundCategory category) {
        //update the sounds volume
        for (SoundInstance sound : sounds) {
            if (category == SoundCategory.MASTER || sound.getCategory() == category)
                sound.volume(sound.getVolume());
        }
    }
}
