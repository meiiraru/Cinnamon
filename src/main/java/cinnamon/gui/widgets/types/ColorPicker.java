package cinnamon.gui.widgets.types;

import cinnamon.gui.widgets.Container;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.PopupWidget;
import cinnamon.gui.widgets.Widget;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.*;
import org.joml.Math;

import java.util.function.Consumer;

public class ColorPicker extends Button {

    private int color = 0xFFFFFFFF; //ARGB
    private int changeColor = color;

    private boolean customTooltip = false;
    private final PopupWidget picker;
    private final ColorWheel wheel;
    private final Rectangle old;

    public ColorPicker(int x, int y, int width, int height) {
        super(x, y, width, height, null, b -> ((ColorPicker) b).openPicker());

        setTooltip(null);

        picker = new PopupWidget(0, 0, 4, 2) {
            @Override
            protected void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
                super.renderWidget(matrices, mouseX, mouseY, delta);
                //render background
                UIHelper.nineQuad(VertexConsumer.MAIN, matrices, getStyle().getResource("color_picker_tex"), getAlignedX() - 2, getAlignedY() - 2, getWidth() + 4, getHeight() + 4, 32, 0, 16, 16, 48, 16);
            }
        };
        picker.closeOnSelect(false);
        picker.setAlignment(Alignment.CENTER);

        //left panel
        ContainerGrid leftSide = new ContainerGrid(0, 0, 0);
        leftSide.setAlignment(Alignment.CENTER_LEFT);
        picker.addWidget(leftSide);

        //buttons
        Container buttons = new Container(0, 0);

        ContainerGrid confirmButtons = new ContainerGrid(0, 0, 1, 2);
        buttons.addWidget(confirmButtons);

        confirmButtons.addWidget(new ConfirmButton(26, Colors.RED.argb, "\u2715", b -> picker.close()));
        confirmButtons.addWidget(new ConfirmButton(26, Colors.GREEN.argb, "\u2713", b -> {
            setColor(changeColor);
            picker.close();
        }));
        confirmButtons.translate(Math.round((64 - confirmButtons.getWidth()) / 2f), 0);

        Button more = new ConfirmButton(12, 0, ">", b -> {
            System.out.println("test");
        });
        more.setX(confirmButtons.getX() + confirmButtons.getWidth() + 4);
        //buttons.addWidget(more);

        leftSide.addWidget(buttons);

        //colour wheel
        Rectangle newColor = new Rectangle(26, changeColor);

        wheel = new ColorWheel(0, 0, 64);
        wheel.setUpdateListener(hsv -> {
            changeColor = ColorUtils.rgbToInt(ColorUtils.hsvToRGB(hsv)) + 0xFF000000;
            newColor.setColor(changeColor);
        });
        leftSide.addWidget(wheel);

        //rectangles
        Container rectangles = new Container(0, 0);
        ContainerGrid colors = new ContainerGrid(0, 0, 1, 2);
        rectangles.addWidget(colors);

        colors.addWidget(old = new Rectangle(26, color));
        colors.addWidget(newColor);
        colors.translate(Math.round((64 - colors.getWidth()) / 2f), 0);

        leftSide.addWidget(rectangles);
    }

    @Override
    public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        Resource tex = getStyle().getResource("color_picker_tex");
        UIHelper.nineQuad(VertexConsumer.MAIN, matrices, tex, getX(), getY(), getWidth(), getHeight(), 16, 0, 16, 16, 48, 16, color);
        UIHelper.nineQuad(VertexConsumer.MAIN, matrices, tex, getX(), getY(), getWidth(), getHeight(), 0, 0, 16, 16, 48, 16);
    }

    @Override
    public void setTooltip(Text tooltip) {
        customTooltip = tooltip != null;
        if (!customTooltip) {
            super.setTooltip(Text.of("#" + ColorUtils.rgbToHex(ColorUtils.intToRGB(color))).append("\n").appendTranslated(ColorNameFinder.getColorName(ColorUtils.hsvToHSL(ColorUtils.rgbToHSV(ColorUtils.intToRGB(color))))));
        } else {
            super.setTooltip(tooltip);
        }
    }

    public int getColor() {
        return color;
    }

    public void setColor(Colors color) {
        setColor(color.argb);
    }

    public void setColor(int color) {
        if (this.color == color)
            return;

        this.color = color;
        if (!customTooltip)
            setTooltip(null);
    }

    protected void openPicker() {
        UIHelper.setPopup(getCenterX(), getCenterY(), picker);
        changeColor = color;
        old.setColor(color);
        wheel.setColor(ColorUtils.rgbToHSV(ColorUtils.intToRGB(color)));
        picker.open();
    }

    private static class ConfirmButton extends Button {
        private final int color;

        public ConfirmButton(int width, int color, String text, Consumer<Button> action) {
            super(0, 0, width, 12, Text.of(text).withStyle(Style.EMPTY.outlined(true)), action);
            this.color = color;
        }

        @Override
        protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, getX(), getY(), getX() + getWidth(), getY() + getHeight(), color));
        }
    }

    private static class Rectangle extends Widget {
        private int color;

        public Rectangle(int width, int color) {
            super(0, 0, width, 8);
            this.color = color;
        }

        public void setColor(int color) {
            this.color = color;
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, getX(), getY(), getX() + getWidth(), getY() + getHeight(), color));
        }
    }
}
