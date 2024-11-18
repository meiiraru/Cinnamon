package cinnamon.model.gltf;

import java.util.ArrayList;
import java.util.List;

public class Mesh {
    private final List<Primitive> primitives = new ArrayList<>();

    public List<Primitive> getPrimitives() {
        return primitives;
    }
}