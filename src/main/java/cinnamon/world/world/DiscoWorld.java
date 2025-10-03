package cinnamon.world.world;

import cinnamon.animation.Animation;
import cinnamon.model.GeometryHelper;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.sound.Sound;
import cinnamon.sound.SoundCategory;
import cinnamon.sound.SoundInstance;
import cinnamon.sound.SoundSpectrum;
import cinnamon.utils.ColorUtils;
import cinnamon.utils.Resource;
import cinnamon.world.entity.terrain.DiscoBall;
import cinnamon.world.entity.terrain.DiscoFloor;
import cinnamon.world.entity.terrain.FloorLight;
import cinnamon.world.entity.terrain.ParticleSpawner;
import cinnamon.world.entity.terrain.Speaker;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static cinnamon.animation.Animation.Loop.LOOP;

public class DiscoWorld extends WorldClient {

    private static final float
            BOOST = 0.1f;
    private static final Function<Float, Float>
            WEIGHTING_FUNCTION = f -> f < 1000 ? 1f : f < 4000 ? 2f : f < 12000 ? 6f : 12f;

    private final SoundSpectrum spectrum = new SoundSpectrum();

    private Sound sound;
    private SoundInstance soundData;

    private final List<Speaker> speakers = new ArrayList<>();
    private final List<ParticleSpawner> spawners = new ArrayList<>();
    private final List<FloorLight> lights = new ArrayList<>();

    @Override
    public void close() {
        super.close();
        soundData.stop();
    }

    @Override
    protected void tempLoad() {
        super.tempLoad();

        Resource soundRes = new Resource("sounds/song.ogg");
        sound = Sound.of(soundRes);
        soundData = playSound(soundRes, SoundCategory.MUSIC, new Vector3f(0, 0, 0)).loop(true);

        DiscoFloor floor = new DiscoFloor(UUID.randomUUID());
        floor.setPos(0f, 1.001f, 0f);
        addEntity(floor);

        DiscoBall discoBall = new DiscoBall(UUID.randomUUID());
        discoBall.setPos(0f, 3f, 0f);
        addEntity(discoBall);
        discoBall.getAnimation("animation1").setLoop(LOOP).play();
        discoBall.getAnimation("animation2").setLoop(LOOP).play();
        discoBall.getAnimation("animation3").setLoop(LOOP).play();
        discoBall.getAnimation("animation4").setLoop(LOOP).play();

        Speaker speaker1 = new Speaker(UUID.randomUUID());
        speaker1.setPos(-1f, 1f, -2f);
        speaker1.setRot(0, 180);
        addEntity(speaker1);
        speakers.add(speaker1);

        Speaker speaker2 = new Speaker(UUID.randomUUID());
        speaker2.setPos(1f, 1f, -2f);
        speaker2.setRot(0, 180);
        addEntity(speaker2);
        speakers.add(speaker2);

        ParticleSpawner spawner1 = new ParticleSpawner(UUID.randomUUID());
        spawner1.setPos(-3f, 1f, -1.5f);
        addEntity(spawner1);
        spawners.add(spawner1);

        ParticleSpawner spawner2 = new ParticleSpawner(UUID.randomUUID());
        spawner2.setPos(3f, 1f, -1.5f);
        addEntity(spawner2);
        spawners.add(spawner2);

        FloorLight light1 = new FloorLight(UUID.randomUUID());
        light1.setPos(-4f, 1.001f, 2f);
        light1.setRot(0, -90);
        addEntity(light1);
        lights.add(light1);

        FloorLight light2 = new FloorLight(UUID.randomUUID());
        light2.setPos(4f, 1.001f, 2f);
        light2.setRot(0, 90);
        addEntity(light2);
        lights.add(light2);

        for (int i = 0; i < lights.size(); i++) {
            FloorLight light = lights.get(i);
            light.getAnimation("up_down").setLoop(LOOP).play();
            Animation anim = light.getAnimation("look_around");
            anim.setLoop(LOOP).setTime(i % 2 == 1 ? anim.getDuration() / 2 : 0).play();
        }
    }

    @Override
    public void tick() {
        super.tick();

        float[] amplitudes = spectrum.getAmplitudes();

        for (Speaker speaker : speakers) {
            if (amplitudes[0] >= 10f)
                speaker.getAnimation("animation1").play();
            if (amplitudes[2] >= 3f)
                speaker.getAnimation("animation2").play();
        }

        if (amplitudes[3] >= 3f && amplitudes[15] >= 1f) {
            for (ParticleSpawner spawner : spawners)
                spawner.bubbles();
        }

        if (amplitudes[0] >= 30f) {
            for (ParticleSpawner spawner : spawners)
                spawner.fire();
        }

        if (amplitudes[7] >= 3f) {
            for (FloorLight light : lights)
                light.getAnimation("flick").play();
        }
    }

    @Override
    public int renderParticles(Camera camera, MatrixStack matrices, float delta) {
        int count = super.renderParticles(camera, matrices, delta);

        //grab the audio spectrum and calculate the amplitudes
        spectrum.updateAmplitudes(sound, soundData, true);
        float[] amplitudes = spectrum.getAmplitudes();

        //draw bars
        matrices.pushMatrix();
        matrices.translate(0, 2f, -2f);
        int bars = amplitudes.length;
        for (int i = 0; i < bars; i++)
            drawBar(matrices, i, bars, amplitudes[i] * BOOST * WEIGHTING_FUNCTION.apply((float) i / bars * spectrum.getMaxFrequency()));
        matrices.popMatrix();

        return count + 1;
    }

    private void drawBar(MatrixStack matrices, int index, int bars, float amplitude) {
        float x = (index - bars / 2f) * 0.15f;
        float y = Math.max(amplitude, 0.05f);

        int color = ColorUtils.rgbToInt(ColorUtils.hsvToRGB(new Vector3f(index / (float) bars, 0.6f, 1f)));
        VertexConsumer.WORLD_MAIN_EMISSIVE.consume(GeometryHelper.box(matrices, x, 0, 0, x + 0.1f, y, 0.1f, color + (0xFF << 24)));
    }
}
