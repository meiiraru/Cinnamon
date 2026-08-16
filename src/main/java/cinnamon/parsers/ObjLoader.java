package cinnamon.parsers;

import cinnamon.model.material.Material;
import cinnamon.model.obj.Face;
import cinnamon.model.obj.Group;
import cinnamon.model.obj.Mesh;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static cinnamon.events.Events.LOGGER;
import static cinnamon.math.Maths.parseVec2;
import static cinnamon.math.Maths.parseVec3;
import static java.lang.Integer.parseInt;

public class ObjLoader {

    private static final Vector3f bbMin = new Vector3f(Float.MAX_VALUE);
    private static final Vector3f bbMax = new Vector3f(-Float.MAX_VALUE);
    private static final Vector3f groupMin = new Vector3f(Float.MAX_VALUE);
    private static final Vector3f groupMax = new Vector3f(-Float.MAX_VALUE);

    public static Mesh load(Resource res) throws IOException {
        LOGGER.debug("Loading model \"%s\"", res);

        InputStream stream = IOUtils.getResource(res);
        if (stream == null)
            throw new RuntimeException("Resource not found: " + res);

        try (stream; InputStreamReader reader = new InputStreamReader(stream); BufferedReader br = new BufferedReader(reader)) {
            Mesh theMesh = new Mesh();
            Group currentGroup = new Group("default");
            Material currentMaterial = null;

            bbMin.set(Float.MAX_VALUE);
            bbMax.set(-Float.MAX_VALUE);
            groupMin.set(Float.MAX_VALUE);
            groupMax.set(-Float.MAX_VALUE);

            for (String line; (line = br.readLine()) != null; ) {
                //skip comments and empty lines
                if (line.isBlank() || line.startsWith("#"))
                    continue;

                //grab first word on the line
                String[] split = line.split(" +", 2);
                switch (split[0]) {
                    //material file
                    case "mtllib" -> {
                        Resource material = res.resolveSibling(split[1]);
                        try {
                            theMesh.getMaterials().putAll(MaterialLoader.load(material));
                        } catch (Exception e) {
                            LOGGER.error("Failed to load material file \"%s\"", material, e);
                        }
                    }

                    //group
                    case "g", "o" -> {
                        //add current group
                        addGroupToMesh(currentGroup, currentMaterial, theMesh);

                        //create new group
                        currentGroup = new Group(split[1]);
                    }

                    //group material
                    case "usemtl" -> {
                        //add current group
                        addGroupToMesh(currentGroup, currentMaterial, theMesh);

                        //new material
                        currentMaterial = theMesh.getMaterials().get(split[1]);
                        //create a new group with same name
                        currentGroup = new Group(currentGroup.getName());
                    }

                    //vertex
                    case "v" -> {
                        Vector3f v = parseVec3(split[1], " +");
                        theMesh.getVertices().add(v);

                        bbMin.min(v);
                        bbMax.max(v);
                        groupMin.min(v);
                        groupMax.max(v);
                    }

                    //uv
                    case "vt" -> theMesh.getUVs().add(parseVec2(split[1], " +"));

                    //normal
                    case "vn" -> theMesh.getNormals().add(parseVec3(split[1], " +"));

                    //faces
                    case "f" -> currentGroup.getFaces().add(parseFace(split[1], theMesh));
                }
            }

            //add last group to the mesh
            addGroupToMesh(currentGroup, currentMaterial, theMesh);

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

    private static void addGroupToMesh(Group group, Material material, Mesh mesh) {
        if (group.isEmpty())
            return;

        group.setMaterial(material);
        group.getBounds().set(groupMin, groupMax);

        mesh.getGroups().add(group);

        groupMin.set(Float.MAX_VALUE);
        groupMax.set(-Float.MAX_VALUE);
    }

    private static Face parseFace(String face, Mesh mesh) {
        String[] split = face.split(" +");

        //prepare arrays
        List<Integer>
                v = new ArrayList<>(),
                vt = new ArrayList<>(),
                vn = new ArrayList<>();

        //fill arrays
        for (String s : split) {
            String[] vtx = s.split("/");

            //v is always present
            v.add(parseIndex(vtx[0], mesh.getVertices().size()));

            //try v/vt/vn
            if (vtx.length == 3) {
                //vn present
                vn.add(parseIndex(vtx[2], mesh.getNormals().size()));

                //test for vt (v//vn)
                if (!vtx[1].isBlank())
                    vt.add(parseIndex(vtx[1], mesh.getUVs().size()));
            }
            //then try v/vt
            else if (vtx.length == 2)
                vt.add(parseIndex(vtx[1], mesh.getUVs().size()));
        }

        return new Face(v, vt, vn);
    }

    private static int parseIndex(String index, int size) {
        int idx = parseInt(index);
        if (idx < 0)
            idx = size + idx;
        else
            idx = idx - 1;
        return idx;
    }
}
