package mayo.world;

import mayo.Client;
import mayo.gui.Toast;
import mayo.gui.screens.DeathScreen;
import mayo.gui.screens.PauseScreen;
import mayo.input.Movement;
import mayo.model.GeometryHelper;
import mayo.render.Camera;
import mayo.render.MatrixStack;
import mayo.render.batch.VertexConsumer;
import mayo.render.framebuffer.Blit;
import mayo.render.framebuffer.Framebuffer;
import mayo.render.shader.Shader;
import mayo.render.shader.Shaders;
import mayo.sound.SoundCategory;
import mayo.sound.SoundManager;
import mayo.sound.SoundSource;
import mayo.text.Text;
import mayo.utils.AABB;
import mayo.utils.Maths;
import mayo.utils.Resource;
import mayo.world.chunk.Chunk;
import mayo.world.collisions.CollisionDetector;
import mayo.world.collisions.CollisionResult;
import mayo.world.collisions.Hit;
import mayo.world.entity.Entity;
import mayo.world.entity.PhysEntity;
import mayo.world.entity.collectable.EffectBox;
import mayo.world.entity.collectable.HealthPack;
import mayo.world.entity.living.Enemy;
import mayo.world.entity.living.Player;
import mayo.world.items.Item;
import mayo.world.items.ItemRenderContext;
import mayo.world.items.MagicWand;
import mayo.world.items.weapons.CoilGun;
import mayo.world.items.weapons.PotatoCannon;
import mayo.world.items.weapons.RiceGun;
import mayo.world.items.weapons.Weapon;
import mayo.world.light.DirectionalLight;
import mayo.world.light.Light;
import mayo.world.light.Spotlight;
import mayo.world.particle.ExplosionParticle;
import mayo.world.particle.Particle;
import mayo.world.terrain.Terrain;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL13.*;

public class World {

    private static final Resource EXPLOSION_SOUND = new Resource("sounds/explosion.ogg");

    public final Hud hud = new Hud();
    private final List<Runnable> scheduledTicks = new ArrayList<>();

    private final List<Terrain> terrain = new ArrayList<>();
    private final List<Entity> entities = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<Light> lights = new ArrayList<>();

    private final SkyBox skyBox = new SkyBox();

    private final Movement movement = new Movement();
    public Player player;
    private int cameraMode = 0;
    private boolean attackPress, usePress;

    private boolean debugRendering, renderShadowMap;
    private boolean isPaused;
    private boolean hideHUD;

    public final float updateTime = 0.05f; // 1/20
    public final float gravity = 0.98f * updateTime;

    public final float renderDistance = 3;
    private int timeOfTheDay = 0;

    //temp
    private final Spotlight flashlight = (Spotlight) new Spotlight().cutOff(25f, 45f).brightness(64);
    private final Framebuffer shadowBuffer = new Framebuffer(2048, 2048, Framebuffer.DEPTH_BUFFER);
    private final Matrix4f lightSpaceMatrix = new Matrix4f();
    private final DirectionalLight sunLight = new DirectionalLight();

    public void init() {
        //set client
        Client client = Client.getInstance();
        client.setScreen(null);
        client.world = this;

        //init hud
        hud.init();

        //add player
        respawn();

        //tutorial toast
        Toast.addToast(Text.of("WASD - move\nR - reload\nMouse - look around\nLeft Click - attack\nF3 - debug\nF5 - third person"), Client.getInstance().font);

        //load level
        LevelLoad.load(this, new Resource("data/levels/level0.json"));

        //playSound(new Resource("sounds/song.ogg"), SoundCategory.MUSIC, new Vector3f(0, 0, 0)).loop(true);
        //rip for-loop
        addLight(sunLight);
        addLight(new Light().pos(-5.5f, 0.5f, 2f).color(0x000000));
        addLight(new Light().pos(-3.5f, 0.5f, 2f).color(0xFF0000));
        addLight(new Light().pos(-1.5f, 0.5f, 2f).color(0x00FF00));
        addLight(new Light().pos(0.5f, 0.5f, 2f).color(0x0000FF));
        addLight(new Light().pos(2.5f, 0.5f, 2f).color(0x00FFFF));
        addLight(new Light().pos(4.5f, 0.5f, 2f).color(0xFF00FF));
        addLight(new Light().pos(6.5f, 0.5f, 2f).color(0xFFFF00));
        addLight(new Light().pos(8.5f, 0.5f, 2f).color(0xFFFFFF));
        addLight(flashlight);
    }

    public void exit() {
        Client client = Client.getInstance();
        client.soundManager.stopAll();
        client.world = null;
    }

