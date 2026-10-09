package cinnamon.math;

import org.joml.Math;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * A representation of a curve in 3D space, defined by control points and various interpolation methods
 */
public abstract class Curve {

    protected final List<Vector3f>
            controlPoints = new ArrayList<>(),
            curve = new ArrayList<>(),
            internalCurve = new ArrayList<>(),
            externalCurve = new ArrayList<>();

    protected boolean loop;
    protected boolean dirty;
    protected int steps = 1;
    protected float width = 1f;

    public Curve() {}

    /**
     * Creates a new curve by copying the properties of another curve
     * @param other the curve to copy
     */
    public Curve(Curve other) {
        this.controlPoints.addAll(other.controlPoints);
        this.loop = other.loop;
        this.steps = other.steps;
        this.width = other.width;
        this.dirty = true;
    }

    /**
     * Adds a new control point to the curve at the specified coordinates
     * @param x The x coordinate of the control point
     * @param y The y coordinate of the control point
     * @param z The z coordinate of the control point
     * @return The current curve instance, allowing for method chaining
     */
    public Curve addPoint(float x, float y, float z) {
        controlPoints.add(new Vector3f(x, y, z));
        dirty = true;
        return this;
    }

    /**
     * Sets the coordinates of an existing control point at the specified index
     * @param index The index of the control point to set
     * @param x The new x coordinate of the control point
     * @param y The new y coordinate of the control point
     * @param z The new z coordinate of the control point
     * @return The current curve instance, allowing for method chaining
     */
    public Curve setPoint(int index, float x, float y, float z) {
        controlPoints.get(index).set(x, y, z);
        dirty = true;
        return this;
    }

    /**
     * Removes the control point at the specified index from the curve
     * @param index The index of the control point to remove
     * @return The current curve instance, allowing for method chaining
     */
    public Curve removePoint(int index) {
        controlPoints.remove(index);
        dirty = true;
        return this;
    }

    /**
     * Offsets all control points of the curve by the specified amounts in each direction
     * @param x The amount to offset in the x direction
     * @param y The amount to offset in the y direction
     * @param z The amount to offset in the z direction
     * @return The current curve instance, allowing for method chaining
     */
    public Curve offset(float x, float y, float z) {
        for (Vector3f vec : controlPoints)
            vec.add(x, y, z);
        dirty = true;
        return this;
    }

    /**
     * Clears all control points from the curve, effectively resetting it
     * @return The current curve instance, allowing for method chaining
     */
    public Curve clear() {
        controlPoints.clear();
        dirty = true;
        return this;
    }

    protected abstract List<Vector3f> calculateCurve(List<Vector3f> controlPoints);

    private static Vector3f rotatePoint(Vector3f a, Vector3f b, float len, float angle) {
        Vector2f temp = new Vector2f(b.x, b.z).sub(a.x, a.z);
        temp.normalize(len);
        temp.rotate(Math.toRadians(angle));

        return new Vector3f(a.x + temp.x, a.y, a.z + temp.y);
    }

    private static Vector3f rotatePoint(Vector3f a, Vector3f b, Vector3f c, float len, float angle) {
        Vector2f ab = new Vector2f(b.x, b.z).sub(a.x, a.z).normalize();
        Vector2f bc = new Vector2f(c.x, c.z).sub(b.x, b.z).normalize();

        ab.add(bc);
        ab.normalize(len);
        ab.rotate(Math.toRadians(angle));

        return new Vector3f(b.x + ab.x, b.y, b.z + ab.y);
    }

    private List<Vector3f> sideCurve(float distance, float angle) {
        int size = curve.size();
        List<Vector3f> list = new ArrayList<>();

        if (size < 2)
            return list;

        //first instance
        Vector3f f1 = curve.get(0);
        Vector3f f2 = curve.get(1);
        if (loop) {
            Vector3f f0 = curve.get(size - 2); //index -1 should be the same as f1
            list.add(rotatePoint(f0, f1, f2, distance, angle));
        } else {
            list.add(rotatePoint(f1, f2, distance, angle));
        }

        //main curve
        for (int i = 1; i < size - 1; i++) {
            Vector3f a = curve.get(i - 1);
            Vector3f b = curve.get(i);
            Vector3f c = curve.get(i + 1);
            list.add(rotatePoint(a, b, c, distance, angle));
        }

        //calculate last instance
        Vector3f l1 = curve.get(size - 1);
        Vector3f l2 = curve.get(size - 2);
        if (loop) {
            Vector3f l0 = curve.get(1); //index 0 should be the same as l1
            list.add(rotatePoint(l0, l1, l2, distance, -angle));
        } else {
            list.add(rotatePoint(l1, l2, distance, -angle));
        }

        return list;
    }

