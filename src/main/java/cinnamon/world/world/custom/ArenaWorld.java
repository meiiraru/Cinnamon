package cinnamon.world.world.custom;

import cinnamon.math.MatrixStack;
import cinnamon.model.GeometryHelper;
import cinnamon.registry.MaterialRegistry;
import cinnamon.render.Camera;
import cinnamon.render.WaterRenderer;
import cinnamon.world.ai.AIBehaviour;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.collectable.ItemEntity;
import cinnamon.world.entity.living.Enemy;
import cinnamon.world.entity.misc.Spawner;
import cinnamon.world.items.BubbleGun;
import cinnamon.world.items.Flashlight;
import cinnamon.world.items.Item;
import cinnamon.world.items.PaintGun;
import cinnamon.world.items.melee.Hammer;
import cinnamon.world.items.weapons.CoilGun;
import cinnamon.world.items.weapons.NailGun;
import cinnamon.world.items.weapons.PotatoCannon;
import cinnamon.world.items.weapons.RiceGun;
import cinnamon.world.terrain.PlaneTerrain;
import cinnamon.world.terrain.PrimitiveTerrain;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.world.WorldClient;
import org.joml.Math;

import java.util.UUID;

public class ArenaWorld extends WorldClient {

    @Override
    protected void levelLoad() {
        this.enableDebugKeys = true;

        int l = 48;
        Terrain platform = new PrimitiveTerrain(GeometryHelper.box(null, -l, 0, -l, l, 1, l, 0xFFFFFFFF)) {
            @Override
            public boolean canBreak(Entity entity) { return false; }
            @Override
            public boolean explode(float explosionStrength) { return false; }
        };
        platform.setMaterial(MaterialRegistry.TERRAZZO_SLAB.getMaterial());
        addTerrain(platform);

        //walls
        l -= 4;
        addTerrain(new PlaneTerrain( 1, 0,  0, -l));
        addTerrain(new PlaneTerrain(-1, 0,  0, -l));
        addTerrain(new PlaneTerrain( 0, 0,  1, -l));
        addTerrain(new PlaneTerrain( 0, 0, -1, -l));

        //zombie spawner
        l -= 4;
        Spawner<Enemy> spawner = new Spawner<>(UUID.randomUUID(), 70, () -> new Enemy(UUID.randomUUID(), 0, AIBehaviour.LOOK, AIBehaviour.MOVE), e -> true);
        spawner.setExtents(l, 0.25f, l);
        spawner.setPos(0f, 5.25f, 0f);
        addEntity(spawner);

        //items
        Item[] items = {
                new CoilGun(30, 3, 100),
                new PotatoCannon(3, 60, 200),
                new RiceGun(8, 40, 150),
                new NailGun(50, 3, 60),
                new PaintGun(),
                new BubbleGun(),
                new Flashlight(0xFFFFCC),
                new Hammer(),
        };

        float angle = 360f / items.length;
        float radius = 3f;

        for (int i = 0; i < items.length; i++) {
            ItemEntity item = new ItemEntity(UUID.randomUUID(), items[i]);
            item.setAge(-1);
            item.setPickUpDelay(0);

            float rad = Math.toRadians(angle * i);
            item.setPos(Math.cos(rad) * radius, 1.5f, Math.sin(rad) * radius);

            addEntity(item);
        }
    }

    public void renderExtras(Camera camera, MatrixStack matrices, float delta) {
        super.renderExtras(camera, matrices, delta);

        //water plane
        WaterRenderer.bindRenderer();
        WaterRenderer.renderWaterPlane(camera, matrices, 0.9f, getSky().fogEnd);
    }

    @Override
    public void respawn(boolean init) {
        super.respawn(init);
        playerEntity.setPos(0.5f, 1.5f, 0.5f);
    }
}
