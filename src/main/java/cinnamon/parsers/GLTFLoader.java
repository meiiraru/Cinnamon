package cinnamon.parsers;

import cinnamon.model.gltf.*;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Base64;

public class GLTFLoader {

    public static GLTFModel load(Resource res) {
        InputStream stream = IOUtils.getResource(res);
        if (stream == null)
            throw new RuntimeException("Resource not found: " + res);

        String path = res.getPath();
        String folder = path.substring(0, path.lastIndexOf("/") + 1);

        try {
            byte[] bytes = stream.readAllBytes();
            JsonObject json = JsonParser.parseString(new String(bytes)).getAsJsonObject();

            //check for major version 2
            String version = json.getAsJsonObject("asset").get("version").getAsString();
            if (version.charAt(0) != '2')
                throw new RuntimeException("Unsupported gltf version: " + version);

            GLTFModel model = new GLTFModel();

            //parse buffers
            model.setBuffers(parseBuffers(json.getAsJsonArray("buffers"), folder));

            //parse nodes
            Node[] nodes = parseNodes(json.getAsJsonArray("nodes"));
            model.setNodes(nodes);

            //parse scenes
            model.setScenes(parseScenes(json.getAsJsonArray("scenes"), nodes));

            //parse meshes
            model.setMeshes(parseMeshes(json.getAsJsonArray("meshes")));

            //parse accessors
            model.setAccessors(parseAccessors(json.getAsJsonArray("accessors")));

            //parse buffer views
            model.setBufferViews(parseBufferViews(json.getAsJsonArray("bufferViews")));

            return model;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read gltf model", e);
        }
    }

    private static int[] parseIntArray(JsonArray array) {
        int[] result = new int[array.size()];
        for (int i = 0; i < array.size(); i++)
            result[i] = array.get(i).getAsInt();
        return result;
    }

    private static float[] parseFloatArray(JsonArray array) {
        float[] result = new float[array.size()];
        for (int i = 0; i < array.size(); i++)
            result[i] = array.get(i).getAsFloat();
        return result;
    }

    private static Node[] parseNodes(JsonArray array) {
        int size = array.size();

        Node[] nodes = new Node[size];
        for (int i = 0; i < nodes.length; i++)
            nodes[i] = new Node();

        for (int i = 0; i < size; i++) {
            JsonObject node = array.get(i).getAsJsonObject();
            Node n = nodes[i];
            if (node.has("name"))
                n.setName(node.get("name").getAsString());
            if (node.has("mesh"))
                n.setMeshIndex(node.get("mesh").getAsInt());
            if (node.has("translation"))
                n.getTranslation().set(parseFloatArray(node.getAsJsonArray("translation")));
            if (node.has("scale"))
                n.getScale().set(parseFloatArray(node.getAsJsonArray("scale")));
            if (node.has("rotation")) {
                float[] f = parseFloatArray(node.getAsJsonArray("rotation"));
                n.getRotation().set(f[0], f[1], f[2], f[3]);
            }
            if (node.has("children")) {
                int[] ii = parseIntArray(node.getAsJsonArray("children"));
                for (int j : ii) n.getChildren().add(nodes[j]);
            }
        }
        return nodes;
    }

    private static Scene[] parseScenes(JsonArray array, Node[] nodes) {
        int size = array.size();
        Scene[] scenes = new Scene[size];

        for (int i = 0; i < size; i++) {
            JsonObject scene = array.get(i).getAsJsonObject();
            Scene s = scenes[i] = new Scene();
            if (scene.has("name"))
                s.setName(scene.get("name").getAsString());
            if (scene.has("nodes")) {
                int[] ii = parseIntArray(scene.getAsJsonArray("nodes"));
                for (int j : ii) s.getNodes().add(nodes[j]);
            }
        }
        return scenes;
    }

    private static Mesh[] parseMeshes(JsonArray array) {
        int size = array.size();
        Mesh[] meshes = new Mesh[size];

        for (int i = 0; i < size; i++) {
            JsonObject mesh = array.get(i).getAsJsonObject();
            Mesh m = meshes[i] = new Mesh();

            if (!mesh.has("primitives"))
                continue;

            JsonArray primitives = mesh.getAsJsonArray("primitives");
            for (int j = 0; j < primitives.size(); j++) {
                JsonObject primitive = primitives.get(j).getAsJsonObject();

                Primitive p = new Primitive();
                if (primitive.has("indices"))
                    p.setIndices(primitive.get("indices").getAsInt());
                if (primitive.has("material"))
                    p.setMaterial(primitive.get("material").getAsInt());
                if (primitive.has("mode"))
                    p.setMode(primitive.get("mode").getAsInt());
                if (primitive.has("attributes")) {
                    JsonObject attributes = primitive.getAsJsonObject("attributes");
                    for (String key : attributes.keySet())
                        p.getAttributes().put(key, attributes.get(key).getAsInt());
                }
                m.getPrimitives().add(p);
            }
        }
        return meshes;
    }

    private static Accessor[] parseAccessors(JsonArray array) {
        int size = array.size();
        Accessor[] accessors = new Accessor[size];

        for (int i = 0; i < size; i++) {
            JsonObject accessor = array.get(i).getAsJsonObject();
            Accessor a = accessors[i] = new Accessor();

            a.setBufferView(accessor.get("bufferView").getAsInt());
            a.setComponentType(accessor.get("componentType").getAsInt());
            a.setCount(accessor.get("count").getAsInt());
            a.setType(accessor.get("type").getAsString());

            if (accessor.has("byteOffset"))
                a.setByteOffset(accessor.get("byteOffset").getAsInt());

            if (accessor.has("max"))
                a.setMax(parseFloatArray(accessor.getAsJsonArray("max")));
            if (accessor.has("min"))
                a.setMin(parseFloatArray(accessor.getAsJsonArray("min")));
        }
        return accessors;
    }

    private static BufferView[] parseBufferViews(JsonArray array) {
        int size = array.size();
        BufferView[] bufferViews = new BufferView[size];

        for (int i = 0; i < size; i++) {
            JsonObject bufferView = array.get(i).getAsJsonObject();
            BufferView bv = bufferViews[i] = new BufferView();

            bv.setBuffer(bufferView.get("buffer").getAsInt());
            bv.setByteOffset(bufferView.get("byteOffset").getAsInt());
            bv.setByteLength(bufferView.get("byteLength").getAsInt());

            if (bufferView.has("byteStride"))
                bv.setByteStride(bufferView.get("byteStride").getAsInt());

            if (bufferView.has("target"))
                bv.setTarget(bufferView.get("target").getAsInt());
        }
        return bufferViews;
    }

    private static ByteBuffer[] parseBuffers(JsonArray array, String folder) {
        ByteBuffer[] buffers = new ByteBuffer[array.size()];

        for (int i = 0; i < array.size(); i++) {
            JsonObject bufferJson = array.get(i).getAsJsonObject();
            String uri = bufferJson.get("uri").getAsString();

            if (uri.startsWith("data:")) {
                //decode Base64
                String base64Data = uri.split(",")[1];
                byte[] decoded = Base64.getDecoder().decode(base64Data);
                buffers[i] = ByteBuffer.wrap(decoded);
            } else {
                //load binary file
                Resource resource = new Resource(folder + uri);
                buffers[i] = IOUtils.getResourceBuffer(resource);
            }
        }

        return buffers;
    }
}
