package cinnamon.world.entity.living;

import cinnamon.Client;
import cinnamon.gui.screens.world.DeathScreen;
import cinnamon.registry.LivingModelRegistry;
import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.settings.Settings;
import cinnamon.utils.AABB;
import cinnamon.utils.Direction;
import cinnamon.vr.XrHandTransform;
import cinnamon.vr.XrManager;
import cinnamon.vr.XrRenderer;
import cinnamon.world.WorldObject;
import cinnamon.world.collisions.Hit;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.world.WorldClient;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class LocalPlayer extends Player {

    private int lastMouseTime = 0;
    private int selectedTerrain = TerrainRegistry.BOX.ordinal();
    private int selectedMaterial = MaterialRegistry.GRASS.ordinal();

    public LocalPlayer() {
        this(Settings.playermodel.get());
    }

    public LocalPlayer(LivingModelRegistry model) {
        super(Client.getInstance().name, Client.getInstance().playerUUID, model);
    }

    @Override
    public void tick() {
        super.tick();

        if (lastMouseTime > 0)
            lastMouseTime--;
    }

    @Override
    public void tickPhysics() {
        if (!super.isRemoved())
            super.tickPhysics();
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

    public int getSelectedTerrain() {
        return selectedTerrain;
    }

    public void setSelectedTerrain(int selectedTerrain) {
        this.selectedTerrain = selectedTerrain;
    }

    public int getSelectedMaterial() {
        return selectedMaterial;
    }

    public void setSelectedMaterial(int selectedMaterial) {
        this.selectedMaterial = selectedMaterial;
    }

    @Override
    public boolean attackAction() {
        if (lastMouseTime > 0)
            return false;

        if (super.attackAction()) {
            lastMouseTime = getInteractionDelay();
            return true;
        }

        if (!getAbilities().canBuild())
            return false;

        Hit<? extends WorldObject> hit = XrManager.isInXR() ? raycastHand(false, 1f, getPickRange()) : getLookingObject(getPickRange());
        if (hit != null && hit.obj() instanceof Terrain t) {
            getWorld().removeTerrain(t);
            lastMouseTime = getInteractionDelay();
            return true;
        }

        return false;
    }

    @Override
    public void stopAttacking() {
        super.stopAttacking();
        lastMouseTime = 0;
    }

    @Override
    public boolean useAction() {
        if (lastMouseTime > 0)
            return false;

        if (super.useAction()) {
            lastMouseTime = getInteractionDelay();
            return true;
        }

        if (!getAbilities().canBuild())
            return false;

        Hit<? extends WorldObject> hit = XrManager.isInXR() ? raycastHand(false, 1f, getPickRange()) : getLookingObject(getPickRange());
        if (hit != null && hit.obj() instanceof Terrain t) {
            Vector3f tpos = new Vector3f(hit.pos()).floor();
            if (tpos.equals(t.getPos()))
                tpos.add(hit.collision().normal());

            AABB entities = new AABB().translate(tpos).expand(1f, 1f, 1f);
            if (getWorld().getEntities(entities).isEmpty()) {
                Terrain tt = TerrainRegistry.values()[selectedTerrain].getFactory().get();
                tt.setMaterial(MaterialRegistry.values()[selectedMaterial]);
                tt.setRotation(Direction.fromRotation(getRot().y).invRotation);
                tt.setPos(tpos.x, tpos.y, tpos.z);
                getWorld().addTerrain(tt);

                lastMouseTime = getInteractionDelay();
                return true;
            }
        }

        return false;
    }

    @Override
    public void stopUsing() {
        super.stopUsing();
        lastMouseTime = 0;
    }

    public void pick() {
        if (!getAbilities().canBuild())
            return;

        Hit<? extends WorldObject> hit = getLookingObject(getPickRange());
        if (hit != null && hit.obj() instanceof Terrain t) {
            selectedTerrain = t.getType().ordinal();
            MaterialRegistry material = t.getMaterial();
            if (material != null) selectedMaterial = material.ordinal();
        }
    }

    @Override
    public void setSelectedItem(int index) {
        super.setSelectedItem(index);
        lastMouseTime = 0;
    }

    private int getInteractionDelay() {
        return getAbilities().godMode() ? 5 : 7;
    }

    @Override
    protected void onDeath() {
        super.onDeath();
        Client.getInstance().setScreen(new DeathScreen());
    }

    @Override
    public Vector3f getLookDir() {
        Vector3f dir = super.getLookDir();
        return XrManager.isInXR() ? dir.rotate(Client.getInstance().camera.getXrRot()) : dir;
    }

    @Override
    public Vector3f getLookDir(float delta) {
        Vector3f dir = super.getLookDir(delta);
        return XrManager.isInXR() ? dir.rotate(Client.getInstance().camera.getXrRot()) : dir;
    }

    public Vector3f getHandPos(boolean left, float delta) {
        if (!XrManager.isInXR())
            return super.getHandPos(left, delta);

        XrHandTransform transform = XrRenderer.getHandTransform(left ? 0 : 1);
        Vector3f tPos = new Vector3f(transform.pos());
        Quaternionf rot = getHandRot(left, delta).mul(new Quaternionf(transform.rot()).invert());
        tPos.rotate(rot);
        return getEyePos(delta).add(tPos);

    }

    public Quaternionf getHandRot(boolean left, float delta) {
        if (!XrManager.isInXR())
            return super.getHandRot(left, delta);

        XrHandTransform transform = XrRenderer.getHandTransform(left ? 0 : 1);
        Vector2f rot = getRot(delta);
        return new Quaternionf()
                .rotateY((float) Math.toRadians(-rot.y))
                .rotateX((float) Math.toRadians(-rot.x))
                .mul(transform.rot());
    }
}
