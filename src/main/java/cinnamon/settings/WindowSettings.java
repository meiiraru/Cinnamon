package cinnamon.settings;

import cinnamon.gui.Screen;
import cinnamon.gui.screens.MainMenu;
import cinnamon.utils.Resource;

import java.util.function.Supplier;

public class WindowSettings {

    //window
    public int defaultWidth = 854, defaultHeight = 480;
    public String title = "Cinnamon";
    public Resource icon = new Resource("textures/icon.png");

    //screen
    public Supplier<Screen> mainScreen = MainMenu::new;
}
