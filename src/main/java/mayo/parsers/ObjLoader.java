package mayo.parsers;

import mayo.model.obj.Face;
import mayo.model.obj.Group;
import mayo.model.obj.Mesh;
import mayo.utils.IOUtils;
import mayo.utils.Resource;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;
import static mayo.utils.Meth.parseVec3;

public class ObjLoader {

    public static Mesh load(Resource res) {
        InputStream stream = IOUtils.getResource(res);
        String path = res.getPath();
        String folder = path.substring(0, path.lastIndexOf("/") + 1);

        if (stream == null)
            throw new RuntimeException("Resource not found: " + res);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
            Mesh theMesh = new Mesh();
            Group currentGroup = new Group("default");

            for (String line; (line = br.readLine()) != null; ) {
                //skip comments and empty lines
                if (line.isBlank() || line.startsWith("#"))
                    continue;

                //grab first word on the line
                String[] split = line.trim().replaceAll(" +", " ").split(" ");
                switch (split[0]) {
                    //material file
                    case "mtllib" -> theMesh.getMaterials().putAll(MtlLoader.load(new Resource(res.getNamespace(), folder + split[1])));

                    //group
                    case "g", "o" -> {
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
                        currentGroup.getBBMin().min(vertex);
                        currentGroup.getBBMax().max(vertex);
                    }

                    //uv
                    case "vt" -> theMesh.getUVs().add(new Vector2f(parseFloat(split[1]), 1 - parseFloat(split[2])));

                    //normal
                    case "vn" -> theMesh.getNormals().add(parseVec3(split[1], split[2], split[3]));

                    //faces
                    case "f" -> currentGroup.getFaces().add(parseFace(split));
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

    private static Face parseFace(String[] split) {
        //prepare arrays
        List<Integer>
                v = new ArrayList<>(),
                vt = new ArrayList<>(),
                vn = new ArrayList<>();

        //fill arrays
        for (int i = 0; i < split.length - 1; i++) {
            String[] vtx = split[i + 1].split("/");

            //v is always present
            v.add(parseInt(vtx[0]) - 1);

            //try v/vt/vn
            if (vtx.length == 3) {
                //vn present
                vn.add(parseInt(vtx[2]) - 1);

                //test for vt (v//vn)
                if (!vtx[1].isBlank())
                    vt.add(parseInt(vtx[1]) - 1);
            }
            //then try v/vt
            else if (vtx.length == 2) {
                vt.add(parseInt(vtx[1]) - 1);
            }
        }

        //convert quads to tris

        //prepare arrays again
        List<Integer>
                vertices = new ArrayList<>(),
                uvs = new ArrayList<>(),
                normals = new ArrayList<>();

        //fill arrays
        for (int i = 1; i <= v.size() - 2; i++) {
            vertices.add(v.get(0));
            vertices.add(v.get(i));
            vertices.add(v.get(i + 1));

            if (!vt.isEmpty()) {
                uvs.add(vt.get(0));
                uvs.add(vt.get(i));
                uvs.add(vt.get(i + 1));
            }

            if (!vn.isEmpty()) {
                normals.add(vn.get(0));
                normals.add(vn.get(i));
                normals.add(vn.get(i + 1));
            }
        }

        return new Face(vertices, uvs, normals);
    }
}
