package cinnamon.gui.screens.extras;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.Toast;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.Widget;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.Label;
import cinnamon.gui.widgets.types.ProgressBar;
import cinnamon.gui.widgets.types.TextField;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Colors;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import cinnamon.world.Hud;
import org.joml.Math;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static cinnamon.Client.LOGGER;
import static org.lwjgl.glfw.GLFW.*;

public class WordleScreen extends ParentedScreen {

    private static final Resource
            ANSWER_WORDS_PATH = new Resource("data/wordle/wordle_answers.moon"),
            GUESS_WORDS_PATH = new Resource("data/wordle/wordle_guesses.moon");
    private static final int
            TRIES = 6,
            WORD_LENGTH = 5;
    private static final Colors[] COLORS = {
            Colors.DARK_GRAY,   //0
            Colors.BLACK,       //1
            Colors.LIGHT_BLACK, //2
            Colors.YELLOW,      //3
            Colors.GREEN,       //4
            Colors.RED,         //5
            Colors.WHITE        //6
    };
    private static final Path SAVE_FILE = IOUtils.ROOT_FOLDER.resolve("wordle.moon");


    //word list
    private final List<String>
            answers = new ArrayList<>(),
            guesses = new ArrayList<>();


    //current game
    private final String[] attempts = new String[TRIES];
    private int tries = 0;
    private String word;
    private String attempt = "";
    private boolean gameOver;


    //game stats
    private final int[] results = new int[TRIES + 1];
    private int playCount;
    private String lastWord;


    //widgets
    private final Letter[][] letters = new Letter[TRIES][WORD_LENGTH];
    private Field field;
    private Stats stats;
    private Keyboard keyboard;
    private KeyboardControls controls;

    public WordleScreen(Screen parentScreen) {
        super(parentScreen);

        BiConsumer<byte[], List<String>> func = (bytes, target) -> {
            String string = new String(bytes, StandardCharsets.UTF_8);
            String[] lines = string.split("\n");

            for (int i = 0; i < lines.length; i++) {
                String word = lines[i].trim().replaceAll(" ", "");
                if (word.startsWith("#"))
                    continue;

                if (word.length() != WORD_LENGTH) {
                    LOGGER.warn("Ignoring wordle word \"%s\" at line %s", word, i + 1);
                    continue;
                }

                target.add(word.toUpperCase());
            }
        };

        //load words
        func.accept(IOUtils.readCompressed(ANSWER_WORDS_PATH), answers);
        func.accept(IOUtils.readCompressed(GUESS_WORDS_PATH), guesses);

        //stats
        playCount = 0;
        Arrays.fill(results, 0);

        resetGame();

        //load save
        loadGame();
    }

    @Override
    public void init() {
        //dummy text field
        field = new Field(width, height, this::onTextUpdate);
        addWidget(field);

        //letters
        ContainerGrid grid = new ContainerGrid(0, 0, 4);

        for (int i = 0; i < TRIES; i++) {
            ContainerGrid word = new ContainerGrid(0, 0, 4, WORD_LENGTH);

            for (int j = 0; j < WORD_LENGTH; j++) {
                Letter l = new Letter();
                word.addWidget(l);
                letters[i][j] = l;
            }

            grid.addWidget(word);
        }

        grid.setPos((width - grid.getWidth()) / 2, (height - grid.getHeight()) / 2);
        addWidget(grid);

        //stats
        stats = new Stats(0, 0, 4, TRIES);
        stats.setAlignment(Alignment.TOP_CENTER);
        stats.setPos(grid.getX() / 2, (height - stats.getHeight()) / 2);
        addWidget(stats);

        //keyboard
        ContainerGrid keyboardGrid = new ContainerGrid(0, 0, 2);
        keyboardGrid.setAlignment(Alignment.TOP_CENTER);

        int gridXWidth = grid.getX() + grid.getWidth();
        keyboard = new Keyboard(0, 0, 2, c -> field.setString(field.getString() + c));
        keyboardGrid.addWidget(keyboard);

        controls = new KeyboardControls(0, 0, 2, keyboard.getWidth(), c -> {
            String s = field.getString();
            if (!s.isEmpty())
                field.setString(s.substring(0, s.length() - 1));
        }, c -> testWord());
        keyboardGrid.addWidgetOnTop(controls);

        keyboardGrid.setPos((width - gridXWidth) / 2 + gridXWidth, (height - keyboardGrid.getHeight()) / 2);
        addWidget(keyboardGrid);

        super.init();
    }

