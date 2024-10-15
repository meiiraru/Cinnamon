package cinnamon.sound;

import org.joml.Vector3f;

public class SoundInstance {

    private final SoundCategory category;
    private float volume = 1f;

    public SoundInstance(SoundCategory category) {
        this.category = category;
    }

    public void free() {}

    public boolean isRemoved() {
        return true;
    }

    public void play() {}

    public void pause() {}

    public void stop() {}

    public boolean isPlaying() {
        return false;
    }

    public boolean isPaused() {
        return false;
    }

    public boolean isStopped() {
        return true;
    }

    public SoundInstance pos(Vector3f pos) {
        return this;
    }

    public SoundInstance pitch(float pitch) {
        return this;
    }

    public SoundInstance loop(boolean loop) {
        return this;
    }

    public SoundInstance volume(float volume) {
        this.volume = volume;
        return this;
    }

    public SoundInstance distance(float distance) {
        return this;
    }

    public SoundInstance maxDistance(float maxDistance) {
        return this;
    }

    public SoundInstance setPlaybackTime(long millis) {
        return this;
    }

    public long getPlaybackTime() {
        return 0;
    }

    public SoundCategory getCategory() {
        return category;
    }

    public float getVolume() {
        return volume;
    }
}
