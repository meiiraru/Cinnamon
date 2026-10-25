package cinnamon.parsers;

import cinnamon.animation.Bone;
import cinnamon.math.Maths;
import cinnamon.model.MeshHelper;
import cinnamon.model.material.Material;
import cinnamon.model.material.MaterialTexture;
import cinnamon.model.mesh.Face;
import cinnamon.model.mesh.Group;
import cinnamon.model.mesh.Mesh;
import cinnamon.render.texture.Texture;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Pair;
import cinnamon.utils.Resource;
import cinnamon.utils.Version;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.*;

import static cinnamon.events.Events.LOGGER;

public class BBModelLoader {

    public static final Version MIN_SUPPORTED_VERSION = new Version("3.0.0");
    public static final float SCALE = 1 / 16f;

    public static Mesh load(Resource res) throws Exception {
        LOGGER.debug("Loading bbmodel \"%s\"", res);

        InputStream stream = IOUtils.getResource(res);
        if (stream == null)
            throw new RuntimeException("Resource not found: " + res);

        Mesh mesh = new Mesh();

        try (stream; InputStreamReader reader = new InputStreamReader(stream)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            //parse meta
            Version ver = checkVersion(json);
            boolean versionBelowFive = ver.compareTo(new Version("5.0.0")) < 0;

            //parse textures
            List<TextureData> materials = new ArrayList<>();
            parseTextures(json, mesh, res, materials, versionBelowFive);
            LOGGER.debug("Parsed %d textures", materials.size());

            //parse elements
            Map<String, Group> parts = new HashMap<>();
            parseElements(json, mesh, parts, materials, versionBelowFive);
            LOGGER.debug("Parsed %d parts", parts.size());

            //parse groups
            Map<String, Bone> groups = new HashMap<>();
            parseGroups(json, groups, versionBelowFive);
            LOGGER.debug("Parsed %d groups", groups.size());

            //parse outliner
            Bone rootBone = parseOutliner(json, groups, parts);
            mesh.setAnimationData(Pair.of(rootBone, List.of()));

            //calculate bounds
            MeshHelper.recalculateBounds(mesh);
        }

        return mesh;
    }


    // -- helpers -- //


