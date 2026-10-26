package cinnamon.model;

import cinnamon.animation.Animation;
import cinnamon.animation.Bone;
import cinnamon.model.material.Material;
import cinnamon.model.mesh.Face;
import cinnamon.model.mesh.Group;
import cinnamon.model.mesh.Mesh;
import cinnamon.render.MatrixStack;
import cinnamon.utils.Pair;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for performing operations on {@link Mesh} objects
 */
public final class MeshHelper {

    private MeshHelper() {}

    /**
     * Removes duplicate vertices, uvs, normals, and tangents from the given {@link Mesh}, modifying the mesh itself
     * @param mesh The {@link Mesh} to remove the duplicates
     */
    public static void stripDuplicateVertices(Mesh mesh) {
        //original mesh data
        List<Vector3f> vertices = mesh.getVertices();
        List<Vector2f> uvs      = mesh.getUVs();
        List<Vector3f> normals  = mesh.getNormals();
        List<Vector3f> tangents = mesh.getTangents();

        //index mapping arrays
        int[] vertexRemap  = new int[vertices.size()];
        int[] uvRemap      = new int[uvs.size()];
        int[] normalRemap  = new int[normals.size()];
        int[] tangentRemap = new int[tangents.size()];

        //build the deduplicated lists and populate the remap arrays
        List<Vector3f> newVertices = deduplicate(vertices, vertexRemap);
        List<Vector2f> newUVs      = deduplicate(uvs,      uvRemap);
        List<Vector3f> newNormals  = deduplicate(normals,  normalRemap);
        List<Vector3f> newTangents = deduplicate(tangents, tangentRemap);

        //update indexes on the faces with the remap arrays
        remapFaceIndexes(mesh, vertexRemap, uvRemap, normalRemap, tangentRemap);

        //update mesh data
        vertices.clear(); vertices.addAll(newVertices);
        uvs.clear(); uvs.addAll(newUVs);
        normals.clear(); normals.addAll(newNormals);
        tangents.clear(); tangents.addAll(newTangents);
    }

    /**
     * Removes unused vertices, uvs, normals, and tangents from the given {@link Mesh}, modifying the mesh itself
     * @param mesh The {@link Mesh} to remove the unused data
     */
    public static void clearUnusedVertices(Mesh mesh) {
        //original mesh data
        List<Vector3f> vertices = mesh.getVertices();
        List<Vector2f> uvs      = mesh.getUVs();
        List<Vector3f> normals  = mesh.getNormals();
        List<Vector3f> tangents = mesh.getTangents();

        boolean[] usedVertices = new boolean[vertices.size()];
        boolean[] usedUVs      = new boolean[uvs.size()];
        boolean[] usedNormals  = new boolean[normals.size()];
        boolean[] usedTangents = new boolean[tangents.size()];

        //mark used indices
        for (Group group : mesh.getGroups()) {
            for (Face face : group.getFaces()) {
                for (int index : face.getVertices())
                    usedVertices[index] = true;

                if (face.hasUVs())
                    for (int index : face.getUVs())
                        usedUVs[index] = true;

                if (face.hasNormals())
                    for (int index : face.getNormals())
                        usedNormals[index] = true;

                if (face.hasTangents())
                    for (int index : face.getTangents())
                        usedTangents[index] = true;
            }
        }

        //index mapping arrays
        int[] vertexRemap  = new int[vertices.size()];
        int[] uvRemap      = new int[uvs.size()];
        int[] normalRemap  = new int[normals.size()];
        int[] tangentRemap = new int[tangents.size()];

        //compact the lists removing unused data
        List<Vector3f> newVertices = compact(vertices, usedVertices, vertexRemap);
        List<Vector2f> newUVs      = compact(uvs, usedUVs, uvRemap);
        List<Vector3f> newNormals  = compact(normals, usedNormals, normalRemap);
        List<Vector3f> newTangents = compact(tangents, usedTangents, tangentRemap);

        //update indexes on the faces with the remap arrays
        remapFaceIndexes(mesh, vertexRemap, uvRemap, normalRemap, tangentRemap);

        //update mesh data
        vertices.clear(); vertices.addAll(newVertices);
        uvs.clear(); uvs.addAll(newUVs);
        normals.clear(); normals.addAll(newNormals);
        tangents.clear(); tangents.addAll(newTangents);
    }

