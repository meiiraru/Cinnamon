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
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cinnamon.events.Events.LOGGER;

public class AnimationLoader {

    public static final float POS_RATIO = 1 / 16f;

    public static Pair<Bone, List<Animation>> load(Resource res) throws Exception {
        LOGGER.debug("Loading animation \"%s\"", res);

        InputStream stream = IOUtils.getResource(res);
        if (stream == null)
            throw new RuntimeException("Resource not found: " + res);

        try (stream; InputStreamReader reader = new InputStreamReader(stream)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

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
        }
    }

    private static Vector3f parseVec3(JsonArray array, float scale, boolean invertX) {
        return new Vector3f(array.get(0).getAsFloat() * (invertX ? -1f : 1f), array.get(1).getAsFloat(), array.get(2).getAsFloat()).mul(scale);
    }

    private static void parseOutliner(Bone bone, JsonArray array, HashMap<String, Bone> boneMap) {
        for (JsonElement element : array) {
            //string only - mesh name
            if (element.isJsonPrimitive()) {
                String name = element.getAsString();
                String newName = name;
                for (int i = 1; boneMap.containsKey(newName); i++)
                    newName = name + "_" + i;

                Bone child = new Bone(newName, true);
                bone.getChildren().add(child);
                boneMap.put(child.getName(), child);
                continue;
            }

            //object - bone
            JsonObject object = element.getAsJsonObject();

            Bone child = new Bone("bone:" + object.get("name").getAsString());
            bone.getChildren().add(child);
            boneMap.put(child.getName(), child);

            child.getTransform().setPivot(parseVec3(object.getAsJsonArray("origin"), POS_RATIO, false));
            if (object.has("rotation"))
                child.getTransform().setPivotRot(parseVec3(object.getAsJsonArray("rotation"), 1f, true));

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
            int duration = animationObject.has("animation_length") ? (int) (animationObject.get("animation_length").getAsFloat() * 1000f) : 1;
            Animation animation = new Animation(name, duration);
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
                Bone bone = boneMap.get("bone:" + animBoneMap.getKey());
                if (bone == null)
                    continue;

                //channels
                JsonObject animBoneObject = animBoneMap.getValue().getAsJsonObject();
                for (Map.Entry<String, JsonElement> animBoneEntry : animBoneObject.entrySet()) {
                    Channel channel;
                    try {
                        channel = Channel.valueOf(animBoneEntry.getKey().toUpperCase());
                    } catch (Exception e) {
                        LOGGER.warn("Invalid channel \"%s\" for bone \"%s\" in animation \"%s\"", animBoneEntry.getKey(), bone.getName(), animation.getName());
                        continue;
                    }
                    boolean isPosition = channel == Channel.POSITION;
                    float scale = isPosition ? POS_RATIO : 1f;

                    //array value - single keyframe with only the transform value
                    if (animBoneEntry.getValue().isJsonArray()) {
                        Vector3f vec = new Vector3f();
                        try {
                            vec.set(parseVec3(animBoneEntry.getValue().getAsJsonArray(), scale, isPosition));
                        } catch (Exception e) {
                            LOGGER.warn("Invalid keyframe data \"%s\" for bone \"%s\" channel \"%s\" in animation \"%s\"", animBoneEntry.getValue(), bone.getName(), channel, animation.getName());
                        }
                        animation.addKeyframe(bone, channel, new Keyframe(1, vec, false));
                        continue;
                    }

                    //object value - multiple keyframes
                    JsonObject animObject = animBoneEntry.getValue().getAsJsonObject();
                    for (Map.Entry<String, JsonElement> keyframeEntry : animObject.entrySet()) {
                        int time = (int) (Float.parseFloat(keyframeEntry.getKey()) * 1000f);

                        //array value - keyframe only contains the transform value
                        if (keyframeEntry.getValue().isJsonArray()) {
                            Vector3f vec = parseVec3(keyframeEntry.getValue().getAsJsonArray(), scale, isPosition);
                            animation.addKeyframe(bone, channel, new Keyframe(time, vec, false));
                            continue;
                        }

                        //object value - keyframe contains extra data
                        JsonObject keyframeObject = keyframeEntry.getValue().getAsJsonObject();

                        boolean catmullrom = keyframeObject.has("lerp_mode") && keyframeObject.get("lerp_mode").getAsString().equals("catmullrom");
                        Vector3f pre = keyframeObject.has("pre") ? parseVec3(keyframeObject.getAsJsonArray("pre"), scale, isPosition) : null;
                        Vector3f post = parseVec3(keyframeObject.getAsJsonArray("post"), scale, isPosition);

                        animation.addKeyframe(bone, channel, new Keyframe(time, post, pre, catmullrom));
                    }
                }
            }
        }

        return animations;
    }
}
