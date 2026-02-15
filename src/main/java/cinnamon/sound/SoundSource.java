package cinnamon.sound;

import cinnamon.utils.Resource;
import org.joml.Vector3f;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL11.AL_SEC_OFFSET;

public class SoundSource extends SoundInstance {

    private final int source;
    private boolean removed;

    private SoundSource(Resource resource, SoundCategory category) {
        super(category);
        this.source = alGenSources();

        int buffer = Sound.of(resource).id;
        alSourcei(source, AL_BUFFER, buffer);

        SoundManager.checkALError();

        //update volume to apply the category modifier
        volume(getVolume());
    }

    public SoundSource(Resource resource, SoundCategory category, Vector3f pos) {
        this(resource, category);

        if (pos == null) { //global positioned sound
            alSourcei(source, AL_SOURCE_RELATIVE, AL_TRUE);
        } else { //world sound
            //default attenuation properties
            distance(0f).maxDistance(32f);
            //position
            pos(pos);
        }
    }

    @Override
    public void free() {
        if (isRemoved())
            return;

        alDeleteSources(source);
        removed = true;
    }

    @Override
    public boolean isRemoved() {
        return removed;
    }

    @Override
    public void play() {
        if (!isRemoved())
            alSourcePlay(source);
    }

    @Override
    public void pause() {
        if (!isRemoved())
            alSourcePause(source);
    }

    @Override
    public void stop() {
        if (!isRemoved())
            alSourceStop(source);
    }

    @Override
    public boolean isPlaying() {
        return !isRemoved() && alGetSourcei(source, AL_SOURCE_STATE) == AL_PLAYING;
    }

    @Override
    public boolean isPaused() {
        return !isRemoved() && alGetSourcei(source, AL_SOURCE_STATE) == AL_PAUSED;
    }

    @Override
    public boolean isStopped() {
        return !isRemoved() && alGetSourcei(source, AL_SOURCE_STATE) == AL_STOPPED;
    }

    @Override
    public SoundSource pos(float x, float y, float z) {
        super.pos(x, y, z);
        if (!isRemoved())
            alSource3f(source, AL_POSITION, x, y, z);
        return this;
    }

    @Override
    public SoundSource pitch(float pitch) {
        super.pitch(pitch);
        if (!isRemoved())
            alSourcef(source, AL_PITCH, pitch);
        return this;
    }

    @Override
    public SoundSource loop(boolean loop) {
        super.loop(loop);
        if (!isRemoved())
            alSourcei(source, AL_LOOPING, loop ? AL_TRUE : AL_FALSE);
        return this;
    }

    @Override
    public SoundSource volume(float volume) {
        super.volume(volume);

        if (isRemoved())
            return this;

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
        super.distance(distance);
        if (!isRemoved())
            alSourcef(source, AL_REFERENCE_DISTANCE, distance);
        return this;
    }

    @Override
    public SoundSource maxDistance(float maxDistance) {
        super.maxDistance(maxDistance);
        if (!isRemoved())
            alSourcef(source, AL_MAX_DISTANCE, maxDistance);
        return this;
    }

    @Override
    public SoundSource setPlaybackTime(long millis) {
        if (!isRemoved())
            alSourcef(source, AL_SEC_OFFSET, millis / 1000f);
        return this;
    }

    @Override
    public long getPlaybackTime() {
        return isRemoved() ? 0 : (long) (alGetSourcef(source, AL_SEC_OFFSET) * 1000f);
    }
}
