package cinnamon.gui;

import cinnamon.gui.widgets.types.Button;
import cinnamon.text.Text;

public abstract class ParentedScreen extends Screen {

    private final Screen parentScreen;

    public ParentedScreen(Screen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void init() {
        super.init();
        addBackButton();
    }

    protected void addBackButton() {
        this.addWidget(new Button(width - 60 - 4, height - 20 - 4, 60, 20, Text.translated("gui.back"), button -> close()));
    }

    @Override
    public boolean closeOnEsc() {
        return true;
    }

    @Override
    public void close() {
        client.setScreen(parentScreen);
    }
}
