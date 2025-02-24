package cinnamon.gui;

import cinnamon.Client;
import cinnamon.render.Font;
import cinnamon.utils.Colors;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static cinnamon.Client.LOGGER;

public class GUIStyle {

    public static final Resource DEFAULT_STYLE = new Resource("data/gui_styles/default.json");
    private static final Map<Resource, GUIStyle> STYLES_CACHE = new HashMap<>();

    //colors
    public int
            textColor = Colors.WHITE.rgba,
            shadowColor = 0xFF161616,
            backgroundColor = 0x44000000,
            accentColor = Colors.PURPLE.rgba,
            disabledColor = 0xFF666666,
            selectedTextColor = 0xFF000000,
            hintColor = Colors.LIGHT_BLACK.rgba,
            disabledHintColor = 0xFFBDC4CE;

    //offsets
    public int
            pressYOffset = 0,
            shadowOffset = 1,
            boldOffset = 1,
            italicOffset = 3;

    //speeds
    public int
            doubleClickDelay = 10,
            blinkSpeed = 20;

    //text field
    public char
            passwordChar = '\u2022';
    public int
            cursorWidth = 1,
            insertWidth = 4;

    //context menu
    public int
            dividerSize = 5;

    //tooltip
    public int
            tooltipBorder = 4;

    //textures
    public Resource
            buttonTex              = new Resource("textures/gui/widgets/button.png"),
            checkboxTex            = new Resource("textures/gui/widgets/checkbox.png"),
            circularProgressTex    = new Resource("textures/gui/widgets/circular_progress_bar.png"),
            containerBackgroundTex = new Resource("textures/gui/widgets/container_background.png"),
            contextMenuTex         = new Resource("textures/gui/widgets/context_menu.png"),
            labelTex               = new Resource("textures/gui/widgets/label.png"),
            loadingTex             = new Resource("textures/gui/widgets/loading.png"),
            progressbarTex         = new Resource("textures/gui/widgets/progress_bar.png"),
            scrollbarTex           = new Resource("textures/gui/widgets/scrollbar.png"),
            sliderTex              = new Resource("textures/gui/widgets/slider.png"),
            textFieldTex           = new Resource("textures/gui/widgets/text_field.png"),
            toastTex               = new Resource("textures/gui/widgets/toast.png"),
            tooltipTex             = new Resource("textures/gui/widgets/tooltip.png");

    //font
    public Resource
            fontRes = new Resource("fonts/mayonnaise.ttf");
    public int
            fontSize = 8,
            fontLineSpacing = 1;
    public boolean
            fontSmooth = false;

    public Font
            font;


    // -- constructor -- //


    private GUIStyle() {}


    public static void free() {
        STYLES_CACHE.clear();
    }

    public static GUIStyle getDefault() {
        return of(DEFAULT_STYLE);
    }

    public static GUIStyle of(Resource res) {
        GUIStyle style = STYLES_CACHE.get(res);
        if (style != null)
            return style;

        style = createStyle(res);
        style.font = Font.getFont(style.fontRes, style.fontSize, style.fontLineSpacing, style.fontSmooth);

        STYLES_CACHE.put(res, style);
        return style;
    }

