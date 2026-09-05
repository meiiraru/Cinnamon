package cinnamon.parsers;

import cinnamon.model.ModelTransform;
import cinnamon.model.material.Material;
import cinnamon.model.mesh.Face;
import cinnamon.model.mesh.Group;
import cinnamon.model.mesh.Mesh;
import cinnamon.render.MatrixStack;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class MergeMesh {

    public static void merge(Mesh src, Mesh other) {
        merge(src, other, null);
    }

    public static void merge(Mesh src, Mesh other, ModelTransform transform) {
        int vertOffset = src.getVertices().size();
        int normOffset = src.getNormals().size();
        int texOffset = src.getUVs().size();
        int tanOffset = src.getTangents().size();

        //merge vertex data
        if (transform == null) {
            src.getVertices().addAll(other.getVertices());
            src.getNormals().addAll(other.getNormals());
            src.getUVs().addAll(other.getUVs());
            src.getTangents().addAll(other.getTangents());
        } else {
            MatrixStack.Pose matrix = transform.getMatrix();

            for (Vector3f v : other.getVertices())
                src.getVertices().add(v.mulPosition(matrix.pos(), new Vector3f()));

            for (Vector3f n : other.getNormals())
                src.getNormals().add(n.mul(matrix.normal(), new Vector3f()));

            for (Vector2f uv : other.getUVs())
                src.getUVs().add(uv.add(transform.getUV(), new Vector2f()));

            for (Vector3f t : other.getTangents())
                src.getTangents().add(t.mul(matrix.normal(), new Vector3f()));
        }

        //merge groups
        for (Group group : other.getGroups()) {
            Group newGroup = new Group(group.getName());
            newGroup.getBounds().set(group.getBounds());
            src.getGroups().add(newGroup);

            //append materials
            if (group.getMaterial() != null) {
                Material material = group.getMaterial();
                String matName = material.getName();
                String newName = matName;

                for (int i = 1; src.getMaterials().containsKey(newName); i++)
                    newName = matName + "_" + i;

                Material newMaterial = new Material(newName);
                src.getMaterials().put(newName, newMaterial);
                newGroup.setMaterial(newMaterial);

                newMaterial.setAlbedo(material.getAlbedo());
                newMaterial.setHeight(material.getHeight());
                newMaterial.setNormal(material.getNormal());
                newMaterial.setAO(material.getAO());
                newMaterial.setRoughness(material.getRoughness());
                newMaterial.setMetallic(material.getMetallic());
                newMaterial.setEmissive(material.getEmissive());
                newMaterial.setHeightScale(material.getHeightScale());
                newMaterial.setAlphaCutout(material.getAlphaCutout());
            }

            //update indexes
            for (Face face : group.getFaces()) {
                int[] vertices = new int[face.getVertices().length];
                int[] uvs = null;
                int[] normals = null;
                int[] tangents = null;

                for (int i = 0; i < face.getVertices().length; i++)
                    vertices[i] = face.getVertices()[i] + vertOffset;

                if (face.hasUVs()) {
                    uvs = new int[face.getUVs().length];
                    for (int i = 0; i < face.getUVs().length; i++)
                        uvs[i] = face.getUVs()[i] + texOffset;
                }

                if (face.hasNormals()) {
                    normals = new int[face.getNormals().length];
                    for (int i = 0; i < face.getNormals().length; i++)
                        normals[i] = face.getNormals()[i] + normOffset;
                }

                if (face.hasTangents()) {
                    tangents = new int[face.getTangents().length];
                    for (int i = 0; i < face.getTangents().length; i++)
                        tangents[i] = face.getTangents()[i] + tanOffset;
                }

                Face newFace = new Face(vertices, uvs, normals, tangents);
                newGroup.getFaces().add(newFace);
            }
        }

        //merge bounds
        src.getBounds().merge(other.getBounds());
    }
}
