package cinnamon.sound;

import org.joml.Vector3f;

public class SoundInstance {

    private final SoundCategory category;
    private final Vector3f pos = new Vector3f();
    private float volume = 1f;
    private float pitch;
    private boolean loop;
    private float distance, maxDistance;
    private boolean removeOnStop = true;

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

    public Vector3f getPos() {
        return pos;
    }

    public SoundInstance pos(Vector3f pos) {
        return pos(pos.x, pos.y, pos.z);
    }

    public SoundInstance pos(float x, float y, float z) {
        this.pos.set(x, y, z);
        return this;
    }

    public float getPitch() {
        return pitch;
    }

    public SoundInstance pitch(float pitch) {
        this.pitch = pitch;
        return this;
    }

    public boolean hasLoop() {
        return loop;
    }

    public SoundInstance loop(boolean loop) {
        this.loop = loop;
        return this;
    }

    public float getVolume() {
        return volume;
    }

    public SoundInstance volume(float volume) {
        this.volume = volume;
        return this;
    }

    public float getDistance() {
        return distance;
    }

    public SoundInstance distance(float distance) {
        this.distance = distance;
        return this;
    }

    public float getMaxDistance() {
        return maxDistance;
    }

    public SoundInstance maxDistance(float maxDistance) {
        this.maxDistance = maxDistance;
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

    public boolean shouldRemoveOnStop() {
        return removeOnStop;
    }

    public SoundInstance removeOnStop(boolean removeOnStop) {
        this.removeOnStop = removeOnStop;
        return this;
    }
}
