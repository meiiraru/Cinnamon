package cinnamon.math;

import org.joml.Vector3f;

public enum Direction {
    NORTH("Z-", (byte) 0b010, new Vector3f( 0,  0, -1)),
    SOUTH("Z+", (byte) 0b000, new Vector3f( 0,  0,  1)),
    EAST( "X+", (byte) 0b001, new Vector3f( 1,  0,  0)),
    WEST( "X-", (byte) 0b011, new Vector3f(-1,  0,  0)),
    UP(   "Y+", (byte) 0b100, new Vector3f( 0,  1,  0)),
    DOWN( "Y-", (byte) 0b101, new Vector3f( 0, -1,  0));

    public final Vector3f vector;
    public final byte rotation, invRotation;
    public final String name, face;

    Direction(String face, byte rotation, Vector3f vector) {
        this.vector = vector;
        this.rotation = rotation;
        this.invRotation = rotation <= 0b011 ? (byte) ((rotation + 2) % 4) : (byte) (rotation ^ 0b1);
        this.name = name().charAt(0) + name().substring(1).toLowerCase() + " " + face;
        this.face = face;
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
