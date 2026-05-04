package cinnamon.world.entity.living;

import cinnamon.Client;
import cinnamon.gui.Screen;
import cinnamon.math.Direction;
import cinnamon.math.Maths;
import cinnamon.math.collision.AABB;
import cinnamon.math.collision.Hit;
import cinnamon.model.material.Material;
import cinnamon.registry.LivingModelRegistry;
import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.render.WorldRenderer;
import cinnamon.settings.Settings;
import cinnamon.utils.Pair;
import cinnamon.vr.XrHandTransform;
import cinnamon.vr.XrManager;
import cinnamon.vr.XrRenderer;
import cinnamon.world.Abilities;
import cinnamon.world.WorldObject;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import cinnamon.world.entity.collectable.ItemEntity;
import cinnamon.world.items.Inventory;
import cinnamon.world.items.Item;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.world.WorldClient;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

public class LocalPlayer extends Player {

    protected int lastMouseTime = 0;
    protected int selectedTerrain = TerrainRegistry.BOX.ordinal();
    protected int selectedMaterial = MaterialRegistry.GRASS.ordinal();

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

        if (!getAbilities().get(Abilities.Ability.CAN_BUILD))
            return false;

        Pair<Hit, ? extends WorldObject> hit = XrManager.isInXR() ? raycastHand(getPickRange()) : getLookingObject(getPickRange());
        if (hit != null && hit.second() instanceof Terrain t && t.isSelectable(this)) {
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

        if (!getAbilities().get(Abilities.Ability.CAN_BUILD))
            return false;

        Pair<Hit, ? extends WorldObject> hit = XrManager.isInXR() ? raycastHand(getPickRange()) : getLookingObject(getPickRange());
        if (hit != null && hit.second() instanceof Terrain t) {
            Vector3f tpos = new Vector3f(hit.first().position()).floor();
            if (tpos.equals(t.getPos()))
                tpos.add(hit.first().normal());

            AABB entities = new AABB().translate(tpos).expand(1f, 1f, 1f);
            for (Entity entity : getWorld().getEntities(entities)) {
                if (entity instanceof PhysEntity)
                    return false;
            }

            Terrain tt = TerrainRegistry.values()[selectedTerrain].getFactory().get();
            tt.setMaterial(MaterialRegistry.values()[selectedMaterial].material);
            tt.setRotation(Direction.fromRotation(Maths.getYaw(getRot())).invRotation);
            tt.setPos(tpos.x, tpos.y, tpos.z);
            getWorld().addTerrain(tt);

            lastMouseTime = getInteractionDelay();
            return true;
        }

        return false;
    }

    @Override
    public void stopUsing() {
        super.stopUsing();
        lastMouseTime = 0;
    }

    public void pick() {
        if (!getAbilities().get(Abilities.Ability.CAN_BUILD))
            return;

        Pair<Hit, ? extends WorldObject> hit = getLookingObject(getPickRange());
        if (hit != null && hit.second() instanceof Terrain t && t.isSelectable(this) && t.getType() != TerrainRegistry.CUSTOM) {
            selectedTerrain = t.getType().ordinal();
            Material material = t.getMaterial();
            if (material != null) {
                MaterialRegistry mat = MaterialRegistry.findByMaterial(material);
                selectedMaterial = mat != null ? mat.ordinal() : MaterialRegistry.DEFAULT.ordinal();
            }
        }
    }

    public void dropItem() {
        Inventory inv = getInventory();
        Item i = inv.getSelectedItem();
        if (i == null)
            return;

        Item drop = i;
        int count = i.getCount();
        if (count > 1) {
            //reduce stack size
            Item copy = i.copy();
            copy.setCount(1);
            drop = copy;
            i.setCount(count - 1);
        } else {
            //remove from inventory
            inv.setItem(inv.getSelectedIndex(), null);
            i.unselect();
        }

        Vector3f dir = Maths.spread(getLookDir(), 12.5f, 5f).mul(0.5f);
        Vector3f dropPos = getEyePos();

        ItemEntity entity = new ItemEntity(UUID.randomUUID(), drop);
        entity.setPos(dropPos);
        entity.setMotion(dir);

        getWorld().addEntity(entity);
    }

    @Override
    public void setSelectedItem(int index) {
        super.setSelectedItem(index);
        lastMouseTime = 0;
    }

    private int getInteractionDelay() {
        return getAbilities().get(Abilities.Ability.GOD_MODE) ? 5 : 7;
    }

    @Override
    protected void onDeath() {
        super.onDeath();
        Screen death = ((WorldClient) getWorld()).deathScreen.get();
        if (death != null)
            Client.getInstance().setScreen(death);
    }

    @Override
    public Vector3f getLookDir() {
        Vector3f dir = super.getLookDir();
        return XrManager.isInXR() ? dir.rotate(WorldRenderer.camera.getXrRot()) : dir;
    }

    @Override
    public Vector3f getLookDir(float delta) {
        Vector3f dir = super.getLookDir(delta);
        return XrManager.isInXR() ? dir.rotate(WorldRenderer.camera.getXrRot()) : dir;
    }

    @Override
    public Vector3f getHandPos(boolean lefty, float delta) {
        if (!XrManager.isInXR())
            return super.getHandPos(lefty, delta);

        XrHandTransform transform = XrRenderer.getHandTransform(lefty ? 0 : 1);
        Vector3f tPos = new Vector3f(transform.pos());
        Quaternionf rot = getHandRot(lefty, delta).mul(new Quaternionf(transform.rot()).invert());
        tPos.rotate(rot);
        return getEyePos(delta).add(tPos);

    }

    @Override
    public Quaternionf getHandRot(boolean lefty, float delta) {
        if (!XrManager.isInXR())
            return super.getHandRot(lefty, delta);

        XrHandTransform transform = XrRenderer.getHandTransform(lefty ? 0 : 1);
        return new Quaternionf(getRot(delta)).mul(transform.rot());
    }

    @Override
    public Vector3f getAimDir(boolean lefty, float delta, float range) {
        return XrManager.isInXR() ? getHandDir(lefty, delta) : super.getAimDir(lefty, delta, range);
    }

    @Override
    public boolean shouldRenderOutline() {
        return true;
    }

    @Override
    public int getOutlineColor() {
        return super.shouldRenderOutline() ? super.getOutlineColor() : 0xFF000000;
    }
}
