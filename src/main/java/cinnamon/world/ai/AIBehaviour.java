package cinnamon.world.ai;

import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.LivingEntity;

import java.util.function.BiConsumer;

public enum AIBehaviour {
    LOOK((src, target) -> src.lookAt(target.getEyePos())),
    MOVE((src, target) -> src.impulse(0, 0, 1)),
    SHOOT((src, target) -> src.attackAction());

    private final BiConsumer<LivingEntity, Entity> consumer;

    AIBehaviour(BiConsumer<LivingEntity, Entity> consumer) {
        this.consumer = consumer;
    }

    public void apply(LivingEntity src, Entity target) {
        this.consumer.accept(src, target);
    }
}
