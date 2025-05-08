package cinnamon.parsers;

import cinnamon.lang.LangManager;
import cinnamon.model.material.Material;
import cinnamon.model.material.MaterialTexture;
import cinnamon.model.obj.Face;
import cinnamon.model.obj.Group;
import cinnamon.model.obj.Mesh;
import cinnamon.utils.Curve;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static cinnamon.render.texture.Texture.TextureParams.MIPMAP;
import static cinnamon.render.texture.Texture.TextureParams.MIPMAP_SMOOTH;

public class CurveToMesh {

    private static final MaterialTexture
            TEXTURE_ALBEDO = new MaterialTexture(new Resource("textures/rollercoaster/albedo.png"), MIPMAP, MIPMAP_SMOOTH),
            TEXTURE_METALLIC = new MaterialTexture(new Resource("textures/rollercoaster/metallic.png")),
            TEXTURE_ROUGHNESS = new MaterialTexture(new Resource("textures/rollercoaster/roughness.png"));

    public static Mesh generateMesh(Curve curve, boolean offset, boolean bottomFace) throws Exception {
        //offset curve to 0-0
        Vector3f center = offset ? curve.getCenter() : new Vector3f();
        curve.offset(-center.x, -center.y, -center.z);

        //grab curve variables
        float curveWidth = curve.getWidth();
        List<Vector3f> internal = curve.getInternalCurve();
        List<Vector3f> external = curve.getExternalCurve();

        //re-offset curve
        curve.offset(center.x, center.y, center.z);

        //check sizes
        int size = internal.size();
        if (size != external.size())
            throw new Exception(LangManager.get("curve.error.internal_size"));

        if (size < 1)
            throw new Exception(LangManager.get("curve.error.empty"));

        //create mesh
        Mesh mesh = new Mesh();

        //default group
        Group group = new Group("root");
        mesh.getGroups().add(group);

        //prepare uv length
        float uv = 0f;

        //faces
        boolean loop = curve.isLooping();
        int max = loop ? size - 1 : size;

        for (int i = 0; i < max; i++) {
            int j = (i + 1) % size;

            //get vertices
            Vector3f a = internal.get(i);
            Vector3f b = external.get(i);
            Vector3f c = internal.get(j);
            Vector3f d = external.get(j);

            //add vertices to mesh
            mesh.getVertices().add(a);
            mesh.getVertices().add(b);

            //calculate uv
            mesh.getUVs().add(new Vector2f(1f, uv));
            mesh.getUVs().add(new Vector2f(0f, uv));
            uv += Math.max(a.distance(c) / curveWidth, b.distance(d) / curveWidth);

            //calculate normals
            mesh.getNormals().add(Maths.normal(a, b, c));
            mesh.getNormals().add(Maths.normal(b, d, a));

            if (i == max - 1)
                continue;

            //add quad
            int k = i * 2, l = j * 2, m = k + 1, n = l + 1;
            List<Integer> indexes = List.of(k, l, n, m);
            group.getFaces().add(new Face(indexes, indexes, indexes));
        }

        if (loop) {
            //add last uv
            mesh.getUVs().add(new Vector2f(1f, uv));
            mesh.getUVs().add(new Vector2f(0f, uv));

            int len = mesh.getVertices().size();
            List<Integer> uvIndexes = List.of(len - 2, len, len + 1, len - 1);
            List<Integer> indexes = List.of(len - 2, 0, 1, len - 1);

            group.getFaces().add(new Face(indexes, uvIndexes, indexes));
        }

        //material
        Material material = new Material("curve");
        mesh.getMaterials().put("curve", material);
        group.setMaterial(material);

        //bottom face
        if (bottomFace) {
            //re-add all faces indexes, but in the inverted order
            Group bottom = new Group("bottom");
            bottom.setMaterial(material);
            mesh.getGroups().add(bottom);

            //add inverted normals
            int normalSize = mesh.getNormals().size();
            List<Vector3f> meshNormals = new ArrayList<>(mesh.getNormals());
            for (Vector3f normal : meshNormals)
                mesh.getNormals().add(normal.negate(new Vector3f()));

            //add inverted faces
            for (Face face : group.getFaces()) {
                List<Integer> vertices = face.getVertices();
                List<Integer> uvs = face.getUVs();
                List<Integer> normals = face.getNormals();

                bottom.getFaces().add(new Face(
                        List.of(vertices.get(3), vertices.get(2), vertices.get(1), vertices.get(0)),
                        List.of(uvs.get(3), uvs.get(2), uvs.get(1), uvs.get(0)),
                        List.of(normals.get(3) + normalSize, normals.get(2) + normalSize, normals.get(1) + normalSize, normals.get(0) + normalSize)
                ));
            }
        }

        //textures
        material.setAlbedo(TEXTURE_ALBEDO);
        material.setMetallic(TEXTURE_METALLIC);
        material.setRoughness(TEXTURE_ROUGHNESS);

        return mesh;
    }
}
