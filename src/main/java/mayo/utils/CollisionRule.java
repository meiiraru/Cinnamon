package mayo.utils;

public enum CollisionRule {
    IGNORE,     //no collision
    SOLID,      //collider cannot enter
    PUSHABLE,   //collider motion is passed to me
    PUSH_BACK   //push me through and push the collider back
}
