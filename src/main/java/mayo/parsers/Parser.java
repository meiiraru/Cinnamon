package mayo.parsers;

import org.joml.Vector2f;
import org.joml.Vector3f;

public class Parser {

    public static Vector2f parseVec2(String x, String y) {
        return new Vector2f(Float.parseFloat(x), Float.parseFloat(y));
    }

    public static Vector3f parseVec3(String x, String y, String z) {
        return new Vector3f(Float.parseFloat(x), Float.parseFloat(y), Float.parseFloat(z));
    }
}
