package cinnamon.sound;

public enum SoundCategory {
    MASTER,
    GUI,
    MUSIC,
    AMBIENT,
    WEATHER,
    ENTITY,
    MISC;

    private float volume = 1f;

    public float getVolume() {
        return volume;
    }

    public void setVolume(SoundManager manager, float volume) {
        this.volume = Math.clamp(volume, 0, 1);
        if (manager != null) manager.updateVolumes(this);
    }
}
