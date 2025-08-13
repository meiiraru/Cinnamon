package cinnamon.collision;

import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

//GJK (Gilbert Johnson Keerthi) algorithm for collision detection
public class ColliderChecker {

    //3D

    public static boolean collides(Collider a, Collider b) {
        //get initial support point in any arbitrary direction
        Vector3f support = support(a, b, new Vector3f(1f, 0f, 0f));

        //create simplex and add initial point
        List<Vector3f> points = new ArrayList<>(4);
        points.addFirst(support);

        //new direction is towards the origin
        Vector3f dir = new Vector3f(support).negate();

        while (true) {
            //get a new support point in the current direction
            support = support(a, b, dir);

            //if the new point is not past the origin in the direction we are looking, no collision
            if (support.dot(dir) <= 0f)
                return false;

            //add the new point to the simplex
            points.addFirst(support);

            //check if the simplex contains the origin
            if (nextSimplex(points, dir))
                return true;
        }
    }

    private static Vector3f support(Collider a, Collider b, Vector3f dir) {
        Vector3f thisFurthest = a.findFurthestPoint(dir.x, dir.y, dir.z);
        Vector3f otherFurthest = b.findFurthestPoint(-dir.x, -dir.y, -dir.z);
        return new Vector3f(thisFurthest).sub(otherFurthest);
    }

    private static boolean nextSimplex(List<Vector3f> points, Vector3f dir) {
        return switch (points.size()) {
            case 2 -> line(points, dir);
            case 3 -> triangle(points, dir);
            case 4 -> tetrahedron(points, dir);
            default -> false; //should never happen
        };
    }

    private static boolean isSameDirection(Vector3f dir, Vector3f ao) {
        return dir.dot(ao) > 0f;
    }

    private static boolean line(List<Vector3f> points, Vector3f dir) {
        Vector3f a = points.get(0);
        Vector3f b = points.get(1);

        Vector3f ao = a.negate(new Vector3f());
        Vector3f ab = b.sub(a, new Vector3f());

        if (isSameDirection(ab, ao)) {
            //set direction to be perpendicular to AB towards the origin
            dir.set(ab).cross(ao).cross(ab);
        } else {
            //remove point b
            points.remove(1);
            dir.set(ao);
        }

        return false;
    }

    private static boolean triangle(List<Vector3f> points, Vector3f dir) {
        Vector3f a = points.get(0);
        Vector3f b = points.get(1);
        Vector3f c = points.get(2);

        Vector3f ao = a.negate(new Vector3f());
        Vector3f ab = b.sub(a, new Vector3f());
        Vector3f ac = c.sub(a, new Vector3f());
        Vector3f abc = ab.cross(ac, new Vector3f());

        if (isSameDirection(abc.cross(ac, new Vector3f()), ao)) {
            if (isSameDirection(ac, ao)) {
                //set direction to be perpendicular to AC towards the origin
                dir.set(ac).cross(ao).cross(ac);
                points.remove(1); //remove point b
            } else {
                points.remove(2); //remove point c
                return line(points, dir); //use line case
            }
        } else if (isSameDirection(ab.cross(abc), ao)) { //no need to care about ab integrity after here
            points.remove(2);
            return line(points, dir);
        } else if (isSameDirection(abc, ao)) {
            dir.set(abc);
        } else {
            //swap points b and c
            points.set(1, c);
            points.set(2, b);
            dir.set(abc.negate());
        }

        return false;
    }

    private static boolean tetrahedron(List<Vector3f> points, Vector3f dir) {
        Vector3f a = points.get(0);
        Vector3f b = points.get(1);
        Vector3f c = points.get(2);
        Vector3f d = points.get(3);

        Vector3f ao = a.negate(new Vector3f());
        Vector3f ab = b.sub(a, new Vector3f());
        Vector3f ac = c.sub(a, new Vector3f());
        Vector3f ad = d.sub(a, new Vector3f());

        Vector3f abc = ab.cross(ac, new Vector3f());
        Vector3f acd = ac.cross(ad, new Vector3f());
        Vector3f adb = ad.cross(ab, new Vector3f());

        if (isSameDirection(abc, ao)) {
            points.remove(3); //remove d
            return triangle(points, dir);
        }

        if (isSameDirection(acd, ao)) {
            points.remove(1); //remove b
            return triangle(points, dir);
        }

        if (isSameDirection(adb, ao)) {
            points.remove(2); //remove c
            return triangle(points, dir);
        }

        //if we reach here, the origin is inside the tetrahedron
        return true;
    }


    //2D


    public static boolean collides2D(Collider a, Collider b) {
        //get initial support point in any arbitrary direction
        Vector2f support = support2D(a, b, new Vector2f(1f, 0f));

        //create simplex and add initial point
        List<Vector2f> points = new ArrayList<>(3);
        points.addFirst(support);

        //new direction is towards the origin
        Vector2f dir = new Vector2f(support).negate();

        while (true) {
            //get a new support point in the current direction
            support = support2D(a, b, dir);

            //if the new point is not past the origin in the direction we are looking, no collision
            if (support.dot(dir) <= 0f)
                return false;

            //add the new point to the simplex
            points.addFirst(support);

            //check if the simplex contains the origin
            if (nextSimplex2D(points, dir))
                return true;
        }
    }

    private static Vector2f support2D(Collider a, Collider b, Vector2f dir) {
        Vector3f thisFurthest = a.findFurthestPoint(dir.x, dir.y, 0f);
        Vector3f otherFurthest = b.findFurthestPoint(-dir.x, -dir.y, 0f);
        return new Vector2f(thisFurthest).sub(otherFurthest.x, otherFurthest.y);
    }

    private static boolean nextSimplex2D(List<Vector2f> points, Vector2f dir) {
        return switch (points.size()) {
            case 2 -> line2D(points, dir);
            case 3 -> triangle2D(points, dir);
            default -> false; //should never happen
        };
    }

    private static Vector2f tripleProduct(Vector2f a, Vector2f b, Vector2f c) {
        Vector3f A = new Vector3f(a.x, a.y, 0f);
        Vector3f B = new Vector3f(b.x, b.y, 0f);
        Vector3f C = new Vector3f(c.x, c.y, 0f);
        return new Vector2f(A.cross(B).cross(C));
    }

    private static boolean isSameDirection2D(Vector2f dir, Vector2f ao) {
        return dir.dot(ao) > 0f;
    }

    private static boolean line2D(List<Vector2f> points, Vector2f dir) {
        Vector2f a = points.get(0);
        Vector2f b = points.get(1);

        Vector2f ao = a.negate(new Vector2f());
        Vector2f ab = b.sub(a, new Vector2f());

        //see if this is enough (no ab check)
        dir.set(tripleProduct(ab, ao, ab));

        return false;
    }

    private static boolean triangle2D(List<Vector2f> points, Vector2f dir) {
        Vector2f a = points.get(0);
        Vector2f b = points.get(1);
        Vector2f c = points.get(2);

        Vector2f ao = a.negate(new Vector2f());
        Vector2f ab = b.sub(a, new Vector2f());
        Vector2f ac = c.sub(a, new Vector2f());

        Vector2f abf = tripleProduct(ac, ab, ab);
        Vector2f acf = tripleProduct(ab, ac, ac);

        if (isSameDirection2D(abf, ao)) {
            //remove point c
            points.remove(2);
            return line2D(points, dir);
        }

        if (isSameDirection2D(acf, ao)) {
            //remove point b
            points.remove(1);
            return line2D(points, dir);
        }

        return true;
    }
}
