package cinnamon.parsers;

import cinnamon.model.material.Material;
import cinnamon.model.mesh.Face;
import cinnamon.model.mesh.Group;
import cinnamon.model.mesh.Mesh;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static cinnamon.events.Events.LOGGER;
import static cinnamon.math.Maths.parseVec2;
import static cinnamon.math.Maths.parseVec3;
import static java.lang.Integer.parseInt;

public class ObjLoader {

    public static Mesh load(Resource res) throws IOException {
        LOGGER.debug("Loading model \"%s\"", res);

        InputStream stream = IOUtils.getResource(res);
        if (stream == null)
            throw new RuntimeException("Resource not found: " + res);

        try (stream; InputStreamReader reader = new InputStreamReader(stream); BufferedReader br = new BufferedReader(reader)) {
            Mesh theMesh = new Mesh();
            Group currentGroup = new Group("default");
            Material currentMaterial = null;

            Vector3f bbMin = new Vector3f(Float.MAX_VALUE);
            Vector3f bbMax = new Vector3f(-Float.MAX_VALUE);
            Vector3f groupMin = new Vector3f(Float.MAX_VALUE);
            Vector3f groupMax = new Vector3f(-Float.MAX_VALUE);

            for (String line; (line = br.readLine()) != null; ) {
                //skip comments and empty lines
                line = line.trim();
                if (line.isBlank() || line.startsWith("#"))
                    continue;

                //grab first word on the line
                int firstSpace = line.indexOf(' ');
                String type = firstSpace == -1 ? line : line.substring(0, firstSpace);
                String data = firstSpace == -1 ? "" : line.substring(firstSpace + 1).trim();

                switch (type) {
                    //material file
                    case "mtllib" -> {
                        Resource material = res.resolveSibling(data);
                        try {
                            theMesh.getMaterials().putAll(MaterialLoader.load(material));
                        } catch (Exception e) {
                            LOGGER.error("Failed to load material file \"%s\"", material, e);
                        }
                    }

                    //group
                    case "g", "o" -> {
                        //add current group
                        addGroupToMesh(currentGroup, currentMaterial, theMesh, groupMin, groupMax);

                        //create new group
                        currentGroup = new Group(data);
                    }

                    //group material
                    case "usemtl" -> {
                        //add current group
                        addGroupToMesh(currentGroup, currentMaterial, theMesh, groupMin, groupMax);

                        //new material
                        currentMaterial = theMesh.getMaterials().get(data);
                        //create a new group with same name
                        currentGroup = new Group(currentGroup.getName());
                    }

                    //vertex
                    case "v" -> {
                        Vector3f v = parseVec3(data, ' ');
                        theMesh.getVertices().add(v);

                        bbMin.min(v);
                        bbMax.max(v);
                        groupMin.min(v);
                        groupMax.max(v);
                    }

                    //uv
                    case "vt" -> theMesh.getUVs().add(parseVec2(data, ' '));

                    //normal
                    case "vn" -> theMesh.getNormals().add(parseVec3(data, ' '));

                    //faces
                    case "f" -> currentGroup.getFaces().add(parseFace(data, theMesh));
                }
            }

            //add last group to the mesh
            addGroupToMesh(currentGroup, currentMaterial, theMesh, groupMin, groupMax);

            //set mesh bounding box
            theMesh.getBounds().set(bbMin, bbMax);

            //check for animations
            Resource anim = res.resolveSibling("animations.json");
            if (IOUtils.hasResource(anim)) {
                try {
                    theMesh.setAnimationData(AnimationLoader.load(anim));
                } catch (Exception e) {
                    LOGGER.error("Failed to load animations for model \"%s\"", res, e);
                }
            }

            //return the mesh
            return theMesh;
        }
    }

    private static void addGroupToMesh(Group group, Material material, Mesh mesh, Vector3f groupMin, Vector3f groupMax) {
        if (group.isEmpty())
            return;

        group.setMaterial(material);
        group.getBounds().set(groupMin, groupMax);

        mesh.getGroups().add(group);

        groupMin.set(Float.MAX_VALUE);
        groupMax.set(-Float.MAX_VALUE);
    }

    private static Face parseFace(String face, Mesh mesh) {
        String[] tokens = face.split(" ");

        int vertexCount = 0;
        for (String token : tokens) {
            if (!token.isBlank())
                vertexCount++;
        }

        //prepare arrays
        int[]
                v = new int[vertexCount],
                vt = null,
                vn = null;

        //fill arrays
        int i = 0;
        for (String s : tokens) {
            if (s.isBlank())
                continue;

            int firstSlash = s.indexOf('/');
            if (firstSlash == -1) {
                //v only
                v[i++] = parseIndex(s, mesh.getVertices().size());
                i++;
                continue;
            }

            int secondSlash = s.indexOf('/', firstSlash + 1);

            //v is always present
            v[i] = parseIndex(s.substring(0, firstSlash), mesh.getVertices().size());

            if (secondSlash == -1) {
                //missing a second slash, meaning its v/vt
                if (vt == null)
                    vt = new int[vertexCount];
                vt[i] = parseIndex(s.substring(firstSlash + 1), mesh.getUVs().size());
            } else {
                //v//vn or v/vt/vn
                //check if second slash is not immediately after first slash, meaning we have vt
                if (secondSlash > firstSlash + 1) {
                    if (vt == null)
                        vt = new int[vertexCount];
                    vt[i] = parseIndex(s.substring(firstSlash + 1, secondSlash), mesh.getUVs().size());
                }
                //add remaining part as vn
                if (vn == null) vn = new int[vertexCount];
                vn[i] = parseIndex(s.substring(secondSlash + 1), mesh.getNormals().size());
            }

            i++;
        }

        return new Face(v, vt, vn);
    }

    private static int parseIndex(String index, int size) {
        int idx = parseInt(index);
        return idx < 0 ? size + idx : idx - 1;
    }
}