    @Override
    protected void addBackButton() {
        Button b = new Button(width - 60 - 4, height - 20 - 4, 60, 20, Text.translated("gui.back"), button -> close());
        b.setStyle(Hud.HUD_STYLE);
        this.addWidget(b);
    }

    @Override
    public void rebuild() {
        super.rebuild();

        stats.update(lastWord, playCount, results);

        for (int i = 0; i < TRIES; i++)
            processWord(i);

        if (!gameOver) {
            updateSelected(tries);
            onTextUpdate(attempt);
            field.setString(attempt);
        }

        controls.showReset(gameOver);
    }

    @Override
    public void removed() {
        super.removed();
        saveGame();
    }

    private void resetGame() {
        word = answers.get((int) (Math.random() * answers.size()));
        //word = "BOOBS";
        attempt = "";
        tries = 0;
        gameOver = false;
        Arrays.fill(attempts, null);
    }

    private void resetWidgets() {
        for (int i = 0; i < TRIES; i++)
            processWord(i);
        updateSelected(tries);
        field.setString("");
        keyboard.reset();
        controls.showReset(false);
    }

    private void processWord(int index) {
        String word = attempts[index];
        for (int i = 0; i < WORD_LENGTH; i++) {
            //no word
            if (word == null) {
                letters[index][i].setChar(null);
                letters[index][i].setColor(COLORS[2]);
                continue;
            }

            //grab character and apply it
            char c = word.charAt(i);
            letters[index][i].setChar(c);

            //correct character
            if (this.word.charAt(i) == c) {
                letters[index][i].setColor(COLORS[4]);
                keyboard.setColor(c, 4);
                continue;
            }

            //if character exists
            int count = WORD_LENGTH - this.word.replace(String.valueOf(c), "").length();
            if (count > 0) {
                //character is right somewhere else
                for (int j = 0; j < WORD_LENGTH; j++) {
                    if (word.charAt(j) == c && this.word.charAt(j) == c)
                        count--;
                }

                if (count > 0) {
                    //character exists but at wrong position somewhere else
                    for (int j = 0; j < i; j++) {
                        if (word.charAt(j) == c)
                            count--;
                    }

                    if (count > 0) {
                        //exist character
                        letters[index][i].setColor(COLORS[3]);
                        keyboard.setColor(c, 3);
                        continue;
                    }
                }
            }

            //wrong character
            letters[index][i].setColor(COLORS[0]);
            keyboard.setColor(c, 2);
        }
    }

    private void updateSelected(int index) {
        for (int i = 0; i < WORD_LENGTH; i++)
            letters[index][i].setColor(COLORS[1]);
    }

    private void onTextUpdate(String text)  {
        this.attempt = text;
        for (int i = 0; i < WORD_LENGTH; i++)
            letters[tries][i].setChar(i < attempt.length() ? attempt.charAt(i) : null);
    }

    private void testWord() {
        if (gameOver) {
            resetGame();
            resetWidgets();
            return;
        }

        if (attempt.length() != WORD_LENGTH) {
            Toast.addToast(Text.translated("gui.wordle.error.word_small"));
            playAnimBadWord();
            return;
        } else if (word.equals(attempt)) {
            Toast.addToast(Text.translated("gui.wordle.success"));
            gameOver = true;
        } else if (!guesses.contains(attempt) && !answers.contains(attempt)) {
            Toast.addToast(Text.translated("gui.wordle.error.word_not_found"));
            playAnimBadWord();
            return;
        }

        attempts[tries] = attempt;
        attempt = "";
        field.setString("");

        processWord(tries);
        tries++;

        if (!gameOver && tries >= TRIES) {
            Toast.addToast(Text.translated("gui.wordle.game_over", word));
            gameOver = true;
            tries++;
        }

        if (!gameOver) {
            updateSelected(tries);
        } else {
            lastWord = word;
            results[tries - 1]++;
            stats.update(lastWord, ++playCount, results);
            controls.showReset(true);
        }
    }

    private void playAnimBadWord() {
        for (Letter letter : letters[tries])
            letter.playWrongAnim();
    }

    @Override
    public boolean keyPress(int key, int scancode, int action, int mods) {
        if (action == GLFW_PRESS && (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER)) {
            testWord();
            return true;
        }

        return super.keyPress(key, scancode, action, mods);
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta) {
        renderSolidBackground(0xFF202020);
    }

