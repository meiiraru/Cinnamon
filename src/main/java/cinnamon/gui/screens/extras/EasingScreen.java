package cinnamon.gui.screens.extras;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.Widget;
import cinnamon.gui.widgets.WidgetList;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.Label;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Text;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;

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

    @Override
    public void init() {
        //back button
        Button back = new Button(4, 4, 16, 16, null, button -> close());
        back.setImage(new Resource("textures/gui/icons/back.png"));
        this.addWidget(back);

        //controllers


        //easing list
        WidgetList list = new WidgetList(width / 2, 4, width - 8, height - 8, 8, 2);

        ContainerGrid group = null;
        for (int i = 0; i < widgets.length; i++) {
            if (i % 3 == 0) {
                ContainerGrid category = new ContainerGrid(0, 0, 4);
                category.addWidget(new Label(0, 0, Text.of(EASING_TYPES.get(i / 3)), font));

                group = new ContainerGrid(0, 0, 4, 3);
                category.addWidget(group);

                list.addWidget(category);
            }

            widgets[i] = new EasingWidget(35, 35, EASINGS.get(i));
            group.addWidget(widgets[i]);
        }

        this.addWidget(list);
        super.init();
    }

    @Override
    protected void addBackButton() {
        //handled above
        //super.addBackButton();
    }

    private static class EasingWidget extends Widget {

        private final Maths.Easing easingFunction;

        public EasingWidget(int width, int height, Maths.Easing easingFunction) {
            super(0, 0, width, height);
            this.easingFunction = easingFunction;
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            //background
            GeometryHelper.rectangle(VertexConsumer.GUI, matrices, getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x88000000);
        }
    }
}
