package cinnamon.world.world;

import cinnamon.math.Maths;
import cinnamon.math.Rotation;
import cinnamon.math.collision.Hit;
import cinnamon.math.collision.shape.AABB;
import cinnamon.messages.MessageCategory;
import cinnamon.messages.MessageManager;
import cinnamon.model.GeometryHelper;
import cinnamon.model.ModelManager;
import cinnamon.model.Vertex;
import cinnamon.registry.EntityRegistry;
import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.TerrainModelRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.WaterRenderer;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.model.ModelRenderer;
import cinnamon.render.shader.Shader;
import cinnamon.sound.SoundCategory;
import cinnamon.sound.SoundManager;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Colors;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Pair;
import cinnamon.utils.Resource;
import cinnamon.world.Abilities;
import cinnamon.world.Decal;
import cinnamon.world.WorldObject;
import cinnamon.world.entity.DamageType;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import cinnamon.world.entity.collectable.EffectBox;
import cinnamon.world.entity.collectable.HealthPack;
import cinnamon.world.entity.collectable.ItemEntity;
import cinnamon.world.entity.living.Dummy;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.entity.living.Player;
import cinnamon.world.entity.misc.Firework;
import cinnamon.world.entity.misc.FireworkStar;
import cinnamon.world.entity.misc.Spawner;
import cinnamon.world.entity.misc.TriggerArea;
import cinnamon.world.entity.projectile.Brick;
import cinnamon.world.entity.vehicle.Cart;
import cinnamon.world.entity.vehicle.ShoppingCart;
import cinnamon.world.gui.Action;
import cinnamon.world.gui.ActionWheel;
import cinnamon.world.gui.Marker;
import cinnamon.world.items.*;
import cinnamon.world.items.weapons.CoilGun;
import cinnamon.world.items.weapons.NailGun;
import cinnamon.world.items.weapons.PotatoCannon;
import cinnamon.world.items.weapons.RiceGun;
import cinnamon.world.light.PointLight;
import cinnamon.world.light.Spotlight;
import cinnamon.world.particle.TextParticle;
import cinnamon.world.terrain.Button;
import cinnamon.world.terrain.ConveyorBelt;
import cinnamon.world.terrain.Glass;
import cinnamon.world.terrain.PlaneTerrain;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.worldgen.TerrainGenerator;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_B;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

public class PlaygroundWorld extends WorldClient {

