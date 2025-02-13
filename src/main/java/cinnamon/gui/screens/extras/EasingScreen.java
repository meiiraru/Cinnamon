package cinnamon.gui.screens.extras;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.SelectableWidget;
import cinnamon.gui.widgets.Tickable;
import cinnamon.gui.widgets.WidgetList;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.Checkbox;
import cinnamon.gui.widgets.types.Label;
import cinnamon.gui.widgets.types.Slider;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.*;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static cinnamon.utils.Maths.Easing.*;

public class EasingScreen extends ParentedScreen {

    public EasingScreen(Screen parentScreen) {
        super(parentScreen);
    }

    private static final List<String> EASING_TYPES = List.of(
            "Sine",         "Quadratic",
            "Cubic",        "Quartic",
            "Quintic",      "Exponential",
            "Circular",     "Back",
            "Elastic",      "Bounce"
    );

    private static final List<Maths.Easing> EASINGS = List.of(
        IN_SINE,    OUT_SINE,    IN_OUT_SINE,       IN_QUAD,   OUT_QUAD,   IN_OUT_QUAD,
        IN_CUBIC,   OUT_CUBIC,   IN_OUT_CUBIC,      IN_QUART,  OUT_QUART,  IN_OUT_QUART,
        IN_QUINT,   OUT_QUINT,   IN_OUT_QUINT,      IN_EXPO,   OUT_EXPO,   IN_OUT_EXPO,
        IN_CIRC,    OUT_CIRC,    IN_OUT_CIRC,       IN_BACK,   OUT_BACK,   IN_OUT_BACK,
        IN_ELASTIC, OUT_ELASTIC, IN_OUT_ELASTIC,    IN_BOUNCE, OUT_BOUNCE, IN_OUT_BOUNCE
    );

    private final EasingWidget[] widgets = new EasingWidget[EASINGS.size()];

    private static final Resource
            PLAY_IMG = new Resource("textures/gui/icons/play.png"),
            PAUSE_IMG = new Resource("textures/gui/icons/pause.png");
    private Slider slider;
    private Button playButton;
    private boolean playing;
    private boolean updateSlider;

    @Override
    public void init() {
        ContainerGrid buttons = new ContainerGrid(4, 4, 12);

        //back button
        Button back = new Button(0, 0, 16, 16, null, button -> close());
        back.setImage(new Resource("textures/gui/icons/back.png"));
        buttons.addWidget(back);

        //controllers
        ContainerGrid controllers = new ContainerGrid(0, 0, 4);
        buttons.addWidget(controllers);

        playButton = new Button(0, 0, 16, 16, null, button -> {
            playing = !playing;
            for (EasingWidget widget : widgets)
                widget.setPlaying(playing);
            button.setImage(playing ? PAUSE_IMG : PLAY_IMG);
        });
        playButton.setImage(PLAY_IMG);
        controllers.addWidget(playButton);

        Checkbox loop = new Checkbox(0, 0, Text.of("Loop"));
        loop.setAction(button -> {
            for (EasingWidget widget : widgets)
                widget.loop(loop.isToggled());
        });
        controllers.addWidget(loop);

        Checkbox lines = new Checkbox(0, 0, Text.of("Show Lines"));
        lines.setAction(button -> {
            for (EasingWidget widget : widgets)
                widget.setRenderLines(lines.isToggled());
        });
        controllers.addWidget(lines);

        this.addWidget(buttons);

        //easing list
        WidgetList list = new WidgetList(width / 2, 4, width - 8, height - 8 - 12, 8, 2);

        ContainerGrid group = null;
        for (int i = 0; i < widgets.length; i++) {
            if (i % 3 == 0) {
                ContainerGrid category = new ContainerGrid(0, 0, 4);
                category.addWidget(new Label(0, 0, Text.of(EASING_TYPES.get(i / 3)).withStyle(Style.EMPTY.outlined(true))));

                group = new ContainerGrid(0, 0, 4, 3);
                category.addWidget(group);

                list.addWidget(category);
            }

            widgets[i] = new EasingWidget(35, 35, EASINGS.get(i));
            widgets[i].setTooltip(Text.of(EASINGS.get(i).name()));
            group.addWidget(widgets[i]);
        }

        list.addWidget(new Label(0, 0, Text.empty()));
        this.addWidget(list);

        Label sliderValue = new Label(0, 0, Text.of("0.00"));
        sliderValue.setAlignment(Alignment.CENTER);

        //slider
        int sliderW = 35 * 6;
        slider = new Slider((width - sliderW) / 2, height - 12, sliderW);
        slider.setMax(EasingWidget.ANIMATION_TIME);
        slider.setTooltipFunction((f, i) -> Text.of("%.2f".formatted(f)));
        slider.setUpdateListener((f, i) -> {
            sliderValue.setText(slider.getTooltip());

            if (updateSlider) {
                updateSlider = false;
                return;
            }
            for (EasingWidget widget : widgets) {
                widget.playTime = i;
                widget.calculatePos();
            }
        });
        this.addWidget(slider);

        sliderValue.setPos(width / 2, 4);
        this.addWidget(sliderValue);

        super.init();
    }