    public void tick() {
        if (isPaused)
            return;

        timeOfTheDay++;

        //run scheduled ticks
        for (Runnable tick : scheduledTicks)
            tick.run();
        scheduledTicks.clear();

        //if the player is dead, show death screen
        Client c = Client.getInstance();
        if (player.isDead())
            c.setScreen(new DeathScreen());

        //terrain
        for (Terrain terrain : terrain)
            terrain.tick();

        //entities
        for (Entity entity : entities)
            entity.tick();

        //remove entities flagged to be removed
        entities.removeIf(Entity::isRemoved);

        //particles
        for (Particle particle : particles)
            particle.tick();

        //remove dead particles
        particles.removeIf(Particle::isRemoved);

        //process input
        this.movement.apply(player);
        processMouseInput();

        //hud
        this.hud.tick();

        //temp
        //enemy spawn
        if (c.options.enemySpawn > 0 && c.ticks % c.options.enemySpawn == 0) {
            Enemy enemy = new Enemy(this, c.options.enemyBehaviour);
            enemy.setPos((int) (Math.random() * 128) - 64, 0, (int) (Math.random() * 128) - 64);
            for (AIBehaviour behaviour : c.options.enemyBehaviour) {
                if (behaviour == AIBehaviour.SHOOT) {
                    enemy.giveItem(new CoilGun(1, 20, 0));
                    break;
                }
            }
            addEntity(enemy);
        }

        //health spawn
        if (c.options.healthSpawn > 0 && c.ticks % c.options.healthSpawn == 0) {
            HealthPack health = new HealthPack(this);
            health.setPos((int) (Math.random() * 128) - 64, 3, (int) (Math.random() * 128) - 64);
            addEntity(health);
        }

        //effect spawn
        if (c.options.boostSpawn > 0 && c.ticks % c.options.boostSpawn == 0) {
            EffectBox box = new EffectBox(this);
            box.setPos((int) (Math.random() * 128) - 64, 3, (int) (Math.random() * 128) - 64);
            addEntity(box);
        }
    }

    public void render(MatrixStack matrices, float delta) {
        Client c = Client.getInstance();
        if (isPaused)
            delta = 1f;

        //set camera
        c.camera.setup(player, cameraMode, delta);

        //render skybox
        Shaders.MODEL_FLAT.getShader().use().setup(
                c.camera.getPerspectiveMatrix(),
                c.camera.getViewMatrix()
        );
        skyBox.setSunAngle(Maths.map((timeOfTheDay + delta) % 24000, 0, 24000, 0, 360));
        skyBox.render(c.camera, matrices);
        sunLight.direction(skyBox.getSunDirection());

        //render shadows
        renderShadows(c.camera, matrices, delta);

        //set world shader
        Shader s = Shaders.MODEL.getShader().use();
        s.setup(c.camera.getPerspectiveMatrix(), c.camera.getViewMatrix());

        //apply lighting
        flashlight.direction(player.getLookDir(delta));
        flashlight.pos(player.getEyePos(delta));
        applyWorldUniforms(s);
        applyShadowUniforms(s);

        //render world
        renderWorld(c.camera.getEntity(), matrices, delta);

        //render debug
        if (debugRendering && !hideHUD) {
            renderHitboxes(c.camera, matrices, delta);
            renderHitResults(matrices);
        }

        //finish rendering
        VertexConsumer.finishAllBatches(c.camera.getPerspectiveMatrix(), c.camera.getViewMatrix());

        //debug shadows
        if (renderShadowMap) {
            renderShadowBuffer(c.window.width, c.window.height, 500);
        }
    }

    private void renderShadows(Camera camera, MatrixStack matrices, float delta) {
        //prepare matrix
        float r = Chunk.CHUNK_SIZE * 2 * 0.5f;
        Matrix4f lightProjection = new Matrix4f().ortho(-r, r, -r, r, -r, r);

        //setup camera
        Vector3f dir = skyBox.getSunDirection();
        Vector3f pos = new Vector3f(camera.getPos());
        Vector2f rot = new Vector2f(camera.getRot());

        camera.setPos(pos.x + dir.x, pos.y + dir.y, pos.z + dir.z);
        camera.lookAt(pos.x, pos.y, pos.z);

        //finish matrix
        lightSpaceMatrix.set(lightProjection).mul(camera.getViewMatrix());

        //shader
        Shader s = Shaders.DEPTH.getShader().use();
        s.setMat4("lightSpaceMatrix", lightSpaceMatrix);

        //framebuffer
        Framebuffer prev = Framebuffer.activeFramebuffer;
        shadowBuffer.use();
        shadowBuffer.clear();

        //null so the camera entity shadows renders even in first person
        renderWorld(null, matrices, delta);

        matrices.push();
        matrices.identity();
        s.applyMatrixStack(matrices);
        VertexConsumer.finishAllBatches(s);
        matrices.pop();

        if (prev != null) prev.use();
        else Framebuffer.useDefault();

        //restore camera
        camera.setPos(pos.x, pos.y, pos.z);
        camera.setRot(rot.x, rot.y);
    }

