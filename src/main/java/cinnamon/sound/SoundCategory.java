package cinnamon.sound;

import cinnamon.math.Maths;

public enum SoundCategory {
    MASTER,
    GUI,

    //world categories
    MUSIC,
    AMBIENT,
    WEATHER,
    ENTITY,
    TERRAIN,
    MISC;

    private float volume = 1f;

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = Maths.clamp(volume, 0, 1);
        SoundManager.updateVolumes(this);
    }
}
