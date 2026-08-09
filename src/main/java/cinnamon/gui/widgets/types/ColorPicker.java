package cinnamon.gui.widgets.types;

import cinnamon.gui.GUISkin;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.GUIListener;
import cinnamon.gui.widgets.PopupWidget;
import cinnamon.gui.widgets.SelectableWidget;
import cinnamon.input.InputManager;
import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Text;
import cinnamon.utils.*;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.function.Consumer;
import java.util.function.Function;

import static org.lwjgl.glfw.GLFW.*;

public class ColorPicker extends Button {

    protected final Vector3f color = new Vector3f(0f, 0f, 1f); //hsv
    protected final Vector3f prevColor = new Vector3f(color);
    protected int colorInt = 0xFFFFFFFF;
    protected int alpha = 0xFF, prevAlpha = 0xFF;

    protected final PopupWidget popup;
    protected final Picker picker;
    protected final Slider hueSlider, alphaSlider;
    protected final TextField[] fields = new TextField[5];

    protected final boolean allowAlpha;
    protected boolean customTooltip = false;
    protected boolean expanded = false;
    protected FieldMode fieldMode = FieldMode.HEX;

    protected Consumer<Integer> colorChangeListener;
    protected Consumer<Integer> colorAcceptListener;
    protected Function<Integer, Text> tooltipFunc = i -> Text.translated(ColorNameFinder.getColorName(ColorUtils.hsvToHSL(color)));

    public ColorPicker(int x, int y, int width, int height) {
        this(x, y, width, height, true);
    }

    public ColorPicker(int x, int y, int width, int height, boolean alphaInput) {
        super(x, y, width, height, null, b -> ((ColorPicker) b).openPicker());
        this.allowAlpha = alphaInput;
        setTooltip(null); //force tooltip update

        //create the popup
        popup = new PopupWidget(0, 0, 4) {
            @Override
            protected void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
                super.renderWidget(matrices, mouseX, mouseY, delta);
                //render background
                UIHelper.nineQuad(VertexConsumer.MAIN, matrices, getSkin().getResource("color_picker_tex"), getAlignedX() - 2, getAlignedY() - 2, getWidth() + 4, getHeight() + 4, 48, 0, 16, 16, 72, 16);
            }

            @Override
            protected void closeFromEscape() {
                setColor(ColorUtils.hsvToInt(prevColor));
                alpha = prevAlpha;
                super.closeFromEscape();
            }
        };
        popup.closeOnSelect(false);
        popup.setAlignment(Alignment.CENTER);
        popup.setCloseListener(popup -> {
            if (colorAcceptListener != null)
                colorAcceptListener.accept(getColor());
        });
        setPopup(popup);

        //main picker
        popup.addWidget(picker = new Picker(0, 0, 120, 50));
        picker.setChangeListener(this::setColor);

        //hue container
        ContainerGrid hueGrid = new ContainerGrid(0, 0, 6, 3);
        hueGrid.setAlignment(Alignment.CENTER);
        popup.addWidget(hueGrid);

        //color preview
        ContainerGrid colorPreviews = new ContainerGrid(0, 0, 1);
        hueGrid.addWidget(colorPreviews);

