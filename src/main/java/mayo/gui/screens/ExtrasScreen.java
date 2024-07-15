package mayo.gui.screens;

import mayo.gui.ParentedScreen;
import mayo.gui.Screen;
import mayo.gui.screens.extras.*;
import mayo.gui.widgets.WidgetList;
import mayo.gui.widgets.types.Button;
import mayo.gui.widgets.types.Label;
import mayo.text.Text;
import mayo.world.MaterialPreviewWorld;

import java.util.function.BiFunction;
import java.util.function.Consumer;

public class ExtrasScreen extends ParentedScreen {

    private static final BiFunction<String, Consumer<Button>, Button> BUTTON_FACTORY = (s, a) -> new Button(0, 0, 180, 20, Text.of(s), a);

    public ExtrasScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void init() {
        //buttons
        WidgetList list = new WidgetList(0, 0, 0, 0, 4);
        list.setShouldRenderBackground(false);

        //title
        list.addWidget(new Label(0, 0, Text.of("Tech demos\n"), font));

        //material debug world
        list.addWidget(BUTTON_FACTORY.apply("Material Debug", button -> new MaterialPreviewWorld().init()));

        //collision screen
        list.addWidget(BUTTON_FACTORY.apply("Collision Test", button -> client.setScreen(new CollisionScreen(this))));

        //curves screen
        list.addWidget(BUTTON_FACTORY.apply("Curves", button -> client.setScreen(new CurvesScreen(this))));

        //widgets test
        list.addWidget(BUTTON_FACTORY.apply("GUI Test", button -> client.setScreen(new WidgetTestScreen(this))));

        //balls
        list.addWidget(BUTTON_FACTORY.apply("Balls", button -> client.setScreen(new BallsScreen(this))));

        //title
        list.addWidget(new Label(0, 0, Text.of("\nGames\n"), font));

        //dvd screen
        list.addWidget(BUTTON_FACTORY.apply("DVD screensaver", button -> client.setScreen(new DVDScreen(this))));

        //wordle
        list.addWidget(BUTTON_FACTORY.apply("Wordle", button -> client.setScreen(new WordleScreen(this))));

        //title
        list.addWidget(new Label(0, 0, Text.of("\nOther\n"), font));

        //CPF validator
        list.addWidget(BUTTON_FACTORY.apply("CPF Validator", button -> client.setScreen(new CPFScreen(this))));

        //spacing (literally)
        list.addWidget(new Label(0, 0, Text.of("\n"), font));

        //back
        list.addWidget(BUTTON_FACTORY.apply("Back", button -> close()));

        //add list to screen
        list.setDimensions(width - 8, Math.min(list.getWidgetsHeight(), height - 8));
        list.setPos(width / 2, (height - list.getHeight()) / 2);
        this.addWidget(list);

        super.init();
    }

    @Override
    protected void addBackButton() {
        //handled by the list above
        //super.addBackButton();
    }
}
