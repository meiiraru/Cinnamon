package cinnamon.gui.widgets.types;

import cinnamon.Client;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.PopupWidget;
import cinnamon.gui.widgets.Widget;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.TextUtils;
import cinnamon.utils.UIHelper;

import java.util.ArrayList;
import java.util.List;

public class MenuBar extends ContainerGrid {

    private final int barWidth, barHeight;
    private PopupWidget currentMenu = null;

    protected final List<Button> tabs = new ArrayList<>();

    public MenuBar(int x, int y, int barWidth, int barHeight, int spacing) {
        super(x, y, spacing, 1);
        this.barWidth = barWidth;
        this.barHeight = barHeight;
        setAlignment(Alignment.CENTER_LEFT);
        setBackground(true);
        addSpacing(true);
    }

    @Override
    protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        //render background
        UIHelper.nineQuad(
                VertexConsumer.MAIN, matrices, getSkin().getResource("menu_bar_tex"),
                getAlignedX(), getAlignedY(),
                getWidth(), getHeight(),
                0f, 0f,
                16, 16,
                35, 16
        );
    }

    @Override
    protected void updateDimensions(int width, int height) {
        super.updateDimensions(getBarWidth(), getBarHeight());
    }

    public MenuBar addTab(Text text, PopupWidget menu) {
        Button tab = new MenuButton(TextUtils.getWidth(text) + 4, getBarHeight(), text, menu, this);
        tab.setSilent(true);
        tab.setRenderBackground(false);

        tabs.add(tab);
        addWidget(tab);

        int column = getWidgets().size();
        setColumns(column);

        return this;
    }

    public MenuBar addSpacing() {
        return addSpacing(false);
    }

    public MenuBar addSpacing(boolean hidden) {
        addWidget(new Spacing(getBarHeight() - getSpacing() * 2, getSpacing(), !hidden));

        int column = getWidgets().size();
        setColumns(column);

        return this;
    }

    public Button getTab(int index) {
        return tabs.get(index);
    }

    public boolean isExpanded() {
        return currentMenu != null && currentMenu.isOpen();
    }

    public int getBarWidth() {
        return barWidth;
    }

    public int getBarHeight() {
        return barHeight;
    }


    // -- children objects -- //


    private static class MenuButton extends Button {

        private final MenuBar parent;

        public MenuButton(int width, int height, Text message, PopupWidget menu, MenuBar parent) {
            super(0, 0, width, height, message, b -> {
                boolean isOpen = parent.currentMenu == b.getPopup() && parent.currentMenu.isOpen();
                if (isOpen) {
                    parent.currentMenu.close();
                    parent.currentMenu = null;
                } else {
                    ((MenuButton) b).openPopup(0, 0);
                }
            });
            this.parent = parent;
            this.setPopup(menu);
            menu.setParentButton(this);
        }

        @Override
        public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            super.renderWidget(matrices, mouseX, mouseY, delta);
            if (this.isHoveredOrFocused()) {
                //render hovered overlay
                UIHelper.nineQuad(
                        VertexConsumer.MAIN, matrices, getSkin().getResource("menu_bar_tex"),
                        getX(), getY(),
                        getWidth(), getHeight(),
                        16f, 0f,
                        16, 16,
                        35, 16
                );

                //if there is a menu open, and we hovered over this button - open this menu instead
                if (parent.currentMenu != null && parent.currentMenu.isOpen() && parent.currentMenu != getPopup())
                    //queue it so we do not mess with the widgets during renderer
                    Client.getInstance().queueTick(() -> openPopup(0, 0));
            }
        }

        @Override
        protected void openPopup(int x, int y) {
            PopupWidget popup = getPopup();
            UIHelper.setPopup(getX(), parent.getAlignedY() + parent.getBarHeight(), popup);
            parent.currentMenu = popup;
            if (popup != null)
                popup.open();
        }
    }

    private static class Spacing extends Widget {

        private final int spacing;

        public Spacing(int height, int spacing, boolean renderSpacer) {
            super(0, 0, 0, height);
            this.spacing = spacing;
            this.setVisible(renderSpacer);
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            if (!isVisible())
                return;

            //render background
            UIHelper.verticalQuad(
                    VertexConsumer.MAIN, matrices, getSkin().getResource("menu_bar_tex"),
                    getX() - spacing / 2f, getY(),
                    3, getHeight(),
                    32f, 0f,
                    3, 16,
                    35, 16
            );
        }
    }
}
