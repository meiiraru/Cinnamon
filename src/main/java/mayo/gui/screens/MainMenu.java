package mayo.gui.screens;

import mayo.Client;
import mayo.gui.Screen;
import mayo.gui.Toast;
import mayo.gui.widgets.ContainerList;
import mayo.gui.widgets.types.Button;
import mayo.gui.widgets.types.ContextMenu;
import mayo.gui.widgets.types.Label;
import mayo.model.GeometryHelper;
import mayo.networking.ServerConnection;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.batch.VertexConsumer;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.*;
import mayo.world.WorldClient;

import java.util.function.Consumer;

public class MainMenu extends Screen {

    @Override
    public void init() {
        super.init();

        //may~o
        Text may = Text.of("May~o v%s \u25E0\u25DE\u25DF\u25E0".formatted(Client.VERSION)).withStyle(Style.EMPTY.italic(true).color(0x66FFFFFF).shadow(true).shadowColor(0x66161616));
        this.addWidget(new Label(may, font, width - TextUtils.getWidth(may, font) - 4, height - TextUtils.getHeight(may, font) - 4));

        //buttons
        ContainerList list = new ContainerList(0, 0, 4);

        //open world
        Button worldButton = new MainButton(Text.of("Singleplayer").withStyle(Style.EMPTY.color(Colors.YELLOW)), button -> {
            //init client
            if (ServerConnection.open()) {
                WorldClient world = new WorldClient();
                world.init();
            } else {
                Toast.addToast(Text.of("Unable to create the internal server"), client.font);
            }
        });
        list.addWidget(worldButton);

        //multiplayer
        Button joinWorld = new MainButton(Text.of("Multiplayer"), button -> client.setScreen(new MultiplayerJoinScreen(this)));
        joinWorld.setTooltip(Text.of("Sorry, not available yet! ").append(Text.of("\u2764").withStyle(Style.EMPTY.color(Colors.PINK))));
        joinWorld.setActive(false);
        list.addWidget(joinWorld);

        //extra stuff
        Button extras = new MainButton(Text.of("Extras"), button -> client.setScreen(new ExtrasScreen(this)));
        list.addWidget(extras);

        ContextMenu ctx = new ContextMenu()
                .addAction(Text.of("Meow 1"), Text.of("Meow 1"), button -> System.out.println("1"))
                .addDivider()
                .addAction(Text.of("Meow 2"), Text.of("Meow 2"), button -> System.out.println("2"))
                .addDivider()
                .addAction(Text.of("Meow 3"), Text.of("Meow 3"), button -> System.out.println("3"));

        ContextMenu ctx2 = new ContextMenu()
                .addAction(Text.of("A"), Text.of("A"), button -> System.out.println("a"))
                .addAction(Text.of("B"), Text.of("B"), button -> System.out.println("b"))
                .addAction(Text.of("C"), Text.of("C"), button -> System.out.println("c"));

        ContextMenu ctx3 = new ContextMenu()
                .addAction(Text.of("1"), Text.of("1"), button -> System.out.println("1"))
                .addAction(Text.of("11"), Text.of("11"), button -> System.out.println("11"))
                .addAction(Text.of("111"), Text.of("111"), button -> System.out.println("111"));

        ctx2.addDivider().addSubMenu(Text.of("||"), ctx3);
        ctx.addDivider().addSubMenu(Text.of("Purr~"), ctx2);

        extras.setContextMenu(ctx);

        //exit
        Button exitButton = new MainButton(Text.of("Exit"), button -> client.window.exit());
        exitButton.setTooltip(Text.of("bye~"));
        list.addWidget(exitButton);

        //add list to screen
        list.setPos(12, (height - list.getHeight()) / 2);
        this.addWidget(list);
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta) {
        GeometryHelper.rectangle(VertexConsumer.GUI, matrices, 0, 0, width, height, -999, 0xFF29224B, 0xFF2E2557, 0xFF553C89, 0xFFDBA8DC);
    }

    private static class MainButton extends Button {

        protected static final Texture
                STAR = Texture.of(new Resource("textures/gui/widgets/main_menu/star.png")),
                LINE = Texture.of(new Resource("textures/gui/widgets/main_menu/line.png"));

        private float hoverX = -40f;

        public MainButton(Text message, Consumer<Button> action) {
            super(0, 0, 148, 20, message, action);
            message.withStyle(Style.EMPTY.shadow(true));
        }

        @Override
        public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            matrices.push();

            float d = UIHelper.tickDelta(0.6f);
            hoverX = Maths.lerp(hoverX, isHoveredOrFocused() ? 0 : -20, d);
            matrices.translate(hoverX, 0, 0);

            super.renderWidget(matrices, mouseX, mouseY, delta);

            matrices.pop();
        }

        @Override
        protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            if (!this.isHoveredOrFocused())
                return;

            VertexConsumer.GUI.consume(GeometryHelper.quad(
                    matrices, getCenterX() - 64, getY(),
                    128, 32
            ), LINE.getID());
        }
    }
}
