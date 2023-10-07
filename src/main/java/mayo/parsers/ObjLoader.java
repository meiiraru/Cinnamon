package mayo.parsers;

import mayo.model.obj.Face;
import mayo.model.obj.Group;
import mayo.model.obj.Material;
import mayo.model.obj.Mesh;
import mayo.utils.IOUtils;
import mayo.utils.Resource;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.parseInt;
import static mayo.utils.Maths.parseVec2;
import static mayo.utils.Maths.parseVec3;

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
            Material currentMaterial = null;

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
                        //add current group
                        if (!currentGroup.isEmpty()) {
                            currentGroup.setMaterial(currentMaterial);
                            theMesh.getGroups().add(currentGroup);
                        }

                        //parse name
                        StringBuilder sb = new StringBuilder();
                        for (int i = 1; i < split.length; i++)
                            sb.append(split[i]).append(" ");

                        //create new group
                        currentGroup = new Group(sb.toString().trim());
                    }

                    //smooth shading
                    case "s" -> currentGroup.setSmooth(!split[1].contains("off"));

                    //group material
                    case "usemtl" -> {
                        //add current group
                        if (!currentGroup.isEmpty()) {
                            currentGroup.setMaterial(currentMaterial);
                            theMesh.getGroups().add(currentGroup);
                        }

                        //new material
                        currentMaterial = theMesh.getMaterials().get(split[1]);
                        //create a new group with same name
                        currentGroup = new Group(currentGroup.getName());
                    }

                    //vertex
                    case "v" -> {
                        Vector3f vertex = parseVec3(split[1], split[2], split[3]);
                        theMesh.getVertices().add(vertex);

                        //mesh min max
                        theMesh.getBBMin().min(vertex);
                        theMesh.getBBMax().max(vertex);
                    }

                    //uv
                    case "vt" -> theMesh.getUVs().add(parseVec2(split[1], split[2]));

                    //normal
                    case "vn" -> theMesh.getNormals().add(parseVec3(split[1], split[2], split[3]));

                    //faces
                    case "f" -> {
                        Face face = parseFace(split);
                        currentGroup.getFaces().add(face);

                        //group min max
                        for (Integer index : face.getVertices()) {
                            Vector3f vertex = theMesh.getVertices().get(index);
                            currentGroup.getBBMin().min(vertex);
                            currentGroup.getBBMax().max(vertex);
                        }
                    }
                }
            }

            //add last group to the mesh
            if (!currentGroup.isEmpty()) {
                currentGroup.setMaterial(currentMaterial);
                theMesh.getGroups().add(currentGroup);
            }

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

        return new Face(v, vt, vn);
    }
}
