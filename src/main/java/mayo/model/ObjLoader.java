package mayo.model;

import mayo.utils.IOUtils;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ObjLoader {

    public static Mesh2 load(String namespace, String path) {
        InputStream stream = IOUtils.getResource(namespace, "models/" + path + ".obj");
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
                    case "mtllib" -> theMesh.setMtllib(split[1]);

                    //group
                    case "g" -> {
                        if (!currentGroup.isEmpty())
                            theMesh.getGroups().add(currentGroup);
                        currentGroup = new Group(split[1]);
                    }

                    //smooth shading
                    case "s" -> currentGroup.setSmooth(!split[1].contains("off"));

                    //group material
                    case "usemtl" -> currentGroup.setMaterial(split[1]);

                    //vertex
                    case "v" -> theMesh.getVertices().add(parseVec3(split[1], split[2], split[3]));

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

            //bake mesh VAOs and VBOs
            theMesh.bake();

            //return the mesh
            return theMesh;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load obj \"" + path + "\"", e);
        }
    }

    private static Vector2f parseVec2(String x, String y) {
        return new Vector2f(Float.parseFloat(x), Float.parseFloat(y));
    }

    private static Vector3f parseVec3(String x, String y, String z) {
        return new Vector3f(Float.parseFloat(x), Float.parseFloat(y), Float.parseFloat(z));
    }

    private static int[] parseFace(String vertex) {
        String[] s = vertex.split("/");
        return new int[]{Integer.parseInt(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2])};
    }
}
