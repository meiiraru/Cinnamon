package cinnamon.parsers;

import cinnamon.model.assimp.Mesh;
import cinnamon.model.assimp.Model;
import cinnamon.model.material.Material;
import cinnamon.model.material.MaterialTexture;
import cinnamon.render.texture.Texture;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static cinnamon.events.Events.LOGGER;
import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.system.MemoryUtil.*;

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

    public static Model load(Resource res) {
        LOGGER.debug("Loading model %s", res);

        String path = res.getPath();
        String parentFolder = path.substring(0, path.lastIndexOf("/") + 1);
        Resource parent = new Resource(res.getNamespace(), parentFolder);

        AIFileIO fileIO = AIFileIO.create()
                .OpenProc((pFileIO, fileName, openMode) -> {
                    ByteBuffer data;
                    String file = memUTF8(fileName);
                    try {
                        Resource resource = new Resource(res.getNamespace(), file);
                        LOGGER.debug("Opening file %s", resource);
                        data = IOUtils.getResourceBuffer(resource);
                    } catch (Exception e) {
                        throw new RuntimeException("Could not open file: " + file);
                    }

                    return AIFile.create()
                            .ReadProc((pFile, pBuffer, size, count) -> {
                                long max = Math.min(data.remaining() / size, count);
                                memCopy(memAddress(data), pBuffer, max * size);
                                data.position(data.position() + (int) (max * size));
                                return max;
                            })
                            .SeekProc((pFile, offset, origin) -> {
                                if (origin == Assimp.aiOrigin_CUR)
                                    data.position(data.position() + (int) offset);
                                else if (origin == Assimp.aiOrigin_SET)
                                    data.position((int) offset);
                                else if (origin == Assimp.aiOrigin_END)
                                    data.position(data.limit() + (int) offset);
                                return 0;
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

        try {
            AIScene scene = aiImportFileEx(path, DEFAULT_FLAGS, fileIO);
            String error = aiGetErrorString();
            if (error != null && !error.isEmpty() || scene == null) {
                aiReleaseImport(scene);
                throw new Exception(error);
            }

            Model model = new Model();

            PointerBuffer meshes = scene.mMeshes();
            int numMeshes = meshes == null ? 0 : meshes.limit();
            LOGGER.debug("Model has %s meshes", numMeshes);

            if (numMeshes == 0) {
                aiReleaseImport(scene);
                return model;
            }

            AINode root = scene.mRootNode();
            if (root == null) {
                LOGGER.debug("Model has no root node");
                aiReleaseImport(scene);
                return model;
            }

            //parse nodes and meshes
            parseNode(root, meshes, new Matrix4f(), model);

            //parse materials
            PointerBuffer material = scene.mMaterials();
            int numMaterials = material == null ? 0 : material.limit();
            LOGGER.debug("Model has %s materials", numMaterials);
            for (int i = 0; i < numMaterials; i++) {
                AIMaterial aimaterial = AIMaterial.create(material.get(i));
                parseMaterial(aimaterial, model, parent);
            }

            aiReleaseImport(scene);
            return model;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load model \"" + res + "\"", e);
        }
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

    private static void parseNode(AINode node, PointerBuffer meshes, Matrix4f transform, Model model) {
        LOGGER.debug("Parsing node %s", node.mName().dataString());

        //parse node transformation
        Matrix4f matrix = transform.mul(parseMatrix4f(node.mTransformation()), new Matrix4f());

        //parse model
        IntBuffer meshIndexes = node.mMeshes();
        if (meshIndexes != null) {
            for (int i = 0; i < meshIndexes.limit(); i++) {
                int meshIndex = meshIndexes.get(i);
                AIMesh aimesh = AIMesh.create(meshes.get(meshIndex));
                processMesh(aimesh, model, matrix);
            }
        }

        //recurse children
        PointerBuffer children = node.mChildren();
        if (children != null) {
            for (int i = 0; i < children.limit(); i++) {
                AINode childNode = AINode.create(children.get(i));
                parseNode(childNode, meshes, matrix, model);
            }
        }
    }

    private static void processMesh(AIMesh aimesh, Model model, Matrix4f transform) {
        //create group
        Mesh mesh = new Mesh(aimesh.mName().dataString());
        model.meshes.add(mesh);

        //aabb
        AIAABB aabb = aimesh.mAABB();
        mesh.aabb
                .set(aabb.mMin().x(), aabb.mMin().y(), aabb.mMin().z(), aabb.mMax().x(), aabb.mMax().y(), aabb.mMax().z())
                .applyMatrix(transform);
        LOGGER.debug("Mesh \"%s\" AABB %s", mesh.name, mesh.aabb);

        //vertex data
        AIVector3D.Buffer vertices = aimesh.mVertices();
        for (int i = 0; i < vertices.limit(); i++)
            mesh.vertices.add(parseVec3(vertices.get(i)).mulPosition(transform));

        AIVector3D.Buffer uvs = aimesh.mTextureCoords(0);
        if (uvs != null) {
            mesh.hasUVs = true;
            for (int i = 0; i < uvs.limit(); i++)
                mesh.uvs.add(parseVec2(uvs.get(i)));
        }

        AIVector3D.Buffer normals = aimesh.mNormals();
        if (normals != null) {
            mesh.hasNormals = true;
            for (int i = 0; i < normals.limit(); i++)
                mesh.normals.add(parseVec3(normals.get(i)));
        }

        AIVector3D.Buffer tangents = aimesh.mTangents();
        if (tangents != null) {
            mesh.hasTangents = true;
            for (int i = 0; i < tangents.limit(); i++)
                mesh.tangents.add(parseVec3(tangents.get(i)));
        }

        mesh.materialIndex = aimesh.mMaterialIndex();

        //faces
        processFaces(aimesh, mesh);
    }

    private static void processFaces(AIMesh aimesh, Mesh mesh) {
        int skips = 0;
        AIFace.Buffer faces = aimesh.mFaces();

        for (int i = 0; i < faces.limit(); i++) {
            AIFace aiface = faces.get(i);
            if (aiface.mNumIndices() < 3) {
                skips++;
                continue;
            }

            IntBuffer indices = aiface.mIndices();
            for (int j = 0; j < indices.limit(); j++) {
                int index = indices.get(j);
                mesh.indices.add(index);
            }
        }

        if (skips > 0)
            LOGGER.debug("Skipped %d faces for group %s with less than 3 indices", skips, mesh.name);
    }

    private static void parseMaterial(AIMaterial aimaterial, Model model, Resource root) {
        AIString name = AIString.malloc();
        aiGetMaterialString(aimaterial, AI_MATKEY_NAME, 0, 0, name);

        Material material = new Material(name.dataString());
        model.materials.add(material);

        //parse textures
        material.setAlbedo(parseTexture(aimaterial, aiTextureType_DIFFUSE, root, Texture.TextureParams.MIPMAP_SMOOTH));
        material.setHeight(parseTexture(aimaterial, aiTextureType_HEIGHT, root));
        material.setNormal(parseTexture(aimaterial, aiTextureType_NORMALS, root, Texture.TextureParams.SMOOTH_SAMPLING));
        material.setAO(parseTexture(aimaterial, aiTextureType_AMBIENT_OCCLUSION, root));
        material.setRoughness(parseTexture(aimaterial, aiTextureType_SHININESS, root));
        material.setMetallic(parseTexture(aimaterial, aiTextureType_METALNESS, root));
        material.setEmissive(parseTexture(aimaterial, aiTextureType_EMISSIVE, root));
    }

    private static MaterialTexture parseTexture(AIMaterial aimaterial, int type, Resource root, Texture.TextureParams... params) {
        if (aiGetMaterialTextureCount(aimaterial, type) == 0)
            return null;

        AIString aipath = AIString.malloc();
        int result = aiGetMaterialTexture(aimaterial, type, 0, aipath, null, null, null, null, null, (IntBuffer) null);
        if (result != aiReturn_SUCCESS) {
            LOGGER.error("Failed to get material texture of type %d: %s", type, aiGetErrorString());
            return null;
        }

        //create texture
        Resource texture = root.resolve(aipath.dataString());
        LOGGER.debug("Found material (type %s) texture: %s", type, texture);
        return new MaterialTexture(texture, params);
    }
}
