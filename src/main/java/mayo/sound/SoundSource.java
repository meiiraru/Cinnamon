package mayo.sound;

import org.joml.Vector3f;

import static org.lwjgl.openal.AL10.*;

public class SoundSource {

    private final int source;
    private final SoundCategory category;
    private boolean removed;
    private float volume = 1f;

    //sound creation
    private SoundSource(int buffer, SoundCategory category) {
        this.source = alGenSources();
        this.category = category;
        alSourcei(source, AL_BUFFER, buffer);
        SoundManager.checkALError();
    }

    //global position sound
    public SoundSource(Sound sound, SoundCategory category) {
        this(sound.getId(), category);
        alSourcei(source, AL_SOURCE_RELATIVE, AL_TRUE);
    }

    //world constructor
    public SoundSource(Sound sound, SoundCategory category, Vector3f pos) {
        this(sound.getId(), category);
        //default attenuation properties
        distance(0f).maxDistance(32f).volume(0.1f);
        //position
        pos(pos);
    }

    public void free() {
        alDeleteSources(source);
        removed = true;
    }

    public boolean isRemoved() {
        return removed;
    }

    public SoundSource play() {
        alSourcePlay(source);
        return this;
    }

    public SoundSource pause() {
        alSourcePause(source);
        return this;
    }

    public SoundSource stop() {
        alSourceStop(source);
        return this;
    }

    public boolean isPlaying() {
        return alGetSourcei(source, AL_SOURCE_STATE) == AL_PLAYING;
    }

    public boolean isPaused() {
        return alGetSourcei(source, AL_SOURCE_STATE) == AL_PAUSED;
    }

    public boolean isStopped() {
        return alGetSourcei(source, AL_SOURCE_STATE) == AL_STOPPED;
    }

    public SoundSource pos(Vector3f pos) {
        alSource3f(source, AL_POSITION, pos.x, pos.y, pos.z);
        return this;
    }

    public SoundSource pitch(float pitch) {
        alSourcef(source, AL_PITCH, pitch);
        return this;
    }

    public SoundSource loop(boolean loop) {
        alSourcei(source, AL_LOOPING, loop ? AL_TRUE : AL_FALSE);
        return this;
    }

    public SoundSource volume(float volume) {
        //save original volume
        this.volume = volume;

        //calculate new volume
        float vol = volume * category.getVolume();
        if (category != SoundCategory.MASTER)
            vol *= SoundCategory.MASTER.getVolume();

        //apply volume
        alSourcef(source, AL_GAIN, vol);
        return this;
    }

    public SoundSource distance(float distance) {
        alSourcef(source, AL_REFERENCE_DISTANCE, distance);
        return this;
    }

    public SoundSource maxDistance(float maxDistance) {
        alSourcef(source, AL_MAX_DISTANCE, maxDistance);
        return this;
    }

    public SoundCategory getCategory() {
        return category;
    }

    public float getVolume() {
        return volume;
    }
}
