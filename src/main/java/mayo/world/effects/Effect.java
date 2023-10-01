package mayo.world.effects;

import java.util.function.BiFunction;

public class Effect {

    private final Type effectType;
    private final int duration;
    private final int amplitude;

    private int time;
    private boolean isDone;

    private Effect(Type effectType, int duration, int amplitude) {
        this.effectType = effectType;
        this.duration = duration;
        this.amplitude = amplitude;
    }

    public void tick() {
        if (time == -1)
            return;

        time++;
        isDone = time >= duration;
    }

    public Type getType() {
        return effectType;
    }

    public int getAmplitude() {
        return amplitude;
    }

    public int getDuration() {
        return duration;
    }

    public int getTime() {
        return time;
    }

    public int getRemainingTime() {
        return time == -1 ?  time : duration - time;
    }

    public boolean isDone() {
        return isDone;
    }

    public enum Type {
        PACIFIST,
        ALWAYS_CRIT,
        NEVER_CRIT,
        DAMAGE_BOOST,
        HEAL,
        SPEED;

        private final BiFunction<Integer, Integer, Effect> function = (i, ii) -> new Effect(this, i, ii);

        public Effect create(int duration) {
            return this.create(duration, 1);
        }

        public Effect create(int duration, int amplitude) {
            return function.apply(duration, amplitude);
        }
    }
}
