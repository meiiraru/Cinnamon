package mayo.gui.screens;

import mayo.gui.ParentedScreen;
import mayo.gui.Screen;
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

        //spacing (literally)
        list.addWidget(new Label(0, 0, Text.of("Tech demos\n"), font));

        Button materialDebug = BUTTON_FACTORY.apply("Material Debug", button -> new MaterialPreviewWorld().init());
        list.addWidget(materialDebug);

        //collision screen
        Button coll = BUTTON_FACTORY.apply("Collision Test", button -> client.setScreen(new CollisionScreen(this)));
        list.addWidget(coll);

        //curves screen
        Button curve = BUTTON_FACTORY.apply("Curves", button -> client.setScreen(new CurvesScreen(this)));
        list.addWidget(curve);

        //widgets test
        Button widgetTest = BUTTON_FACTORY.apply("GUI Test", button -> client.setScreen(new WidgetTestScreen(this)));
        list.addWidget(widgetTest);

        //balls
        Button balls = BUTTON_FACTORY.apply("Balls", button -> client.setScreen(new BallsScreen(this)));
        list.addWidget(balls);

        //spacing (literally)
        list.addWidget(new Label(0, 0, Text.of("\nGames\n"), font));

        //dvd screen
        Button dvd = BUTTON_FACTORY.apply("DVD screensaver", button -> client.setScreen(new DVDScreen(this)));
        list.addWidget(dvd);

        //wordle
        Button worlde = BUTTON_FACTORY.apply("Wordle", button -> client.setScreen(new WordleScreen(this)));
        list.addWidget(worlde);

        //spacing (literally)
        list.addWidget(new Label(0, 0, Text.of("\n"), font));

        //back
        Button back = BUTTON_FACTORY.apply("Back", button -> close());
        list.addWidget(back);

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