        Button newColor = new Button(0, 0, 16, 6, null, b -> popup.close()) {
            @Override
            public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
                renderButton(matrices, getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused(), getSkin(), getColor(), 4f);
            }
        };
        colorPreviews.addWidget(newColor);

        Button oldColor = new Button(0, 0, 16, 6, null, b -> {
            setColor(ColorUtils.hsvToInt(prevColor));
            this.alpha = prevAlpha;
            popup.close();
        }) {
            @Override
            public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
                int color = ColorUtils.hsvToInt(prevColor) & 0xFFFFFF | (prevAlpha << 24);
                renderButton(matrices, getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused(), getSkin(), color, 4f);
            }
        };
        colorPreviews.addWidget(oldColor);

        //sliders
        ContainerGrid sliders = new ContainerGrid(0, 0, 2);
        hueGrid.addWidget(sliders);

        //hue slider
        int sliderWidth = 120 - 12 - 6 - 16 - 6; //base width - button width - padding - color preview width - padding
        hueSlider = new Slider(0, 0, sliderWidth) {
            @Override
            protected void renderHorizontalProgress(MatrixStack matrices, int x, int y, int width, int left, int right) {
                int steps = 12;
                float x1 = x + 1;
                float w = (left + right - 2) / (float) steps;
                for (int i = 0; i < steps; i++) {
                    int color1 = ColorUtils.hsvToInt( i      / (float) steps, 1f, 1f);
                    int color2 = ColorUtils.hsvToInt((i + 1) / (float) steps, 1f, 1f);

                    Vertex[] rect = GeometryHelper.rectangle(matrices, x1, y + 1, x1 + w, y + 3, 0);
                    rect[3].color(color1); rect[2].color(color2);
                    rect[0].color(color1); rect[1].color(color2);

                    VertexConsumer.MAIN.consume(rect);
                    x1 += w;
                }
            }
        };
        hueSlider.setMin(0);
        hueSlider.setMax(360);
        hueSlider.showValueTooltip(false);
        hueSlider.setUpdateListener((f, i) -> {
            color.x = f;
            setColor(color);
        });
        sliders.addWidget(hueSlider);

        //alpha slider
        alphaSlider = new Slider(0, 0, sliderWidth) {
            @Override
            protected void renderHorizontalProgress(MatrixStack matrices, int x, int y, int width, int left, int right) {
                VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices, x + 1, y + 1, width + 8 - 2, 2, 0f, 0f, (width + 8 - 2) / 4f, 0f, 2 / 4f), getSkin().getResource("alpha_checkerboard_tex"));
                Vertex[] rectangle = GeometryHelper.rectangle(matrices, x + 1, y + 1, x + width + 8 - 1, y + 3, colorInt);
                rectangle[3].color(colorInt & 0xFFFFFF);
                rectangle[0].color(colorInt & 0xFFFFFF);
                matrices.translate(0f, 0f, UIHelper.getDepthOffset());
                VertexConsumer.MAIN.consume(rectangle);
            }
        };
        alphaSlider.setMin(0);
        alphaSlider.setMax(255);
        alphaSlider.showValueTooltip(false);
        alphaSlider.setUpdateListener((f, i) -> {alpha = i; updateFields(); acceptColor();});

        //input fields
        ContainerGrid fields = new ContainerGrid(0, 0, 4, 2);
        fields.setAlignment(Alignment.TOP_LEFT);

        //expand button
        Button expand = new Button(0, 0, 12, 12, Text.of("\u23F7"), b -> {
            expanded = !expanded;
            b.setMessage(Text.of(expanded ? "\u23F6" : "\u23F7"));

            if (expanded) {
                if (allowAlpha)
                    sliders.addWidget(alphaSlider);
                popup.addWidget(fields);
            } else {
                sliders.removeWidget(alphaSlider);
                popup.removeWidget(fields);
            }

            UIHelper.setPopup(getCenterX(), getCenterY(), popup);
        });
        hueGrid.addWidget(expand);

        //fields
        int hexWidth =  120 - 12 - 4; //base width - button width - padding
        int rgbWidth = (120 - 12 - 4 - (4 * (allowAlpha ? 3 : 2))) / (allowAlpha ? 4 : 3); //base width - button width - padding - (padding between fields * 3) / 4 field
        ContainerGrid fieldGroup = new ContainerGrid(0, 0, 4, 4);
        fields.addWidget(fieldGroup);

        int len = this.fields.length;
        Label[] labels = new Label[len];
        ContainerGrid[] groups = new ContainerGrid[len];

        for (int i = 0; i < len; i++) {
            boolean first = i == 0;

            this.fields[i] = new TextField(0, 0, first ? hexWidth : rgbWidth, 12) {
                @Override
                protected void onFocusChange(boolean focused) {
                    super.onFocusChange(focused);
                    if (focused) this.setCursorToStart();
                }
            };
            this.fields[i].setListener(s -> inputColor());
            this.fields[i].setCharLimit(3);
            this.fields[i].setFilter(TextField.Filter.NUMBERS);

            labels[i] = new Label(0, 0, Text.of("X"));

            groups[i] = new ContainerGrid(0, 0, 2);
            groups[i].setAlignment(Alignment.TOP_CENTER);
            groups[i].addWidget(this.fields[i]); groups[i].addWidget(labels[i]);
        }

        this.fields[0].setCharLimit(allowAlpha ? 8 : 6);
        this.fields[0].setFilter(TextField.Filter.HEXADECIMAL);
        this.fields[0].setFormatting("#********");
        labels[0].setText(Text.of(allowAlpha ? "HEXA" : "HEX"));

        //swap fields mode
        Button swapMode = new Button(0, 0, 12, 12, Text.of("\u23F5"), b -> {
            FieldMode fieldMode = FieldMode.values()[(this.fieldMode.ordinal() + 1) % FieldMode.values().length];
            switch (fieldMode) {
                case HEX -> {
                    groups[0].forceUpdate();
                    fieldGroup.removeWidget(groups[1]); fieldGroup.removeWidget(groups[2]); fieldGroup.removeWidget(groups[3]); fieldGroup.removeWidget(groups[4]);
                    fieldGroup.addWidget(groups[0]);
                }
                case RGB -> {
                    labels[1].setText(Text.of("R")); labels[2].setText(Text.of("G")); labels[3].setText(Text.of("B")); labels[4].setText(Text.of("A"));
                    groups[1].forceUpdate(); groups[2].forceUpdate(); groups[3].forceUpdate(); groups[4].forceUpdate();
                    fieldGroup.removeWidget(groups[0]);
                    fieldGroup.addWidgets(groups[1], groups[2], groups[3]);
                    if (allowAlpha) fieldGroup.addWidget(groups[4]);
                }
                case HSV -> {
                    labels[1].setText(Text.of("H")); labels[2].setText(Text.of("S")); labels[3].setText(Text.of("V")); labels[4].setText(Text.of("A"));
                    groups[1].forceUpdate(); groups[2].forceUpdate(); groups[3].forceUpdate(); groups[4].forceUpdate();
                }
            }
            this.fieldMode = fieldMode;
            this.updateFields();
        });
        fields.addWidget(swapMode);

        //force a run on the swap button to set the initial field mode
        this.fieldMode = FieldMode.HSV;
        swapMode.executeAction();
    }

    protected void openPicker() {
        UIHelper.setPopup(getCenterX(), getCenterY(), popup);
        this.setColor(color);
        this.prevColor.set(color);
        this.prevAlpha = this.alpha;
        popup.open();
    }

    @Override
    public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderButton(matrices, getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused(), getSkin(), getColor(), 8f);
    }

    protected static void renderButton(MatrixStack matrices, int x, int y, int width, int height, boolean hovered, GUISkin skin, int color, float checkerboardScale) {
        Resource tex = skin.getResource("color_picker_tex");
        Resource checkerboardTex = skin.getResource("alpha_checkerboard_tex");
        float d = UIHelper.getDepthOffset();

        matrices.pushMatrix();
        UIHelper.nineQuad(VertexConsumer.MAIN, matrices, tex, x, y, width, height, hovered ? 16 : 0, 0, 16, 16, 72, 16);
        matrices.translate(0f, 0f, d);
        VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices, x + 1, y + 1, width - 2, height - 2, 0f, 0f, (width - 2) / checkerboardScale, 0f, (height - 2) / checkerboardScale), checkerboardTex);
        matrices.translate(0f, 0f, d);
        UIHelper.nineQuad(VertexConsumer.MAIN, matrices, tex, x, y, width, height, 32, 0, 16, 16, 72, 16, color);
        matrices.popMatrix();
    }

    @Override
    public void setTooltip(Text tooltip) {
        customTooltip = tooltip != null;
        super.setTooltip(!customTooltip && tooltipFunc != null ? tooltipFunc.apply(getColor()) : tooltip);
    }

    public void setTooltipFunc(Function<Integer, Text> tooltipFunc) {
        this.tooltipFunc = tooltipFunc;
    }

    public int getColor() {
        return colorInt & 0xFFFFFF | (alpha << 24);
    }

    public void setColor(Colors color) {
        setColor(color.argb);
    }

    public void setColor(int color) {
        this.alpha = (color >> 24) & 0xFF;
        this.setColor(ColorUtils.intToHSV(color));
    }

    public void setColor(Vector3f hsv) {
        int pureHue = ColorUtils.hsvToInt(hsv.x, 1f, 1f);
        this.colorInt = ColorUtils.hsvToInt(hsv);
        this.color.set(hsv);
        this.picker.setColor(hsv);
        this.hueSlider.setPercentage(hsv.x);
        this.hueSlider.setColor(pureHue);
        this.alphaSlider.setValue(alpha);
        this.alphaSlider.setColor(pureHue);
        this.updateFields();
        this.acceptColor();
    }

    protected void acceptColor() {
        if (!customTooltip && this.tooltipFunc != null)
            setTooltip(null);

        if (colorChangeListener != null)
            colorChangeListener.accept(getColor());
    }

    public void setColorChangeListener(Consumer<Integer> colorChangeListener) {
        this.colorChangeListener = colorChangeListener;
    }

    public void setColorAcceptListener(Consumer<Integer> colorAcceptListener) {
        this.colorAcceptListener = colorAcceptListener;
    }

    protected void updateFields() {
        for (TextField field : this.fields) {
            if (field.isFocused())
                return;
        }

        switch (fieldMode) {
            case HEX -> this.fields[0].updateText(allowAlpha ? String.format("%08X", getColor()) : String.format("%06X", getColor() & 0xFFFFFF));
            case RGB -> {
                this.fields[1].updateText((colorInt >> 16) & 0xFF);
                this.fields[2].updateText((colorInt >>  8) & 0xFF);
                this.fields[3].updateText( colorInt        & 0xFF);
                this.fields[4].updateText(alpha);
            }
            case HSV -> {
                this.fields[1].updateText(Math.round(color.x * 360f));
                this.fields[2].updateText(Math.round(color.y * 100f));
                this.fields[3].updateText(Math.round(color.z * 100f));
                this.fields[4].updateText(alpha);
            }
        }
    }

    protected void inputColor() {
        switch (fieldMode) {
            case HEX -> {
                String hex = this.fields[0].getText();
                int alpha;
                if (hex.length() > 6) {
                    alpha = Integer.parseInt(hex.substring(0, hex.length() - 6), 16);
                    hex = hex.substring(hex.length() - 6);
                } else {
                    alpha = 0xFF;
                }
                Vector3f rgb = ColorUtils.hexStringToRGB(hex);
                setColor(ColorUtils.rgbToInt(rgb) & 0xFFFFFF | (alpha << 24));
            }
            case RGB -> {
                float r = Math.clamp(0, 255, safeParse(this.fields[1].getText())) / 255f;
                float g = Math.clamp(0, 255, safeParse(this.fields[2].getText())) / 255f;
                float b = Math.clamp(0, 255, safeParse(this.fields[3].getText())) / 255f;
                float a = Math.clamp(0, 255, safeParse(this.fields[4].getText())) / 255f;
                setColor(ColorUtils.rgbaToIntARGB(r, g, b, a));
            }
            case HSV -> {
                float h    = Math.clamp(0, 360, safeParse(this.fields[1].getText())) / 360f;
                float s    = Math.clamp(0, 100, safeParse(this.fields[2].getText())) / 100f;
                float v    = Math.clamp(0, 100, safeParse(this.fields[3].getText())) / 100f;
                this.alpha = Math.clamp(0, 255, safeParse(this.fields[4].getText()));
                setColor(new Vector3f(h, s, v));
            }
        }
    }

    protected static int safeParse(String text) {
        return text.isEmpty() ? 0 : Integer.parseInt(text);
    }

    protected static class Picker extends SelectableWidget {
        protected int color;
        protected final Vector3f hsv = new Vector3f();
        protected boolean pressed, shift, ctrl;

        protected Consumer<Vector3f> changeListener;

        public Picker(int x, int y, int width, int height) {
            super(x, y, width, height);
            setSelectable(false);
        }

        @Override
        public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            int hue = ColorUtils.hsvToInt(hsv.x, 1f, 1f);
            Vertex[] vertices = GeometryHelper.rectangle(matrices, getX(), getY(), getX() + getWidth(), getY() + getHeight(), hue);

            matrices.pushMatrix();
            float d = UIHelper.getDepthOffset();

            //base color
            VertexConsumer.MAIN.consume(vertices);

            //white horizontal layer
            vertices[3].color(0xFFFFFFFF); vertices[2].color(0x00FFFFFF);
            vertices[0].color(0xFFFFFFFF); vertices[1].color(0x00FFFFFF);
            matrices.translate(0f, 0f, d);
            VertexConsumer.MAIN.consume(vertices);

            //black vertical layer
            vertices[3].color(0x00000000); vertices[2].color(0x00000000);
            vertices[0].color(0xFF000000); vertices[1].color(0xFF000000);
            matrices.translate(0f, 0f, d);
            VertexConsumer.MAIN.consume(vertices);

            //directional lines
            float x = getX() + getWidth()  * hsv.y;
            float y = getY() + getHeight() * (1f - hsv.z);

            if (shift) { //saturation
                matrices.translate(0f, 0f, d);
                VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, getX(), y, getX() + getWidth(), y, 1, 0x44000000));
            } else if (ctrl) { //value
                matrices.translate(0f, 0f, d);
                VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, x, getY(), x, getY() + getHeight(), 1, 0x44000000));
            }

            //render crosshair
            Resource tex = getSkin().getResource("color_picker_tex");

            Vertex[] crosshair = GeometryHelper.quad(matrices, x - 4, y - 4, 8, 8, 64f, 0f, 8f, 8f, 72, 16);
            matrices.translate(0f, 0f, d);
            VertexConsumer.MAIN.consume(crosshair, tex);

            for (Vertex vertex : crosshair) {
                vertex.uv(vertex.getUV().x, vertex.getUV().y + 0.5f);
                vertex.color(color);
            }
            matrices.translate(0f, 0f, d);
            VertexConsumer.MAIN.consume(crosshair, tex);

            matrices.popMatrix();
        }

        public void setColor(Vector3f hsv) {
            this.color = ColorUtils.hsvToInt(hsv);
            this.hsv.set(hsv);
        }

        @Override
        public GUIListener mousePress(int button, int action, int mods) {
            if (isHoveredOrFocused() && button == GLFW_MOUSE_BUTTON_1 && action == GLFW_PRESS) {
                setColorAtPos(InputManager.getMouseX(), InputManager.getMouseY());
                pressed = true;
                return this;
            }
            if (pressed)
                pressed = false;
            return super.mousePress(button, action, mods);
        }

        @Override
        public GUIListener mouseMove(int x, int y) {
            if (pressed) {
                setColorAtPos(x, y);
                return this;
            }
            return super.mouseMove(x, y);
        }

        @Override
        public GUIListener keyPress(int key, int scancode, int action, int mods) {
            this.shift = (mods & GLFW_MOD_SHIFT)   != 0;
            this.ctrl  = (mods & GLFW_MOD_CONTROL) != 0;

            if (action != GLFW_RELEASE) {
                switch (key) {
                    case GLFW_KEY_UP    -> setColorAtPos((int) (getX() + getWidth() * hsv.y),     (int) (getY() + getHeight() * (1f - hsv.z) - 1));
                    case GLFW_KEY_DOWN  -> setColorAtPos((int) (getX() + getWidth() * hsv.y),     (int) (getY() + getHeight() * (1f - hsv.z) + 1));
                    case GLFW_KEY_LEFT  -> setColorAtPos((int) (getX() + getWidth() * hsv.y - 1), (int) (getY() + getHeight() * (1f - hsv.z)));
                    case GLFW_KEY_RIGHT -> setColorAtPos((int) (getX() + getWidth() * hsv.y + 1), (int) (getY() + getHeight() * (1f - hsv.z)));
                    default -> {return super.keyPress(key, scancode, action, mods);}
                }
                return this;
            }

            return super.keyPress(key, scancode, action, mods);
        }

        protected void setColorAtPos(int x, int y) {
            if (!ctrl || shift)
                this.hsv.y = Math.clamp(0f, 1f,      (x - getX()) / (float) getWidth());
            if (!shift)
                this.hsv.z = Math.clamp(0f, 1f, 1f - (y - getY()) / (float) getHeight());
            this.color = ColorUtils.hsvToInt(this.hsv);

            if (changeListener != null)
                changeListener.accept(this.hsv);
        }

        public void setChangeListener(Consumer<Vector3f> changeListener) {
            this.changeListener = changeListener;
        }

        @Override
        public boolean isHovered() {
            return pressed || super.isHovered();
        }
    }

    protected enum FieldMode {
        HEX, RGB, HSV
    }
}