    private void recalculate() {
        if (!dirty)
            return;

        //main curve
        curve.clear();
        curve.addAll(calculateCurve(controlPoints));

        //get distance from main curve
        float distance = width * 0.5f;

        //internal
        internalCurve.clear();
        internalCurve.addAll(sideCurve(distance, 90));

        //external
        externalCurve.clear();
        externalCurve.addAll(sideCurve(distance, -90));

        dirty = false;
    }

    /**
     * Calculates and returns the center point of the curve based on its control points
     * @return A {@link Vector3f} representing the center point of the curve
     */
    public Vector3f getCenter() {
        //min-max vectors
        Vector3f min = new Vector3f(Integer.MAX_VALUE);
        Vector3f max = new Vector3f(Integer.MIN_VALUE);

        //grab min and max
        for (Vector3f vec : controlPoints) {
            min.min(vec);
            max.max(vec);
        }

        //grab offset
        return min.add(max).mul(0.5f);
    }


    // -- getters and setters -- //


    /**
     * Sets whether the curve should loop back to its starting point or not
     * @param loop {@code true} if the curve should loop, {@code false} otherwise
     * @return The current curve instance, allowing for method chaining
     */
    public Curve loop(boolean loop) {
        this.loop = loop;
        this.dirty = true;
        return this;
    }

    /**
     * Gets if the curve is set to loop back to its starting point
     * @return {@code true} if the curve is set to loop, {@code false} otherwise
     */
    public boolean isLooping() {
        return loop;
    }

    /**
     * Sets the number of steps used to calculate the curve, affecting its smoothness
     * @param steps The number of steps to use for curve calculation
     * @return The current curve instance, allowing for method chaining
     */
    public Curve steps(int steps) {
        this.steps = steps;
        this.dirty = true;
        return this;
    }

    /**
     * Gets the number of steps used to calculate the curve
     * @return The number of steps used for curve calculation
     */
    public int getSteps() {
        return steps;
    }

    /**
     * Sets the width of the curve, which affects the distance of the internal and external curves from the main curve
     * @param width The width of the curve
     * @return The current curve instance, allowing for method chaining
     */
    public Curve width(float width) {
        this.width = width;
        this.dirty = true;
        return this;
    }

    /**
     * Gets the width of the curve
     * @return The width of the curve
     */
    public float getWidth() {
        return width;
    }

    /**
     * Calculates and returns the main curve based on the control points and interpolation method
     * @return A {@link List} of {@link Vector3f} representing the points of the main curve
     */
    public List<Vector3f> getCurve() {
        this.recalculate();
        return curve;
    }

    /**
     * Calculates and returns the internal curve, which is offset from the main curve by half the width
     * @return A {@link List} of {@link Vector3f} representing the points of the internal curve
     * @see #getExternalCurve()
     */
    public List<Vector3f> getInternalCurve() {
        this.recalculate();
        return internalCurve;
    }

    /**
     * Calculates and returns the external curve, which is offset from the main curve by half the width
     * @return A {@link List} of {@link Vector3f} representing the points of the external curve
     * @see #getInternalCurve()
     */
    public List<Vector3f> getExternalCurve() {
        this.recalculate();
        return externalCurve;
    }

    /**
     * Gets the list of control points that define the shape of the curve
     * @return A {@link List} of {@link Vector3f} representing the control points of the curve
     */
    public List<Vector3f> getControlPoints() {
        return controlPoints;
    }

