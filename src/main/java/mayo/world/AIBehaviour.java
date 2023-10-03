package mayo.world;

import mayo.world.entity.living.LivingEntity;

import java.util.function.Consumer;

public enum AIBehaviour {
    WALK(entity -> {
        entity.move(0, 0, 1);
        entity.lookAt(entity.getWorld().player.getEyePos());
    }),
    SHOOT(LivingEntity::attack);

    private final Consumer<LivingEntity> consumer;

    AIBehaviour(Consumer<LivingEntity> consumer) {
        this.consumer = consumer;
    }

    public void apply(LivingEntity entity) {
        this.consumer.accept(entity);
    }
}
