package mayo.sound;

import org.joml.Vector3f;

import static org.lwjgl.openal.AL10.*;

public class SoundSource {

    private final int source;
    private boolean removed;

    //sound creation
    private SoundSource(int buffer) {
        this.source = alGenSources();
        alSourcei(source, AL_BUFFER, buffer);
        SoundManager.checkALError();
    }

    //global position sound
    public SoundSource(Sound sound) {
        this(sound.getId());
        alSourcei(source, AL_SOURCE_RELATIVE, AL_TRUE);
    }

    //world constructor
    public SoundSource(Sound sound, Vector3f pos) {
        this(sound.getId());
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
        alSourcef(source, AL_GAIN, volume);
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

}
