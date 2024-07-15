package cinnamon.world;

import cinnamon.world.entity.living.LivingEntity;

import java.util.function.Consumer;

public enum AIBehaviour {
    WALK(entity -> {
        entity.move(0, 0, 1);
        entity.rotate(0, 0.1f);
    }),
    SHOOT(LivingEntity::attackAction);

    private final Consumer<LivingEntity> consumer;

    AIBehaviour(Consumer<LivingEntity> consumer) {
        this.consumer = consumer;
    }

    public void apply(LivingEntity entity) {
        this.consumer.accept(entity);
    }
}
