package mayo.sound;

import mayo.render.Camera;
import mayo.utils.Resource;
import org.joml.Vector3f;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL11.AL_LINEAR_DISTANCE_CLAMPED;
import static org.lwjgl.openal.ALC10.*;

public class SoundManager {

    private final List<SoundSource> sounds = new ArrayList<>();
    private long context;
    private long device;

    public void init() {
        //initialize audio device
        String defaultDevice = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
        device = alcOpenDevice(defaultDevice);
        checkALCError(device);

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
    }

    public void free() {
        alcDestroyContext(context);
        alcCloseDevice(device);
        Sound.freeAllSounds();
    }

    public static void checkALError() {
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
        //free stopped sounds
        for (SoundSource sound : sounds)
            if (sound.isStopped())
                sound.free();

        //remove sounds
        sounds.removeIf(SoundSource::isRemoved);

        //setup listener properties
        Vector3f pos = camera.getPos();
        Vector3f forward = camera.getForwards();
        Vector3f up = camera.getUp();

        alListener3f(AL_POSITION, pos.x, pos.y, pos.z);
        alListenerfv(AL_ORIENTATION, new float[]{
                forward.x, forward.y, forward.z,
                up.x, up.y, up.z
        });
    }

    public SoundSource playSound(Resource resource, SoundCategory category) {
        Sound sound = Sound.of(resource);
        SoundSource source = new SoundSource(sound, category);
        sounds.add(source);
        source.play();
        return source;
    }

    public SoundSource playSound(Resource resource, SoundCategory category, Vector3f position) {
        Sound sound = Sound.of(resource);
        SoundSource source = new SoundSource(sound, category, position);
        sounds.add(source);
        source.play();
        return source;
    }

    public void stopAll() {
        for (SoundSource sound : sounds)
            sound.stop();
    }

    public void pauseAll() {
        for (SoundSource sound : sounds)
            sound.pause();
    }

    public void resumeAll() {
        for (SoundSource sound : sounds)
            sound.play();
    }

    public int getSoundCount() {
        return sounds.size();
    }

    public void updateVolumes(SoundCategory category) {
        //update the sounds volume
        for (SoundSource sound : sounds) {
            if (category == SoundCategory.MASTER || sound.getCategory() == category)
                sound.volume(sound.getVolume());
        }
    }
}