    private static void grabPoints(List<Vector3f> controlPoints, boolean loop, int size, int i, Vector3f[] out) {
        int pprev, prev, next, nnext;

        if (loop) {
            pprev = i - 1;
            prev = i;
            next = i + 1;
            nnext =  i + 2;
        } else {
            pprev = Math.max(i - 1, 0);
            prev = Math.max(i, 0);
            next = Math.min(i + 1, size - 1);
            nnext = Math.min(next + 1, size - 1);
        }

        out[0].set(controlPoints.get(Maths.modulo(pprev, size)));
        out[1].set(controlPoints.get(Maths.modulo(prev, size)));
        out[2].set(controlPoints.get(next % size));
        out[3].set(controlPoints.get(nnext % size));
    }


    // -- types -- //


    /**
     * A linear interpolation curve that connects control points with straight lines
     */
    public static class Linear extends Curve {
        public Linear() {
            super();
        }

        public Linear(Curve other) {
            super(other);
        }

        @Override
        protected List<Vector3f> calculateCurve(List<Vector3f> controlPoints) {
            if (controlPoints.size() < 2)
                return new ArrayList<>();

            List<Vector3f> curve = new ArrayList<>(controlPoints);
            if (loop) curve.add(controlPoints.getFirst());
            return curve;
        }
    }

    /**
     * A Hermite interpolation curve that uses control points and tangents to create a smooth curve
     */
    public static class Hermite extends Curve {
        protected float weight = 5f;

        public Hermite() {
            super();
        }

        public Hermite(Curve other) {
            super(other);
            if (other instanceof Hermite h)
                this.weight = h.weight;
        }

        /**
         * Gets the weight used in the Hermite interpolation, which affects the tension of the curve
         * @return The weight used in the Hermite interpolation
         */
        public float getWeight() {
            return weight;
        }

        /**
         * Sets the weight used in the Hermite interpolation, which affects the tension of the curve
         * @param weight The weight to use in the Hermite interpolation
         * @return The current Hermite curve instance, allowing for method chaining
         */
        public Hermite weight(float weight) {
            this.weight = weight;
            this.dirty = true;
            return this;
        }

        @Override
        protected List<Vector3f> calculateCurve(List<Vector3f> controlPoints) {
            int size = controlPoints.size();
            List<Vector3f> curve = new ArrayList<>();

            if (size < 4)
                return curve;

            int max = loop ? size - 1 : size - 3;
            Vector3f last = null;

            for (int i = 0; i < max; i += 2) {
                Vector3f p0 = controlPoints.get(i);
                Vector3f r0 = controlPoints.get((i + 1) % size).sub(p0, new Vector3f());
                Vector3f p3 = controlPoints.get((i + 2) % size);
                Vector3f r3 = controlPoints.get((i + 3) % size).sub(p3, new Vector3f());

                for (float j = 0; j <= steps; j++) {
                    float t = j / steps;
                    Vector3f vec = new Vector3f(
                            Maths.hermite(p0.x, p3.x, r0.x, r3.x, weight, t),
                            Maths.hermite(p0.y, p3.y, r0.y, r3.y, weight, t),
                            Maths.hermite(p0.z, p3.z, r0.z, r3.z, weight, t)
                    );
                    if (last == null || !last.equals(vec))
                        curve.add(vec);
                    last = vec;
                }
            }

            return curve;
        }
    }

    /**
     * A Bezier interpolation curve that uses control points to create a smooth curve
     */
    public static class Bezier extends Curve {
        public Bezier() {
            super();
        }

        public Bezier(Curve other) {
            super(other);
        }

        @Override
        protected List<Vector3f> calculateCurve(List<Vector3f> controlPoints) {
            int size = controlPoints.size();
            List<Vector3f> curve = new ArrayList<>();

            if (size < 3)
                return curve;

            Vector3f last = null;

            //never loops
            for (int i = 0; i < size - 3; i += 3) {
                Vector3f p0 = controlPoints.get(i);
                Vector3f p1 = controlPoints.get(i + 1);
                Vector3f p2 = controlPoints.get(i + 2);
                Vector3f p3 = controlPoints.get(i + 3);

                for (float j = 0; j <= steps; j++) {
                    float t = j / steps;
                    Vector3f vec = new Vector3f(
                            Maths.bezier(p0.x, p1.x, p2.x, p3.x, t),
                            Maths.bezier(p0.y, p1.y, p2.y, p3.y, t),
                            Maths.bezier(p0.z, p1.z, p2.z, p3.z, t)
                    );
                    if (last == null || !last.equals(vec))
                        curve.add(vec);
                    last = vec;
                }
            }

            return curve;
        }
    }

