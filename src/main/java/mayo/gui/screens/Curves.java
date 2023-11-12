package mayo.gui.screens;

import mayo.Client;
import mayo.gui.ParentedScreen;
import mayo.gui.Screen;
import mayo.gui.widgets.SelectableWidget;
import mayo.model.GeometryHelper;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.Window;
import mayo.render.batch.VertexConsumer;
import mayo.text.Text;
import mayo.utils.ColorUtils;
import mayo.utils.Maths;
import mayo.utils.TextUtils;
import mayo.utils.UIHelper;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class Curves extends ParentedScreen {

    private static final int POINT_SIZE = 10;
    private static final int R = (int) (POINT_SIZE * 0.5f);
    private final List<Point> points = new ArrayList<>();
    private Point selected;

    private final List<Vector2f> curve = new ArrayList<>();
    private int steps = 30;

    public Curves(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        int size = points.size();
        Font f = Client.getInstance().font;

        //draw text
        for (int i = 0; i < size; i++) {
            Point p = points.get(i);
            f.render(VertexConsumer.FONT_FLAT, matrices, p.getX() + POINT_SIZE, p.getY() + POINT_SIZE, Text.of("p" + i));
        }

        //draw lines
        for (int i = 0; i < size; i++) {
            Point a = points.get(i);
            Point b = points.get((i + 1) % size);

            UIHelper.drawLine(VertexConsumer.GUI, matrices, a.getX() + R, a.getY() + R, b.getX() + R, b.getY() + R, 2, 0x88FF72AD);
        }

        //draw curve
        updateCurve();
        for (int i = 0; i < curve.size() - 1; i++) {
            int colorA = 0x7272FF;
            int colorB = 0x72FF72;
            float t = (float) i / (curve.size() - 1);
            int color = ColorUtils.lerpRGBColor(colorA, colorB, t);

            Vector2f a = curve.get(i);
            Vector2f b = curve.get(i + 1);
            UIHelper.drawLine(VertexConsumer.GUI, matrices, a.x, a.y, b.x, b.y, 4, color + (0xFF << 24));
        }

        f.render(VertexConsumer.FONT_FLAT, matrices, width / 2f, 4, Text.of("Curve quality: " + steps), TextUtils.Alignment.CENTER);

        if (selected != null) {
            Text t = Text.of("x" + selected.getX() + " y" + selected.getY());
            f.render(VertexConsumer.FONT_FLAT, matrices, 4, height - 4 - TextUtils.getHeight(t, f), t);
        }
    }

    @Override
    public void rebuild() {
        super.rebuild();
        for (Point point : points)
            addWidget(point);
    }

    @Override
    public void mousePress(int button, int action, int mods) {
        if (action == GLFW.GLFW_PRESS) {
            Window w = Client.getInstance().window;
            switch (button) {
                case GLFW.GLFW_MOUSE_BUTTON_1 -> {
                    Point p = getHovered();
                    if (p == null) {
                        addPoint(w.mouseX, w.mouseY);
                    } else {
                        selected = p;
                    }
                }
                case GLFW.GLFW_MOUSE_BUTTON_2 -> removePoint();
            }
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
            selected = null;
        }

        super.mousePress(button, action, mods);
    }

    @Override
    public void mouseMove(int x, int y) {
        Window w = Client.getInstance().window;

        if (w.mouse1Press) {
            if (selected != null) {
                selected.setPos(x - R, y - R);
                updateCurve();
            }
        } else if (w.mouse2Press) {
            removePoint();
        }

        super.mouseMove(x, y);
    }

    @Override
    public void scroll(double x, double y) {
        steps = Math.max(steps + (int) Math.signum(y), 1);
        updateCurve();
        super.scroll(x, y);
    }

    private Point getHovered() {
        for (int i = points.size() - 1; i >= 0; i--) {
            Point p = points.get(i);
            if (p.isHovered())
                return p;
        }
        return null;
    }

    private void addPoint(int x, int y) {
        Point p = new Point(x - R, y - R);
        points.add(p);
        addWidget(p);
        updateCurve();
        selected = p;
    }

    private void removePoint() {
        Point remove = getHovered();
        if (remove != null) {
            points.remove(remove);
            removeWidget(remove);
            updateCurve();
        }
    }

    private void updateCurve() {
        curve.clear();

        //hermite();
        //bezier();
        bezierDeCasteljau();
    }

    private void hermite() {
        int size = points.size() - 1;
        if (size < 3)
            return;

        Vector2f p0 = points.get(0).getCenter();
        Vector2f p3 = points.get(2).getCenter();

        Vector2f r0 = points.get(1).getCenter().sub(p0);
        Vector2f r3 = points.get(3).getCenter().sub(p3);

        for (float j = 0; j <= steps; j++) {
            float t = j / steps;
            curve.add(new Vector2f(
                    Maths.hermite(p0.x, p3.x, r0.x, r3.x, 10f, t),
                    Maths.hermite(p0.y, p3.y, r0.y, r3.y, 10f, t)
            ));
        }
    }

    private void bezier() {
        int size = points.size() - 1;
        if (size < 3)
            return;

        Vector2f p0 = points.get(0).getCenter();
        Vector2f p1 = points.get(1).getCenter();
        Vector2f p2 = points.get(2).getCenter();
        Vector2f p3 = points.get(3).getCenter();

        for (float j = 0; j <= steps; j++) {
            float t = j / steps;
            curve.add(new Vector2f(
                    Maths.bezier(p0.x, p1.x, p2.x, p3.x, t),
                    Maths.bezier(p0.y, p1.y, p2.y, p3.y, t)
            ));
        }
    }

    private void bezierDeCasteljau() {
        int size = points.size();
        if (size < 1)
            return;

        float[] pointsX = new float[size];
        float[] pointsY = new float[size];

        for (int i = 0; i < size; i++) {
            Vector2f pos = points.get(i).getCenter();
            pointsX[i] = pos.x;
            pointsY[i] = pos.y;
        }

        for (float j = 0; j <= steps; j++) {
            float t = j / steps;
            curve.add(new Vector2f(
                    Maths.bezierDeCasteljau(t, pointsX),
                    Maths.bezierDeCasteljau(t, pointsY)
            ));
        }
    }

    private static class Point extends SelectableWidget {

        private int alpha = 0x88;

        public Point(int x, int y) {
            super(x, y, POINT_SIZE, POINT_SIZE);
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            float d = Maths.magicDelta(0.6f, delta);
            alpha = (int) Maths.lerp(alpha, this.isHovered() ? 0xFF : 0x88, d);
            GeometryHelper.circle(VertexConsumer.GUI, matrices, getX() + R, getY() + R, R, 12, 0xAD72FF + (alpha << 24));
        }

        public Vector2f getCenter() {
            return new Vector2f(getCenterX(), getCenterY());
        }
    }
}
