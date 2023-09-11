package mayo.parsers;

import mayo.model.obj.Face;
import mayo.model.obj.Group;
import mayo.model.obj.Mesh2;
import mayo.utils.IOUtils;
import mayo.utils.Resource;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static java.lang.Integer.parseInt;
import static mayo.parsers.Parser.parseVec2;
import static mayo.parsers.Parser.parseVec3;

public class ObjLoader {

    public static Mesh2 load(Resource res) {
        InputStream stream = IOUtils.getResource(res);
        String path = res.getPath();
        String folder = path.substring(0, path.lastIndexOf("/") + 1);

        if (stream == null)
            throw new RuntimeException("Resource not found: " + res);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
            Mesh2 theMesh = new Mesh2();
            Group currentGroup = new Group("default");

            for (String line; (line = br.readLine()) != null; ) {
                //skip comments and empty lines
                if (line.isBlank() || line.startsWith("#"))
                    continue;

                //grab first word on the line
                String[] split = line.split(" ");
                switch (split[0]) {
                    //material file
                    case "mtllib" -> theMesh.getMaterials().putAll(MtlLoader.load(new Resource(res.getNamespace(), folder + split[1])));

                    //group
                    case "g" -> {
                        if (!currentGroup.isEmpty())
                            theMesh.getGroups().add(currentGroup);
                        currentGroup = new Group(split[1]);
                    }

                    //smooth shading
                    case "s" -> currentGroup.setSmooth(!split[1].contains("off"));

                    //group material
                    case "usemtl" -> currentGroup.setMaterial(theMesh.getMaterials().get(split[1]));

                    //vertex
                    case "v" -> {
                        Vector3f vertex = parseVec3(split[1], split[2], split[3]);
                        theMesh.getVertices().add(vertex);
                        theMesh.getBBMin().min(vertex);
                        theMesh.getBBMax().max(vertex);
                    }

                    //uv
                    case "vt" -> theMesh.getUVs().add(parseVec2(split[1], split[2]));

                    //normal
                    case "vn" -> theMesh.getNormals().add(parseVec3(split[1], split[2], split[3]));

                    //faces
                    case "f" -> {
                        // v/vt/vn v/vt/vn v/vt/vn
                        int[] v1 = parseFace(split[1]);
                        int[] v2 = parseFace(split[2]);
                        int[] v3 = parseFace(split[3]);

                        currentGroup.getFaces().add(new Face(
                                new int[]{v1[0], v2[0], v3[0]},
                                new int[]{v1[1], v2[1], v3[1]},
                                new int[]{v1[2], v2[2], v3[2]}
                        ));
                    }
                }
            }

            //add group to the mesh
            if (!currentGroup.isEmpty())
                theMesh.getGroups().add(currentGroup);

            //return the mesh
            return theMesh;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load obj \"" + path + "\"", e);
        }
    }

    private static int[] parseFace(String vertex) {
        String[] s = vertex.split("/");
        return new int[]{parseInt(s[0]) - 1, parseInt(s[1]) - 1, parseInt(s[2]) - 1};
    }
}
