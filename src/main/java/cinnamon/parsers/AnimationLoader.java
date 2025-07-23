package cinnamon.parsers;

import cinnamon.animation.Animation;
import cinnamon.animation.Bone;
import cinnamon.animation.Channel;
import cinnamon.animation.Keyframe;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Pair;
import cinnamon.utils.Resource;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.joml.Vector3f;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cinnamon.events.Events.LOGGER;

public class AnimationLoader {

    public static final float POS_RATIO = 1 / 16f;

    public static Pair<Bone, List<Animation>> load(Resource res) {
        LOGGER.debug("Loading animation %s", res.getPath());

        InputStream stream = IOUtils.getResource(res);
        if (stream == null)
            throw new RuntimeException("Resource not found: " + res);

        //read stream as string
        try {
            String string = new String(stream.readAllBytes());
            JsonObject json = JsonParser.parseString(string).getAsJsonObject();

            //root bone
            Bone root = new Bone("root");
            HashMap<String, Bone> boneMap = new HashMap<>();

            //parse outliner
            JsonArray bonesArray = json.getAsJsonArray("outliner");
            parseOutliner(root, bonesArray, boneMap);

            //parse animations
            JsonObject animationsObject = json.getAsJsonObject("animations");
            List<Animation> animations = parseAnimations(animationsObject, boneMap);

            return new Pair<>(root, animations);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load animation \"" + res + "\"", e);
        }
    }

    private static Vector3f parseVec3(JsonArray array, boolean position) {
        return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()).mul(position ? POS_RATIO : 1f);
    }

    private static void parseOutliner(Bone bone, JsonArray array, HashMap<String, Bone> boneMap) {
        for (JsonElement element : array) {
            //string only - mesh name
            if (element.isJsonPrimitive()) {
                Bone child = new Bone(element.getAsString(), true);
                bone.getChildren().add(child);
                boneMap.put(child.getName(), child);
                continue;
            }

            //object - bone
            JsonObject object = element.getAsJsonObject();

            Bone child = new Bone(object.get("name").getAsString());
            child.getTransform().setPivot(parseVec3(object.getAsJsonArray("origin"), true));
            bone.getChildren().add(child);
            boneMap.put(child.getName(), child);

            if (object.has("children"))
                parseOutliner(child, object.getAsJsonArray("children"), boneMap);
        }
    }

    private static List<Animation> parseAnimations(JsonObject object, HashMap<String, Bone> boneMap) {
        List<Animation> animations = new ArrayList<>();

        for (Map.Entry<String, JsonElement> animationMap : object.entrySet()) {
            JsonObject animationObject = animationMap.getValue().getAsJsonObject();

            //animation data
            String name = animationMap.getKey();
            Animation animation = new Animation(name);
            animations.add(animation);

            if (animationObject.has("loop")) {
                String loop = animationObject.get("loop").getAsString();
                if (loop.equals("true"))
                    animation.setLoop(Animation.Loop.LOOP);
                else if (loop.equals("hold_on_last_frame"))
                    animation.setLoop(Animation.Loop.HOLD);
            }

            //animation bones
            for (Map.Entry<String, JsonElement> animBoneMap : animationObject.getAsJsonObject("bones").entrySet()) {
                Bone bone = boneMap.get(animBoneMap.getKey());
                if (bone == null)
                    continue;

                //channels
                JsonObject animBoneObject = animBoneMap.getValue().getAsJsonObject();
                for (Map.Entry<String, JsonElement> animBoneEntry : animBoneObject.entrySet()) {
                    Channel channel = Channel.valueOf(animBoneEntry.getKey().toUpperCase());

                    //keyframes
                    JsonObject animObject = animBoneEntry.getValue().getAsJsonObject();
                    for (Map.Entry<String, JsonElement> keyframeEntry : animObject.entrySet()) {
                        int time = (int) (Float.parseFloat(keyframeEntry.getKey()) * 1000f);
                        boolean isPosition = channel == Channel.POSITION;

                        //array value - keyframe only contains the transform value
                        if (keyframeEntry.getValue().isJsonArray()) {
                            Vector3f vec = parseVec3(keyframeEntry.getValue().getAsJsonArray(), isPosition);
                            animation.addKeyframe(bone, channel, new Keyframe(time, vec, false));
                            continue;
                        }

                        //object value - keyframe contains extra data
                        JsonObject keyframeObject = keyframeEntry.getValue().getAsJsonObject();

                        boolean catmullrom = keyframeObject.has("lerp_mode") && keyframeObject.get("lerp_mode").getAsString().equals("catmullrom");
                        Vector3f pre = keyframeObject.has("pre") ? parseVec3(keyframeObject.getAsJsonArray("pre"), isPosition) : null;
                        Vector3f post = parseVec3(keyframeObject.getAsJsonArray("post"), isPosition);

                        animation.addKeyframe(bone, channel, new Keyframe(time, post, pre, catmullrom));
                    }
                }
            }
        }

        return animations;
    }
}
