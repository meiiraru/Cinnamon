package mayo.gui.screens;

import mayo.gui.ParentedScreen;
import mayo.gui.Screen;
import mayo.gui.Toast;
import mayo.gui.widgets.ContainerList;
import mayo.gui.widgets.GUIListener;
import mayo.gui.widgets.SelectableWidget;
import mayo.gui.widgets.types.Button;
import mayo.gui.widgets.types.Label;
import mayo.gui.widgets.types.SelectionBox;
import mayo.gui.widgets.types.ToggleButton;
import mayo.model.GeometryHelper;
import mayo.parsers.CurveToMesh;
import mayo.parsers.ObjExporter;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.Window;
import mayo.render.batch.VertexConsumer;
import mayo.text.Text;
import mayo.utils.*;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class CurvesScreen extends ParentedScreen {

    private static final int POINT_SIZE = 10;
    private static final int R = (int) (POINT_SIZE * 0.5f);
    private final List<Point> points = new ArrayList<>();

    private Curve curve = new Curve.BSpline().steps(30).loop(true).width(20f);

    private int anchorX, anchorY;

    private boolean renderPointsText = true, renderLines = true;
    private int lastSelected = 3;

    public CurvesScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void init() {
        //widget list
        ContainerList list = new ContainerList(4, 4, 4);

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
        help.setSelectable(true);
        list.addWidget(help);

        //selection box
        SelectionBox box = new SelectionBox(0, 0, 60, 12)
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
        ToggleButton pointsButton = new ToggleButton(0, 0, Text.of("Render points index"));
        pointsButton.setAction(button -> renderPointsText = pointsButton.isToggled());
        pointsButton.setToggled(renderPointsText);
        list.addWidget(pointsButton);

        //lines button
        ToggleButton linesButton = new ToggleButton(0, 0, Text.of("Render lines"));
        linesButton.setAction(button -> renderLines = linesButton.isToggled());
        linesButton.setToggled(renderLines);
        list.addWidget(linesButton);

        //loop button
        ToggleButton loopButton = new ToggleButton(0, 0, Text.of("Loop"));
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

        super.init();
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

        if (focused instanceof Point selected) {
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
                    if (focused == null)
                        addPoint(w.mouseX, w.mouseY);
                }
                case GLFW.GLFW_MOUSE_BUTTON_2 -> removePoint();
                case GLFW.GLFW_MOUSE_BUTTON_3 -> {
                    anchorX = w.mouseX;
                    anchorY = w.mouseY;
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseMove(int x, int y) {
        Window w = client.window;

        if (w.mouse1Press) {
            if (focused instanceof Point selected) {
                selected.setPos(x - R, y - R);
                int i = points.indexOf(selected);
                if (i != -1)
                    curve.setPoint(i, x, randomHeight(), y);
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

    private void addPoint(int x, int y) {
        Point p = new Point(x - R, y - R);
        points.add(p);
        addWidget(p);

        curve.addPoint(x, randomHeight(), y);
    }

    private void removePoint() {
        Point p = null;
        for (Point point : points) {
            if (point.isHovered()) {
                p = point;
                break;
            }
        }

        if (p == null)
            return;

        int i = points.indexOf(p);

        points.remove(i);
        removeWidget(p);
        curve.removePoint(i);
    }

    private static int randomHeight() {
        return (int) (Math.random() * 11) * 10;
    }

    private static class Point extends SelectableWidget {

        private float alpha = 0.5f;

        public Point(int x, int y) {
            super(x, y, POINT_SIZE, POINT_SIZE);
        }

        @Override
        public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            float d = UIHelper.tickDelta(0.6f);
            alpha = Maths.lerp(alpha, this.isHoveredOrFocused() ? 1f : 0.5f, d);
            GeometryHelper.circle(VertexConsumer.GUI, matrices, getX() + R, getY() + R, R, 12, 0xAD72FF + ((int) (alpha * 255) << 24));
        }

        @Override
        public GUIListener mousePress(int button, int action, int mods) {
            if (isActive() && isHovered() && action == GLFW_PRESS && button == GLFW_MOUSE_BUTTON_1) {
                UIHelper.focusWidget(this);
                return this;
            }

            return super.mousePress(button, action, mods);
        }

        @Override
        public GUIListener keyPress(int key, int scancode, int action, int mods) {
            if (isFocused() && action != GLFW_RELEASE) {
                int x = getX();
                int y = getY();

                switch (key) {
                    case GLFW_KEY_LEFT  -> setPos(x - 1, y);
                    case GLFW_KEY_RIGHT -> setPos(x + 1, y);
                    case GLFW_KEY_UP    -> setPos(x, y - 1);
                    case GLFW_KEY_DOWN  -> setPos(x, y + 1);
                    default -> {return null;}
                }

                return this;
            }

            return super.keyPress(key, scancode, action, mods);
        }
    }
}
