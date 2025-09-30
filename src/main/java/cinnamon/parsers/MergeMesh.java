package cinnamon.parsers;

import cinnamon.model.Transform;
import cinnamon.model.material.Material;
import cinnamon.model.obj.Face;
import cinnamon.model.obj.Group;
import cinnamon.model.obj.Mesh;
import cinnamon.render.MatrixStack;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;

public class MergeMesh {

    public static void merge(Mesh src, Mesh other) {
        merge(src, other, null);
    }

    public static void merge(Mesh src, Mesh other, Transform transform) {
        int vertOffset = src.getVertices().size();
        int normOffset = src.getNormals().size();
        int texOffset = src.getUVs().size();

        //merge vertex data
        if (transform == null) {
            src.getVertices().addAll(other.getVertices());
            src.getNormals().addAll(other.getNormals());
            src.getUVs().addAll(other.getUVs());
        } else {
            MatrixStack.Matrices matrix = transform.getMatrix();

            for (Vector3f v : other.getVertices())
                src.getVertices().add(v.mulPosition(matrix.pos(), new Vector3f()));

            for (Vector3f n : other.getNormals())
                src.getNormals().add(n.mul(matrix.normal(), new Vector3f()));

            for (Vector2f uv : other.getUVs())
                src.getUVs().add(uv.add(transform.getUV(), new Vector2f()));
        }

        //merge groups
        for (Group group : other.getGroups()) {
            Group newGroup = new Group(group.getName());
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
            }

            //update indexes
            for (Face face : group.getFaces()) {
                Face newFace = new Face(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
                newGroup.getFaces().add(newFace);

                for (Integer vertex : face.getVertices())
                    newFace.getVertices().add(vertex + vertOffset);
                for (Integer uv : face.getUVs())
                    newFace.getUVs().add(uv + texOffset);
                for (Integer normal : face.getNormals())
                    newFace.getNormals().add(normal + normOffset);
            }
        }
    }
}
