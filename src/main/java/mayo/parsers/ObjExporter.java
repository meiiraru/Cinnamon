package mayo.parsers;

import mayo.model.obj.Face;
import mayo.model.obj.Group;
import mayo.model.obj.Material;
import mayo.model.obj.Mesh;
import mayo.utils.IOUtils;
import mayo.utils.Resource;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public class ObjExporter {

    public static final Path EXPORT_FOLDER = IOUtils.ROOT_FOLDER.resolve("exported");

    public static void export(String meshName, Mesh mesh) throws IOException {
        //prepare variables
        Path folder = EXPORT_FOLDER.resolve(meshName);
        StringBuilder
                string = new StringBuilder(),
                mtlString = new StringBuilder();

        //materials
        string.append("mtllib %s.mtl\n".formatted(meshName));
        for (Material material : mesh.getMaterials().values())
            writeMaterial(folder, mtlString, material);

        //write vertices
        for (Vector3f vertex : mesh.getVertices())
            string.append("v %f %f %f\n".formatted(vertex.x, vertex.y, vertex.z));

        //uvs
        for (Vector2f uv : mesh.getUVs())
            string.append("vt %f %f\n".formatted(uv.x, uv.y));

        //normals
        for (Vector3f normal : mesh.getNormals())
            string.append("vn %f %f %f\n".formatted(normal.x, normal.y, normal.z));

        //groups
        for (Group group : mesh.getGroups())
            writeGroup(string, group);

        //write obj file
        Path file = folder.resolve(meshName + ".obj");
        IOUtils.writeFile(file, string.toString().getBytes(StandardCharsets.UTF_8));

        //write material file
        Path materialFile = folder.resolve(meshName + ".mtl");
        IOUtils.writeFile(materialFile, mtlString.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void writeGroup(StringBuilder string, Group group) {
        string.append("g %s\n".formatted(group.getName()));
        string.append("usemtl %s\n".formatted(group.getMaterial().getName()));

        for (Face face : group.getFaces()) {
            List<Integer>
                    v = face.getVertices(),
                    vt = face.getUVs(),
                    vn = face.getNormals();

            string.append("f ");

            for (int i = 0; i < v.size(); i++) {
                //v always present
                string.append("%s".formatted(v.get(i) + 1));

                //append vt v/vt
                if (!vt.isEmpty())
                    string.append("/%s".formatted(vt.get(i) + 1));
                //if vt is not present, but we have vn, add '/' v//vn
                else if (!vn.isEmpty())
                    string.append("/");

                //append vn
                if (!vn.isEmpty())
                    string.append("/%s".formatted(vn.get(i) + 1));

                //spacing
                string.append(" ");
            }

            //new line
            string.append("\n");
        }
    }

    private static void writeMaterial(Path path, StringBuilder string, Material material) throws IOException {
        //name
        string.append("newmtl %s\n".formatted(material.getName()));

        //textures
        writeTexture(path, string, "map_Ka", material.getAmbientTex());
        writeTexture(path, string, "map_Kd", material.getDiffuseTex());
        writeTexture(path, string, "map_Ks", material.getSpColorTex());
        writeTexture(path, string, "map_Ns", material.getSpHighlightTex());
        writeTexture(path, string, "map_Ke", material.getEmissiveTex());
        writeTexture(path, string, "map_d", material.getAlphaTex());
        writeTexture(path, string, "map_bump", material.getBumpTex());
        writeTexture(path, string, "disp", material.getDisplacementTex());
        writeTexture(path, string, "decal", material.getStencilDecalTex());

        //properties
        writeVector("Ka", string, material.getAmbientColor());
        writeVector("Kd", string, material.getDiffuseColor());
        writeVector("Ks", string, material.getSpecularColor());
        writeVector("Tf", string, material.getFilterColor());

        string.append("Ns %s\n".formatted(material.getSpecularExponent()));
        string.append("Ni %s\n".formatted(material.getRefractionIndex()));
        string.append("illum %s\n".formatted(material.getIllumModel()));
    }

    private static void writeVector(String key, StringBuilder string, Vector3f vec) {
        string.append(key);
        string.append(" %s %s %s\n".formatted(vec.x, vec.y, vec.z));
    }

    private static void writeTexture(Path path, StringBuilder string, String key, Resource texture) throws IOException {
        if (texture == null)
            return;

        //write texture file
        String[] split = texture.getPath().split("/");
        String textureName = split[split.length - 1];

        InputStream input = IOUtils.getResource(texture);
        IOUtils.writeFile(path.resolve(textureName), input.readAllBytes());

        //write texture material
        string.append(key);
        string.append(" %s\n".formatted(textureName));
    }
}