    private static <T> List<T> deduplicate(List<T> original, int[] remap) {
        Map<T, Integer> map = new HashMap<>();
        List<T> uniqueList = new ArrayList<>();

        for (int i = 0; i < original.size(); i++) {
            T item = original.get(i);
            Integer existingIndex = map.get(item);
            if (existingIndex == null) {
                int newIndex = uniqueList.size();
                map.put(item, newIndex);
                uniqueList.add(item);
                remap[i] = newIndex;
            } else {
                remap[i] = existingIndex;
            }
        }
        return uniqueList;
    }

    private static <T> List<T> compact(List<T> original, boolean[] used, int[] remap) {
        List<T> compactedList = new ArrayList<>();
        for (int i = 0; i < original.size(); i++) {
            if (used[i]) {
                remap[i] = compactedList.size();
                compactedList.add(original.get(i));
            } else {
                remap[i] = -1; //-1 flag as deleted
            }
        }
        return compactedList;
    }

    private static void remapFaceIndexes(Mesh mesh, int[] vRemap, int[] uvRemap, int[] nRemap, int[] tRemap) {
        for (Group group : mesh.getGroups()) {
            for (Face face : group.getFaces()) {
                //vertices
                int[] faceVertices = face.getVertices();
                for (int j = 0; j < faceVertices.length; j++)
                    faceVertices[j] = vRemap[faceVertices[j]];

                //uvs
                if (face.hasUVs()) {
                    int[] faceUVs = face.getUVs();
                    for (int j = 0; j < faceUVs.length; j++)
                        faceUVs[j] = uvRemap[faceUVs[j]];
                }

                //normals
                if (face.hasNormals()) {
                    int[] faceNormals = face.getNormals();
                    for (int j = 0; j < faceNormals.length; j++)
                        faceNormals[j] = nRemap[faceNormals[j]];
                }

                //tangents
                if (face.hasTangents()) {
                    int[] faceTangents = face.getTangents();
                    for (int j = 0; j < faceTangents.length; j++)
                        faceTangents[j] = tRemap[faceTangents[j]];
                }
            }
        }
    }

    /**
     * Centers the mesh around the origin by subtracting the center of the mesh bounding box from each vertex position
     * @param mesh The {@link Mesh} to center
     */
    public static void centerMesh(Mesh mesh) {
        Vector3f center = mesh.getBounds().getCenter();
        for (Vector3f vertex : mesh.getVertices())
            vertex.sub(center);
    }

    /**
     * Merges the vertex data, groups, and materials of a {@link Mesh} into another mesh<br>
     * The source mesh will be modified to include the data from the other mesh<br>
     * The other mesh will remain unchanged
     * @param src The {@link Mesh} to merge into
     * @param other The {@link Mesh} to merge from
     * @see #merge(Mesh, Mesh, ModelTransform)
     */
    public static void merge(Mesh src, Mesh other) {
        merge(src, other, null);
    }

    /**
     * Merges the vertex data, groups, and materials of a {@link Mesh} into another mesh with an optional {@link ModelTransform} applied to the other mesh vertex data<br>
     * The source mesh will be modified to include the data from the other mesh<br>
     * The other mesh will remain unchanged
     * @param src The {@link Mesh} to merge into
     * @param other The {@link Mesh} to merge from
     * @param transform An optional {@link ModelTransform} to apply to the other mesh vertex data before merging
     * @see #merge(Mesh, Mesh)
     */
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
                newMaterial.setNormalScale(material.getNormalScale());
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

