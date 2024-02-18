package mayo.gui.screens;

import mayo.gui.ParentedScreen;
import mayo.gui.Screen;
import mayo.gui.widgets.WidgetList;
import mayo.gui.widgets.types.Button;
import mayo.text.Text;

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
        WidgetList list = new WidgetList(0, 0, 200, 100, 4);

        //dvd screen
        Button dvd = BUTTON_FACTORY.apply("DVD screensaver", button -> client.setScreen(new DVDScreen(this)));
        list.addWidget(dvd);

        //collision screen
        Button coll = BUTTON_FACTORY.apply("Collision Test", button -> client.setScreen(new CollisionScreen(this)));
        list.addWidget(coll);

        //curves screen
        Button curve = BUTTON_FACTORY.apply("Curves", button -> client.setScreen(new CurvesScreen(this)));
        list.addWidget(curve);

        //widgets test
        Button widgetTest = BUTTON_FACTORY.apply("GUI Test", button -> client.setScreen(new WidgetTestScreen(this)));
        list.addWidget(widgetTest);

        //wordle
        Button worlde = BUTTON_FACTORY.apply("Wordle", button -> client.setScreen(new WordleScreen(this)));
        list.addWidget(worlde);

        //balls
        Button balls = BUTTON_FACTORY.apply("Balls", button -> client.setScreen(new BallsScreen(this)));
        list.addWidget(balls);

        //back
        Button back = BUTTON_FACTORY.apply("Back", button -> close());
        list.addWidget(back);

        //add list to screen
        list.setPos((width - list.getWidth()) / 2, (height - list.getHeight()) / 2);
        this.addWidget(list);

        super.init();
    }

    @Override
    protected void addBackButton() {
        //handled by the list above
        //super.addBackButton();
    }
}
