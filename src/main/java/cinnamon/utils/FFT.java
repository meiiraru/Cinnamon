package cinnamon.utils;

import org.joml.Math;

public class FFT {

    private final int n;
    private final float[] cosTable, sinTable;
    private final float[] real, imag;

    public FFT(int size) {
        if (size <= 0 || (size & (size - 1)) != 0)
            throw new IllegalArgumentException("FFT size must be a power of 2");

        this.n = size;
        this.cosTable = new float[n / 2];
        this.sinTable = new float[n / 2];

        for (int i = 0; i < n / 2; i++) {
            cosTable[i] = (float) Math.cos(2f * Math.PI * i / n);
            sinTable[i] = (float) Math.sin(2f * Math.PI * i / n);
        }

        this.real = new float[n];
        this.imag = new float[n];
    }

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

    public int size() {
        return n;
    }
}