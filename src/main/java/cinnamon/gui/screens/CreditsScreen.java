package cinnamon.gui.screens;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.WidgetList;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.Label;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;

import static cinnamon.Client.LOGGER;

public class CreditsScreen extends ParentedScreen {

    private static final Resource CREDITS_JSON = new Resource("data/credits.json");
    private static final Style BASE = Style.EMPTY.shadow(true);
    private static final Style TITLE = BASE.outlined(true).bold(true);
    private static final Label EMPTY = new Label(0, 0, Text.empty());

    public CreditsScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void init() {
        try {
            this.readJson();
        } catch (Exception e) {
            LOGGER.error("Failed to load credits file", e);
        }
        super.init();
    }

    @Override
    protected void addBackButton() {
        //close button
        Button closeButton = new Button(width - 4 - 16, 4, 16, 16, null, button -> close());
        closeButton.setIcon(new Resource("textures/gui/icons/close.png"));
        closeButton.setTooltip(Text.translated("gui.close"));
        addWidget(closeButton);
    }

    private void readJson() throws Exception {
        if (!IOUtils.hasResource(CREDITS_JSON))
            return;

        JsonObject json;
        try (InputStream stream = IOUtils.getResource(CREDITS_JSON); InputStreamReader reader = new InputStreamReader(stream)) {
            json = JsonParser.parseReader(reader).getAsJsonObject();
        }

        if (!json.has("sections"))
            return;

        WidgetList list = new WidgetList(width / 2, 20, (int) (width * 0.9f), height - 40, 4);
        addWidget(list);
        boolean first = true;

        for (JsonElement element : json.getAsJsonArray("sections")) {
            //add empty label for spacing
            if (!first) {
                list.addWidget(EMPTY);
                list.addWidget(EMPTY);
            }

            //try to parse as a simple first
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                String line = element.getAsString();
                list.addWidget(new Label(0, 0, Text.of(line).withStyle(BASE)));
                continue;
            }

            //parse as an object
            JsonObject section = element.getAsJsonObject();
            String title = section.get("title").getAsString();
            JsonArray content = section.getAsJsonArray("content");

            //add label for title
            list.addWidget(new Label(0, 0, Text.of(title).withStyle(TITLE)));

            //add sub-list for content
            ContainerGrid grid = new ContainerGrid(0, 0, 4);
            grid.setAlignment(Alignment.TOP_CENTER);
            list.addWidget(grid);

            for (JsonElement contentElement : content) {
                String line = contentElement.getAsString();
                grid.addWidget(new Label(0, 0, Text.of(line).withStyle(BASE)));
            }

            first = false;
        }
    }
}
