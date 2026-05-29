package cinnamon.math;

import org.joml.Vector3f;

public enum Direction {
    NORTH("Z-", 0f,   new Vector3f( 0,  0, -1)),
    SOUTH("Z+", 180f, new Vector3f( 0,  0,  1)),
    EAST( "X+", 90f,  new Vector3f( 1,  0,  0)),
    WEST( "X-", 270f, new Vector3f(-1,  0,  0)),
    UP(   "Y+", 0f,   new Vector3f( 0,  1,  0)),
    DOWN( "Y-", 0f,   new Vector3f( 0, -1,  0));

    public final Vector3f vector;
    public final float yaw;
    public final String name, face;

    Direction(String face, float yaw, Vector3f vector) {
        this.vector = vector;
        this.yaw = yaw;
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
