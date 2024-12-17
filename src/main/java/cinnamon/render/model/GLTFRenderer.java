package cinnamon.render.model;

import cinnamon.model.gltf.*;
import cinnamon.model.material.Material;
import cinnamon.registry.MaterialRegistry;
import cinnamon.render.MaterialApplier;
import cinnamon.render.MatrixStack;
import cinnamon.render.shader.Shader;
import cinnamon.render.texture.Texture;
import cinnamon.utils.AABB;
import cinnamon.utils.Pair;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

public class GLTFRenderer extends ModelRenderer {

    private final GLTFModel model;
    private final NodeRenderer node;
    private final Vector3f
            bbMin = new Vector3f(Integer.MAX_VALUE),
            bbMax = new Vector3f(Integer.MIN_VALUE);

    public GLTFRenderer(GLTFModel model) {
        this.model = model;

        //grab the first node from the first scene
        Node root = model.getScenes()[0].getNodes().getFirst();

        //create node renderer
        this.node = new NodeRenderer(root, model);

        //calculate min/max AABB
        for (AABB aabb : getPreciseAABB()) {
            bbMin.min(aabb.getMin());
            bbMax.max(aabb.getMax());
        }
    }

    @Override
    public void free() {
        node.free();
    }

    @Override
    public void render(MatrixStack matrices) {
        render(matrices, null);
    }

    @Override
    public void render(MatrixStack matrices, Material material) {
        //bind matrices
        Shader.activeShader.applyMatrixStack(matrices);

        //bind material
        int texCount;
        if (material == null || (texCount = MaterialApplier.applyMaterial(material)) == -1)
            texCount = MaterialApplier.applyMaterial(MaterialRegistry.MISSING);

        //render
        node.render();

        //unbind all used textures
        Texture.unbindAll(texCount);
    }

    @Override
    public void renderWithoutMaterial(MatrixStack matrices) {
        render(matrices, null);
    }

    @Override
    public AABB getAABB() {
        return new AABB(bbMin, bbMax);
    }

    @Override
    public List<AABB> getPreciseAABB() {
        List<AABB> list = new ArrayList<>();
        node.getAABB(list);
        return list;
    }

    public GLTFModel getModel() {
        return model;
    }

    private static class NodeRenderer {

        private final List<NodeRenderer> children = new ArrayList<>();

        private MeshProperties meshProperties;

        public NodeRenderer(Node node, GLTFModel model) {
            if (node.isMesh())
                meshProperties = new MeshProperties(extractMeshData(node, model));

            for (Node child : node.getChildren())
                children.add(new NodeRenderer(child, model));
        }

        public Map<String, ByteBuffer> extractMeshData(Node node, GLTFModel model) {
            int meshIndex = node.getMeshIndex();
            Mesh mesh = model.getMeshes()[meshIndex];

            Map<String, ByteBuffer> data = new HashMap<>();

            for (Primitive primitive : mesh.getPrimitives()) {
                //extract attributes
                for (Map.Entry<String, Integer> attribute : primitive.getAttributes().entrySet()) {
                    String name = attribute.getKey();
                    int accessorIndex = attribute.getValue();
                    Accessor accessor = model.getAccessors()[accessorIndex];
                    BufferView bufferView = model.getBufferViews()[accessor.getBufferView()];

                    //extract data
                    ByteBuffer buffer = model.getBuffers()[bufferView.getBuffer()];
                    int start = bufferView.getByteOffset() + accessor.getByteOffset();
                    int length = accessor.getCount() * getElementSize(accessor);

                    data.put(name, buffer.slice(start, length));
                }
            }

            return data;
        }

        public void free() {
            if (meshProperties != null)
                meshProperties.free();
        }

        public void render() {
            //render
            if (meshProperties != null)
                meshProperties.render();

            //render child
            for (NodeRenderer child : children)
                child.render();
        }

        public void getAABB(List<AABB> list) {
            if (meshProperties != null)
                list.add(new AABB(meshProperties.bbMin, meshProperties.bbMax));

            for (NodeRenderer child : children)
                child.getAABB(list);
        }

        private static int getElementSize(Accessor accessor) {
            int componentSize = getComponentSize(accessor.getComponentType());
            int componentCount = getComponentCount(accessor.getType());
            return componentSize * componentCount;
        }

        private static int getComponentSize(int componentType) {
            return switch (componentType) {
                case GL_BYTE, GL_UNSIGNED_BYTE -> 1;
                case GL_SHORT, GL_UNSIGNED_SHORT -> 2;
                case GL_UNSIGNED_INT, GL_FLOAT -> 4;
                default -> throw new IllegalArgumentException("Unknown component type.");
            };
        }

        private static int getComponentCount(String type) {
            return switch (type) {
                case "SCALAR" -> 1;
                case "VEC2" -> 2;
                case "VEC3" -> 3;
                case "VEC4" -> 4;
                default -> throw new IllegalArgumentException("Unknown type.");
            };
        }
    }

    private static class MeshProperties {
        private final int vao, vbo;
        private final int vertexCount;
        private final Vector3f
            bbMin = new Vector3f(Integer.MAX_VALUE),
            bbMax = new Vector3f(Integer.MIN_VALUE);

        public MeshProperties(Map<String, ByteBuffer> dataMap) {
            ByteBuffer posBuffer = dataMap.get("POSITION");
            ByteBuffer uvBuffer = dataMap.get("TEXCOORD_0");
            ByteBuffer normalBuffer = dataMap.get("NORMAL");

            List<VertexData> data = new ArrayList<>();

            while (posBuffer.hasRemaining()) {
                Vector3f pos = new Vector3f(posBuffer.getFloat(), posBuffer.getFloat(), posBuffer.getFloat());
                Vector2f uv = new Vector2f(uvBuffer.getFloat(), uvBuffer.getFloat());
                Vector3f normal = new Vector3f(normalBuffer.getFloat(), normalBuffer.getFloat(), normalBuffer.getFloat());

                data.add(new VertexData(pos, uv, normal));
                this.bbMin.min(pos);
                this.bbMax.max(pos);
            }

            //triangulate the faces using ear clipping
            List<VertexData> sorted = VertexData.triangulate(data);

            //calculate tangents
            VertexData.calculateTangents(sorted);

            //generate buffers
            Pair<Integer, Integer> buffers = generateBuffers(sorted);
            this.vao = buffers.first();
            this.vbo = buffers.second();

            this.vertexCount = sorted.size();
        }

        public void free() {
            glDeleteBuffers(vao);
            glDeleteBuffers(vbo);
        }

        public void render() {
            glBindVertexArray(vao);
            glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        }
    }
}
