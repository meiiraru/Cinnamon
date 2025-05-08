package cinnamon.parsers;

import cinnamon.model.material.Material;
import cinnamon.model.material.MaterialTexture;
import cinnamon.model.obj.Face;
import cinnamon.model.obj.Group;
import cinnamon.model.obj.Mesh;
import cinnamon.render.texture.Texture;
import cinnamon.utils.IOUtils;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class ObjExporter {

    public static final Path EXPORT_FOLDER = IOUtils.ROOT_FOLDER.resolve("exported");
    private static final DecimalFormat df = new DecimalFormat("#.######", DecimalFormatSymbols.getInstance(Locale.US));

    public static void export(String meshName, Mesh mesh) throws IOException {
        export(meshName, mesh, new Matrix4f(), EXPORT_FOLDER);
    }

    public static void export(String meshName, Mesh mesh, Matrix4f poseMat, Path exportTarget) throws IOException {
        //prepare variables
        Path folder = IOUtils.parseNonDuplicatePath(exportTarget.resolve(meshName));

        StringBuilder
                string = new StringBuilder(),
                mtlString = new StringBuilder();

        //materials
        string.append("mtllib %s.%s\n".formatted(meshName, "mtl"));

        for (Material material : mesh.getMaterials().values()) {
            //material name
            mtlString.append("newmtl %s\n".formatted(material.getName()));

            //material data
            writeMaterial(folder, mtlString, material);
        }

        //write vertices
        for (Vector3f vertex : mesh.getVertices()) {
            Vector4f v = new Vector4f(vertex, 1f).mul(poseMat);
            string.append("v %s %s %S\n".formatted(df.format(v.x), df.format(v.y), df.format(v.z)));
        }

        //uvs
        for (Vector2f uv : mesh.getUVs())
            string.append("vt %s %s\n".formatted(df.format(uv.x), df.format(uv.y)));

        //normals
        for (Vector3f normal : mesh.getNormals())
            string.append("vn %s %s %s\n".formatted(df.format(normal.x), df.format(normal.y), df.format(normal.z)));


        //check if the face should be inverted when the pose matrix scale is negative
        boolean invertFace = poseMat.determinant() < 0f;

        //groups
        for (Group group : mesh.getGroups())
            writeGroup(string, group, invertFace);

        //write obj file
        Path file = folder.resolve(meshName + ".obj");
        IOUtils.writeFile(file, string.toString().getBytes(StandardCharsets.UTF_8));

        //write material file
        Path materialFile = folder.resolve(meshName + ".mtl");
        IOUtils.writeFile(materialFile, mtlString.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void writeGroup(StringBuilder string, Group group, boolean invert) {
        string.append("g %s\n".formatted(group.getName()));
        string.append("usemtl %s\n".formatted(group.getMaterial().getName()));

        for (Face face : group.getFaces()) {
            List<Integer>
                    v = face.getVertices(),
                    vt = face.getUVs(),
                    vn = face.getNormals();

            string.append("f ");

            for (int i = 0; i < v.size(); i++) {
                int j = invert ? v.size() - 1 - i : i;

                //v always present
                string.append("%s".formatted(v.get(j) + 1));

                //append vt v/vt
                if (!vt.isEmpty())
                    string.append("/%s".formatted(vt.get(j) + 1));
                //if vt is not present, but we have vn, add '/' v//vn
                else if (!vn.isEmpty())
                    string.append("/");

                //append vn
                if (!vn.isEmpty())
                    string.append("/%s".formatted(vn.get(j) + 1));

                //spacing
                string.append(" ");
            }

            //new line
            string.append("\n");
        }
    }

    private static void writeMaterial(Path path, StringBuilder string, Material material) throws IOException {
        //textures
        writeTexture(path, string, "map_Kd", material.getAlbedo());
        writeTexture(path, string, "bump", material.getHeight());
        writeTexture(path, string, "norm", material.getNormal(), "-bm " + material.getHeightScale() + " ");
        writeTexture(path, string, "map_ao", material.getAO());
        writeTexture(path, string, "map_Pr", material.getRoughness());
        writeTexture(path, string, "map_Pm", material.getMetallic());
        writeTexture(path, string, "map_Ke", material.getEmissive());
    }

    private static void writeTexture(Path path, StringBuilder string, String key, MaterialTexture texture) throws IOException {
        writeTexture(path, string, key, texture, "");
    }

    private static void writeTexture(Path path, StringBuilder string, String key, MaterialTexture texture, String extra) throws IOException {
        if (texture == null)
            return;

        String textureName = texture.texture().getPath();
        if (texture.texture().getNamespace().isEmpty()) {
            textureName = textureName.replaceAll("\\\\", "/");
            textureName = textureName.substring(textureName.lastIndexOf('/') + 1);
        }

        //write texture file
        InputStream input = IOUtils.getResource(texture.texture());
        IOUtils.writeFile(path.resolve(textureName), input.readAllBytes());

        //write texture material
        string.append(key)
            .append(" ")
            .append(extra);

        for (Texture.TextureParams param : texture.params())
            string.append("-").append(param.aliases[0]).append(" ");

        string
                .append(textureName)
                .append("\n");
    }
}
