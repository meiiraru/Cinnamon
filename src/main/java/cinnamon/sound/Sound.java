package cinnamon.sound;

import cinnamon.utils.Resource;
import cinnamon.utils.SoundIO;

import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;

import static cinnamon.events.Events.LOGGER;
import static org.lwjgl.openal.AL10.*;

public class Sound {

    private static final Map<Resource, Sound> SOUNDS_MAP = new HashMap<>();

    public final int id, channels, sampleRate, duration;
    public final ShortBuffer buffer;

    protected Sound(int id, int channels, int sampleRate, int duration, ShortBuffer soundBuffer) {
        this.id = id;
        this.channels = channels;
        this.sampleRate = sampleRate;
        this.duration = duration;
        this.buffer = soundBuffer;
    }

    public static Sound of(Resource resource) {
        Sound saved = SOUNDS_MAP.get(resource);
        if (saved != null)
            return saved;

        return cacheSound(resource, loadSound(resource));
    }

    protected static Sound cacheSound(Resource resource, Sound sound) {
        SOUNDS_MAP.put(resource, sound);
        return sound;
    }

    protected static Sound loadSound(Resource resource) {
        String ext = resource.getExtension().toLowerCase();
        SoundIO.SoundData data = switch (ext) {
            case "ogg" -> SoundIO.loadOgg(resource);
            case "wav" -> SoundIO.loadWav(resource);
            default -> throw new RuntimeException("Unsupported sound file format: \"" + ext + "\" for resource: " + resource);
        };

        //register the buffer
        int id = alGenBuffers();
        alBufferData(id, data.channels() == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, data.buffer(), data.sampleRate());
        SoundManager.checkALError();

        //create the sound object
        LOGGER.debug("Loaded sound: \"%s\" (id: %s, channels: %s, sampleRate: %s, duration: %sms)", resource, id, data.channels(), data.sampleRate(), data.duration());
        return new Sound(id, data.channels(), data.sampleRate(), data.duration(), data.buffer().duplicate());
    }

    public static void freeAllSounds() {
        for (Sound s : SOUNDS_MAP.values())
            s.free();
        SOUNDS_MAP.clear();
    }

    public void free() {
        alDeleteBuffers(id);
        buffer.clear();
    }
}
