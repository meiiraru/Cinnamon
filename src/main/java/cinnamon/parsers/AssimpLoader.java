package cinnamon.parsers;

import cinnamon.model.material.Material;
import cinnamon.model.material.MaterialTexture;
import cinnamon.model.mesh.Face;
import cinnamon.model.mesh.Group;
import cinnamon.model.mesh.Mesh;
import cinnamon.render.texture.Texture;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static cinnamon.events.Events.LOGGER;
import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.system.MemoryUtil.memUTF8;

public class AssimpLoader {

    private static final int DEFAULT_FLAGS =
            aiProcess_CalcTangentSpace |
            aiProcess_JoinIdenticalVertices |
            aiProcess_Triangulate |
            aiProcess_GenSmoothNormals |
            aiProcess_SplitLargeMeshes |
            //aiProcess_ValidateDataStructure |
            aiProcess_RemoveRedundantMaterials |
            aiProcess_GenUVCoords |
            aiProcess_OptimizeMeshes |
            aiProcess_OptimizeGraph |
            aiProcess_GenBoundingBoxes;

    public static Mesh load(Resource res) throws Exception {
        LOGGER.debug("Loading model \"%s\"", res);        

        AIScene scene = getSceneFor(res);
        if (scene == null)
            throw new Exception(aiGetErrorString());

        Mesh mesh = new Mesh();

        PointerBuffer meshes = scene.mMeshes();
        int numMeshes = meshes == null ? 0 : meshes.limit();
        LOGGER.debug("Model has %s meshes", numMeshes);

        if (numMeshes == 0) {
            aiReleaseImport(scene);
            return mesh;
        }

        AINode root = scene.mRootNode();
        if (root == null) {
            LOGGER.debug("Model has no root node");
            aiReleaseImport(scene);
            return mesh;
        }

        //parse materials
        List<Material> materials = new ArrayList<>();
        PointerBuffer material = scene.mMaterials();
        int numMaterials = material == null ? 0 : material.limit();
        LOGGER.debug("Model has %s materials", numMaterials);
        for (int i = 0; i < numMaterials; i++) {
            AIMaterial aimaterial = AIMaterial.create(material.get(i));
            parseMaterial(scene, aimaterial, mesh, res, materials);
        }

        //parse nodes and meshes
        parseNode(root, meshes, new Matrix4f(), mesh, materials);

        //calculate the entire model AABB
        if (!mesh.getGroups().isEmpty())
            mesh.getBounds().set(mesh.getGroups().getFirst().getBounds());
        for (Group group : mesh.getGroups())
            mesh.getBounds().merge(group.getBounds());

        aiReleaseImport(scene);
        return mesh;
    }

    private static Vector3f parseVec3(AIVector3D vec) {
        return new Vector3f(vec.x(), vec.y(), vec.z());
    }

    private static Vector2f parseVec2(AIVector3D vec) {
        return new Vector2f(vec.x(), vec.y());
    }

    private static Matrix4f parseMatrix4f(AIMatrix4x4 mat) {
        return new Matrix4f(
                mat.a1(), mat.b1(), mat.c1(), mat.d1(),
                mat.a2(), mat.b2(), mat.c2(), mat.d2(),
                mat.a3(), mat.b3(), mat.c3(), mat.d3(),
                mat.a4(), mat.b4(), mat.c4(), mat.d4()
        );
    }

