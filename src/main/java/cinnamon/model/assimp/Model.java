package cinnamon.model.assimp;

import cinnamon.model.material.Material;

import java.util.ArrayList;
import java.util.List;

public class Model {
    public final List<Mesh> meshes = new ArrayList<>();
    public final List<Material> materials = new ArrayList<>();
}
