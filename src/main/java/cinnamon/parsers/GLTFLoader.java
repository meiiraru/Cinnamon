package cinnamon.parsers;

import cinnamon.model.gltf.Node;
import cinnamon.model.gltf.Scene;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;

public class GLTFLoader {

    public static Scene[] load(Resource res) {
        InputStream stream = IOUtils.getResource(res);
        if (stream == null)
            throw new RuntimeException("Resource not found: " + res);

        String path = res.getPath();
        String folder = path.substring(0, path.lastIndexOf("/") + 1);

        try {
            byte[] bytes = stream.readAllBytes();

            JsonObject json = JsonParser.parseString(new String(bytes)).getAsJsonObject();

            //parse nodes
            Node[] nodes = parseNodes(json.getAsJsonArray("nodes"));

            //parse scenes
            return parseScenes(json.getAsJsonArray("scenes"), nodes);
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
}