    /**
     * Creates a deep copy of the given {@link Mesh}, including its vertex data, groups, and materials
     * @param mesh The {@link Mesh} to clone
     * @return A new {@link Mesh} that is a deep copy of the given mesh
     */
    public static Mesh clone(Mesh mesh) {
        //create new mesh
        Mesh newMesh = new Mesh();
        newMesh.getBounds().set(mesh.getBounds());

        //copy vertex data
        for (Vector3f v : mesh.getVertices())
            newMesh.getVertices().add(new Vector3f(v));

        for (Vector2f uv : mesh.getUVs())
            newMesh.getUVs().add(new Vector2f(uv));

        for (Vector3f n : mesh.getNormals())
            newMesh.getNormals().add(new Vector3f(n));

        for (Vector3f t : mesh.getTangents())
            newMesh.getTangents().add(new Vector3f(t));

        //copy materials
        for (Map.Entry<String, Material> entry : mesh.getMaterials().entrySet()) {
            String name = entry.getKey();
            Material material = entry.getValue();
            Material newMaterial = new Material(name);

            newMaterial.setAlbedo(material.getAlbedo());
            newMaterial.setHeight(material.getHeight());
            newMaterial.setNormal(material.getNormal());
            newMaterial.setAO(material.getAO());
            newMaterial.setRoughness(material.getRoughness());
            newMaterial.setMetallic(material.getMetallic());
            newMaterial.setEmissive(material.getEmissive());
            newMaterial.setHeightScale(material.getHeightScale());
            newMaterial.setNormalScale(material.getNormalScale());
            newMaterial.setAlphaCutout(material.getAlphaCutout());
            newMesh.getMaterials().put(name, newMaterial);
        }

        //copy groups
        for (Group group : mesh.getGroups()) {
            Group newGroup = new Group(group.getName());
            newGroup.getBounds().set(group.getBounds());
            newMesh.getGroups().add(newGroup);

            //copy faces
            for (Face face : group.getFaces()) {
                int[] vertices = face.getVertices().clone();
                int[] uvs      = face.hasUVs()      ? face.getUVs().clone()      : null;
                int[] normals  = face.hasNormals()  ? face.getNormals().clone()  : null;
                int[] tangents = face.hasTangents() ? face.getTangents().clone() : null;

                Face newFace = new Face(vertices, uvs, normals, tangents);
                newGroup.getFaces().add(newFace);
            }

            //copy material reference
            if (group.getMaterial() != null) {
                String matName = group.getMaterial().getName();
                Material newMaterial = newMesh.getMaterials().get(matName);
                newGroup.setMaterial(newMaterial);
            }
        }

        //copy animation data
        Pair<Bone, List<Animation>> animData = mesh.getAnimationData();
        if (animData != null) {
            Map<Bone, Bone> boneMap = new HashMap<>();
            Bone bone = new Bone(animData.first(), boneMap);

            List<Animation> animations = new ArrayList<>();
            for (Animation animation : animData.second())
                animations.add(new Animation(animation, boneMap));

            newMesh.setAnimationData(Pair.of(bone, animations));
        }

        //return the new mesh
        return newMesh;
    }

    /**
     * Recalculates the bounding boxes of the given {@link Mesh} and its {@link Group}s based on their vertex data
     * @param mesh The {@link Mesh} to recalculate the bounding boxes for
     */
    public static void recalculateBounds(Mesh mesh) {
        //calculate bounding boxes
        Vector3f bbMin = new Vector3f(Float.MAX_VALUE);
        Vector3f bbMax = new Vector3f(-Float.MAX_VALUE);

        for (Group group : mesh.getGroups()) {
            if (group.isEmpty()) {
                group.getBounds().set(0, 0, 0, 0, 0, 0);
                continue;
            }

            Vector3f groupMin = new Vector3f(Float.MAX_VALUE);
            Vector3f groupMax = new Vector3f(-Float.MAX_VALUE);

            for (Face face : group.getFaces()) {
                for (int vertex : face.getVertices()) {
                    Vector3f v = mesh.getVertices().get(vertex);
                    groupMin.min(v);
                    groupMax.max(v);
                }
            }

            group.getBounds().set(groupMin, groupMax);
            bbMin.min(groupMin);
            bbMax.max(groupMax);
        }

        if (!mesh.getVertices().isEmpty())
            mesh.getBounds().set(bbMin, bbMax);
        else
            mesh.getBounds().set(0, 0, 0, 0, 0, 0);
    }

