package cinnamon.gui;

import cinnamon.Client;
import cinnamon.utils.Colors;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;

import static cinnamon.Client.LOGGER;

public class GUIStyle {

    private static final Resource RESOURCE = new Resource("data/gui/settings.json");

    //colors
    public static int
            textColor = Colors.WHITE.rgba,
            shadowColor = 0xFF161616,
            backgroundColor = 0x44000000,
            accentColor = Colors.PURPLE.rgba,
            selectedTextColor = 0xFF000000,
            hintColor = Colors.LIGHT_BLACK.rgba,
            mainMenuTextColor = 0xFFD3AB7A;

    //offsets
    public static int
            pressYOffset = 0,
            shadowOffset = 1,
            boldOffset = 1,
            italicOffset = 3;
    public static float
            depthOffset = 0.01f;

    //speeds
    public static int
            doubleClickDelay = 10,
            blinkSpeed = 20;

    //text field
    public static char
            passwordChar = '\u2022';
    public static int
            cursorWidth = 1,
            insertWidth = 4;

    //context menu
    public static int
            dividerSize = 5;

    //tooltip
    public static int
            tooltipBorder = 4;

    public static void init() {
        LOGGER.info("Applying gui style settings");
        JsonObject json = JsonParser.parseReader(new InputStreamReader(IOUtils.getResource(RESOURCE))).getAsJsonObject();
        try {
            textColor = Integer.parseUnsignedInt(json.get("text_color").getAsString(), 16);
            shadowColor = Integer.parseUnsignedInt(json.get("shadow_color").getAsString(), 16);
            backgroundColor = Integer.parseUnsignedInt(json.get("background_color").getAsString(), 16);
            accentColor = Integer.parseUnsignedInt(json.get("accent_color").getAsString(), 16);
            selectedTextColor = Integer.parseUnsignedInt(json.get("selected_text_color").getAsString(), 16);
            hintColor = Integer.parseUnsignedInt(json.get("hint_color").getAsString(), 16);
            mainMenuTextColor = Integer.parseUnsignedInt(json.get("main_menu_text_color").getAsString(), 16);

            pressYOffset = json.get("pressed_y_offset").getAsInt();
            shadowOffset = json.get("shadow_offset").getAsInt();
            boldOffset = json.get("bold_offset").getAsInt();
            italicOffset = json.get("italic_offset").getAsInt();
            depthOffset = json.get("depth_offset").getAsFloat();

            doubleClickDelay = (int) ((json.get("double_click_delay").getAsInt() / 1000f) * Client.TPS);
            blinkSpeed = (int) ((json.get("cursor_blink_speed").getAsInt() / 1000f) * Client.TPS);

            passwordChar = json.get("password_char").getAsString().charAt(0);
            cursorWidth = json.get("cursor_width").getAsInt();
            insertWidth = json.get("cursor_insert_width").getAsInt();

            dividerSize = json.get("context_menu_divider_size").getAsInt();

            tooltipBorder = json.get("tooltip_border").getAsInt();
        } catch (Exception e) {
            LOGGER.error("Failed to load GUI style settings", e);
        }
    }
}
