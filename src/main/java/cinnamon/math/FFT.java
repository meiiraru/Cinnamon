package cinnamon.math;

import org.joml.Math;

/**
 * A simple implementation of the Fast Fourier Transform (FFT) algorithm for real-valued input data
 */
public class FFT {

    private final int n;
    private final float[] cosTable, sinTable;
    private final float[] real, imag;

    /**
     * Constructs an FFT instance with the specified size<br>
     * The size must be a power of 2
     * @param size the size of the FFT
     */
    public FFT(int size) {
        if (size <= 0 || (size & (size - 1)) != 0)
            throw new IllegalArgumentException("FFT size must be a power of 2");

        this.n = size;
        this.cosTable = new float[n / 2];
        this.sinTable = new float[n / 2];

        for (int i = 0; i < n / 2; i++) {
            cosTable[i] = Math.cos(Math.PI_TIMES_2_f * i / n);
            sinTable[i] = Math.sin(Math.PI_TIMES_2_f * i / n);
        }

        this.real = new float[n];
        this.imag = new float[n];
    }

    /**
     * Performs the FFT on the given real-valued data array<br>
     * The input array must have a length equal to the FFT size<br>
     * The output will be stored in the same array, with the real and imaginary parts interleaved<br>
     * {@code {real[0], imag[0], real[1], imag[1], ..., real[n/2-1], imag[n/2-1]}}
     * @param data the input data array
     */
    public void realForward(float[] data) {
        if (data == null || data.length != n)
            throw new IllegalArgumentException("Data length must match FFT size");

        System.arraycopy(data, 0, real, 0, n);
        for (int i = 0; i < n; i++)
            imag[i] = 0f;

        transform(real, imag);

        for (int i = 0; i < n / 2; i++) {
            data[i * 2]     = real[i];
            data[i * 2 + 1] = imag[i];
        }
    }

    //based on the FFT implementation from Project Nayuki
    private void transform(float[] real, float[] imag) {
        //bit-reversal permutation
        int levels = 31 - Integer.numberOfLeadingZeros(n);
        for (int i = 0; i < n; i++) {
            int j = Integer.reverse(i) >>> (32 - levels);
            if (j > i) {
                float tr = real[i]; real[i] = real[j]; real[j] = tr;
                float ti = imag[i]; imag[i] = imag[j]; imag[j] = ti;
            }
        }

        //Cooley-Tukey FFT
        for (int size = 2; size <= n; size *= 2) {
            int halfSize = size / 2;
            int tableStep = n / size;
            for (int i = 0; i < n; i += size) {
                for (int j = i, k = 0; j < i + halfSize; j++, k += tableStep) {
                    float tpre =  real[j + halfSize] * cosTable[k] + imag[j + halfSize] * sinTable[k];
                    float tpim = -real[j + halfSize] * sinTable[k] + imag[j + halfSize] * cosTable[k];
                    real[j + halfSize] = real[j] - tpre;
                    imag[j + halfSize] = imag[j] - tpim;
                    real[j] += tpre;
                    imag[j] += tpim;
                }
            }
        }
    }

    /**
     * Gets the size of the FFT
     * @return the size of the FFT
     */
    public int size() {
        return n;
    }
}