    @Override
    protected void levelLoad() {
        this.enableDebugKeys = true;

        //load level
        int l = 32;
        TerrainGenerator.fill(this, -l, 0, -l, l, 0, l, MaterialRegistry.GRASS2.material);

        //TerrainGenerator.fill(this, -l, 1, -l,  l, 1, -l, MaterialRegistry.BRICK_WALL);
        //TerrainGenerator.fill(this, -l, 1,  l,  l, 1,  l, MaterialRegistry.BRICK_WALL);
        //TerrainGenerator.fill(this, -l, 1, -l, -l, 1,  l, MaterialRegistry.BRICK_WALL);
        //TerrainGenerator.fill(this,  l, 1, -l,  l, 1,  l, MaterialRegistry.BRICK_WALL);

        //0, 0
        Terrain t = TerrainRegistry.BOX.getFactory().get();
        t.setMaterial(MaterialRegistry.COBBLESTONE.material);
        removeTerrain(new AABB().translate(0.5f, 0.5f, 0.5f));
        addTerrain(t);

        //menger sponge
        TerrainGenerator.generateMengerSponge(this, 2, -23, 1, -23, MaterialRegistry.GOLD.material);
        TerrainGenerator.generateMengerSponge(this, 1, -11, 1, -17, MaterialRegistry.CHROME.material);

        //conveyor belt
        float beltSpeed = 0.15f;
        ConveyorBelt cb1 = new ConveyorBelt(beltSpeed);
        cb1.setPos(5, 1, 5);
        addTerrain(cb1);
        ConveyorBelt cb2 = new ConveyorBelt(beltSpeed);
        cb2.setPos(5, 1, 10);
        cb2.setRotation(Rotation.Y.rotationDeg(270f));
        addTerrain(cb2);
        ConveyorBelt cb3 = new ConveyorBelt(beltSpeed);
        cb3.setPos(0, 1, 10);
        cb3.setRotation(Rotation.Y.rotationDeg(180f));
        addTerrain(cb3);
        ConveyorBelt cb4 = new ConveyorBelt(beltSpeed);
        cb4.setPos(0, 1, 5);
        cb4.setRotation(Rotation.Y.rotationDeg(90f));
        addTerrain(cb4);

        //sphere
        Terrain t2 = TerrainRegistry.SPHERE.getFactory().get();
        t2.setMaterial(MaterialRegistry.GOLD.material);
        t2.setPos(15, 1, -3);
        addTerrain(t2);

        //roses
        for (float x = -29; x < -24; x += Maths.range(0.5f, 0.8f)) {
            for (float z = -4; z < 3; z += Maths.range(0.5f, 0.8f)) {
                Terrain rose = TerrainRegistry.ROSE.getFactory().get();
                rose.setPos(x, 1, z);
                rose.setRotation(Rotation.Y.rotationDeg((float) (Math.random() * 360)));
                addTerrain(rose);
            }
        }

        //torii gate
        Terrain torii = new Terrain(TerrainModelRegistry.TORII_GATE.resource, TerrainRegistry.CUSTOM);
        torii.setPos(38, 0.5f, -38);
        torii.setRotation(0, 45, 0);
        addTerrain(torii);

        //playSound(new Resource("sounds/song.ogg"), SoundCategory.MUSIC, new Vector3f(0, 0, 0)).loop(true);

        //lights
        setTime(1000L);

        addLight(new PointLight().pos(32.5f, 3.5f, 0.5f).color(0xFFFF44).volumetricStrength(0.5f));

        //rgb spotlights
        TerrainGenerator.fill(this, 4, 1, 24, 9, 3, 24, MaterialRegistry.COBBLESTONE2.material);

        TerrainGenerator.fill(this, -1, 1, 22, 14, 4, 22, MaterialRegistry.COBBLESTONE2.material);
        removeTerrain(new AABB(0, 1, 22, 13, 3, 22).translate(0.5f));
        TerrainGenerator.fill(this, 0, 1, 22, 13, 3, 22, Glass::new);

        float r = 0.75f;
        for (int i = 0; i < 3; i++) {
            float radi = Math.toRadians(120f) * i;
            float x = Math.sin(radi) * r;
            float y = Math.cos(radi) * r;
            addLight(new Spotlight().angle(15f).glareSize(1f).direction(0, 0, 1).pos(x + 7f, y + 2.5f, 21f).color(0xFF << (8 * i)));
        }

        //entities
        Cart c = new Cart(UUID.randomUUID());
        c.setPos(10, 2, 10);
        this.addEntity(c);

        Cart c2 = new Cart(UUID.randomUUID());
        c2.setPos(10, 2, 8);
        this.addEntity(c2);

        ShoppingCart s = new ShoppingCart(UUID.randomUUID());
        s.setPos(15, 2, 10);
        this.addEntity(s);

        ShoppingCart s2 = new ShoppingCart(UUID.randomUUID());
        s2.setPos(15, 2, 8);
        this.addEntity(s2);

        ShoppingCart s3 = new ShoppingCart(UUID.randomUUID());
        s3.setPos(15, 2, 6);
        this.addEntity(s3);

        Dummy d = new Dummy(UUID.randomUUID());
        d.setPos(-10, 2, 10);
        this.addEntity(d);

        Dummy d2 = new Dummy(UUID.randomUUID());
        d2.setPos(-15, 2, 10);
        this.addEntity(d2);

        Spawner<ItemEntity> brickSpawner = new Spawner<>(UUID.randomUUID(), 0f, 1, () -> {
            ItemEntity e = new ItemEntity(UUID.randomUUID(), new BrickItem(1));
            e.setPickUpDelay(0);
            return e;
        });
        brickSpawner.setPos(7f, 4f, 27f);
        brickSpawner.setRenderCooldown(true);
        this.addEntity(brickSpawner);

        Spawner<ItemEntity> potatoSpawner = new Spawner<>(UUID.randomUUID(), 0f, 1, () -> {
            ItemEntity e = new ItemEntity(UUID.randomUUID(), new PotatoItem(1));
            e.setPickUpDelay(0);
            return e;
        });
        potatoSpawner.setPos(-5.5f, 4f, 10f);
        potatoSpawner.setRenderCooldown(true);
        this.addEntity(potatoSpawner);

        Spawner<EffectBox> effectBox = new Spawner<>(UUID.randomUUID(), 0f, 100, () -> new EffectBox(UUID.randomUUID()));
        effectBox.setPos(-1.5f, 4f, 10f);
        effectBox.setRenderCooldown(true);
        this.addEntity(effectBox);

        Spawner<HealthPack> healthPack = new Spawner<>(UUID.randomUUID(), 0f, 100, () -> new HealthPack(UUID.randomUUID()));
        healthPack.setPos(2.5f, 4f, 10f);
        healthPack.setRenderCooldown(true);
        this.addEntity(healthPack);

        TriggerArea trigger = new TriggerArea(UUID.randomUUID(), 1f, 1f, 1f);
        trigger.setPos(32.5f, 1f, 0.5f);
        trigger.setStayTrigger(e -> {
            if (e instanceof LivingEntity living)
                living.damage(null, DamageType.TERRAIN, 10, false);
        });
        Vertex[][] spikes = GeometryHelper.cone(null, 32.5f, 1f, 0.5f, 1f, 0.5f, 12, 0xFFFF0000);
        trigger.addRenderFeature((src, camera, matrices, delta) -> VertexConsumer.WORLD_MAIN.consume(spikes));
        this.addEntity(trigger);

        //debug weapons
        spawnDebugWeapons();

        //test buttons
        Button btn1 = new Button();
        btn1.setPos(-18f, 1f, -25f);
        btn1.setOnPress(e -> addParticle(new TextParticle(Text.of("Beware..."), 60, btn1.getTransform().getPos())));
        btn1.setOnRelease(e -> addParticle(new TextParticle(Text.of("Brick!"), 60, btn1.getTransform().getPos())));
        addTerrain(btn1);

        Button btn2 = new Button();
        btn2.setPos(-20f, 1f, -25f);
        btn2.setOnPress(e -> {
            Brick b = new Brick(UUID.randomUUID(), null);
            Vector3f pos = e.getTransform().getPos();
            b.setPos(pos.x, pos.y + 3f, pos.z);
            //e.lookAt(b.getTransform().getPos());
            addEntity(b);
        });
        addTerrain(btn2);

        //ground plane
        addTerrain(new PlaneTerrain(0, 1, 0, 0.75f));

        //frog custom entity
        Entity frog = new PhysEntity(UUID.randomUUID(), null) {
            private static final String[] msg = {"purr...", "mrrr...", "ibbit!"};
            private static final Resource tex = new Resource("textures/misc/frog.png");
            private int petted = 0;
            private int jump = -1;

            @Override
            public void tick() {
                super.tick();
                if (petted > 0)
                    petted--;

                if (jump > 0)
                    jump--;
                if (jump <= 0) {
                    if (jump == 0) {
                        float pitch = -30 + Maths.range(-15, 15);
                        float yaw = Maths.range(0, 360);
                        rotateTo(0, yaw, 0);
                        getImpulse().add(Maths.rotToDir(pitch, yaw));
                    }
                    jump = Maths.range(60, 100);
                }

                scaleTo(1f, petted > 0 ? 0.8f : 1f, 1f);
            }

            @Override
            protected void renderModel(Camera camera, MatrixStack matrices, float delta) {
                VertexConsumer.WORLD_MAIN.consume(GeometryHelper.quad(matrices,  0.5f, 1f, -1f, -1f, 0f, 0f, 1f, 1f, 2, 1), tex);
                VertexConsumer.WORLD_MAIN.consume(GeometryHelper.quad(matrices, -0.5f, 1f,  1f, -1f, 1f, 0f, 1f, 1f, 2, 1), tex);
            }

            @Override
            public boolean onUse(LivingEntity source) {
                if (petted == 0) {
                    petted = 5;
                    TextParticle tp = new TextParticle(Text.of(Maths.randomArr(msg)).withStyle(Style.EMPTY.outlined(true)), 60, getAABB().getRandomPoint(new Vector3f()));
                    ((WorldClient) getWorld()).addParticle(tp);
                }
                return true;
            }

            @Override
            public EntityRegistry getType() {
                return EntityRegistry.UNKNOWN;
            }
        };
        frog.setPos(-5, 1, -5);
        addEntity(frog);
    }

