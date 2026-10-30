package cinnamon.animation;

import cinnamon.math.Maths;
import org.joml.Vector3f;

import java.util.List;

public enum Interpolation {
    LINEAR((keyframes, i, j, t) -> {
        Vector3f a = keyframes.get(i).getValue();
        Vector3f b = keyframes.get(j).getPreValue();
        return Maths.lerp(a, b, t);
    }),
    CATMULLROM((keyframes, i, j, t) -> {
        int len = keyframes.size();
        Vector3f a = keyframes.get(i).getValue();
        Vector3f b = keyframes.get(j).getPreValue();
        Vector3f aa = keyframes.get(Maths.modulo(i - 1, len)).getValue();
        Vector3f bb = keyframes.get(Maths.modulo(j + 1, len)).getPreValue();
        return new Vector3f(
                Maths.catmullRom(aa.x, a.x, b.x, bb.x, t),
                Maths.catmullRom(aa.y, a.y, b.y, bb.y, t),
                Maths.catmullRom(aa.z, a.z, b.z, bb.z, t)
        );
    }),
    STEP((keyframes, i, j, t) -> {
        if (t >= 1f)
            return new Vector3f(keyframes.get(j).getPreValue());
        else
            return new Vector3f(keyframes.get(i).getValue());
    }),
    BEZIER((keyframes, i, j, t) -> {
        //TODO - fallback to linear for now
        Vector3f a = keyframes.get(i).getValue();
        Vector3f b = keyframes.get(j).getPreValue();
        return Maths.lerp(a, b, t);
    });

    private final InterpolationFunction function;

    Interpolation(InterpolationFunction function) {
        this.function = function;
    }

    public Vector3f interpolate(List<Keyframe> keyframes, int i, int j, float t) {
        return function.interpolate(keyframes, i, j, t);
    }

    @FunctionalInterface
    public interface InterpolationFunction {
        Vector3f interpolate(List<Keyframe> keyframes, int i, int j, float t);
    }
}