    @Override
    protected void addBackButton() {
        //handled above
        //super.addBackButton();
    }

    @Override
    public void tick() {
        super.tick();
        if (playing) {
            updateSlider = true;
            slider.updateValue(widgets[0].playTime);
            playing = widgets[0].playing;
            playButton.setImage(playing ? PAUSE_IMG : PLAY_IMG);
        }
    }

    private static class EasingWidget extends SelectableWidget implements Tickable {

        private static final int QUALITY = 50;
        private static final int ANIMATION_TIME = 40; //2s

        private final List<Vector2f> points = new ArrayList<>();
        private final int color = Colors.randomRainbow().rgb;

        private boolean renderLines, loop;

        private int playTime;
        private boolean playing, invert;
        private final Vector2f oPos, pos;

        public EasingWidget(int width, int height, Maths.Easing easingFunction) {
            super(0, 0, width, height);

            oPos = new Vector2f(0f, height);
            pos = new Vector2f(oPos);

            float size = QUALITY - 1;
            for (int i = 0; i < size; i++) {
                float x = i / size;
                float y = easingFunction.get(x) * height;
                points.add(new Vector2f(x * width, height - y));
            }
            points.add(new Vector2f(width, 0));
        }

        @Override
        public void tick() {
            oPos.set(pos);

            if (!playing)
                return;

            if (playTime > ANIMATION_TIME) {
                invert = !invert;
                playTime = ANIMATION_TIME;

                if (!loop) {
                    playing = false;
                    return;
                }
            }

            if (playTime < 0) {
                invert = !invert;
                playTime = 0;

                if (!loop) {
                    playing = false;
                    return;
                }
            }

            calculatePos();
            playTime += invert ? -1 : 1;
        }

        private void calculatePos() {
            float time = playTime / (float) ANIMATION_TIME * getWidth();
            int currentIndex = Math.max(0, Maths.binarySearch(0, points.size(), index -> time <= points.get(index).x) - 1);
            int nextIndex = Math.min(points.size() - 1, currentIndex + 1);

            Vector2f a = points.get(currentIndex);
            Vector2f b = points.get(nextIndex);

            float delta = (time - a.x) / (b.x - a.x);
            pos.set(Maths.lerp(a, b, delta));
        }

        @Override
        public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            //background
            VertexConsumer.GUI.consume(GeometryHelper.rectangle(matrices, getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x88000000));

            //graph
            matrices.push();
            matrices.translate(getX(), getY(), 0);

            int size = points.size();
            for (int i = 0; i < size - 1; i++) {
                float t = (float) i / (size - 1);
                int color = ColorUtils.lerpHSVColor(Colors.WHITE.rgb, this.color, t);

                Vector2f a = points.get(i);
                Vector2f b = points.get(i + 1);
                VertexConsumer.GUI.consume(GeometryHelper.line(matrices, a.x, a.y, b.x, b.y, 1f, color + (0xFF << 24)));
            }

            //cursor
            int color = this.color + (0xFF << 24);
            Vector2f a = Maths.lerp(oPos, pos, delta);

            VertexConsumer.GUI.consume(GeometryHelper.circle(matrices, a.x, a.y, 2, 12, color));

            if (renderLines) {
                VertexConsumer.GUI.consume(GeometryHelper.rectangle(matrices, 0f, a.y, getWidth(), a.y + 1, color));
                VertexConsumer.GUI.consume(GeometryHelper.rectangle(matrices, a.x, 0f, a.x + 1, getHeight(), color));
            }

            matrices.pop();
        }

        public void setPlaying(boolean bool) {
            playing = bool;
        }

        public void setRenderLines(boolean bool) {
            this.renderLines = bool;
        }

        public void loop(boolean bool) {
            this.loop = bool;
        }
    }
}
