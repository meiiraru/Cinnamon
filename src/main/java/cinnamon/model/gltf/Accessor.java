package cinnamon.model.gltf;

public class Accessor {

    private int
            bufferView,
            byteOffset,
            componentType,
            count;
    private String type;
    private float[] min, max;

    public int getBufferView() {
        return bufferView;
    }

    public void setBufferView(int bufferView) {
        this.bufferView = bufferView;
    }

    public int getByteOffset() {
        return byteOffset;
    }

    public void setByteOffset(int byteOffset) {
        this.byteOffset = byteOffset;
    }

    public int getComponentType() {
        return componentType;
    }

    public void setComponentType(int componentType) {
        this.componentType = componentType;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public float[] getMin() {
        return min;
    }

    public void setMin(float... min) {
        this.min = min;
    }

    public float[] getMax() {
        return max;
    }

    public void setMax(float... max) {
        this.max = max;
    }
}
