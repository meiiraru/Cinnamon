package mayo.utils;

import org.joml.Quaternionf;

public interface Rotation {
    Rotation X = amount -> new Quaternionf().rotationX(amount);
    Rotation Y = amount -> new Quaternionf().rotationY(amount);
    Rotation Z = amount -> new Quaternionf().rotationZ(amount);

    Quaternionf rotation(float f);

    default Quaternionf rotationDeg(float f) {
        return this.rotation((float) Math.toRadians(f));
    }
}