    /**
     * Bakes the static animation bone transforms (pivot position and rotation) directly into the mesh vertices<br>
     * This functionally flattens the mesh geometry and clears out the skeleton/animation data
     * @param mesh The {@link Mesh} to bake
     */
    public static void bakeBones(Mesh mesh) {
        Pair<Bone, List<Animation>> animData = mesh.getAnimationData();
        if (animData == null)
            return;

        //bake the bone transforms into the group matrices map
        Bone root = animData.first();
        Map<String, MatrixStack.Pose> groupMatrices = new HashMap<>();
        bakeGroupTransforms(root, new MatrixStack(), groupMatrices);

        //backup the existing mesh data arrays before repopulating
        List<Vector3f> oldVertices = new ArrayList<>(mesh.getVertices());
        List<Vector2f> oldUVs      = new ArrayList<>(mesh.getUVs());
        List<Vector3f> oldNormals  = new ArrayList<>(mesh.getNormals());
        List<Vector3f> oldTangents = new ArrayList<>(mesh.getTangents());

        mesh.getVertices().clear();
        mesh.getUVs().clear();
        mesh.getNormals().clear();
        mesh.getTangents().clear();

        //bake the transforms into the mesh data
        for (Group group : mesh.getGroups()) {
            MatrixStack.Pose mat = groupMatrices.get(group.getName());

            for (Face face : group.getFaces()) {
                //vertices
                int[] vIndices = face.getVertices();
                for (int i = 0; i < vIndices.length; i++) {
                    Vector3f v = new Vector3f(oldVertices.get(vIndices[i]));
                    if (mat != null) v.mulPosition(mat.pos());

                    vIndices[i] = mesh.getVertices().size();
                    mesh.getVertices().add(v);
                }

                //uvs (no transform applied)
                if (face.hasUVs()) {
                    int[] uvIndices = face.getUVs();
                    for (int i = 0; i < uvIndices.length; i++) {
                        Vector2f uv = new Vector2f(oldUVs.get(uvIndices[i]));

                        uvIndices[i] = mesh.getUVs().size();
                        mesh.getUVs().add(uv);
                    }
                }

                //normals
                if (face.hasNormals()) {
                    int[] nIndices = face.getNormals();
                    for (int i = 0; i < nIndices.length; i++) {
                        Vector3f n = new Vector3f(oldNormals.get(nIndices[i]));
                        if (mat != null) n.mul(mat.normal());

                        nIndices[i] = mesh.getNormals().size();
                        mesh.getNormals().add(n);
                    }
                }

                //tangents
                if (face.hasTangents()) {
                    int[] tIndices = face.getTangents();
                    for (int i = 0; i < tIndices.length; i++) {
                        Vector3f t = new Vector3f(oldTangents.get(tIndices[i]));
                        if (mat != null) t.mul(mat.normal());

                        tIndices[i] = mesh.getTangents().size();
                        mesh.getTangents().add(t);
                    }
                }
            }
        }

        //clean up any redundant vertices after baking
        stripDuplicateVertices(mesh);
    }

    private static void bakeGroupTransforms(Bone bone, MatrixStack matrices, Map<String, MatrixStack.Pose> groupMatrices) {
        //copy the transform to the matrix stack
        matrices.pushMatrix();
        bone.getTransform()
                .clearAnimationPose()
                .applyTransform(matrices);

        for (Bone child : bone.getChildren()) {
            if (child.isModel()) {
                //store the current matrix for this group
                groupMatrices.put(child.getName(), matrices.peek());
            } else {
                bakeGroupTransforms(child, matrices, groupMatrices);
            }
        }

        //clear the transform after baking
        bone.getTransform().identity();
        matrices.popMatrix();
    }
}
