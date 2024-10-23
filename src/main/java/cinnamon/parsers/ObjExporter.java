package cinnamon.parsers;

import cinnamon.model.obj.Face;
import cinnamon.model.obj.Group;
import cinnamon.model.obj.Mesh;
import cinnamon.model.material.Material;
import cinnamon.model.material.MtlMaterial;
import cinnamon.model.material.PBRMaterial;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
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
        boolean isPbr = mesh.getMaterials().values().stream().anyMatch(material -> material instanceof PBRMaterial);
        string.append("mtllib %s.%s\n".formatted(meshName, isPbr ? "pbr" : "mtl"));

        for (Material material : mesh.getMaterials().values()) {
            //material name
            mtlString.append("newmtl %s\n".formatted(material.getName()));

            //material data
            if (material instanceof PBRMaterial pbr) {
                writePBR(folder, mtlString, pbr);
            } else if (material instanceof MtlMaterial mtl) {
                writeMtl(folder, mtlString, mtl);
            }
        }

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
        Path materialFile = folder.resolve(meshName + (isPbr ? ".pbr" : ".mtl"));
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

    private static void writeMtl(Path path, StringBuilder string, MtlMaterial material) throws IOException {
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

    private static void writePBR(Path path, StringBuilder string, PBRMaterial material) throws IOException {
        //textures
        writeTexture(path, string, "albedo", material.getAlbedo());
        writeTexture(path, string, "height", material.getHeight());
        writeTexture(path, string, "normal", material.getNormal());
        writeTexture(path, string, "roughness", material.getRoughness());
        writeTexture(path, string, "metallic", material.getMetallic());
        writeTexture(path, string, "ao", material.getAO());
        writeTexture(path, string, "emissive", material.getEmissive());
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
