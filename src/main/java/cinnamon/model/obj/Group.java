package cinnamon.model.obj;

import cinnamon.math.collision.AABB;
import cinnamon.model.material.Material;

import java.util.ArrayList;
import java.util.List;

public class Group {

    private final String name;
    private final List<Face> faces = new ArrayList<>();
    private final AABB bounds = new AABB();

    private Material material;

    public Group(String name) {
        this.name = name;
    }

    public boolean isEmpty() {
        return faces.isEmpty();
    }

    public String getName() {
        return name;
    }

    public List<Face> getFaces() {
        return faces;
    }

    public AABB getBounds() {
        return bounds;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }
}
