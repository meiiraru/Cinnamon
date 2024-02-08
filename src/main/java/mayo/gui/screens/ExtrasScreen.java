package mayo.gui.screens;

import mayo.gui.ParentedScreen;
import mayo.gui.Screen;
import mayo.gui.widgets.ContainerList;
import mayo.gui.widgets.types.Button;
import mayo.text.Text;

public class ExtrasScreen extends ParentedScreen {

    public ExtrasScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void init() {
        //buttons
        ContainerList list = new ContainerList(0, 0, 4);

        //dvd screen
        Button dvd = new Button(0, 0, 180, 20, Text.of("DVD screensaver"), button -> client.setScreen(new DVDScreen(this)));
        list.addWidget(dvd);

        //collision screen
        Button coll = new Button(0, 0, 180, 20, Text.of("Collision Test"), button -> client.setScreen(new CollisionScreen(this)));
        list.addWidget(coll);

        //curves screen
        Button curve = new Button(0, 0, 180, 20, Text.of("Curves"), button -> client.setScreen(new CurvesScreen(this)));
        list.addWidget(curve);

        //back
        Button back = new Button(0, 0, 180, 20, Text.of("Back"), button -> close());
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
