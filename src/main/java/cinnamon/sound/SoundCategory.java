package cinnamon.sound;

import cinnamon.math.Maths;

public enum SoundCategory {
    MASTER(0.5f),
    GUI,

    //world categories
    MUSIC,
    AMBIENT,
    WEATHER,
    ENTITY,
    TERRAIN,
    MISC;

    private float volume;

    SoundCategory() {
        this(1f);
    }

    SoundCategory(float volume) {
        this.volume = volume;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = Maths.clamp(volume, 0, 1);
        SoundManager.updateVolumes(this);
    }
}