    private static GUIStyle createStyle(Resource res) {
        LOGGER.debug("Loading gui style for %s", res);
        GUIStyle style = new GUIStyle();

        try {
            JsonObject json = JsonParser.parseReader(new InputStreamReader(IOUtils.getResource(res))).getAsJsonObject();

            //parent
            Resource parent = json.has("parent") ? new Resource(json.get("parent").getAsString()) : DEFAULT_STYLE;
            GUIStyle p = parent.equals(res) ? style : of(parent);

            //colors
            Parser.COLOR.apply(json, "text_color",          o -> style.textColor         = (int) o, p.textColor);
            Parser.COLOR.apply(json, "shadow_color",        o -> style.shadowColor       = (int) o, p.shadowColor);
            Parser.COLOR.apply(json, "background_color",    o -> style.backgroundColor   = (int) o, p.backgroundColor);
            Parser.COLOR.apply(json, "accent_color",        o -> style.accentColor       = (int) o, p.accentColor);
            Parser.COLOR.apply(json, "disabled_color",      o -> style.disabledColor     = (int) o, p.disabledColor);
            Parser.COLOR.apply(json, "selected_text_color", o -> style.selectedTextColor = (int) o, p.selectedTextColor);
            Parser.COLOR.apply(json, "hint_color",          o -> style.hintColor         = (int) o, p.hintColor);
            Parser.COLOR.apply(json, "disabled_hint_color", o -> style.disabledHintColor = (int) o, p.disabledHintColor);

            //offsets
            Parser.INT.apply(json, "pressed_y_offset", o -> style.pressYOffset = (int) o, p.pressYOffset);
            Parser.INT.apply(json, "shadow_offset",    o -> style.shadowOffset = (int) o, p.shadowOffset);
            Parser.INT.apply(json, "bold_offset",      o -> style.boldOffset   = (int) o, p.boldOffset);
            Parser.INT.apply(json, "italic_offset",    o -> style.italicOffset = (int) o, p.italicOffset);

            //speeds
            Parser.TIME.apply(json, "double_click_delay", o -> style.doubleClickDelay = (int) o, p.doubleClickDelay);
            Parser.TIME.apply(json, "cursor_blink_speed", o -> style.blinkSpeed       = (int) o, p.blinkSpeed);

            //text field
            Parser.CHAR.apply(json, "password_char",       o -> style.passwordChar = (char) o, p.passwordChar);
            Parser.INT.apply(json,  "cursor_width",        o -> style.cursorWidth  =  (int) o, p.cursorWidth);
            Parser.INT.apply(json,  "cursor_insert_width", o -> style.insertWidth  =  (int) o, p.insertWidth);

            //context menu
            Parser.INT.apply(json, "context_menu_divider_size", o -> style.dividerSize = (int) o, p.dividerSize);

            //tooltip
            Parser.INT.apply(json, "tooltip_border", o -> style.tooltipBorder = (int) o, p.tooltipBorder);

            //textures
            Parser.RESOURCE.apply(json, "button_tex",               o -> style.buttonTex              = (Resource) o, p.buttonTex);
            Parser.RESOURCE.apply(json, "checkbox_tex",             o -> style.checkboxTex            = (Resource) o, p.checkboxTex);
            Parser.RESOURCE.apply(json, "circular_progress_tex",    o -> style.circularProgressTex    = (Resource) o, p.circularProgressTex);
            Parser.RESOURCE.apply(json, "container_background_tex", o -> style.containerBackgroundTex = (Resource) o, p.containerBackgroundTex);
            Parser.RESOURCE.apply(json, "context_menu_tex",         o -> style.contextMenuTex         = (Resource) o, p.contextMenuTex);
            Parser.RESOURCE.apply(json, "label_tex",                o -> style.labelTex               = (Resource) o, p.labelTex);
            Parser.RESOURCE.apply(json, "loading_tex",              o -> style.loadingTex             = (Resource) o, p.loadingTex);
            Parser.RESOURCE.apply(json, "progress_bar_tex",         o -> style.progressbarTex         = (Resource) o, p.progressbarTex);
            Parser.RESOURCE.apply(json, "scroll_bar_tex",           o -> style.scrollbarTex           = (Resource) o, p.scrollbarTex);
            Parser.RESOURCE.apply(json, "slider_tex",               o -> style.sliderTex              = (Resource) o, p.sliderTex);
            Parser.RESOURCE.apply(json, "text_field_tex",           o -> style.textFieldTex           = (Resource) o, p.textFieldTex);
            Parser.RESOURCE.apply(json, "toast_tex",                o -> style.toastTex               = (Resource) o, p.toastTex);
            Parser.RESOURCE.apply(json, "tooltip_tex",              o -> style.tooltipTex             = (Resource) o, p.tooltipTex);

            //font
            Parser.RESOURCE.apply(json, "font",              o -> style.fontRes         = (Resource) o, p.fontRes);
            Parser.INT.apply(json,      "font_size",         o -> style.fontSize        =      (int) o, p.fontSize);
            Parser.INT.apply(json,      "font_line_spacing", o -> style.fontLineSpacing =      (int) o, p.fontLineSpacing);
            Parser.BOOLEAN.apply(json,  "font_smooth",       o -> style.fontSmooth      =  (boolean) o, p.fontSmooth);
        } catch (Exception e) {
            LOGGER.error("Failed to load gui style for %s", res, e);
        }

        return style;
    }

    private enum Parser {
        INT(JsonElement::getAsInt),
        COLOR(JsonElement -> Integer.parseUnsignedInt(JsonElement.getAsString(), 16)),
        STRING(JsonElement::getAsString),
        FLOAT(JsonElement::getAsFloat),
        TIME((JsonElement e) -> (int) ((e.getAsInt() / 1000f) * Client.TPS)),
        CHAR(JsonElement -> JsonElement.getAsString().charAt(0)),
        RESOURCE(JsonElement -> new Resource(JsonElement.getAsString())),
        BOOLEAN(JsonElement::getAsBoolean);

        private final Function<JsonElement, Object> func;

        Parser(Function<JsonElement, Object> func) {
            this.func = func;
        }

        public void apply(JsonObject json, String key, Consumer<Object> consumer, Object defaultValue) {
            JsonElement element = json.get(key);
            consumer.accept(element != null ? func.apply(element) : defaultValue);
        }
    }
}
