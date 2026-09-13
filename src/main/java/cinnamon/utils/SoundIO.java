package cinnamon.utils;

import cinnamon.sound.Sound;
import cinnamon.sound.SoundSource;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBVorbisInfo;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Path;

import static org.lwjgl.stb.STBVorbis.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class SoundIO {

    public static void saveSound(SoundSource sound, Path outputPath) {
        saveSound(sound.getSound(), outputPath);
    }

    public static void saveSound(Sound sound, Path outputPath) {
        try {
            //prepare the buffer for reading
            sound.buffer.rewind();

            //convert ShortBuffer back into a byte array (short = 2 bytes)
            int byteCount = sound.buffer.remaining() * 2;
            ByteBuffer byteBuf = ByteBuffer.allocate(byteCount);

            //we must use the native byte order to match how the ShortBuffer was holding the data
            boolean bigEndian = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
            byteBuf.order(ByteOrder.nativeOrder());

            //quickly copy all shorts into our byte buffer
            byteBuf.asShortBuffer().put(sound.buffer);
            byte[] audioBytes = byteBuf.array();

            //define the AudioFormat (16-bit signed PCM)
            AudioFormat format = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    sound.sampleRate,
                    16, //16-bit
                    sound.channels,
                    sound.channels * 2, //frame size (channels * 2 bytes)
                    sound.sampleRate,
                    bigEndian
            );

            //write the file using AudioSystem
            try (ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
                 AudioInputStream ais = new AudioInputStream(bais, format, audioBytes.length / format.getFrameSize())) {

                //AudioSystem handles the WAV file creation and header generation
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outputPath.toFile());
            }

            //rewind the buffer again to allow for future reads
            sound.buffer.rewind();

        } catch (Exception e) {
            throw new RuntimeException("Failed to save WAV file to \"" + outputPath + "\"", e);
        }
    }

    public static SoundData loadOgg(Resource resource) {
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
            int sampleRate = info.sample_rate();

            //create a buffer for the sound
            int channels = info.channels();
            ShortBuffer pcm = BufferUtils.createShortBuffer(stb_vorbis_stream_length_in_samples(decoder) * channels);

            //save the sound in the buffer
            stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm);
            stb_vorbis_close(decoder);

            //calculate the duration in milliseconds
            int duration = (int) (pcm.capacity() / (float) sampleRate * 1000f / (float) channels);

            //return the sound data
            return new SoundData(channels, sampleRate, duration, pcm);
        }
    }

    public static SoundData loadWav(Resource resource) {
        try {
            //load resource into a byte array
            ByteBuffer buffer = IOUtils.getResourceBuffer(resource);
            byte[] rawBytes = new byte[buffer.remaining()];
            buffer.get(rawBytes);

            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(rawBytes); AudioInputStream stream = AudioSystem.getAudioInputStream(inputStream)) {
                AudioFormat sourceFormat = stream.getFormat();

                //OpenAL expects 16-bit PCM data in the system native byte order
                boolean bigEndian = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
                AudioFormat targetFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        sourceFormat.getSampleRate(),
                        16, //standardize to 16-bit
                        sourceFormat.getChannels(),
                        sourceFormat.getChannels() * 2, //frame size (2 bytes per sample per channel)
                        sourceFormat.getSampleRate(),
                        bigEndian
                );

                //convert the audio stream to pcm format
                AudioInputStream pcmStream = AudioSystem.getAudioInputStream(targetFormat, stream);
                byte[] audioBytes = pcmStream.readAllBytes();
                pcmStream.close();

                //load into the buffer
                ByteBuffer directBuffer = BufferUtils.createByteBuffer(audioBytes.length);
                directBuffer.put(audioBytes);
                directBuffer.flip();

                //endianness is handled by the AudioSystem, so can safely cast to ShortBuffer
                ShortBuffer pcm = directBuffer.asShortBuffer();

                //calculate the duration in milliseconds
                int channels = targetFormat.getChannels();
                int sampleRate = (int) targetFormat.getSampleRate();
                int duration = (int) (pcm.capacity() / (float) sampleRate * 1000f / (float) channels);

                //return the sound data
                return new SoundData(channels, sampleRate, duration, pcm);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load WAV file \"" + resource + "\"", e);
        }
    }

    public record SoundData(int channels, int sampleRate, int duration, ShortBuffer buffer) {}
}
