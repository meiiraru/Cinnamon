package cinnamon.animation;

import cinnamon.utils.Maths;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Animation {

    //animation data
    private final String name;
    private int duration;

    //keyframes
    private final Map<Bone, Map<Channel, List<Keyframe>>> keyframes = new HashMap<>();

    //playback
    private Loop loop = Loop.ONCE;
    private State state = State.STOPPED;
    private int time = 0, initTime = 0;

    public Animation(String name) {
        this.name = name;
    }

    public Animation(Animation other, Map<Bone, Bone> boneMap) {
        this.name = other.name;
        this.duration = other.duration;

        for (Map.Entry<Bone, Bone> entry : boneMap.entrySet()) {
            Map<Channel, List<Keyframe>> keyframe = other.keyframes.get(entry.getKey());
            if (keyframe != null) keyframes.put(entry.getValue(), keyframe);
        }
    }

    private void updateTime() {
        time = (int) (System.currentTimeMillis() - initTime);

        if (time > duration) {
            switch (loop) {
                case ONCE -> {
                    state = State.STOPPED;
                    time = initTime = 0;
                }
                case HOLD -> {
                    time = duration;
                    state = State.PAUSED;
                }
                case LOOP -> {
                    time %= duration;
                    initTime = (int) System.currentTimeMillis() - time;
                }
            }
        }
    }

    public int update() {
        if (state == State.STOPPED)
            return 0;

        if (state == State.PLAYING)
            updateTime();

        int count = 0;

        //grab the current keyframes
        for (Map.Entry<Bone, Map<Channel, List<Keyframe>>> boneEntry : keyframes.entrySet()) {
            //grab the bone and its channels
            Bone bone = boneEntry.getKey();
            Map<Channel, List<Keyframe>> channels = boneEntry.getValue();

            //for each channel
            for (Map.Entry<Channel, List<Keyframe>> channelsEntry : channels.entrySet()) {
                Channel channel = channelsEntry.getKey();
                List<Keyframe> keyframes = channelsEntry.getValue();

                //get the current keyframe and the next
                int i = Math.max(Maths.binarySearch(0, keyframes.size() - 1, index -> time <= keyframes.get(index).getTime()) - 1, 0);
                int j = Math.min(keyframes.size() - 1, i + 1);

                Keyframe current = keyframes.get(i);
                Keyframe next = keyframes.get(j);

                //calculate the delta time between the two keyframes
                float dt = current == next ? 0 : (time - current.getTime()) / (float) (next.getTime() - current.getTime());

                Vector3f a = current.getValue();
                Vector3f b = next.getPreValue();

                //interpolate the value
                Vector3f value;
                if (current.isCatmullrom()) {
                    Vector3f aa = keyframes.get(Math.max(0, i - 1)).getValue();
                    Vector3f bb = keyframes.get(Math.min(keyframes.size() - 1, j + 1)).getPreValue();
                    value = new Vector3f(
                            Maths.catmullRom(aa.x, a.x, b.x, bb.x, dt),
                            Maths.catmullRom(aa.y, a.y, b.y, bb.y, dt),
                            Maths.catmullRom(aa.z, a.z, b.z, bb.z, dt)
                    );
                } else {
                    value = Maths.lerp(a, b, dt);
                }

                //apply the value to the bone
                channel.apply(bone.getTransform(), value);
                count++;
            }
        }

        return count;
    }

    public void addKeyframe(Bone bone, Channel channel, Keyframe keyframe) {
        List<Keyframe> list = keyframes
                .computeIfAbsent(bone, k -> new HashMap<>(3, 1f))
                .computeIfAbsent(channel, k -> new ArrayList<>());
        list.add(keyframe);
        list.sort(Keyframe::compareTo);
        this.duration = Math.max(this.duration, keyframe.getTime());
    }

    public int getTime() {
        return time;
    }

    public Animation setTime(int time) {
        this.time = time;
        return this;
    }

    private void setState(State state) {
        if (state == this.state)
            return;

        if (state == State.PLAYING)
            initTime = (int) (System.currentTimeMillis() - time);
        if (state == State.STOPPED)
            time = initTime = 0;

        this.state = state;
    }

    public void play() {
        setState(State.PLAYING);
    }

    public void pause() {
        setState(State.PAUSED);
    }

    public void stop() {
        setState(State.STOPPED);
    }

    public boolean isPlaying() {
        return state == State.PLAYING;
    }

    public boolean isPaused() {
        return state == State.PAUSED;
    }

    public boolean isStopped() {
        return state == State.STOPPED;
    }

    public Loop getLoop() {
        return loop;
    }

    public Animation setLoop(Loop loop) {
        this.loop = loop;
        return this;
    }

    public String getName() {
        return name;
    }

    public int getDuration() {
        return duration;
    }

    private enum State {
        STOPPED,
        PAUSED,
        PLAYING
    }

    public enum Loop {
        LOOP,
        ONCE,
        HOLD
    }
}
