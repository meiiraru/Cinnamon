package cinnamon.world.terrain;

import cinnamon.Client;
import cinnamon.commands.CommandParser;
import cinnamon.commands.CommandSource;
import cinnamon.gui.screens.world.TerminalScreen;
import cinnamon.math.Maths;
import cinnamon.math.collision.Hit;
import cinnamon.messages.MessageCategory;
import cinnamon.messages.MessageManager;
import cinnamon.model.material.Material;
import cinnamon.registry.TerrainModelRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.text.Text;
import cinnamon.world.WorldRules;
import cinnamon.world.entity.Entity;
import cinnamon.world.world.WorldClient;

public class Terminal extends Terrain  {

    protected final TerminalScreen screen;
    private Material variant;

    private String command = "";
    private boolean active = false;
    private boolean sendOutputToChat = false;
    private Text output;

    public Terminal() {
        super(TerrainModelRegistry.TERMINAL.resource, TerrainRegistry.TERMINAL);
        this.screen = new TerminalScreen(this);
    }

    @Override
    public void tick() {
        super.tick();

        if (isActive() && getWorld().getRules().get(WorldRules.ENABLE_TERMINAL) && isCommandValid(getCommandString()))
            processCommand();
    }

    protected void processCommand() {
        String s = getCommandString().trim();
        s = s.startsWith("/") ? s.substring(1) : s;

        WorldClient world = (WorldClient) getWorld();
        CommandSource source = new CommandSource(
                null,
                "@",
                world,
                this.getTransform().getPos(),
                this.getTransform().getRot(),
                Maths.quatToDir(this.getTransform().getRot())
        );
        this.output = CommandParser.runCommand(source, s);

        if (this.output != null) {
            this.screen.addOutputEntry(this.output);

            if (isSendingOutputToChat())
                MessageManager.addMessage(this.output, MessageCategory.SYSTEM, null);
        }
    }

    protected boolean isCommandValid(String cmd) {
        return !cmd.isBlank();
    }

    @Override
    protected void renderModel(Camera camera, Material material, MatrixStack matrices, float delta) {
        super.renderModel(camera, material == null ? variant : material, matrices, delta);
    }

    @Override
    public boolean interact(Entity entity, Hit hit) {
        Client.getInstance().setScreen(screen);
        return true;
    }

    public void setCommandString(String command) {
        this.command = command;
    }

    public String getCommandString() {
        return command;
    }

    public void setSendOutputToChat(boolean sendOutputToChat) {
        this.sendOutputToChat = sendOutputToChat;
    }

    public boolean isSendingOutputToChat() {
        return sendOutputToChat;
    }

    public Text getOutputText() {
        return output;
    }

    public void setActive(boolean active) {
        this.active = active;

        if (model == null) {
            variant = null;
        } else {
            variant = model.getMaterials().get(active ? "terminal_on" : "terminal_off");
        }
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public boolean explode(float explosionStrength) {
        return false;
    }
}
