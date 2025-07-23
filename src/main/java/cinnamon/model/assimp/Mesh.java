package cinnamon.model.assimp;

import cinnamon.utils.AABB;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Mesh {

    //vertices data
    public final List<Vector3f>
            vertices = new ArrayList<>(),
            normals = new ArrayList<>(),
            tangents = new ArrayList<>();
    public final List<Vector2f>
            uvs = new ArrayList<>();

    //indices data
    public final List<Integer>
            indices = new ArrayList<>();

    //AABB
    public final AABB aabb = new AABB();

    //properties
    public final String name;
    public int materialIndex;
    public boolean
            hasTangents,
            hasNormals,
            hasUVs;

    public Mesh(String name) {
        this.name = name;
    }
}
