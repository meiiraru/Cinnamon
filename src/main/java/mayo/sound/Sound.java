package mayo.sound;

import mayo.utils.IOUtils;
import mayo.utils.Resource;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBVorbisInfo;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL10.AL_FORMAT_STEREO16;
import static org.lwjgl.stb.STBVorbis.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Sound {

    private static final Map<Resource, Sound> SOUNDS_MAP = new HashMap<>();

    private final int id;

    private Sound(int id) {
        this.id = id;
    }

    public static Sound of(Resource resource) {
        Sound saved = SOUNDS_MAP.get(resource);
        if (saved != null)
            return saved;

        return cacheSound(resource, new Sound(loadSound(resource)));
    }

    private static Sound cacheSound(Resource resource, Sound sound) {
        SOUNDS_MAP.put(resource, sound);
        return sound;
    }

    private static int loadSound(Resource resource) {
        try (STBVorbisInfo info = STBVorbisInfo.malloc()) {
            //prepare buffers
            IntBuffer error = BufferUtils.createIntBuffer(1);

            //load sound file
            ByteBuffer soundBuffer = IOUtils.getResourceBuffer(resource);
            long decoder = stb_vorbis_open_memory(soundBuffer, error, null);
            if (decoder == NULL)
                throw new RuntimeException("Failed to open Ogg Vorbis file, error: " + error.get(0));

            //get up information about the sound
            stb_vorbis_get_info(decoder, info);

            //create a buffer for the sound
            int channels = info.channels();
            ShortBuffer pcm = BufferUtils.createShortBuffer(stb_vorbis_stream_length_in_samples(decoder) * channels);

            //save the sound in the buffer
            stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm);
            stb_vorbis_close(decoder);

            //register the buffer
            int id = alGenBuffers();

            alBufferData(id, channels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, pcm, info.sample_rate());
            SoundManager.checkALError();

            //return the buffer id
            return id;
        }
    }

    public static void freeAllSounds() {
        for (Sound s : SOUNDS_MAP.values())
            s.free();
    }

    public int getId() {
        return id;
    }

    public void free() {
        alDeleteBuffers(id);
    }
}
