package cinnamon.sound;

import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBVorbisInfo;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.stb.STBVorbis.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Sound {

    private static final Map<Resource, Sound> SOUNDS_MAP = new HashMap<>();

    public final int id, channels, sampleRate, duration;
    private final ShortBuffer buffer;

    private Sound(int id, int channels, int sampleRate, int duration, ShortBuffer soundBuffer) {
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

    private static Sound cacheSound(Resource resource, Sound sound) {
        SOUNDS_MAP.put(resource, sound);
        return sound;
    }

    private static Sound loadSound(Resource resource) {
        try (STBVorbisInfo info = STBVorbisInfo.malloc()) {
            //prepare buffers
            IntBuffer error = BufferUtils.createIntBuffer(1);

            //load sound file
            ByteBuffer soundBuffer;
            if (resource.getNamespace().isEmpty()) {
                    byte[] bytes = IOUtils.readFile(Path.of(resource.getPath()));
                    if (bytes == null)
                        throw new RuntimeException("Failed to read file: " + resource.getPath());
                    soundBuffer = IOUtils.getBufferForStream(new ByteArrayInputStream(bytes));
            } else {
                soundBuffer = IOUtils.getResourceBuffer(resource);
            }

            long decoder = stb_vorbis_open_memory(soundBuffer, error, null);
            if (decoder == NULL)
                throw new RuntimeException("Failed to open Ogg Vorbis file, error: " + error.get(0));

            //get up information about the sound
            stb_vorbis_get_info(decoder, info);
            int sampleRate = info.sample_rate();

            //create a buffer for the sound
            int channels = info.channels();
            ShortBuffer pcm = BufferUtils.createShortBuffer(stb_vorbis_stream_length_in_samples(decoder) * channels);

            //save the sound in the buffer
            stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm);
            stb_vorbis_close(decoder);

            //register the buffer
            int id = alGenBuffers();

            alBufferData(id, channels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, pcm, sampleRate);
            SoundManager.checkALError();

            //calculate the duration of the sound in milliseconds
            int duration = (int) (pcm.capacity() / (float) sampleRate * 1000 / channels);

            //copy the sound buffer for storage
            ShortBuffer pcmCopy = BufferUtils.createShortBuffer(pcm.capacity());
            pcmCopy.put(pcm).flip();

            //return the buffer id
            return new Sound(id, channels, sampleRate, duration, pcmCopy);
        }
    }

    public static void freeAllSounds() {
        for (Sound s : SOUNDS_MAP.values())
            s.free();
        SOUNDS_MAP.clear();
    }

    public ShortBuffer getBuffer() {
        return buffer;
    }

    public void free() {
        alDeleteBuffers(id);
        buffer.clear();
    }
}
