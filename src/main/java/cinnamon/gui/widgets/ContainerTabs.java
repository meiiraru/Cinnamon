package cinnamon.gui.widgets;

import cinnamon.gui.widgets.types.Button;
import cinnamon.input.InputManager;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Resource;
import cinnamon.utils.UIHelper;
import org.joml.Math;

import java.util.ArrayList;
import java.util.List;

public class ContainerTabs extends ContainerGrid {

    private final ContainerGrid buttons = new ContainerGrid(0, 0, 0) {
        @Override
        public GUIListener mouseScroll(double x, double y) {
            if (y != 0 && UIHelper.isMouseOver(this, InputManager.getMouseX(), InputManager.getMouseY()) && rollTabs(y < 0))
                return this;
            return super.mouseScroll(x, y);
        }
    };
    private final List<Container> pages = new ArrayList<>();

    private final int width;

    protected int currentPage = -1;

    public ContainerTabs(int x, int y, int width) {
        this(x, y, width, 4);
    }

    public ContainerTabs(int x, int y, int width, int spacing) {
        super(x, y, spacing);
        this.width = width;
        this.addWidget(buttons);
        this.setBackground(true);
    }

    @Override
    protected void updateDimensions(int width, int height) {
        super.updateDimensions(this.width, height);
    }

    @Override
    protected void renderWidgets(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        //render lines
        Resource res = getSkin().getResource("selection_panel_tex");
        int x = getAlignedX();
        int y = getAlignedY();
        float w = Math.ceil((getWidth() - buttons.getWidth()) / 2f);
        UIHelper.horizontalQuad(VertexConsumer.MAIN, matrices, res, x, y, w, 20, 0f, 0f, 16, 16, 64, 16);
        UIHelper.horizontalQuad(VertexConsumer.MAIN, matrices, res, x + getWidth() - w, y, w, 20, 0f, 0f, 16, 16, 64, 16);

        //render children
        UIHelper.pushStencil(matrices, getAlignedX(), getAlignedY(), getWidth(), getHeight());
        super.renderWidgets(matrices, mouseX, mouseY, delta);
        UIHelper.popStencil();
    }

    public void addTab(Text label, Container page) {
        int index = pages.size();
        pages.add(page);

        TabButton button = new TabButton(label, index, this);
        buttons.addWidget(button);
        buttons.setColumns(index + 1);
    }

    public void clearTabs() {
        buttons.clear();
        buttons.setColumns(1);
        pages.clear();
        this.updateDimensions();
    }

    public void setPage(int index) {
        if (index < 0 || index >= pages.size())
            return;

        if (currentPage != -1)
            removeWidget(pages.get(currentPage));

        currentPage = index;
        addWidget(pages.get(currentPage));
    }

    public int getCurrentPageIndex() {
        return currentPage;
    }

    public List<Container> getPages() {
        return pages;
    }

    public boolean rollTabs(boolean forward) {
        return false;
    }

    protected static class TabButton extends Button {
        private final int index;
        private final ContainerTabs parent;

        public TabButton(Text message, int index, ContainerTabs parent) {
            super(0, 0, 60, 20, message, b -> parent.setPage(index));
            this.index = index;
            this.parent = parent;
        }

        @Override
        protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            UIHelper.nineQuad(
                    VertexConsumer.MAIN, matrices, getSkin().getResource("selection_panel_tex"),
                    getX(), getY(),
                    getWidth(), getHeight(),
                    (isSelected() ? 3 : isHoveredOrFocused() ? 2 : 1) * 16f, 0f,
                    16, 16,
                    64, 16
            );
        }

        @Override
        protected void renderText(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            Text text = getFormattedMessage();
            int x = getCenterX();
            int y = getCenterY() + (!isSelected() ? getSkin().getInt("pressed_y_offset") : 0);
            text.render(VertexConsumer.MAIN, matrices, x, y, Alignment.CENTER);
        }

        @Override
        public void onRun() {
            if (!isSelected())
                super.onRun();
        }

        public boolean isSelected() {
            return parent.getCurrentPageIndex() == index;
        }
    }
}