    /**
     * A B-Spline interpolation curve that uses control points to create a smooth curve with local control
     */
    public static class BSpline extends Curve {
        public BSpline() {
            super();
        }

        public BSpline(Curve other) {
            super(other);
        }

        @Override
        protected List<Vector3f> calculateCurve(List<Vector3f> controlPoints) {
            int size = controlPoints.size();
            List<Vector3f> curve = new ArrayList<>();

            if (size < 2)
                return curve;

            Vector3f last = null;

            for (int i = loop ? 0 : -1; i < size; i++) {
                Vector3f[] out = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
                grabPoints(controlPoints, loop, size, i, out);

                for (float j = 0; j <= steps; j++) {
                    float t = j / steps;
                    Vector3f vec = new Vector3f(
                            Maths.bSpline(out[0].x, out[1].x, out[2].x, out[3].x, t),
                            Maths.bSpline(out[0].y, out[1].y, out[2].y, out[3].y, t),
                            Maths.bSpline(out[0].z, out[1].z, out[2].z, out[3].z, t)
                    );
                    if (last == null || !last.equals(vec))
                        curve.add(vec);
                    last = vec;
                }
            }

            return curve;
        }
    }

    /**
     * A Bezier interpolation curve that uses De Casteljau's algorithm to create a smooth curve from control points
     */
    public static class BezierDeCasteljau extends Curve {
        public BezierDeCasteljau() {
            super();
        }

        public BezierDeCasteljau(Curve other) {
            super(other);
        }

        @Override
        protected List<Vector3f> calculateCurve(List<Vector3f> controlPoints) {
            int size = controlPoints.size();
            List<Vector3f> curve = new ArrayList<>();

            if (size < 2)
                return curve;

            int len = loop ? size + 1 : size;
            float[] pointsX = new float[len];
            float[] pointsY = new float[len];
            float[] pointsZ = new float[len];

            for (int i = 0; i < len; i++) {
                Vector3f pos = controlPoints.get(i % size);
                pointsX[i] = pos.x;
                pointsY[i] = pos.y;
                pointsZ[i] = pos.z;
            }

            Vector3f last = null;
            int steps = this.steps * len;

            for (float j = 0; j <= steps; j++) {
                float t = j / steps;
                Vector3f vec = new Vector3f(
                        Maths.bezierDeCasteljau(t, pointsX),
                        Maths.bezierDeCasteljau(t, pointsY),
                        Maths.bezierDeCasteljau(t, pointsZ)
                );
                if (last == null || !last.equals(vec))
                    curve.add(vec);
                last = vec;
            }

            return curve;
        }
    }

    /**
     * A Catmull-Rom interpolation curve that uses control points to create a smooth curve that passes through the control points
     */
    public static class CatmullRom extends Curve {
        public CatmullRom() {
            super();
        }

        public CatmullRom(Curve other) {
            super(other);
        }

        @Override
        protected List<Vector3f> calculateCurve(List<Vector3f> controlPoints) {
            int size = controlPoints.size();
            List<Vector3f> curve = new ArrayList<>();

            if (size < 2)
                return curve;

            Vector3f last = null;

            for (int i = loop ? 0 : -1; i < size; i++) {
                Vector3f[] out = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
                grabPoints(controlPoints, loop, size, i, out);

                for (float j = 0; j <= steps; j++) {
                    float t = j / steps;
                    Vector3f vec = new Vector3f(
                            Maths.catmullRom(out[0].x, out[1].x, out[2].x, out[3].x, t),
                            Maths.catmullRom(out[0].y, out[1].y, out[2].y, out[3].y, t),
                            Maths.catmullRom(out[0].z, out[1].z, out[2].z, out[3].z, t)
                    );
                    if (last == null || !last.equals(vec))
                        curve.add(vec);
                    last = vec;
                }
            }

            return curve;
        }
    }
}
