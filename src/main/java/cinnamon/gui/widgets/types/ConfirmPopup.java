package cinnamon.gui.widgets.types;

import cinnamon.Client;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.PopupWidget;
import cinnamon.gui.widgets.Widget;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ConfirmPopup extends PopupWidget {

    protected final ContainerGrid actions;
    protected final Label message;

    protected boolean renderBackground = true;

    public ConfirmPopup(int x, int y, int spacing) {
        this(x, y, spacing, 1);
    }

    public ConfirmPopup(int x, int y, int spacing, int columns) {
        super(x, y, spacing, 1);
        this.actions = new ContainerGrid(0, 0, spacing, columns);
        this.message = new Label(0, 0, Text.empty());

        this.setAlignment(Alignment.CENTER);
        this.addWidgets(message, actions);
        this.voidOutsideClicks(false);
        this.closeOnEscape(false);
    }

    public ConfirmPopup setMessage(Text message) {
        return this.setMessage(message, Alignment.CENTER);
    }

    public ConfirmPopup setMessage(Text message, Alignment alignment) {
        this.message.setText(message);
        this.message.setAlignment(alignment);
        return this;
    }

    public ConfirmPopup setMessageAlignment(Alignment alignment) {
        this.message.setAlignment(alignment);
        return this;
    }

    public ConfirmPopup addAction(Widget widget) {
        this.actions.addWidget(widget);
        return this;
    }

    public ConfirmPopup addAction(Text text, Text tooltip, Runnable action) {
        Button button = new Button(0, 0, 60, 12, text, b -> action.run());
        button.setTooltip(tooltip);
        this.actions.addWidget(button);
        return this;
    }

    @Override
    protected void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (renderBackground)
            this.renderBackground(matrices, delta);
        super.renderWidget(matrices, mouseX, mouseY, delta);
    }

    protected void renderBackground(MatrixStack matrices, float delta) {
        float w = Client.getInstance().window.getGUIWidth();
        float h = Client.getInstance().window.getGUIHeight();
        VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, 0, 0, w, h, 0xDD000000));
    }

    public void setRenderBackground(boolean renderBackground) {
        this.renderBackground = renderBackground;
    }

    @Override
    protected void updateDimensions(int width, int height) {
        super.updateDimensions(Client.getInstance().window.getGUIWidth(), Client.getInstance().window.getGUIHeight());
    }

    public static class OK extends ConfirmPopup {
        public OK(Text message, Runnable callback) {
            super(0, 0, 12);
            setMessage(message);
            addAction(Text.translated("gui.ok"), null, () -> {
                callback.run();
                close();
            });
        }
    }

    public static class YesNo extends ConfirmPopup {
        public YesNo(Text message, Consumer<Boolean> callback) {
            super(0, 0, 12, 2);
            setMessage(message);
            addAction(Text.translated("gui.yes"), null, () -> {
                callback.accept(true);
                close();
            });
            addAction(Text.translated("gui.no"), null, () -> {
                callback.accept(false);
                close();
            });
        }
    }

    public static class Input extends ConfirmPopup {

        public Input(Text message, Text placeholder, BiConsumer<Boolean, String> callback) {
            super(0, 0, 12);
            setMessage(message);
            TextField input = new TextField(0, 0, 60 + 12 + 60, 16);
            input.setHintText(placeholder);
            addAction(input);

            ContainerGrid actionGrid = new ContainerGrid(0, 0, 12, 2);
            addAction(actionGrid);

            Button ok = new Button(0, 0, 60, 12, Text.translated("gui.ok"), b -> {
                callback.accept(true, input.getText());
                close();
            });
            actionGrid.addWidget(ok);

            Button cancel = new Button(0, 0, 60, 12, Text.translated("gui.cancel"), b -> {
                callback.accept(false, input.getText());
                close();
            });
            actionGrid.addWidget(cancel);
        }
    }
}
