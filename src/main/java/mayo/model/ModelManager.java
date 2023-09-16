package mayo.model;

import mayo.model.obj.Mesh;
import mayo.parsers.ObjLoader;
import mayo.utils.Resource;

import java.util.HashMap;
import java.util.Map;

public class ModelManager {

    private static final Map<Resource, Mesh> MESH_MAP = new HashMap<>();

    public static Mesh load(Resource resource) {
        //already loaded, return it
        Mesh mesh = MESH_MAP.get(resource);
        if (mesh != null)
            return mesh;

        //load mesh :)
        Mesh m = ObjLoader.load(new Resource("models/bullet/bullet.obj")).bake();
        MESH_MAP.put(resource, m);
        return m;
    }
}