    @Override
    public void renderWater(Camera camera, MatrixStack matrices, float delta) {
        super.renderWater(camera, matrices, delta);
        WaterRenderer.renderWaterPlane(camera, matrices, 0.9f, getSky().fogEnd);
    }

    @Override
    public void renderFire(Camera camera, MatrixStack matrices, float delta) {
        super.renderFire(camera, matrices, delta);

        matrices.pushMatrix();
        matrices.translate(0.5f, 2f, -16.5f);
        matrices.scale(2f);

        float time = getTime() + delta;

        ModelRenderer fire = ModelManager.getRenderer(new Resource("models/misc/cross.obj"));

        int len = 16;
        float r = 1.5f;
        for (int i = 0; i < len; i++) {
            float angle = Math.PI_TIMES_2_f / len * i;

            matrices.pushMatrix();
            matrices.translate(Math.sin(angle) * r, 0f, Math.cos(angle) * r);

            Shader.activeShader.setFloat("time", time * 0.03f + i);
            fire.renderWithoutMaterial(matrices);

            matrices.popMatrix();
        }

        fire.renderWithoutMaterial(matrices);
        matrices.popMatrix();
    }

    @Override
    public void keyPress(int key, int scancode, int action, int mods) {
        super.keyPress(key, scancode, action, mods);

        if (enableDebugKeys && key == GLFW_KEY_B) {
            if (action == GLFW_PRESS) {
                openOverlay(genDebugActionWheel());
            } else if (action == GLFW_RELEASE) {
                closeOverlay();
            }
        }
    }

