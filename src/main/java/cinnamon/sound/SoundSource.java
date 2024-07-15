package cinnamon.sound;

import cinnamon.utils.Resource;
import org.joml.Vector3f;

import static org.lwjgl.openal.AL10.*;

public class SoundSource extends SoundInstance {

    private final int source;
    private boolean removed;

    private SoundSource(Resource resource, SoundCategory category) {
        super(category);
        this.source = alGenSources();

        int buffer = Sound.of(resource).getId();
        alSourcei(source, AL_BUFFER, buffer);

        SoundManager.checkALError();
    }

    public SoundSource(Resource resource, SoundCategory category, Vector3f pos) {
        this(resource, category);

        if (pos == null) { //global positioned sound
            alSourcei(source, AL_SOURCE_RELATIVE, AL_TRUE);
        } else { //world sound
            //default attenuation properties
            distance(0f).maxDistance(32f).volume(0.1f);
            //position
            pos(pos);
        }
    }

    @Override
    public void free() {
        alDeleteSources(source);
        removed = true;
    }

    @Override
    public boolean isRemoved() {
        return removed;
    }

    @Override
    public void play() {
        alSourcePlay(source);
    }

    @Override
    public void pause() {
        alSourcePause(source);
    }

    @Override
    public void stop() {
        alSourceStop(source);
    }

    @Override
    public boolean isPlaying() {
        return alGetSourcei(source, AL_SOURCE_STATE) == AL_PLAYING;
    }

    @Override
    public boolean isPaused() {
        return alGetSourcei(source, AL_SOURCE_STATE) == AL_PAUSED;
    }

    @Override
    public boolean isStopped() {
        return alGetSourcei(source, AL_SOURCE_STATE) == AL_STOPPED;
    }

    @Override
    public SoundSource pos(Vector3f pos) {
        alSource3f(source, AL_POSITION, pos.x, pos.y, pos.z);
        return this;
    }

    @Override
    public SoundSource pitch(float pitch) {
        alSourcef(source, AL_PITCH, pitch);
        return this;
    }

    @Override
    public SoundSource loop(boolean loop) {
        alSourcei(source, AL_LOOPING, loop ? AL_TRUE : AL_FALSE);
        return this;
    }

    @Override
    public SoundSource volume(float volume) {
        super.volume(volume);

        //calculate new volume
        SoundCategory category = getCategory();
        float vol = volume * category.getVolume();
        if (category != SoundCategory.MASTER)
            vol *= SoundCategory.MASTER.getVolume();

        //apply volume
        alSourcef(source, AL_GAIN, vol);
        return this;
    }

    @Override
    public SoundSource distance(float distance) {
        alSourcef(source, AL_REFERENCE_DISTANCE, distance);
        return this;
    }

    @Override
    public SoundSource maxDistance(float maxDistance) {
        alSourcef(source, AL_MAX_DISTANCE, maxDistance);
        return this;
    }
}
