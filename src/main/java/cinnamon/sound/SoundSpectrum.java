package cinnamon.sound;

import cinnamon.utils.FFT;
import cinnamon.utils.UIHelper;
import org.joml.Math;

import java.nio.ShortBuffer;

public class SoundSpectrum {

    private final int fftSize, bars, maxFrequency;
    private final FFT fft;
    private final float[] amplitudes;

    public SoundSpectrum() {
        this(1024, 20, 16000);
    }

    public SoundSpectrum(int fftSize, int bars, int maxFrequency) {
        this.fftSize = fftSize;
        this.bars = bars;
        this.maxFrequency = maxFrequency;
        this.fft = new FFT(fftSize);
        this.amplitudes = new float[bars];
    }

    public float[] getSoundSamples(Sound sound, SoundInstance soundData) {
        if (sound == null || soundData == null)
            return null;

        //convert current time to sample index
        int startIndex = (int) ((soundData.getPlaybackTime() / 1000f) * sound.sampleRate * sound.channels);

        //make sure the index does not exceed the buffer capacity
        if (startIndex + fftSize * sound.channels > sound.buffer.capacity())
            startIndex = sound.buffer.capacity() - fftSize * sound.channels;

        //get a slice of the buffer starting from the correct sample
        ShortBuffer slicedBuffer = sound.buffer.duplicate();
        slicedBuffer.position(startIndex);
        slicedBuffer.limit(startIndex + fftSize * sound.channels);

        ShortBuffer slice = slicedBuffer.slice();

        //extract samples from the slice
        int numSamples = slice.remaining() / sound.channels;
        float[] samples = new float[numSamples];

        //only take samples for one channel (assuming stereo)
        for (int i = 0; i < numSamples; i++) {
            //take the first channel only
            samples[i] = slice.get(i * sound.channels) / (float) Short.MAX_VALUE;
        }

        return samples;
    }

    public void updateAmplitudes(Sound sound, SoundInstance soundData, boolean smooth) {
        //set the amplitudes all back to 0
        for (int i = 0; i < amplitudes.length; i++)
            amplitudes[i] = smooth ? Math.lerp(amplitudes[i], 0f, UIHelper.tickDelta(0.6f)) : 0f;

        //get sound samples
        float[] soundSamples = getSoundSamples(sound, soundData);
        if (soundSamples == null)
            return;

        //apply hann window to smooth the spectrum before applying FFT
        int length = soundSamples.length;
        for (int i = 0; i < length; i++)
            soundSamples[i] *= 0.5f * (1 - Math.cos((Math.PI_TIMES_2_f * i) / (length - 1)));

        //apply fast fourier transform
        fft.realForward(soundSamples);

        //get amplitudes
        int maxFreqBin = (int) (maxFrequency / (sound.sampleRate * 0.5f) * (length * 0.5f));
        for (int i = 0; i < bars; i++) {
            int lowIndex = i * maxFreqBin / bars;
            int highIndex = Math.min((i + 1) * maxFreqBin / bars - 1, length / 2 - 1);

            float sum = 0;
            int count = 0;

            //get magnitude from FFT data (real and imaginary part)
            for (int j = lowIndex; j <= highIndex; j++) {
                float real = soundSamples[j * 2];
                float imag = soundSamples[j * 2 + 1];
                float magnitude = Math.sqrt(real * real + imag * imag);
                sum += magnitude;
                count++;
            }

            //average amplitude for the frequency band
            float amplitude = (count > 0 ? sum / count : 0);
            amplitudes[i] = Math.max(amplitude, amplitudes[i]);
        }
    }

    public float[] getAmplitudes() {
        return amplitudes;
    }

    public int getMaxFrequency() {
        return maxFrequency;
    }

    public int getFftSize() {
        return fftSize;
    }
}
