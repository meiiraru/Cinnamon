package mayo.gui.screens;

import mayo.gui.ParentedScreen;
import mayo.gui.Screen;
import mayo.gui.widgets.SelectableWidget;
import mayo.gui.widgets.types.ComboBox;
import mayo.gui.widgets.types.Label;
import mayo.gui.widgets.types.ToggleButton;
import mayo.gui.widgets.types.WidgetList;
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

    private int anchorX, anchorY;

    private int curveType = 2;
    private boolean renderPointsText = true, renderLines = true;

    public Curves(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void init() {
        super.init();

        WidgetList list = new WidgetList(4, 4, 4);

        Label help = new Label(Text.of("\u2753 Help"), client.font, 0, 0);
        help.setTooltip(Text.of("""
                Mouse 1
                    Add control point

                Mouse 1 (select and drag)
                    Move control point

                Mouse 2
                    Remove control point

                Mouse 3 (drag)
                    Move all points

                Scroll
                    Change curve quality"""));
        list.addWidget(help);

        ComboBox box = new ComboBox(0, 0, 60, 12)
                .closeOnSelect(true)
                .setChangeListener(i -> {
                    this.curveType = i;
                    updateCurve();
                })
                .addEntry(Text.of("Hermite"))
                .addEntry(Text.of("Bezier"))
                .addEntry(Text.of("B-Spline"))
                .addEntry(Text.of("Bezier De Casteljau"));

        box.select(curveType); //widget recreation
        list.addWidget(box);

        ToggleButton pointsButton = new ToggleButton(0, 0, 12, Text.of("Render points index"));
        pointsButton.setAction(button -> renderPointsText = pointsButton.isToggled());
        pointsButton.setToggled(renderPointsText);
        list.addWidget(pointsButton);

        ToggleButton linesButton = new ToggleButton(0, 0, 12, Text.of("Render lines"));
        linesButton.setAction(button -> renderLines = linesButton.isToggled());
        linesButton.setToggled(renderLines);
        list.addWidget(linesButton);

        this.addWidget(list);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int size = points.size();
        Font f = client.font;

        //draw text
        if (renderPointsText) {
            for (int i = 0; i < size; i++) {
                Point p = points.get(i);
                f.render(VertexConsumer.FONT_FLAT, matrices, p.getX() + POINT_SIZE, p.getY() + POINT_SIZE, Text.of("p" + i));
            }
        }

        //draw lines
        if (renderLines) {
            for (int i = 0; i < size; i++) {
                Point a = points.get(i);
                Point b = points.get((i + 1) % size);

                UIHelper.drawLine(VertexConsumer.GUI, matrices, a.getX() + R, a.getY() + R, b.getX() + R, b.getY() + R, 2, 0x88FF72AD);
            }
        }

        //draw curve
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

        super.render(matrices, mouseX, mouseY, delta);

        f.render(VertexConsumer.FONT_FLAT, matrices, width - 4, 4, Text.of(client.fps + " fps"), TextUtils.Alignment.RIGHT);
    }

    @Override
    public void rebuild() {
        super.rebuild();
        for (Point point : points)
            addWidget(point);
    }

    @Override
    public boolean mousePress(int button, int action, int mods) {
        boolean child = super.mousePress(button, action, mods);
        if (child) return true;

        if (action == GLFW.GLFW_PRESS) {
            Window w = client.window;
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
                case GLFW.GLFW_MOUSE_BUTTON_3 -> {
                    anchorX = w.mouseX;
                    anchorY = w.mouseY;
                }
            }
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
            selected = null;
        }

        return false;
    }

    @Override
    public boolean mouseMove(int x, int y) {
        Window w = client.window;

        if (w.mouse1Press) {
            if (selected != null) {
                selected.setPos(x - R, y - R);
                updateCurve();
            }
        } else if (w.mouse2Press) {
            removePoint();
        } else if (w.mouse3Press) {
            int dx = x - anchorX;
            int dy = y - anchorY;
            anchorX = x;
            anchorY = y;

            for (Point point : points)
                point.setPos(point.getX() + dx, point.getY() + dy);

            updateCurve();
        }

        return super.mouseMove(x, y);
    }

    @Override
    public boolean scroll(double x, double y) {
        boolean child = super.scroll(x, y);
        if (child) return true;

        steps = Math.max(steps + (int) Math.signum(y), 1);
        updateCurve();

        return false;
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
        switch (curveType) {
            case 0 -> hermite();
            case 1 -> bezier();
            case 2 -> bSpline();
            case 3 -> bezierDeCasteljau();
        }
    }

    private void hermite() {
        int size = points.size();
        if (size < 4)
            return;

        for (int i = 0; i <= size - 2; i += 2) {
            Vector2f p0 = points.get(i).getCenter();
            Vector2f r0 = points.get((i + 1) % size).getCenter().sub(p0);
            Vector2f p3 = points.get((i + 2) % size).getCenter();
            Vector2f r3 = points.get((i + 3) % size).getCenter().sub(p3);

            for (float j = 0; j <= steps; j++) {
                float t = j / steps;
                curve.add(new Vector2f(
                        Maths.hermite(p0.x, p3.x, r0.x, r3.x, 10f, t),
                        Maths.hermite(p0.y, p3.y, r0.y, r3.y, 10f, t)
                ));
            }
        }
    }

    private void bezier() {
        int size = points.size() - 1;
        if (size < 2)
            return;

        for (int i = 0; i <= size - 3; i += 3) {
            Vector2f p0 = points.get(i).getCenter();
            Vector2f p1 = points.get(i + 1).getCenter();
            Vector2f p2 = points.get(i + 2).getCenter();
            Vector2f p3 = points.get(i + 3).getCenter();

            for (float j = 0; j <= steps; j++) {
                float t = j / steps;
                curve.add(new Vector2f(
                        Maths.bezier(p0.x, p1.x, p2.x, p3.x, t),
                        Maths.bezier(p0.y, p1.y, p2.y, p3.y, t)
                ));
            }
        }
    }

    private void bSpline() {
        int size = points.size();
        if (size < 2)
            return;

        for (int i = 0; i < size; i++) {
            Vector2f p0 = points.get(i).getCenter();
            Vector2f p1 = points.get((i + 1) % size).getCenter();
            Vector2f p2 = points.get((i + 2) % size).getCenter();
            Vector2f p3 = points.get((i + 3) % size).getCenter();

            for (float j = 0; j <= steps; j++) {
                float t = j / steps;
                curve.add(new Vector2f(
                        Maths.bSpline(p0.x, p1.x, p2.x, p3.x, t),
                        Maths.bSpline(p0.y, p1.y, p2.y, p3.y, t)
                ));
            }
        }
    }

    private void bezierDeCasteljau() {
        int size = points.size();
        if (size < 1)
            return;

        float[] pointsX = new float[size + 1];
        float[] pointsY = new float[size + 1];

        for (int i = 0; i <= size; i++) {
            Vector2f pos = points.get(i % size).getCenter();
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
        public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            float d = Maths.magicDelta(0.6f, delta);
            alpha = (int) Maths.lerp(alpha, this.isHovered() ? 0xFF : 0x88, d);
            GeometryHelper.circle(VertexConsumer.GUI, matrices, getX() + R, getY() + R, R, 12, 0xAD72FF + (alpha << 24));
        }

        public Vector2f getCenter() {
            return new Vector2f(getCenterX(), getCenterY());
        }
    }
}
