package cinnamon.model.obj;

import cinnamon.animation.Animation;
import cinnamon.animation.Bone;
import cinnamon.math.collision.shape.AABB;
import cinnamon.model.material.Material;
import cinnamon.utils.Pair;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mesh {

    //vertices data
    private final List<Vector3f>
            vertices = new ArrayList<>(),
            normals = new ArrayList<>();
    private final List<Vector2f>
            uvs = new ArrayList<>();

    //groups
    private final List<Group>
            groups = new ArrayList<>();

    //materials
    private final Map<String, Material>
            materials = new HashMap<>();

    //bounding box
    private final AABB bounds = new AABB();

    //animations
    private Pair<Bone, List<Animation>> animData;


    // -- getters -- //


    public List<Vector3f> getVertices() {
        return vertices;
    }

    public List<Vector3f> getNormals() {
        return normals;
    }

    public List<Vector2f> getUVs() {
        return uvs;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public Map<String, Material> getMaterials() {
        return materials;
    }

    public AABB getBounds() {
        return bounds;
    }

    public Pair<Bone, List<Animation>> getAnimationData() {
        return animData;
    }

    public void setAnimationData(Pair<Bone, List<Animation>> animData) {
        this.animData = animData;
    }
}