    private void renderWorld(Entity camEntity, MatrixStack matrices, float delta) {
        //render terrain
        for (Terrain terrain : terrain)
            terrain.render(matrices, delta);

        //render entities
        for (Entity entity : entities) {
            if (camEntity != entity || isThirdPerson())
                entity.render(matrices, delta);
        }

        //render particles
        for (Particle particle : particles)
            particle.render(matrices, delta);
    }

    public void renderHand(Camera camera, MatrixStack matrices, float delta) {
        Item item = player.getHoldingItem();
        if (item == null)
            return;

        //set world shader
        Shader s = Shaders.MODEL.getShader().use();
        s.setup(camera.getPerspectiveMatrix(), camera.getViewMatrix());

        //apply lighting
        applyWorldUniforms(s);

        matrices.push();

        //camera transforms
        matrices.translate(camera.getPos());
        matrices.rotate(camera.getRotation());

        //screen transform
        matrices.translate(0.75f, -0.5f, -1);

        //render item
        item.render(ItemRenderContext.FIRST_PERSON, matrices, delta);

        matrices.pop();
    }

    private void renderShadowBuffer(int width, int height, int size) {
        glViewport(width - size, height - size, size, size);
        Blit.copy(shadowBuffer, 0, Shaders.DEPTH_BLIT.getShader());
        glViewport(0, 0, width, height);
    }

    private void renderHitboxes(Camera camera, MatrixStack matrices, float delta) {
        Vector3f cameraPos = camera.getPos();
        AABB area = new AABB();
        area.translate(player.getPos());
        area.inflate(8f);

        for (Terrain t : getTerrain(area))
            t.renderDebugHitbox(matrices, delta);

        for (Entity e : getEntities(area)) {
            if (e != player || isThirdPerson())
                e.renderDebugHitbox(matrices, delta);
        }

        for (Light l : getLights(area)) {
            Vector3f pos = l.getPos();
            if (cameraPos.distanceSquared(pos) <= 0.1f)
                continue;

            float r = 0.125f;
            int color = l.getColor() + (0xFF << 24);
            GeometryHelper.pushCube(VertexConsumer.MAIN_FLAT, matrices, pos.x - r, pos.y - r, pos.z - r, pos.x + r, pos.y + r, pos.z + r, color);
        }
    }

    private void renderHitResults(MatrixStack matrices) {
        float f = 0.025f;
        float r = player.getPickRange();

        Hit<Terrain> terrain = player.getLookingTerrain(r);
        if (terrain != null) {
            for (AABB aabb : terrain.obj().getGroupsAABB())
                GeometryHelper.pushCube(VertexConsumer.LINES, matrices, aabb.minX(), aabb.minY(), aabb.minZ(), aabb.maxX(), aabb.maxY(), aabb.maxZ(), 0xFFFFFF00);

            Vector3f pos = terrain.pos();
            GeometryHelper.pushCube(VertexConsumer.MAIN_FLAT, matrices, pos.x - f, pos.y - f, pos.z - f, pos.x + f, pos.y + f, pos.z + f, 0xFF00FFFF);
        }

        Hit<Entity> entity = player.getLookingEntity(r);
        if (entity != null) {
            AABB aabb = entity.obj().getAABB();
            GeometryHelper.pushCube(VertexConsumer.LINES, matrices, aabb.minX(), aabb.minY(), aabb.minZ(), aabb.maxX(), aabb.maxY(), aabb.maxZ(), 0xFFFFFF00);

            Vector3f pos = entity.pos();
            GeometryHelper.pushCube(VertexConsumer.MAIN_FLAT, matrices, pos.x - f, pos.y - f, pos.z - f, pos.x + f, pos.y + f, pos.z + f, 0xFF00FFFF);
        }
    }

