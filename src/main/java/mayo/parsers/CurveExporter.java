package mayo.parsers;

import mayo.utils.Curve;
import mayo.utils.IOUtils;
import mayo.utils.Resource;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CurveExporter {

    private static final Path
            FOLDER = IOUtils.ROOT_FOLDER.resolve("curves"),
            OBJ = FOLDER.resolve("curve.obj"),
            MTL = FOLDER.resolve("curve.mtl"),
            KD = FOLDER.resolve("curve_kd.png"),
            KS = FOLDER.resolve("curve_ks.png"),
            DATA = FOLDER.resolve("data.json");
    public static final Resource
            TEXTURE_KD = new Resource("textures/rollercoaster/kd.png"),
            TEXTURE_KS = new Resource("textures/rollercoaster/ks.png");

    public static void exportCurve(Curve curve) throws Exception {
        //offset curve to 0-0
        Vector3f center = curve.getCenter();
        curve.offset(-center.x, 0, -center.z);

        //grab curve variables
        float curveWidth = curve.getWidth();
        boolean loop = curve.isLooping();
        List<Vector3f> mainCurve = curve.getCurve();
        List<Vector3f> internal = curve.getInternalCurve();
        List<Vector3f> external = curve.getExternalCurve();

        //re-offset curve
        curve.offset(center.x, 0, center.z);

        //check sizes
        int size = internal.size();

        if (size != external.size())
            throw new Exception("Curve internal size is different than external size");

        if (size < 2)
            throw new Exception("Cannot create curve with size smaller than 2");

        //create obj
        Obj obj = new Obj();

        //vertices
        for (int i = 0; i < size; i++) {
            obj.addVertex(internal.get(i));
            obj.addVertex(external.get(i));
        }

        //prepare uvs
        float uv = 0f;

        //faces
        int max = loop ? size : size - 1;
        for (int i = 0; i < max; i++) {
            int j = (i + 1) % size;

            //internal
            int p0 = i * 2 + 1;
            int p1 = j * 2 + 1;
            //external
            int p2 = j * 2 + 2;
            int p3 = i * 2 + 2;
            //add
            obj.addFace(p0, p1, p2, p3);

            //calculate UVs
            obj.addUV(new Vector2f(1, uv));
            obj.addUV(new Vector2f(0, uv));
            uv += Math.max(
                    internal.get(j).sub(internal.get(i), new Vector3f()).length() / curveWidth,
                    external.get(j).sub(external.get(i), new Vector3f()).length() / curveWidth
            );

            //calculate normals
            obj.addNormal(calculateNormal(internal.get(i), internal.get(j), external.get(i)));
            obj.addNormal(calculateNormal(internal.get(j), external.get(i), external.get(j)));
        }

        //add missing UVs and normals
        if (!loop) {
            //uvs
            obj.addUV(new Vector2f(1, uv));
            obj.addUV(new Vector2f(0, uv));

            //normals
            obj.addNormal(new Vector3f(1));
            obj.addNormal(new Vector3f(1));
        }

        //write files
        writeMainCurve(mainCurve);
        writeMtl();
        writeTexture();
        writeObj(obj);
    }

    private static Vector3f calculateNormal(Vector3f a, Vector3f b, Vector3f c) {
        Vector3f ab = a.mul(b, new Vector3f());
        Vector3f ac = a.mul(c, new Vector3f());
        return ab.cross(ac);
    }

    private static void writeMainCurve(List<Vector3f> mainCurve) {
        StringBuilder string = new StringBuilder("[");
        for (Vector3f vec : mainCurve) {
            string.append("\n\t[%s, %s, %s],".formatted(vec.x, vec.y, vec.z));
        }
        string.setCharAt(string.length() - 1, '\n'); //safe to assume that mainCurve will run as we need at least size 2
        string.append("]");
        IOUtils.writeFile(DATA, string.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void writeMtl() {
        IOUtils.writeFile(MTL, "newmtl curve\nmap_Kd curve_kd.png\nmap_Ks curve_ks.png".getBytes(StandardCharsets.UTF_8));
    }

    private static void writeTexture() throws IOException {
        InputStream kd = IOUtils.getResource(TEXTURE_KD);
        InputStream ks = IOUtils.getResource(TEXTURE_KS);
        IOUtils.writeFile(KD, kd.readAllBytes());
        IOUtils.writeFile(KS, ks.readAllBytes());
    }

    private static void writeObj(Obj obj) {
        StringBuilder string = new StringBuilder("mtllib curve.mtl\n");

        //vertices
        for (Vector3f vertex : obj.vertices)
            string.append("v %f %f %f\n".formatted(vertex.x, vertex.y, vertex.z));

        //uvs
        for (Vector2f uv : obj.uvs)
            string.append("vt %f %f\n".formatted(uv.x, uv.y));

        //normals
        for (Vector3f normal : obj.normals)
            string.append("vn %f %f %f\n".formatted(normal.x, normal.y, normal.z));

        //use mtl
        string.append("usemtl curve\n");

        //faces
        for (int[] face : obj.faces) {
            string.append("f");
            for (int i : face)
                string.append(" %s/%s/%s".formatted(i, i, i)); //it is safe to assume that we have the same indexing for UVs and normals
            string.append("\n");
        }

        //write
        IOUtils.writeFile(OBJ, string.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static class Obj {
        private final List<Vector3f>
                vertices = new ArrayList<>(),
                normals = new ArrayList<>();
        private final List<Vector2f>
                uvs = new ArrayList<>();
        private final List<int[]>
                faces = new ArrayList<>();

        public void addVertex(Vector3f vertex) {
            this.vertices.add(vertex);
        }

        public void addUV(Vector2f uv) {
            this.uvs.add(uv);
        }

        public void addNormal(Vector3f normal) {
            this.normals.add(normal);
        }

        public void addFace(int... indexes) {
            faces.add(indexes);
        }
    }
}
