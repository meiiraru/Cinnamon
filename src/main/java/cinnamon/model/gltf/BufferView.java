package cinnamon.model.gltf;

public class BufferView {

    private int
            buffer,
            byteOffset,
            byteLength,
            byteStride = 0;
    private int target = 0;

    public int getBuffer() {
        return buffer;
    }

    public void setBuffer(int buffer) {
        this.buffer = buffer;
    }

    public int getByteOffset() {
        return byteOffset;
    }

    public void setByteOffset(int byteOffset) {
        this.byteOffset = byteOffset;
    }

    public int getByteLength() {
        return byteLength;
    }

    public void setByteLength(int byteLength) {
        this.byteLength = byteLength;
    }

    public int getByteStride() {
        return byteStride;
    }

    public void setByteStride(int byteStride) {
        this.byteStride = byteStride;
    }

    public int getTarget() {
        return target;
    }

    public void setTarget(int target) {
        this.target = target;
    }
}