    public void applyWorldUniforms(Shader s) {
        //camera
        s.setVec3("camPos", Client.getInstance().camera.getPos());

        //fog
        s.setFloat("fogStart", Chunk.getFogStart(this));
        s.setFloat("fogEnd", Chunk.getFogEnd(this));
        s.setColor("fogColor", Chunk.fogColor);

        //lighting
        s.setColor("ambient", 0x202020);//Chunk.ambientLight);

        s.setInt("lightCount", lights.size());
        for (int i = 0; i < lights.size(); i++) {
            lights.get(i).pushToShader(s, i);
        }
    }

    public void applyShadowUniforms(Shader s) {
        s.setMat4("lightSpaceMatrix", lightSpaceMatrix);
        s.setInt("shadowMap", 3);
        s.setVec3("shadowDir", skyBox.getSunDirection());

        glActiveTexture(GL_TEXTURE3); //0-1-2 used by the material
        glBindTexture(GL_TEXTURE_2D, shadowBuffer.getDepthBuffer());

        glActiveTexture(GL_TEXTURE0);
    }

    public void addTerrain(Terrain terrain) {
        scheduledTicks.add(() -> this.terrain.add(terrain));
    }

    public void addEntity(Entity entity) {
        scheduledTicks.add(() -> {
            this.entities.add(entity);
            entity.onAdd();
        });
    }

    public void addParticle(Particle particle) {
        if (particle.shouldRender())
            scheduledTicks.add(() -> this.particles.add(particle));
    }

    public void addLight(Light light) {
        scheduledTicks.add(() -> this.lights.add(light));
    }

    public SoundSource playSound(Resource sound, SoundCategory category, Vector3f position) {
        return Client.getInstance().soundManager.playSound(sound, category, position);
    }

    public void mousePress(int button, int action, int mods) {
        boolean press = action != GLFW_RELEASE;

        switch (button) {
            case GLFW_MOUSE_BUTTON_1 -> {
                if (attackPress && !press)
                    player.stopAttacking();
                attackPress = press;
            }
            case GLFW_MOUSE_BUTTON_2 -> {
                if (usePress && !press)
                    player.stopUsing();
                usePress = press;
            }
        }

        processMouseInput();
    }

    private void processMouseInput() {
        if (attackPress)
            player.attack();
        if (usePress)
            player.use();
    }

    public void mouseMove(double x, double y) {
        if (!isPaused)
            movement.mouseMove(x, y);
    }

    public void scroll(double x, double y) {
        player.setSelectedItem(player.getInventory().getSelectedIndex() - (int) Math.signum(y));
    }

    public void keyPress(int key, int scancode, int action, int mods) {
        if (isPaused)
            return;

        movement.keyPress(key, action);

        if (action == GLFW_RELEASE)
            return;

        if (key >= GLFW_KEY_1 && key <= GLFW_KEY_9)
            player.setSelectedItem(key - GLFW_KEY_1);

        switch (key) {
            case GLFW_KEY_R -> {
                Item i = player.getHoldingItem();
                if (i instanceof Weapon weapon && !weapon.isOnCooldown() && i.getCount() < i.getStackCount())
                    weapon.setOnCooldown();
            }
            case GLFW_KEY_ESCAPE -> Client.getInstance().setScreen(new PauseScreen());
            case GLFW_KEY_F1 -> this.hideHUD = !this.hideHUD;
            case GLFW_KEY_F3 -> this.debugRendering = !this.debugRendering;
            case GLFW_KEY_F4 -> this.renderShadowMap = !this.renderShadowMap;
            case GLFW_KEY_F5 -> this.cameraMode = (this.cameraMode + 1) % 3;
            case GLFW_KEY_F7 -> this.timeOfTheDay -= 100;
            case GLFW_KEY_F8 -> this.timeOfTheDay += 100;
        }
    }

    public int entityCount() {
        return entities.size();
    }

    public int terrainCount() {
        return terrain.size();
    }

    public int particleCount() {
        return particles.size();
    }

    public int lightCount() {
        return lights.size();
    }

    public List<Entity> getEntities(AABB region) {
        List<Entity> list = new ArrayList<>();
        for (Entity entity : entities) {
            if (region.intersects(entity.getAABB()))
                list.add(entity);
        }
        return list;
    }

    public List<Terrain> getTerrain(AABB region) {
        List<Terrain> list = new ArrayList<>();
        for (Terrain terrain : this.terrain) {
            if (region.intersects(terrain.getAABB()))
                list.add(terrain);
        }
        return list;
    }

    public List<Particle> getParticles(AABB region) {
        List<Particle> list = new ArrayList<>();
        for (Particle particle : this.particles) {
            if (region.isInside(particle.getPos()))
                list.add(particle);
        }
        return list;
    }

