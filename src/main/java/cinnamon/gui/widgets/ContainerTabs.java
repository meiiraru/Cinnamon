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

    protected final ContainerGrid tabBar = new ContainerGrid(0, 0, 8, 3);
    protected final ContainerGrid buttons = new ContainerGrid(0, 0, 0) {
        @Override
        public GUIListener mouseScroll(double x, double y) {
            if (y != 0 && UIHelper.isMouseOver(this, InputManager.getMouseX(), InputManager.getMouseY())) {
                if (y > 0) prevButton.onRun();
                else       nextButton.onRun();
                return this;
            }
            return super.mouseScroll(x, y);
        }
    };
    protected final Button prevButton, nextButton;
    protected final List<TabButton> tabButtons = new ArrayList<>();
    protected final List<Container> pages = new ArrayList<>();

    protected final int width, maxTabs;

    protected int currentPage = -1;
    protected int tabOffset = 0;

    public ContainerTabs(int x, int y, int width) {
        this(x, y, width, 4);
    }

    public ContainerTabs(int x, int y, int width, int spacing) {
        super(x, y, spacing);
        this.width = width;
        this.maxTabs = Math.max(1, (width - 40 - tabBar.getSpacing() * 2) / 60);
        this.buttons.setColumns(maxTabs);
        this.addWidget(tabBar);

        this.tabBar.setAlignment(Alignment.TOP_CENTER);
        this.tabBar.addWidget(buttons);

        prevButton = new Button(0, 0, 20, 20, Text.of("\u25C2"), b -> rollTabs(false));
        prevButton.setRenderBackground(false);
        prevButton.setActive(false);
        nextButton = new Button(0, 0, 20, 20, Text.of("\u25B8"), b -> rollTabs(true));
        nextButton.setActive(false);
        nextButton.setRenderBackground(false);
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
        UIHelper.nineQuad(VertexConsumer.MAIN, matrices, res, x, y, w, 20, 0f, 0f, 16, 16, 64, 16);
        UIHelper.nineQuad(VertexConsumer.MAIN, matrices, res, x + getWidth() - w, y, w, 20, 0f, 0f, 16, 16, 64, 16);

        //render children
        boolean stencilNeeded = currentPage != -1 && pages.get(currentPage).getHeight() > getHeight() - getTabsYOffset();
        if (stencilNeeded)
            UIHelper.pushStencil(getAlignedX(), getAlignedY(), getWidth(), getHeight());
        super.renderWidgets(matrices, mouseX, mouseY, delta);
        if (stencilNeeded)
            UIHelper.popStencil();
    }

    public void addTab(Text label, Container page) {
        int index = pages.size();
        pages.add(page);

        TabButton button = new TabButton(label, index, this);
        tabButtons.add(button);
        if (index < maxTabs)
            buttons.addWidget(button);
        else if (index == maxTabs) {
            tabBar.insertWidgetBefore(prevButton, buttons);
            tabBar.insertWidgetAfter(nextButton, buttons);
            nextButton.setActive(true);
        }
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

    public void rollTabs(boolean forward) {
        //tried to roll backwards but already at the start
        if (!forward && tabOffset == 0)
            return;

        //tried to roll forwards but already at the end
        int max = tabButtons.size();
        if (forward && tabOffset + maxTabs >= max)
            return;

        //apply offset
        tabOffset += forward ? 1 : -1;

        //enable the buttons
        prevButton.setActive(tabOffset > 0);
        nextButton.setActive(tabOffset + maxTabs < max);

        //rebuild the buttons container
        buttons.clear();
        for (int i = tabOffset; i < Math.min(tabOffset + maxTabs, max); i++)
            buttons.addWidget(tabButtons.get(i));
    }

    public int getTabOffset() {
        return tabOffset;
    }

    public int getMaxTabs() {
        return maxTabs;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTabsYOffset() {
        return 20 + getSpacing();
    }

    protected static class TabButton extends Button {
        protected final int index;
        protected final ContainerTabs parent;

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
