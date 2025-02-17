package cinnamon.gui.screens;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.screens.extras.*;
import cinnamon.gui.widgets.WidgetList;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.Label;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.world.world.DiscoWorld;
import cinnamon.world.world.MaterialPreviewWorld;
import cinnamon.world.world.RollerCoasterWorld;

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
        WidgetList list = new WidgetList(width / 2, height / 2, width - 8, height - 8, 4);
        list.setAlignment(Alignment.CENTER);
        addWidget(list);

        //title
        list.addWidget(new Label(0, 0, Text.of("Custom Worlds\n")));

        //material debug world
        list.addWidget(BUTTON_FACTORY.apply("Material Debug", button -> new MaterialPreviewWorld().init()));

        //roller coaster world
        list.addWidget(BUTTON_FACTORY.apply("Roller Coaster", button -> new RollerCoasterWorld().init()));

        //disco world
        list.addWidget(BUTTON_FACTORY.apply("Disco", button -> new DiscoWorld().init()));

        //title
        list.addWidget(new Label(0, 0, Text.of("\nTech demos\n")));

        //collision screen
        list.addWidget(BUTTON_FACTORY.apply("Collision Test", button -> client.setScreen(new CollisionScreen(this))));

        //curves screen
        list.addWidget(BUTTON_FACTORY.apply("Curves", button -> client.setScreen(new CurvesScreen(this))));

        //widgets test
        list.addWidget(BUTTON_FACTORY.apply("GUI Test", button -> client.setScreen(new WidgetTestScreen(this))));

        //balls
        list.addWidget(BUTTON_FACTORY.apply("Balls", button -> client.setScreen(new BallsScreen(this))));

        //easings
        list.addWidget(BUTTON_FACTORY.apply("Easings", button -> client.setScreen(new EasingScreen(this))));

        //sound visualizer
        list.addWidget(BUTTON_FACTORY.apply("Sound Visualizer", button -> client.setScreen(new SoundVisualizerScreen(this))));

        //model viewer
        list.addWidget(BUTTON_FACTORY.apply("Model Viewer", button -> client.setScreen(new ModelViewerScreen(this))));

        //title
        list.addWidget(new Label(0, 0, Text.of("\nGames\n")));

        //dvd screen
        list.addWidget(BUTTON_FACTORY.apply("DVD screensaver", button -> client.setScreen(new DVDScreen(this))));

        //wordle
        list.addWidget(BUTTON_FACTORY.apply("Wordle", button -> client.setScreen(new WordleScreen(this))));

        //title
        list.addWidget(new Label(0, 0, Text.of("\nOther\n")));

        //CPF validator
        list.addWidget(BUTTON_FACTORY.apply("CPF Validator", button -> client.setScreen(new CPFScreen(this))));

        //spacing (literally)
        list.addWidget(new Label(0, 0, Text.of("\n")));

        //back
        list.addWidget(BUTTON_FACTORY.apply("Back", button -> close()));

        super.init();
    }

    @Override
    protected void addBackButton() {
        //handled by the list above
        //super.addBackButton();
    }
}
