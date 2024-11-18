package cinnamon.model.gltf;

import java.nio.ByteBuffer;

public class GLTFModel {
    private ByteBuffer[] buffers;
    private Node[] nodes;
    private Scene[] scenes;
    private Mesh[] meshes;
    private Accessor[] accessors;
    private BufferView[] bufferViews;

    public ByteBuffer[] getBuffers() {
        return buffers;
    }

    public void setBuffers(ByteBuffer... buffers) {
        this.buffers = buffers;
    }

    public Node[] getNodes() {
        return nodes;
    }

    public void setNodes(Node... nodes) {
        this.nodes = nodes;
    }

    public Scene[] getScenes() {
        return scenes;
    }

    public void setScenes(Scene... scenes) {
        this.scenes = scenes;
    }

    public Mesh[] getMeshes() {
        return meshes;
    }

    public void setMeshes(Mesh... meshes) {
        this.meshes = meshes;
    }

    public Accessor[] getAccessors() {
        return accessors;
    }

    public void setAccessors(Accessor... accessors) {
        this.accessors = accessors;
    }

    public BufferView[] getBufferViews() {
        return bufferViews;
    }

    public void setBufferViews(BufferView... bufferViews) {
        this.bufferViews = bufferViews;
    }
}
