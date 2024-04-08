package mayo.parsers;

import mayo.model.obj.Face;
import mayo.model.obj.Group;
import mayo.model.obj.Mesh;
import mayo.model.obj.material.MtlMaterial;
import mayo.utils.Curve;
import mayo.utils.Resource;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;

public class CurveToMesh {

    private static final Resource
            TEXTURE_KD = new Resource("textures/rollercoaster/kd.png"),
            TEXTURE_KS = new Resource("textures/rollercoaster/ks.png");

    public static Mesh generateMesh(Curve curve, boolean offset) throws Exception {
        //offset curve to 0-0
        Vector3f center = offset ? curve.getCenter() : new Vector3f();
        curve.offset(-center.x, -center.y, -center.z);

        //grab curve variables
        float curveWidth = curve.getWidth();
        boolean loop = curve.isLooping();
        List<Vector3f> internal = curve.getInternalCurve();
        List<Vector3f> external = curve.getExternalCurve();

        //re-offset curve
        curve.offset(center.x, center.y, center.z);

        //check sizes
        int size = internal.size();

        if (size != external.size())
            throw new Exception("Curve internal size is different than external size");

        if (size < 2)
            throw new Exception("Cannot create curve with size smaller than 2");

        //create mesh
        Mesh mesh = new Mesh();

        //vertices
        for (int i = 0; i < size; i++) {
            mesh.getVertices().add(internal.get(i));
            mesh.getVertices().add(external.get(i));
        }

        //default group
        Group group = new Group("default");
        mesh.getGroups().add(group);

        //prepare uvs
        float uv = 0f;

        //faces
        int max = loop ? size : size - 1;
        for (int i = 0; i < max; i++) {
            int j = (i + 1) % size;

            //internal
            int p0 = i * 2;
            int p1 = j * 2;
            //external
            int p2 = j * 2 + 1;
            int p3 = i * 2 + 1;
            //add
            List<Integer> indexes = List.of(p0, p1, p2, p3);
            group.getFaces().add(new Face(indexes, indexes, indexes));

            //calculate UVs
            mesh.getUVs().add(new Vector2f(1, uv));
            mesh.getUVs().add(new Vector2f(0, uv));
            uv += Math.max(
                    internal.get(j).sub(internal.get(i), new Vector3f()).length() / curveWidth,
                    external.get(j).sub(external.get(i), new Vector3f()).length() / curveWidth
            );

            //calculate normals
            mesh.getNormals().add(calculateNormal(internal.get(i), internal.get(j), external.get(i)));
            mesh.getNormals().add(calculateNormal(external.get(i), internal.get(i), external.get(j)));
        }

        //add missing UVs and normals
        if (!loop) {
            //uvs
            mesh.getUVs().add(new Vector2f(1, uv));
            mesh.getUVs().add(new Vector2f(0, uv));
            //normals
            mesh.getNormals().add(new Vector3f(1));
            mesh.getNormals().add(new Vector3f(1));
        }

        //material
        MtlMaterial material = new MtlMaterial("curve");
        mesh.getMaterials().put("curve", material);
        group.setMaterial(material);

        //textures
        material.setDiffuseTex(TEXTURE_KD);
        material.setSpColorTex(TEXTURE_KS);

        //specular exponent
        material.setSpecularExponent(256);

        return mesh;
    }

    private static Vector3f calculateNormal(Vector3f a, Vector3f b, Vector3f c) {
        Vector3f ba = b.sub(a, new Vector3f());
        Vector3f ca = c.sub(a, new Vector3f());
        return ba.cross(ca);
    }
}
