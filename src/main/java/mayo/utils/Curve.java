package mayo.utils;

import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public abstract class Curve {

    protected final List<Vector3f>
            controlPoints = new ArrayList<>(),
            curve = new ArrayList<>(),
            internalCurve = new ArrayList<>(),
            externalCurve = new ArrayList<>();

    protected boolean loop;
    protected boolean dirty = true;
    protected int steps = 1;
    protected float width = 1f;

    public Curve() {}

    public Curve(Curve other) {
        this.controlPoints.addAll(other.controlPoints);
        this.loop = other.loop;
        this.steps = other.steps;
        this.width = other.width;
    }

    public Curve addPoint(float x, float y, float z) {
        controlPoints.add(new Vector3f(x, y, z));
        dirty = true;
        return this;
    }

    public Curve setPoint(int index, float x, float y, float z) {
        controlPoints.get(index).set(x, y, z);
        dirty = true;
        return this;
    }

    public Curve removePoint(int index) {
        controlPoints.remove(index);
        dirty = true;
        return this;
    }

    public Curve offset(float x, float y, float z) {
        for (Vector3f vec : controlPoints)
            vec.add(x, y, z);
        dirty = true;
        return this;
    }

    public Curve clear() {
        controlPoints.clear();
        dirty = true;
        return this;
    }

    protected abstract List<Vector3f> calculateCurve(List<Vector3f> controlPoints);

    private static Vector3f rotatePoint(Vector3f a, Vector3f b, float len, float angle) {
        Vector2f temp = new Vector2f(b.x, b.z).sub(a.x, a.z);
        temp.normalize(len);
        Maths.rotate(temp, angle);

        return new Vector3f(a.x + temp.x, a.y, a.z + temp.y);
    }

    private static Vector3f rotatePoint(Vector3f a, Vector3f b, Vector3f c, float len, float angle) {
        Vector2f ab = new Vector2f(b.x, b.z).sub(a.x, a.z).normalize();
        Vector2f bc = new Vector2f(c.x, c.z).sub(b.x, b.z).normalize();

        ab.add(bc);
        ab.normalize(len);
        Maths.rotate(ab, angle);

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


    public Curve loop(boolean loop) {
        this.loop = loop;
        this.dirty = true;
        return this;
    }

    public boolean isLooping() {
        return loop;
    }

    public Curve steps(int steps) {
        this.steps = steps;
        this.dirty = true;
        return this;
    }

    public int getSteps() {
        return steps;
    }

    public Curve width(float width) {
        this.width = width;
        this.dirty = true;
        return this;
    }

    public float getWidth() {
        return width;
    }

    public List<Vector3f> getCurve() {
        this.recalculate();
        return curve;
    }

    public List<Vector3f> getInternalCurve() {
        this.recalculate();
        return internalCurve;
    }

    public List<Vector3f> getExternalCurve() {
        this.recalculate();
        return externalCurve;
    }

    public List<Vector3f> getControlPoints() {
        return controlPoints;
    }


    // -- types -- //


    public static class Linear extends Curve {
        public Linear() {
            super();
        }

        public Linear(Curve other) {
            super(other);
        }

        @Override
        protected List<Vector3f> calculateCurve(List<Vector3f> controlPoints) {
            int size = controlPoints.size();
            List<Vector3f> curve = new ArrayList<>();

            if (size < 2)
                return curve;

            curve.addAll(controlPoints);

            if (loop)
                curve.add(controlPoints.get(0));

            return curve;
        }
    }

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

        public float getWeight() {
            return weight;
        }

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

                Vector3f p0 = controlPoints.get(Maths.modulo(pprev, size));
                Vector3f p1 = controlPoints.get(Maths.modulo(prev, size));
                Vector3f p2 = controlPoints.get(next % size);
                Vector3f p3 = controlPoints.get(nnext % size);

                for (float j = 0; j <= steps; j++) {
                    float t = j / steps;
                    Vector3f vec = new Vector3f(
                            Maths.bSpline(p0.x, p1.x, p2.x, p3.x, t),
                            Maths.bSpline(p0.y, p1.y, p2.y, p3.y, t),
                            Maths.bSpline(p0.z, p1.z, p2.z, p3.z, t)
                    );
                    if (last == null || !last.equals(vec))
                        curve.add(vec);
                    last = vec;
                }
            }

            return curve;
        }
    }

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

            if (size < 1)
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
}