    private static AIScene getSceneFor(Resource res) {
        if (res.getNamespace().isEmpty())
            return aiImportFileEx(res.getPath(), DEFAULT_FLAGS, null);

        AIFileIO fileIO = AIFileIO.create()
                .OpenProc((pFileIO, fileName, openMode) -> {
                    ByteBuffer data;
                    String file = memUTF8(fileName);
                    try {
                        Resource resource = new Resource(res.getNamespace(), file);
                        LOGGER.debug("Opening file \"%s\"", resource);
                        data = IOUtils.getResourceBuffer(resource);
                    } catch (Exception e) {
                        throw new RuntimeException("Could not open file: " + file);
                    }

                    return AIFile.create()
                            .ReadProc((pFile, pBuffer, size, count) -> {
                                long bytesToRead = size * count;
                                if (data.position() + bytesToRead > data.limit())
                                    bytesToRead = data.limit() - data.position();

                                memCopy(memAddress(data), pBuffer + data.position(), bytesToRead);
                                data.position((int) (data.position() + bytesToRead));
                                return bytesToRead / size;
                            })
                            .SeekProc((pFile, offset, origin) -> {
                                switch (origin) {
                                    case aiOrigin_SET -> data.position((int) offset);
                                    case aiOrigin_CUR -> data.position((int) (data.position() + offset));
                                    case aiOrigin_END -> data.position((int) (data.limit() + offset));
                                }
                                return data.position() > data.limit() ? aiReturn_FAILURE : aiReturn_SUCCESS;
                            })
                            .FileSizeProc(pFile -> data.limit())
                            .address();
                })
                .CloseProc((pFileIO, pFile) -> {
                    AIFile aiFile = AIFile.create(pFile);
                    aiFile.ReadProc().free();
                    aiFile.SeekProc().free();
                    aiFile.FileSizeProc().free();
                });

        AIScene scene = aiImportFileEx(res.getPath(), DEFAULT_FLAGS, fileIO);
        fileIO.OpenProc().free();
        fileIO.CloseProc().free();

        return scene;
    }

    private static void parseNode(AINode node, PointerBuffer meshes, Matrix4f transform, Mesh mesh, List<Material> materials) {
        LOGGER.debug("Parsing node \"%s\"", node.mName().dataString());

        //parse node transformation
        Matrix4f matrix = transform.mul(parseMatrix4f(node.mTransformation()), new Matrix4f());

        //parse model
        IntBuffer meshIndexes = node.mMeshes();
        if (meshIndexes != null) {
            for (int i = 0; i < meshIndexes.limit(); i++) {
                int meshIndex = meshIndexes.get(i);
                AIMesh aimesh = AIMesh.create(meshes.get(meshIndex));
                processMesh(aimesh, mesh, matrix, materials);
            }
        }

        //recurse children
        PointerBuffer children = node.mChildren();
        if (children != null) {
            for (int i = 0; i < children.limit(); i++) {
                AINode childNode = AINode.create(children.get(i));
                parseNode(childNode, meshes, matrix, mesh, materials);
            }
        }
    }

    private static void processMesh(AIMesh aimesh, Mesh mesh, Matrix4f transform, List<Material> materials) {
        //create group
        Group group = new Group(aimesh.mName().dataString());
        mesh.getGroups().add(group);

        //aabb
        AIAABB aabb = aimesh.mAABB();
        group.getBounds()
                .set(aabb.mMin().x(), aabb.mMin().y(), aabb.mMin().z(), aabb.mMax().x(), aabb.mMax().y(), aabb.mMax().z())
                .applyMatrix(transform);
        LOGGER.debug("Mesh \"%s\" AABB %s", group.getName(), group.getBounds());

        //offsets of the vertex data
        int vOffset = mesh.getVertices().size();
        int uvOffset = mesh.getUVs().size();
        int nOffset = mesh.getNormals().size();
        int tOffset = mesh.getTangents().size();

        //vertex data
        AIVector3D.Buffer vertices = aimesh.mVertices();
        for (int i = 0; i < vertices.limit(); i++)
            mesh.getVertices().add(parseVec3(vertices.get(i)).mulPosition(transform));

        boolean hasUVs = false;
        AIVector3D.Buffer uvs = aimesh.mTextureCoords(0);
        if (uvs != null) {
            hasUVs = true;
            for (int i = 0; i < uvs.limit(); i++)
                mesh.getUVs().add(parseVec2(uvs.get(i)));
        }

        boolean hasNormals = false;
        AIVector3D.Buffer normals = aimesh.mNormals();
        if (normals != null) {
            hasNormals = true;
            Matrix3f normalMatrix = new Matrix3f(transform).invert().transpose();
            for (int i = 0; i < normals.limit(); i++)
                mesh.getNormals().add(parseVec3(normals.get(i)).mul(normalMatrix).normalize());
        }

        boolean hasTangents = false;
        AIVector3D.Buffer tangents = aimesh.mTangents();
        if (tangents != null) {
            hasTangents = true;
            for (int i = 0; i < tangents.limit(); i++)
                mesh.getTangents().add(parseVec3(tangents.get(i)).mulDirection(transform).normalize());
        }

        int materialIndex = aimesh.mMaterialIndex();
        group.setMaterial(materials.get(materialIndex));

        //faces
        processFaces(aimesh, group, vOffset, hasUVs ? uvOffset : -1, hasNormals ? nOffset : -1, hasTangents ? tOffset : -1);
    }