    public List<Light> getLights(AABB region) {
        List<Light> list = new ArrayList<>();
        for (Light light : this.lights) {
            if (region.isInside(light.getPos()))
                list.add(light);
        }
        return list;
    }

    public List<AABB> getTerrainCollisions(AABB region) {
        List<AABB> list = new ArrayList<>();
        for (Terrain terrain : this.terrain) {
            if (region.intersects(terrain.getAABB()))
                list.addAll(terrain.getGroupsAABB());
        }
        return list;
    }

    public boolean isDebugRendering() {
        return this.debugRendering;
    }

    public void respawn() {
        entities.clear();
        scheduledTicks.clear();
        player = new Player(this, Client.getInstance().options.player);
        player.giveItem(new CoilGun(1, 5, 0));
        player.giveItem(new PotatoCannon(3, 40, 30));
        player.giveItem(new RiceGun(8, 80, 60));
        player.getInventory().setItem(player.getInventory().getSize() - 1, new MagicWand(1));
        player.setPos(0, 3, 8);
        addEntity(player);
    }

    public void setPaused(boolean pause) {
        this.isPaused = pause;
        this.movement.reset();

        SoundManager soundManager = Client.getInstance().soundManager;
        if (pause) soundManager.pauseAll();
        else soundManager.resumeAll();
    }

    public int getCameraMode() {
        return cameraMode;
    }

    public boolean isThirdPerson() {
        return cameraMode > 0;
    }

    public boolean hideHUD() {
        return hideHUD;
    }

    public void explode(Vector3f pos, float range, float strength, Entity source) {
        AABB explosionBB = new AABB().inflate(range).translate(pos);
        int damage = (int) (4 * strength);

        for (Entity entity : getEntities(explosionBB)) {
            if (entity == source || entity.isRemoved())
                continue;

            //damage entities
            entity.damage(source, DamageType.EXPLOSION, damage, false);

            //knock back
            if (entity instanceof PhysEntity e) {
                Vector3f dir = explosionBB.getCenter().sub(e.getAABB().getCenter(), new Vector3f()).normalize().mul(-1);
                e.knockback(dir, 0.5f * strength);
            }
        }

        //particles
        for (int i = 0; i < 30 * range; i++) {
            ExplosionParticle particle = new ExplosionParticle(this, (int) (Math.random() * 10) + 15);
            particle.setPos(explosionBB.getRandomPoint());
            particle.setScale(5f);
            addParticle(particle);
        }

        //sound
        playSound(EXPLOSION_SOUND, SoundCategory.ENTITY, pos).maxDistance(64f).volume(0.5f);
    }

    public Hit<Terrain> raycastTerrain(AABB area, Vector3f pos, Vector3f dirLen) {
        //prepare variables
        CollisionResult terrainColl = null;
        Terrain tempTerrain = null;

        //loop through terrain in area
        for (Terrain t : getTerrain(area)) {
            //loop through its groups AABBs
            for (AABB aabb : t.getGroupsAABB()) {
                //check for collision
                CollisionResult result = CollisionDetector.collisionRay(aabb, pos, dirLen);
                //store collision if it is closer than previous collision
                if (result != null && (terrainColl == null || result.near() < terrainColl.near())) {
                    terrainColl = result;
                    tempTerrain = t;
                }
            }
        }

        //no collisions
        if (terrainColl == null)
            return null;

        //return terrain collision data
        float d = terrainColl.near();
        return new Hit<>(terrainColl, tempTerrain, new Vector3f(pos).add(dirLen.x * d, dirLen.y * d, dirLen.z * d));
    }

    public Hit<Entity> raycastEntity(AABB area, Vector3f pos, Vector3f dirLen, Predicate<Entity> predicate) {
        //prepare variables
        CollisionResult entityColl = null;
        Entity tempEntity = null;

        //loop through entities in area
        for (Entity e : getEntities(area)) {
            //check for the predicate if the entity is valid
            if (predicate.test(e)) {
                //check for collision
                CollisionResult result = CollisionDetector.collisionRay(e.getAABB(), pos, dirLen);
                //store collision if it is closer than previous collision
                if (result != null && (entityColl == null || result.near() < entityColl.near())) {
                    entityColl = result;
                    tempEntity = e;
                }
            }
        }

        //no collisions
        if (entityColl == null)
            return null;

        //return entity collision data
        float d = entityColl.near();
        return new Hit<>(entityColl, tempEntity, new Vector3f(pos).add(dirLen.x * d, dirLen.y * d, dirLen.z * d));
    }
}
