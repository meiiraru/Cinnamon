package cinnamon.world.entity.living;

import cinnamon.Client;
import cinnamon.registry.LivingModelRegistry;
import cinnamon.render.Camera;
import cinnamon.settings.Settings;
import cinnamon.world.world.WorldClient;

public class LocalPlayer extends Player {

    public LocalPlayer() {
        this(Settings.playermodel.get());
    }

    public LocalPlayer(LivingModelRegistry model) {
        super(Client.getInstance().name, Client.getInstance().playerUUID, model);
    }

    @Override
    public void tickPhysics() {
        if (!super.isRemoved())
            super.tickPhysics();
    }

    @Override
    public boolean shouldRender(Camera camera) {
        return (camera.getEntity() != this || ((WorldClient) getWorld()).isThirdPerson()) && super.shouldRender(camera);
    }

    @Override
    protected void spawnDeathParticles() {
        if (((WorldClient) getWorld()).isThirdPerson())
            super.spawnDeathParticles();
    }

    @Override
    protected void spawnHealthChangeParticle(int amount, boolean crit) {
        if (((WorldClient) getWorld()).isThirdPerson())
            super.spawnHealthChangeParticle(amount, crit);
    }

    @Override
    public boolean isRemoved() {
        return false;
    }
}
