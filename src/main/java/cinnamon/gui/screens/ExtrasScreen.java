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
import cinnamon.world.world.PrimitiveTestWorld;
import cinnamon.world.world.RollerCoasterWorld;
import cinnamon.world.world.TransparentWorld;

import java.util.function.BiFunction;
import java.util.function.Consumer;

public class ExtrasScreen extends ParentedScreen {

    private static final BiFunction<String, Consumer<Button>, Button> BUTTON_FACTORY = (s, a) -> new Button(0, 0, 180, 20, Text.translated(s), a);

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
        list.addWidget(new Label(0, 0, Text.translated("gui.extras_screen.custom_worlds").append("\n")));

        //material debug world
        list.addWidget(BUTTON_FACTORY.apply("gui.extras_screen.custom_worlds.material_test", button -> new MaterialPreviewWorld().init()));

        //roller coaster world
        list.addWidget(BUTTON_FACTORY.apply("gui.extras_screen.custom_worlds.rollercoaster", button -> new RollerCoasterWorld().init()));

        //disco world
        list.addWidget(BUTTON_FACTORY.apply("gui.extras_screen.custom_worlds.disco", button -> new DiscoWorld().init()));

        //transparent world
        list.addWidget(BUTTON_FACTORY.apply("gui.extras_screen.custom_worlds.transparent", button -> new TransparentWorld().init()));

        //primitive debug world
        list.addWidget(BUTTON_FACTORY.apply("gui.extras_screen.custom_worlds.primitive_test", button -> new PrimitiveTestWorld().init()));

        //title
        list.addWidget(new Label(0, 0, Text.of("\n").appendTranslated("gui.extras_screen.tech_demos").append("\n")));

        //collision screen
        list.addWidget(BUTTON_FACTORY.apply("gui.extras_screen.tech_demos.collision_test", button -> client.setScreen(new CollisionScreen(this))));

        //new collision screen
        list.addWidget(BUTTON_FACTORY.apply("gui.extras_screen.tech_demos.new_collision_test", button -> client.setScreen(new NewCollisionScreen(this))));

        //curves screen
        list.addWidget(BUTTON_FACTORY.apply("gui.extras_screen.tech_demos.curves", button -> client.setScreen(new CurvesScreen(this))));

        //widgets test
        list.addWidget(BUTTON_FACTORY.apply("gui.extras_screen.tech_demos.gui_test", button -> client.setScreen(new WidgetTestScreen(this))));

        //balls
        list.addWidget(BUTTON_FACTORY.apply("gui.extras_screen.tech_demos.balls", button -> client.setScreen(new BallsScreen(this))));

        //easings
        list.addWidget(BUTTON_FACTORY.apply("gui.extras_screen.tech_demos.easings", button -> client.setScreen(new EasingScreen(this))));

        //sound visualizer
        list.addWidget(BUTTON_FACTORY.apply("gui.extras_screen.tech_demos.sound_visualizer", button -> client.setScreen(new SoundVisualizerScreen(this))));

        //model viewer
        list.addWidget(BUTTON_FACTORY.apply("gui.extras_screen.tech_demos.model_viewer", button -> client.setScreen(new ModelViewerScreen(this))));

        //panorama viewer
        list.addWidget(BUTTON_FACTORY.apply("gui.extras_screen.tech_demos.panorama_viewer", button -> client.setScreen(new PanoramaScreen(this))));

        //vec sum screen
        list.addWidget(BUTTON_FACTORY.apply("gui.extras_screen.tech_demos.vec_sum", button -> client.setScreen(new VecScreen(this))));

        //midi player
        list.addWidget(BUTTON_FACTORY.apply("gui.extras_screen.tech_demos.midi_player", button -> client.setScreen(new MIDIScreen(this))));

        //title
        list.addWidget(new Label(0, 0, Text.of("\n").appendTranslated("gui.extras_screen.games").append("\n")));

        //dvd screen
        list.addWidget(BUTTON_FACTORY.apply("gui.extras_screen.games.dvd_screensaver", button -> client.setScreen(new DVDScreen(this))));

        //wordle
        list.addWidget(BUTTON_FACTORY.apply("gui.extras_screen.games.wordle", button -> client.setScreen(new WordleScreen(this))));

        //title
        list.addWidget(new Label(0, 0, Text.of("\n").appendTranslated("gui.extras_screen.other").append("\n")));

        //CPF validator
        list.addWidget(BUTTON_FACTORY.apply("gui.extras_screen.other.hdr_fix", button -> client.setScreen(new HDRFixScreen(this))));

        //CPF validator
        list.addWidget(BUTTON_FACTORY.apply("gui.extras_screen.other.cpf_validator", button -> client.setScreen(new CPFScreen(this))));

        //spacing (literally)
        list.addWidget(new Label(0, 0, Text.of("\n")));

        //back
        list.addWidget(BUTTON_FACTORY.apply("gui.back", button -> close()));

        super.init();
    }

    @Override
    protected void addBackButton() {
        //handled by the list above
        //super.addBackButton();
    }
}