    public static Vector3f parseVec3(JsonArray array) {
        return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    public static void applyTransform(Vector3f vec, Vector3f pivot, Quaternionf rot) {
        if (pivot != null) vec.sub(pivot);
        if (rot   != null) vec.rotate(rot);
        if (pivot != null) vec.add(pivot);
    }


    // -- version -- //


    public static Version checkVersion(JsonObject root) {
        JsonObject meta = root.has("meta") ? root.getAsJsonObject("meta") : null;
        if (meta == null || !meta.has("format_version"))
            throw new RuntimeException("Invalid bbmodel file: missing meta.format_version");

        String version = meta.get("format_version").getAsString();
        LOGGER.debug("bbmodel version: %s", version);

        //make version match the semver pattern
        String[] parts = version.split("\\.");
        if (parts.length < 3)
            version += ".0";

        Version ver = new Version(version);
        if (MIN_SUPPORTED_VERSION.compareTo(ver) > 0)
            throw new RuntimeException("Unsupported bbmodel version: " + version);

        return ver;
    }


    // -- elements -- //


    private static void parseElements(JsonObject root, Mesh mesh, Map<String, Group> parts, List<TextureData> materials, boolean versionBelowFive) {
        if (!root.has("elements"))
            return;

        Set<String> groupNames = new HashSet<>();

        for (JsonElement element : root.getAsJsonArray("elements")) {
            if (!element.isJsonObject())
                continue;

            JsonObject obj = element.getAsJsonObject();
            String type = obj.has("type") ? obj.get("type").getAsString() : "cube";
            String uuid = obj.get("uuid").getAsString();

            Group part = switch (type) {
                case "cube" -> parseCube(obj, mesh, materials, groupNames);
                case "mesh" -> parseMesh(obj, mesh, materials, versionBelowFive, groupNames);
                default -> null;
            };

            if (part != null) {
                parts.put(uuid, part);
                mesh.getGroups().add(part);
            }
        }
    }

    private static Group parseCube(JsonObject obj, Mesh mesh, List<TextureData> materials, Set<String> groupNames) {
        //create group
        String rawName = obj.has("name") ? obj.get("name").getAsString() : "cube";
        String name = rawName;
        for (int i = 1; groupNames.contains(name); i++)
            name = rawName + "_" + i;

        Group group = new Group(name);
        groupNames.add(name);

        //coords
        Vector3f from = parseVec3(obj.getAsJsonArray("from")).mul(SCALE);
        Vector3f to = parseVec3(obj.getAsJsonArray("to")).mul(SCALE);

        //inflate
        if (obj.has("inflate")) {
            float inflate = obj.get("inflate").getAsFloat() * SCALE;
            from.sub(inflate, inflate, inflate);
            to.add(inflate, inflate, inflate);
        }

        float x1 = from.x, y1 = from.y, z1 = from.z;
        float x2 = to.x,   y2 = to.y,   z2 = to.z;

        //transform
        Vector3f pivot    = obj.has("origin")   ? parseVec3(obj.getAsJsonArray("origin")).mul(SCALE) : null;
        Vector3f rotation = obj.has("rotation") ? parseVec3(obj.getAsJsonArray("rotation"))          : null;
        Quaternionf rot = rotation != null ? new Quaternionf().rotationZYX(Math.toRadians(rotation.z), Math.toRadians(rotation.y), Math.toRadians(rotation.x)) : null;

        //map the faces to the cube vertices
        JsonObject facesObj = obj.getAsJsonObject("faces");
        addCubeFace(mesh, materials, group, facesObj, "north", new Vector3f( 0,  0, -1), pivot, rot, new Vector3f(x2, y1, z1), new Vector3f(x1, y1, z1), new Vector3f(x1, y2, z1), new Vector3f(x2, y2, z1));
        addCubeFace(mesh, materials, group, facesObj, "south", new Vector3f( 0,  0,  1), pivot, rot, new Vector3f(x1, y1, z2), new Vector3f(x2, y1, z2), new Vector3f(x2, y2, z2), new Vector3f(x1, y2, z2));
        addCubeFace(mesh, materials, group, facesObj, "east",  new Vector3f( 1,  0,  0), pivot, rot, new Vector3f(x2, y1, z2), new Vector3f(x2, y1, z1), new Vector3f(x2, y2, z1), new Vector3f(x2, y2, z2));
        addCubeFace(mesh, materials, group, facesObj, "west",  new Vector3f(-1,  0,  0), pivot, rot, new Vector3f(x1, y1, z1), new Vector3f(x1, y1, z2), new Vector3f(x1, y2, z2), new Vector3f(x1, y2, z1));
        addCubeFace(mesh, materials, group, facesObj, "up",    new Vector3f( 0,  1,  0), pivot, rot, new Vector3f(x2, y2, z1), new Vector3f(x1, y2, z1), new Vector3f(x1, y2, z2), new Vector3f(x2, y2, z2));
        addCubeFace(mesh, materials, group, facesObj, "down",  new Vector3f( 0, -1,  0), pivot, rot, new Vector3f(x1, y1, z1), new Vector3f(x2, y1, z1), new Vector3f(x2, y1, z2), new Vector3f(x1, y1, z2));

        return group;
    }

    private static void addCubeFace(Mesh mesh, List<TextureData> materials, Group group, JsonObject facesObj, String faceName, Vector3f normal, Vector3f pivot, Quaternionf rot, Vector3f... corners) {
        if (!facesObj.has(faceName))
            return;

        JsonObject faceData = facesObj.getAsJsonObject(faceName);

        //get texture
        if (faceData.has("texture") && faceData.get("texture").isJsonNull())
            return;

        int w = 1, h = 1;

        int texture = faceData.has("texture") ? faceData.get("texture").getAsInt() : -1;
        if (texture >= 0 && texture < materials.size()) {
            TextureData data = materials.get(texture);
            w = data.width();
            h = data.height();
            group.setMaterial(data.material());
        }

        //get uvs
        JsonArray uvData = faceData.getAsJsonArray("uv");
        float u1 = uvData.get(0).getAsFloat() / w;
        float v1 = 1f - uvData.get(1).getAsFloat() / h;
        float u2 = uvData.get(2).getAsFloat() / w;
        float v2 = 1f - uvData.get(3).getAsFloat() / h;

        //skip degenerate faces
        if (u1 == u2 || v1 == v2)
            return;

        Vector2f[] uvs = new Vector2f[] {
                new Vector2f(u1, v2), new Vector2f(u2, v2),
                new Vector2f(u2, v1), new Vector2f(u1, v1)
        };

        //uv rotation
        int uvRot = faceData.has("rotation") ? faceData.get("rotation").getAsInt() / 90 : 0;
        if (faceName.equals("up"))
            uvRot += 2;

        //add normal to the mesh
        int nIdx = mesh.getNormals().size();
        mesh.getNormals().add(normal);

        //indices array
        int[] vIndices  = new int[4];
        int[] uvIndices = new int[4];
        int[] nIndices  = new int[]{nIdx, nIdx, nIdx, nIdx};

        //add vertices and uvs to the mesh
        for (int i = 0; i < 4; i++) {
            //vertex
            Vector3f pos = corners[i];
            applyTransform(pos, pivot, rot);

            vIndices[i] = mesh.getVertices().size();
            mesh.getVertices().add(pos);

            //uv
            uvIndices[i] = mesh.getUVs().size();
            mesh.getUVs().add(uvs[(i + uvRot) % 4]);
        }

        //add face to the group
        group.getFaces().add(new Face(vIndices, uvIndices, nIndices));
    }

    private static Group parseMesh(JsonObject obj, Mesh mesh, List<TextureData> materials, boolean versionBelowFive, Set<String> groupNames) {
        //create group
        String rawName = obj.has("name") ? obj.get("name").getAsString() : "mesh";
        String name = rawName;
        for (int i = 1; groupNames.contains(name); i++)
            name = rawName + "_" + i;

        Group group = new Group(name);
        groupNames.add(name);

        //read the vertices map
        Map<String, Vector3f> localVertices = new HashMap<>();
        JsonObject verticesObj = obj.getAsJsonObject("vertices");
        for (String key : verticesObj.keySet())
            localVertices.put(key, parseVec3(verticesObj.getAsJsonArray(key)).mul(SCALE));

        //transform
        Vector3f pivot    = obj.has("origin")   ? parseVec3(obj.getAsJsonArray("origin")).mul(SCALE) : null;
        Vector3f rotation = obj.has("rotation") ? parseVec3(obj.getAsJsonArray("rotation"))          : null;
        Quaternionf rot = rotation != null ? new Quaternionf().rotationXYZ(Math.toRadians(rotation.x), Math.toRadians(rotation.y), Math.toRadians(rotation.z)) : null;

        //read the faces map
        JsonObject facesObj = obj.getAsJsonObject("faces");
        for (String faceKey : facesObj.keySet()) {
            JsonObject faceObj = facesObj.getAsJsonObject(faceKey);
            JsonArray faceVerts = faceObj.getAsJsonArray("vertices");
            JsonObject faceUvs = faceObj.getAsJsonObject("uv");

            //prepare the indices arrays
            int numVerts = faceVerts.size();
            if (numVerts < 3)
                continue;

            //texture
            if (faceObj.has("texture") && faceObj.get("texture").isJsonNull())
                continue;

            int w = 1, h = 1;

            int texture = faceObj.has("texture") ? faceObj.get("texture").getAsInt() : -1;
            if (texture >= 0 && texture < materials.size()) {
                TextureData data = materials.get(texture);
                w = data.width();
                h = data.height();
                group.setMaterial(data.material());
            }

            //only in v5.0 faces are ordered correctly in the [C, A, B, D] order
            if (versionBelowFive && numVerts >= 4) {
                Vector3f v1 = localVertices.get(faceVerts.get(0).getAsString());
                Vector3f v2 = localVertices.get(faceVerts.get(1).getAsString());
                Vector3f v3 = localVertices.get(faceVerts.get(2).getAsString());
                Vector3f v4 = localVertices.get(faceVerts.get(3).getAsString());

                if (Maths.testOppositeSides(v2, v3, v1, v4)) {
                    JsonElement temp = faceVerts.get(2);
                    faceVerts.set(2, faceVerts.get(1));
                    faceVerts.set(1, faceVerts.get(0));
                    faceVerts.set(0, temp);
                } else if (Maths.testOppositeSides(v1, v2, v3, v4)) {
                    JsonElement temp = faceVerts.get(2);
                    faceVerts.set(2, faceVerts.get(1));
                    faceVerts.set(1, temp);
                }
            }

            int[] vIndices  = new int[numVerts];
            int[] uvIndices = new int[numVerts];
            int[] nIndices  = new int[numVerts];

            //calculate the normal for this face
            Vector3f v0 = localVertices.get(faceVerts.get(0).getAsString());
            Vector3f v1 = localVertices.get(faceVerts.get(1).getAsString());
            Vector3f v2 = localVertices.get(faceVerts.get(2).getAsString());
            Vector3f normal = Maths.normal(v0, v1, v2);

            if (rotation != null)
                normal.rotate(rot);

            int nIdx = mesh.getNormals().size();
            mesh.getNormals().add(normal);

            //add the vertices and uvs to the mesh
            for (int i = 0; i < numVerts; i++) {
                String vId = faceVerts.get(i).getAsString();

                //copy position to avoid modifying the original vertex
                Vector3f pos = new Vector3f(localVertices.get(vId));
                if (pivot != null)
                    pos.add(pivot);
                applyTransform(pos, pivot, rot);

                vIndices[i] = mesh.getVertices().size();
                mesh.getVertices().add(pos);

                //get uv from the face uvs map
                JsonArray uvArray = faceUvs.getAsJsonArray(vId);
                Vector2f uv = new Vector2f(uvArray.get(0).getAsFloat() / w, 1f - (uvArray.get(1).getAsFloat() / h));
                uvIndices[i] = mesh.getUVs().size();
                mesh.getUVs().add(uv);

                //add normal index
                nIndices[i] = nIdx;
            }

            //add the face to the group
            group.getFaces().add(new Face(vIndices, uvIndices, nIndices));
        }

        return group;
    }


    // -- groups -- //


    private static void parseGroups(JsonObject root, Map<String, Bone> groups, boolean versionBelowFive) {
        String key = versionBelowFive ? "outliner" : "groups";

        if (!root.has(key))
            return;

        parseGroups(root.getAsJsonArray(key), groups);
    }

    private static void parseGroups(JsonArray elements, Map<String, Bone> groups) {
        for (JsonElement element : elements) {
            if (!element.isJsonObject())
                continue;

            //parse group
            JsonObject groupObj = element.getAsJsonObject();
            String name = "bone:" + groupObj.get("name").getAsString();
            String uuid = groupObj.has("uuid") ? groupObj.get("uuid").getAsString() : name;

            Bone bone = new Bone(name);

            //apply transforms
            if (groupObj.has("origin"))
                bone.getTransform().setPivot(parseVec3(groupObj.getAsJsonArray("origin")).mul(SCALE));
            if (groupObj.has("rotation")) {
                Vector3f rot = parseVec3(groupObj.getAsJsonArray("rotation"));
                bone.getTransform().setPivotRot(-rot.x, -rot.y, rot.z);
            }

            groups.put(uuid, bone);

            //children
            if (groupObj.has("children"))
                parseGroups(groupObj.getAsJsonArray("children"), groups);
        }
    }


    // -- outliner -- //


    private static Bone parseOutliner(JsonObject root, Map<String, Bone> groups, Map<String, Group> parts) {
        Bone rootBone = new Bone("bone:_root");
        if (!root.has("outliner"))
            return rootBone;

        parseOutliner(rootBone, root.getAsJsonArray("outliner"), groups, parts);
        return rootBone;
    }

    private static void parseOutliner(Bone parent, JsonArray elements, Map<String, Bone> groups, Map<String, Group> parts) {
        for (JsonElement element : elements) {
            //string - raw part
            if (!element.isJsonObject()) {
                //get part
                String uuid = element.getAsString();
                Group part = parts.get(uuid);

                if (part == null)
                    continue;

                //update name
                String name = part.getName();

                //add as a child
                parent.getChildren().add(new Bone(name, true));
            }
            //object - group
            else {
                //get group
                JsonObject obj = element.getAsJsonObject();
                String uuid = obj.has("uuid") ? obj.get("uuid").getAsString() : obj.get("name").getAsString();

                Bone bone = groups.get(uuid);
                if (bone == null)
                    continue;

                //add as a child
                parent.getChildren().add(bone);

                //parse children
                if (obj.has("children"))
                    parseOutliner(bone, obj.getAsJsonArray("children"), groups, parts);
            }
        }
    }


    // -- textures -- //


    private static void parseTextures(JsonObject root, Mesh mesh, Resource res, List<TextureData> materials, boolean versionBelowFive) {
        if (!root.has("textures"))
            return;

        //grab resolution
        int resW = 1, resH = 1;
        if (root.has("resolution")) {
            JsonObject resolution = root.get("resolution").getAsJsonObject();
            if (resolution.has("width"))
                resW = resolution.get("width").getAsInt();
            if (resolution.has("height"))
                resH = resolution.get("height").getAsInt();
        }

        Resource workingDir = versionBelowFive ? res : res.getParent();
        for (JsonElement element : root.getAsJsonArray("textures")) {
            JsonObject texture = element.getAsJsonObject();

            //grab size
            int width = texture.has("uv_width") ? texture.get("uv_width").getAsInt() : resW;
            int height = texture.has("uv_height") ? texture.get("uv_height").getAsInt() : resH;

            //find unique name
            String rawName = IOUtils.getFilenameWithoutExtension(texture.get("name").getAsString());
            String name = rawName;
            for (int i = 1; mesh.getMaterials().containsKey(name); i++)
                name = rawName + "_" + i;

            //check if the texture is an emissive texture for an existing material
            String matName = name;
            Material material = null;
            boolean nameEmi = false;
            if (rawName.endsWith("_e") || rawName.endsWith("_emissive")) {
                nameEmi = true;
                matName = rawName.substring(0, rawName.lastIndexOf('_'));
                if (mesh.getMaterials().containsKey(matName)) {
                    material = mesh.getMaterials().get(matName);
                    materials.add(new TextureData(material, width, height));
                }
            }

            //create a new material
            if (material == null) {
                material = new Material(matName);
                materials.add(new TextureData(material, width, height));
                mesh.getMaterials().put(matName, material);
            }

            //material type and tex
            boolean isEmissive = nameEmi || (texture.has("render_mode") && texture.get("render_mode").getAsString().equals("emissive"));
            MaterialTexture matTex = null;

            //parse relative path
            String relPath = texture.has("relative_path") ? texture.get("relative_path").getAsString() : "";
            if (!relPath.isBlank()) {
                Resource texRes = workingDir.resolve(relPath);
                try {
                    if (IOUtils.hasResource(texRes)) {
                        matTex = new MaterialTexture(texRes);
                        LOGGER.debug("Loaded external texture: %s", texRes);
                    }
                } catch (Exception ignored) {}
            }

            //otherwise parse base64 source
            if (matTex == null) {
                //create generated resource
                Resource texRes = new Resource("generated/bbmodel/" + res + "/" + name + ".png");

                //encode bytes
                String base64 = texture.get("source").getAsString();
                base64 = base64.substring("data:image/png;base64,".length());
                byte[] decodedBytes = Base64.getDecoder().decode(base64);

                ByteBuffer buffer = MemoryUtil.memAlloc(decodedBytes.length);
                buffer.put(decodedBytes);
                buffer.flip();

                //force texture register
                Texture.of(texRes, buffer);

                matTex = new MaterialTexture(texRes);
                MemoryUtil.memFree(buffer);
                LOGGER.debug("Loaded embedded texture: %s", texRes);
            }

            //set material texture
            if (isEmissive) material.setEmissive(matTex);
            else            material.setAlbedo(matTex);
        }
    }

    private record TextureData(Material material, int width, int height) {}
}