package mayo.utils;

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

    public Vector3f getPoint(int index) {
        return new Vector3f(controlPoints.get(index));
    }

    public Curve removePoint(int index) {
        controlPoints.remove(index);
        dirty = true;
        return this;
    }

    protected abstract List<Vector3f> calculateCurve(List<Vector3f> controlPoints);

    //TODO
    private List<Vector3f> sideControlPoints(boolean external) {
        return new ArrayList<>();
    }

    private void recalculate() {
        if (!dirty)
            return;

        //main curve
        curve.clear();
        curve.addAll(calculateCurve(controlPoints));

        //internal
        internalCurve.clear();
        internalCurve.addAll(sideControlPoints(false));

        //external
        externalCurve.clear();
        externalCurve.addAll(sideControlPoints(true));

        dirty = false;
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


    // -- types -- //


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

            for (int i = 0; i < max; i += 2) {
                Vector3f p0 = controlPoints.get(i);
                Vector3f r0 = controlPoints.get((i + 1) % size).sub(p0, new Vector3f());
                Vector3f p3 = controlPoints.get((i + 2) % size);
                Vector3f r3 = controlPoints.get((i + 3) % size).sub(p3, new Vector3f());

                for (float j = 0; j <= steps; j++) {
                    float t = j / steps;
                    curve.add(new Vector3f(
                            Maths.hermite(p0.x, p3.x, r0.x, r3.x, weight, t),
                            Maths.hermite(p0.y, p3.y, r0.y, r3.y, weight, t),
                            Maths.hermite(p0.z, p3.z, r0.z, r3.z, weight, t)
                    ));
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

            //never loops
            for (int i = 0; i < size - 3; i += 3) {
                Vector3f p0 = controlPoints.get(i);
                Vector3f p1 = controlPoints.get(i + 1);
                Vector3f p2 = controlPoints.get(i + 2);
                Vector3f p3 = controlPoints.get(i + 3);

                for (float j = 0; j <= steps; j++) {
                    float t = j / steps;
                    curve.add(new Vector3f(
                            Maths.bezier(p0.x, p1.x, p2.x, p3.x, t),
                            Maths.bezier(p0.y, p1.y, p2.y, p3.y, t),
                            Maths.bezier(p0.z, p1.z, p2.z, p3.z, t)
                    ));
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

            if (size == 0)
                return curve;

            for (int i = -1; i < size; i++) {
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

                Vector3f p0 = controlPoints.get((int) Maths.modulo(pprev, size));
                Vector3f p1 = controlPoints.get((int) Maths.modulo(prev, size));
                Vector3f p2 = controlPoints.get(next % size);
                Vector3f p3 = controlPoints.get(nnext % size);

                for (float j = 0; j <= steps; j++) {
                    float t = j / steps;
                    curve.add(new Vector3f(
                            Maths.bSpline(p0.x, p1.x, p2.x, p3.x, t),
                            Maths.bSpline(p0.y, p1.y, p2.y, p3.y, t),
                            Maths.bSpline(p0.z, p1.z, p2.z, p3.z, t)
                    ));
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

            for (float j = 0; j <= steps; j++) {
                float t = j / steps;
                curve.add(new Vector3f(
                        Maths.bezierDeCasteljau(t, pointsX),
                        Maths.bezierDeCasteljau(t, pointsY),
                        Maths.bezierDeCasteljau(t, pointsZ)
                ));
            }

            return curve;
        }
    }
}
