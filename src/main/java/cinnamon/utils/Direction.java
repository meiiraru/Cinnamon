package cinnamon.utils;

import org.joml.Vector3f;

public enum Direction {
    NORTH("Z-", (byte) 0, new Vector3f(0, 0, -1)),
    SOUTH("Z+", (byte) 2, new Vector3f(0, 0, 1)),
    EAST("X+", (byte) 3, new Vector3f(1, 0, 0)),
    WEST("X-", (byte) 1, new Vector3f(-1, 0, 0));

    public final Vector3f vector;
    public final byte rotation, invRotation;
    public final String name;

    Direction(String face, byte rotation, Vector3f vector) {
        this.vector = vector;
        this.rotation = rotation;
        this.invRotation = (byte) ((rotation + 2) % 4);
        this.name = name().charAt(0) + name().substring(1).toLowerCase() + " " + face;
    }

    public static Direction fromRotation(float yaw) {
        yaw = Maths.modulo(yaw, 360);
        if (yaw >= 45 && yaw < 135) {
            return EAST;
        } else if (yaw >= 135 && yaw < 225) {
            return SOUTH;
        } else if (yaw >= 225 && yaw < 315) {
            return WEST;
        } else {
            return NORTH;
        }
    }
}