    private static void processFaces(AIMesh aimesh, Group group, int vOffset, int uvOffset, int nOffset, int tOffset) {
        int skips = 0;
        AIFace.Buffer faces = aimesh.mFaces();

        for (int i = 0; i < faces.limit(); i++) {
            AIFace aiface = faces.get(i);
            if (aiface.mNumIndices() < 3) {
                skips++;
                continue;
            }

            List<Integer> vIndices = new ArrayList<>();
            List<Integer> uvIndices = new ArrayList<>();
            List<Integer> nIndices = new ArrayList<>();
            List<Integer> tIndices = new ArrayList<>();

            IntBuffer indices = aiface.mIndices();
            for (int j = 0; j < indices.limit(); j++) {
                int index = indices.get(j);

                vIndices.add(index + vOffset);
                if (uvOffset != -1) uvIndices.add(index + uvOffset);
                if (nOffset != -1) nIndices.add(index + nOffset);
                if (tOffset != -1) tIndices.add(index + tOffset);
            }

            Face face = new Face(vIndices, uvIndices, nIndices, tIndices);
            group.getFaces().add(face);
        }

        if (skips > 0)
            LOGGER.debug("Skipped %d faces for group \"%s\" with less than 3 indices", skips, group.getName());
    }

    private static void parseMaterial(AIScene scene, AIMaterial aimaterial, Mesh mesh, Resource res, List<Material> materials) {
        AIString name = AIString.create();
        aiGetMaterialString(aimaterial, AI_MATKEY_NAME, 0, 0, name);

        String matName = name.dataString();
        Material material = new Material(matName);
        mesh.getMaterials().put(matName, material);
        materials.add(material);

        //parse textures
        material.setAlbedo(parseTexture(scene, aimaterial, aiTextureType_DIFFUSE, res, Texture.TextureParams.MIPMAP_SMOOTH));
        material.setHeight(parseTexture(scene, aimaterial, aiTextureType_HEIGHT, res));
        material.setNormal(parseTexture(scene, aimaterial, aiTextureType_NORMALS, res, Texture.TextureParams.SMOOTH_SAMPLING));
        material.setAO(parseTexture(scene, aimaterial, aiTextureType_AMBIENT_OCCLUSION, res));
        material.setRoughness(parseTexture(scene, aimaterial, aiTextureType_SHININESS, res));
        material.setMetallic(parseTexture(scene, aimaterial, aiTextureType_METALNESS, res));
        material.setEmissive(parseTexture(scene, aimaterial, aiTextureType_EMISSIVE, res));
    }

    private static MaterialTexture parseTexture(AIScene scene, AIMaterial aimaterial, int type, Resource res, Texture.TextureParams... params) {
        if (aiGetMaterialTextureCount(aimaterial, type) == 0)
            return null;

        AIString aipath = AIString.create();
        int result = aiGetMaterialTexture(aimaterial, type, 0, aipath, null, null, null, null, null, (IntBuffer) null);
        if (result != aiReturn_SUCCESS) {
            LOGGER.error("Failed to get material texture of type %d: %s", type, aiGetErrorString());
            return null;
        }

        String path = aipath.dataString();
        Resource texture;
        AITexture embedded = aiGetEmbeddedTexture(scene, path);

        LOGGER.debug("Found material (type %s) texture \"%s\"%s", type, path, embedded == null ? "" : " (embedded)");

        if (embedded != null) {
            texture = new Resource("assimp/" + res + "/" + path);
            //force load of embedded texture to assign the resource path
            Texture.of(texture, embedded, params);
        } else {
            texture = res.resolveSibling(path);
        }

        //create texture
        return new MaterialTexture(texture, params);
    }
}
