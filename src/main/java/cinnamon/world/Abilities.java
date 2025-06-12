package cinnamon.world;

public class Abilities {

    private boolean
            godMode,
            canFly,
            noclip,
            canBuild = true;

    public Abilities godMode(boolean godMode) {
        this.godMode = godMode;
        return this;
    }

    public boolean godMode() {
        return godMode;
    }

    public Abilities canFly(boolean canFly) {
        this.canFly = canFly;
        return this;
    }

    public boolean canFly() {
        return canFly;
    }

    public Abilities noclip(boolean noclip) {
        this.noclip = noclip;
        return this;
    }

    public boolean noclip() {
        return noclip;
    }

    public Abilities canBuild(boolean canBuild) {
        this.canBuild = canBuild;
        return this;
    }

    public boolean canBuild() {
        return canBuild;
    }
}
