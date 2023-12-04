package mayo.gui.screens;

import mayo.gui.ParentedScreen;
import mayo.gui.Screen;
import mayo.gui.Toast;
import mayo.gui.widgets.SelectableWidget;
import mayo.gui.widgets.types.*;
import mayo.model.GeometryHelper;
import mayo.parsers.CurveToMesh;
import mayo.parsers.ObjExporter;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.Window;
import mayo.render.batch.VertexConsumer;
import mayo.text.Text;
import mayo.utils.ColorUtils;
import mayo.utils.Curve;
import mayo.utils.Maths;
import mayo.utils.TextUtils;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class CurvesScreen extends ParentedScreen {

    private static final int POINT_SIZE = 10;
    private static final int R = (int) (POINT_SIZE * 0.5f);
    private final List<Point> points = new ArrayList<>();
    private Point selected;

    private Curve curve = new Curve.BSpline().steps(30).loop(true).width(20f);

    private int anchorX, anchorY;

    private boolean renderPointsText = true, renderLines = true;
    private int lastSelected = 3;

    public CurvesScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void init() {
        super.init();

        //widget list
        WidgetList list = new WidgetList(4, 4, 4);

        //help label
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

        //selection box
        SelectionBox box = new SelectionBox(0, 0, 60, 12)
                .closeOnSelect(true)
                .setChangeListener(i -> {
                    lastSelected = i;
                    switch (i) {
                        case 0 -> curve = new Curve.Linear(curve);
                        case 1 -> curve = new Curve.Hermite(curve);
                        case 2 -> curve = new Curve.Bezier(curve);
                        case 3 -> curve = new Curve.BSpline(curve);
                        case 4 -> curve = new Curve.BezierDeCasteljau(curve);
                    }
                })
                .addEntry(Text.of("Linear"))
                .addEntry(Text.of("Hermite"))
                .addEntry(Text.of("Bezier"))
                .addEntry(Text.of("B-Spline"))
                .addEntry(Text.of("Bezier De Casteljau"));

        box.select(lastSelected); //widget recreation
        list.addWidget(box);

        //points button
        ToggleButton pointsButton = new ToggleButton(0, 0, 12, Text.of("Render points index"));
        pointsButton.setAction(button -> renderPointsText = pointsButton.isToggled());
        pointsButton.setToggled(renderPointsText);
        list.addWidget(pointsButton);

        //lines button
        ToggleButton linesButton = new ToggleButton(0, 0, 12, Text.of("Render lines"));
        linesButton.setAction(button -> renderLines = linesButton.isToggled());
        linesButton.setToggled(renderLines);
        list.addWidget(linesButton);

        //loop button
        ToggleButton loopButton = new ToggleButton(0, 0, 12, Text.of("Loop"));
        loopButton.setAction(button -> curve.loop(!curve.isLooping()));
        loopButton.setToggled(curve.isLooping());
        list.addWidget(loopButton);

        //export button
        Button exportCurve = new Button(0, 0, 60, 12, Text.of("Export"), button -> {
            try {
                ObjExporter.export("curve", CurveToMesh.generateMesh(curve, true));
                Toast.addToast(Text.of("Curve exported!"), client.font);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.addToast(Text.of(e.getMessage()), client.font);
            }
        });
        list.addWidget(exportCurve);

        //add list to screen
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
                f.render(VertexConsumer.FONT, matrices, p.getX() + POINT_SIZE, p.getY() + POINT_SIZE, Text.of("p" + i));
            }
        }

        //draw lines
        if (renderLines) {
            int max = curve.isLooping() ? size : size - 1;
            for (int i = 0; i < max; i++) {
                Point a = points.get(i);
                Point b = points.get((i + 1) % size);

                GeometryHelper.drawLine(VertexConsumer.GUI, matrices, a.getX() + R, a.getY() + R, b.getX() + R, b.getY() + R, 2, 0x88FF72AD);
            }
        }

        //draw curves
        renderCurve(matrices, curve.getCurve(), 0x7272FF, 0x72FF72);
        renderCurve(matrices, curve.getExternalCurve(), 0x72FFAD, 0xFF72AD);
        renderCurve(matrices, curve.getInternalCurve(), 0xFF7272, 0xFFFF72);

        //draw texts
        f.render(VertexConsumer.FONT, matrices, width / 2f, 4, Text.of("Curve quality: " + this.curve.getSteps() + "\nCurve Size: " + this.curve.getCurve().size()), TextUtils.Alignment.CENTER);

        if (selected != null) {
            Text t = Text.of("x" + selected.getX() + " y" + selected.getY());
            f.render(VertexConsumer.FONT, matrices, 4, height - 4 - TextUtils.getHeight(t, f), t);
        }

        //draw children
        super.render(matrices, mouseX, mouseY, delta);

        //draw FPS
        f.render(VertexConsumer.FONT, matrices, width - 4, 4, Text.of(client.fps + " fps"), TextUtils.Alignment.RIGHT);
    }

    private static void renderCurve(MatrixStack matrices, List<Vector3f> curve, int colorA, int colorB) {
        int size = curve.size();
        for (int i = 0; i < size - 1; i++) {
            float t = (float) i / (size - 1);
            int color = ColorUtils.lerpRGBColor(colorA, colorB, t);

            Vector3f a = curve.get(i);
            Vector3f b = curve.get(i + 1);
            GeometryHelper.drawLine(VertexConsumer.GUI, matrices, a.x, a.z, b.x, b.z, 1, color + (0xFF << 24));
        }
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
                int i = points.indexOf(selected);
                if (i != -1)
                    curve.setPoint(i, x, (int) (Math.random() * 11) * 10, y);
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
            curve.offset(dx, 0, dy);
        }

        return super.mouseMove(x, y);
    }

    @Override
    public boolean scroll(double x, double y) {
        boolean child = super.scroll(x, y);
        if (child) return true;

        curve.steps(Math.max(curve.getSteps() + (int) Math.signum(y), 1));

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
        selected = p;

        curve.addPoint(x, (int) (Math.random() * 11) * 10, y);
    }

    private void removePoint() {
        Point remove = getHovered();
        if (remove != null) {
            int i = points.indexOf(remove);

            points.remove(i);
            removeWidget(remove);

            curve.removePoint(i);
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
    }
}
