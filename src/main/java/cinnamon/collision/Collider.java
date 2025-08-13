package cinnamon.collision;

import org.joml.Vector3f;

public abstract class Collider {

    protected abstract Vector3f findFurthestPoint(float dx, float dy, float dz);
}