    private void saveGame() {
        try {
            StringBuilder save = new StringBuilder();

            //save stats
            for (int i = 0; i < results.length; i++)
                save.append("results.").append(i).append("=").append(results[i]).append("\n");
            save.append("playCount=").append(playCount).append("\n");
            if (lastWord != null)
                save.append("lastWord=").append(lastWord).append("\n");

            //save game state
            if (!gameOver) {
                save.append("tries=").append(tries).append("\n");
                save.append("word=").append(word).append("\n");
                for (int i = 0; i < attempts.length; i++) {
                    if (attempts[i] != null)
                        save.append("attempts.").append(i).append("=").append(attempts[i]).append("\n");
                }
            }

            //write file
            IOUtils.writeFileCompressed(SAVE_FILE, save.toString().getBytes());
        } catch (Exception e) {
            LOGGER.error("Failed to save the game to a file", e);
        }
    }

    private void loadGame() {
        try {
            //read file
            byte[] bytes = IOUtils.readFileCompressed(SAVE_FILE);
            if (bytes == null)
                return;

            String save = new String(bytes);
            String[] lines = save.split("\n");
            for (String line : lines) {
                String[] split = line.split("=", 2);
                if (split.length < 2)
                    continue;

                String key = split[0];
                String value = split[1];

                switch (key) {
                    //stats check
                    case "playCount" -> playCount = Integer.parseInt(value);
                    case "lastWord" -> lastWord = value;

                    //game state check
                    case "tries" -> tries = Integer.parseInt(value);
                    case "word" -> word = value;

                    //arrays
                    default -> {
                        String[] split2 = key.split("\\.", 2);
                        if (split2.length < 2)
                            continue;

                        String key2 = split2[0];
                        int i = Integer.parseInt(split2[1]);

                        switch (key2) {
                            case "results" -> results[i] = Integer.parseInt(value);
                            case "attempts" -> attempts[i] = value;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load save file", e);
        }
    }

    private static class Field extends ContainerGrid {
        private final TextField field;
        public Field(int x, int y, Consumer<String> listener) {
            super(x, y, 4);
            field = new TextField(0, 0, 16, 16) {
                @Override
                public void setFocused(boolean focused) {
                    super.setFocused(true);
                }
            };
            field.setFocused(true);
            field.setCharLimit(WORD_LENGTH);
            field.setFilter(TextField.Filter.LETTERS);
            field.setListener(s -> listener.accept(s.trim().toUpperCase()));
            this.listeners.add(field);
        }

        public void setString(String string) {
            field.setText(string);
        }

        public String getString() {
            return field.getText();
        }
    }

    private static class Letter extends Widget {
        private Text text;
        private Colors color = COLORS[2];

        public Letter() {
            super(0, 0, 20, 20);
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            VertexConsumer.MAIN.consume(GeometryHelper.rectangle(
                    matrices,
                    getX(), getY(),
                    getX() + getWidth(), getY() + getHeight(),
                    -1, color.argb
            ));

            if (text != null)
                text.render(VertexConsumer.MAIN, matrices, getCenterX(), getCenterY(), Alignment.CENTER);
        }

        public void setColor(Colors color) {
            this.color = color;
        }

        public void setChar(Character c) {
            if (c !=  null) {
                this.text = Text.of(c).withStyle(Style.EMPTY.outlined(true));
                playSetAnim();
            } else {
                this.text = null;
            }
        }

        public void playSetAnim() {

        }

        public void playWrongAnim() {

        }
    }

    private static class Stats extends ContainerGrid {
        private static final String SKULL = "\u2620";
        private final Label lastWord, playCount;
        private final ProgressBar[] triesBar;
        private final Label[] triesCount;

        public Stats(int x, int y, int spacing, int tries) {
            super(x, y, spacing);

            lastWord = new Label(0, 0, Text.translated("gui.wordle.last_word", "???"));
            lastWord.setAlignment(Alignment.TOP_CENTER);
            addWidget(lastWord);

            playCount = new Label(0, 0, Text.translated("gui.wordle.game_count", 0));
            playCount.setAlignment(Alignment.TOP_CENTER);
            addWidget(playCount);

            Label triesLabel = new Label(0, 0, Text.translated("gui.wordle.stats"));
            triesLabel.setAlignment(Alignment.TOP_CENTER);
            addWidget(triesLabel);

            ContainerGrid bars = new ContainerGrid(0, 0, spacing, 3);

            int length = tries + 1;
            triesBar = new ProgressBar[length];
            triesCount = new Label[length];

            for (int i = 0; i < length; i++) {
                Colors color = COLORS[i == length - 1 ? 5 : i > length / 2 ? 3 : 4];

                bars.addWidget(new Label(0, 0, Text.of(i == length -1 ? SKULL : i + 1)));

                triesBar[i] = new ProgressBar(0, 0, 40, 8, 0f);
                triesBar[i].setColor(color);
                triesBar[i].setStyle(Hud.HUD_STYLE);
                bars.addWidget(triesBar[i]);

                triesCount[i] = new Label(0, 0, Text.of(0));
                bars.addWidget(triesCount[i]);
            }

            addWidget(bars);
        }

        public void update(String lastWord, int playCount, int[] results) {
            this.lastWord.setText(Text.translated("gui.wordle.last_word", lastWord == null ? "???" : lastWord));

            this.playCount.setText(Text.translated("gui.wordle.game_count", playCount));

            for (int i = 0; i < results.length; i++) {
                triesBar[i].setProgress(playCount == 0 ? 0 : (float) results[i] / playCount);
                triesCount[i].setText(Text.of(results[i]));
            }

            updateDimensions();
        }
    }

    private static class Keyboard extends ContainerGrid {
        private static final String[] ORDER = {
                "ABCDE",
                "FGHIJ",
                "KLMNO",
                "PQRST",
                "UVWXY",
                "Z"
        };

        private final Map<Character, Char> characters = new HashMap<>();

        public Keyboard(int x, int y, int spacing, Consumer<Character> action) {
            super(x, y, spacing);

            for (String s : ORDER) {
                ContainerGrid grid = new ContainerGrid(0, 0, spacing, 10);

                for (int i = 0; i < s.length(); i++) {
                    char c = s.charAt(i);
                    Char ch = new Char(10, 12, c, action);
                    characters.put(c, ch);
                    grid.addWidget(ch);
                }

                addWidget(grid);
            }
        }

        public void setColor(char c, int color) {
            Char ch = characters.get(c);
            if (ch != null) ch.setStatus(color);
        }

        public void reset() {
            for (Char c : characters.values())
                c.reset();
        }
    }

    private static class Char extends Button {

        private int status;

        public Char(int width, int height, char c, Consumer<Character> action) {
            super(0, 0, width, height, Text.of(c).withStyle(Style.EMPTY.outlined(true)), button -> action.accept(c));
            setSilent(true);
        }

        @Override
        protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            VertexConsumer.MAIN.consume(GeometryHelper.rectangle(
                    matrices,
                    getX(), getY(),
                    getX() + getWidth(), getY() + getHeight(),
                    -1, COLORS[isHoveredOrFocused() ? 6 : status].argb
            ));
        }

        public void setStatus(int status) {
            if (status > this.status)
                this.status = status;
        }

        public void reset() {
            this.status = 0;
        }
    }

    private static class KeyboardControls extends ContainerGrid {
        private static final char
                REMOVE_CHAR = '\u2715',
                ACCEPT_CHAR = '\u2713',
                RESET_CHAR = '\u21BA';
        private final Char remove, accept, reset;
        private boolean showReset;

        public KeyboardControls(int x, int y, int spacing, int width, Consumer<Character> removeAction, Consumer<Character> acceptAction) {
            super(x, y, spacing, 2);

            int w = (width - spacing) / 2;

            remove = new Char(w, 12, REMOVE_CHAR, removeAction);
            remove.setStatus(5);
            this.addWidget(remove);

            accept = new Char(w, 12, ACCEPT_CHAR, acceptAction);
            accept.setStatus(4);
            this.addWidget(accept);

            reset = new Char(width, 12, RESET_CHAR, acceptAction);
            reset.setStatus(3);
        }

        public void showReset(boolean showReset) {
            if (this.showReset == showReset)
                return;

            if (showReset) {
                this.removeWidget(remove);
                this.removeWidget(accept);
                this.addWidget(reset);
            } else {
                this.addWidget(remove);
                this.addWidget(accept);
                this.removeWidget(reset);
            }

            this.showReset = showReset;
        }
    }
}
