package cinnamon.animation;

import cinnamon.world.Transform;
import org.joml.Vector3f;

import java.util.function.BiConsumer;

public enum Channel {
    POSITION(Transform::setPos),
    ROTATION(Transform::setRot),
    SCALE(Transform::setScale);

    private final BiConsumer<Transform, Vector3f> consumer;

    Channel(BiConsumer<Transform, Vector3f> consumer) {
        this.consumer = consumer;
    }

    public void apply(Transform transform, Vector3f vector) {
        consumer.accept(transform, vector);
    }
}
