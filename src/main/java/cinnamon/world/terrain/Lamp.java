package cinnamon.world.terrain;

import cinnamon.registry.TerrainModelRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.world.light.Light;
import cinnamon.world.light.PointLight;
import cinnamon.world.world.WorldClient;
import org.joml.Vector3f;

public class Lamp extends Terrain {

    protected final Light light = new PointLight().falloff(1.5f, 3f).intensity(2f).shadowIntensity(0f).volumetricStrength(0f).color(0xFFFFE6B3);

    public Lamp() {
        super(TerrainModelRegistry.LAMP.resource, TerrainRegistry.LAMP);
    }

    @Override
    public void onUpdated() {
        super.onUpdated();
        ((WorldClient) world).addLight(light);
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        ((WorldClient) getWorld()).removeLight(light);
    }

    @Override
    public void setPos(float x, float y, float z) {
        super.setPos(x, y, z);
        Vector3f center = this.aabb.getCenter();
        light.pos(center.x, y + 0.85f * getTransform().getScale().y, center.z);
    }

    @Override
    public void setScale(float x, float y, float z) {
        super.setScale(x, y, z);
        ((PointLight) light).falloff(1.5f * x, 3f * x);
    }

    public Light getLight() {
        return light;
    }
}