    @Override
    public void respawn(boolean init) {
        super.respawn(init);
        playerEntity.setPos(0.5f, init ? 1.5f : 100f, 0.5f);
    }

    protected void spawnDebugWeapons() {
        Item[] items = {
                new CoilGun(30, 3, 100),
                new PotatoCannon(3, 60, 200),
                new RiceGun(8, 40, 150),
                new PaintGun(),
                new BubbleGun(),
                new Flashlight(0xFFFFCC),
                new MagicWand(),
                new NailGun(50, 3, 60),
        };

        for (int i = 0; i < items.length; i++) {
            ItemEntity item = new ItemEntity(UUID.randomUUID(), items[i]);
            item.setAge(-1);
            item.setPos(i * 2f + 0.5f, 2f, -8.5f);
            item.setPickUpDelay(0);
            this.addEntity(item);
        }
    }

    protected ActionWheel genDebugActionWheel() {
        ActionWheel aw = new ActionWheel();
        aw.setTitle(Text.of("Debug Actions"));

        //noclip
        Action noclip = new Action(Text.of("Toggle Noclip"), () -> {
            if (playerEntity instanceof Player player) {
                boolean value = !player.getAbilities().get(Abilities.Ability.NOCLIP);
                player.getAbilities().set(Abilities.Ability.NOCLIP, value);
                MessageManager.addMessage(Text.of("Noclip " + (value ? "enabled" : "disabled")), MessageCategory.SYSTEM, null);
            }
        });
        noclip.setIcon(new Resource("textures/gui/action_wheel/phase.png"));
        aw.addAction(noclip);

        //spray
        Action spray = new Action(Text.of("Spray"), () -> {
            Pair<Hit, Terrain> hit = playerEntity.getLookingTerrain(playerEntity.getPickRange());
            if (hit == null)
                return;

            Vector3f normal = hit.first().normal();
            Quaternionf rotation = Maths.dirToQuat(normal);
            if (Math.abs(normal.y) > 0.5f)
                rotation.rotateZ(Math.toRadians(-Maths.getYaw(playerEntity.getTransform().getRot()) * Math.signum(normal.y)));

            Resource folder = new Resource("textures/misc");
            List<String> resources = IOUtils.listResources(folder, false);
            resources.removeIf(s -> !s.endsWith(".png"));
            String res = resources.get((int) (Math.random() * resources.size()));

            Decal decal = new Decal(6000, folder.resolve(res));
            decal.getTransform()
                    .setPos(hit.first().position())
                    .setRot(rotation)
                    //.setRot(WorldRenderer.camera.getRotation())
                    .setScale(1f, 1f, 0.5f);
            addDecal(decal);
        });
        spray.setIcon(new Resource("textures/gui/action_wheel/spray.png"));
        aw.addAction(spray);

        //spawn weapons
        Action weapons = new Action(Text.of("Spawn Weapons"), this::spawnDebugWeapons);
        weapons.setIcon(new Resource("textures/gui/action_wheel/gun.png"));
        aw.addAction(weapons);

        //firework
        Action firework = new Action(Text.of("Spawn Firework"), () -> {
            Firework f = new Firework(UUID.randomUUID(), Maths.range(30, 60), Maths.spread(new Vector3f(0, 1f, 0), 30, 30).mul(2f),
                    new FireworkStar(
                            new Integer[]{0xFFa19f7f, 0xFFcfa959, 0xFF9b8136, 0xFF908264, 0xFFebc789, 0xFFb39b5b},
                            null,
                            true, true,
                            FireworkStar.Shape.BALL
                    ),
                    new FireworkStar(
                            new Integer[]{Colors.WHITE.argb},
                            null,
                            false, true,
                            FireworkStar.Shape.STAR
                    )
            );
            f.setPos(0, 1.5f, 0);
            addEntity(f);
        });
        firework.setIcon(new Resource("textures/gui/action_wheel/firework.png"));
        aw.addAction(firework);

        //marker
        Action marker = new Action(Text.of("Add Marker"), () -> {
            Pair<Hit, ? extends WorldObject> hit = playerEntity.getLookingObject(128f);
            if (hit != null) {
                SoundManager.playSound(Marker.MARKER_SND, SoundCategory.GUI);
                if (hit.second() instanceof Entity e) {
                    hud.addMarker(new Marker(e, null, 600, Colors.randomRainbow().argb));
                } else {
                    hud.addMarker(new Marker(hit.first().position(), null, 600, Colors.randomRainbow().argb));
                }
            }
        });
        marker.setIcon(new Resource("textures/gui/action_wheel/pin.png"));
        aw.addAction(marker);

        return aw;
    }
}
