package mayo.gui.screens;

import mayo.Client;
import mayo.gui.ParentedScreen;
import mayo.gui.Screen;
import mayo.gui.Toast;
import mayo.gui.widgets.ContainerList;
import mayo.gui.widgets.Widget;
import mayo.gui.widgets.types.Button;
import mayo.gui.widgets.types.Label;
import mayo.gui.widgets.types.ProgressBar;
import mayo.gui.widgets.types.TextField;
import mayo.model.GeometryHelper;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.batch.VertexConsumer;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.*;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

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

        ReadFile func = (text, line, target) -> {
            String word = text.trim().replaceAll(" ", "");
            if (word.startsWith("#"))
                return;

            if (word.length() != WORD_LENGTH) {
                System.out.println("Ignoring wordle word \"" + word + "\" at line " + line);
                return;
            }

            target.add(word.toUpperCase());
        };

        //load words
        IOUtils.readStringLines(ANSWER_WORDS_PATH, (s, i) -> func.readLine(s, i, answers));
        IOUtils.readStringLines(GUESS_WORDS_PATH, (s, i) -> func.readLine(s, i, guesses));

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
        field = new Field(font, width, height, this::onTextUpdate);
        addWidget(field);

        //letters
        ContainerList list = new ContainerList(0, 0, 4);

        for (int i = 0; i < TRIES; i++) {
            ContainerList word = new ContainerList(0, 0, 4, WORD_LENGTH);

            for (int j = 0; j < WORD_LENGTH; j++) {
                Letter l = new Letter();
                word.addWidget(l);
                letters[i][j] = l;
            }

            list.addWidget(word);
        }

        list.setPos((width - list.getWidth()) / 2, (height - list.getHeight()) / 2);
        addWidget(list);

        //stats
        stats = new Stats(0, 0, 4, font, TRIES);
        stats.setAlignment(Alignment.CENTER);
        stats.setPos(list.getX() / 2, (height - stats.getHeight()) / 2);
        addWidget(stats);

        //keyboard
        ContainerList keyboardList = new ContainerList(0, 0, 2);
        keyboardList.setAlignment(Alignment.CENTER);

        int listXWidth = list.getX() + list.getWidth();
        keyboard = new Keyboard(0, 0, 2, c -> field.append(String.valueOf(c)));
        keyboardList.addWidget(keyboard);

        controls = new KeyboardControls(0, 0, 2, keyboard.getWidth(), c -> field.remove(1), c -> testWord());
        keyboardList.addWidgetOnTop(controls);

        keyboardList.setPos((width - listXWidth) / 2 + listXWidth, (height - keyboardList.getHeight()) / 2);
        addWidget(keyboardList);

        super.init();
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
        if (gameOver) {
            field.setString("");
            return;
        }

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
            Toast.addToast(Text.of("Word too small"), font);
            playAnimBadWord();
            return;
        } else if (word.equals(attempt)) {
            Toast.addToast(Text.of("Congrats!"), font);
            gameOver = true;
        } else if (!guesses.contains(attempt) && !answers.contains(attempt)) {
            Toast.addToast(Text.of("Word not in the word list!"), font);
            playAnimBadWord();
            return;
        }

        attempts[tries] = attempt;
        attempt = "";
        field.setString("");

        processWord(tries);
        tries++;

        if (!gameOver && tries >= TRIES) {
            Toast.addToast(Text.of("Game Over\nThe word was " + word), font);
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
        GeometryHelper.rectangle(
                VertexConsumer.GUI, matrices,
                0, 0,
                width, height,
                -999, 0xFF202020
        );
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
            IOUtils.writeFile(SAVE_FILE, save.toString().getBytes());
        } catch (Exception e) {
            System.out.println("Failed to save the game to a file");
            e.printStackTrace();
        }
    }

    private void loadGame() {
        try {
            //read file
            byte[] bytes = IOUtils.readFileBytes(SAVE_FILE);
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
            System.out.println("Failed to load save file");
            e.printStackTrace();
        }
    }

    private interface ReadFile {
        void readLine(String text, int line, List<String> target);
    }

    private static class Field extends ContainerList {
        private final TextField field;
        public Field(Font font, int x, int y, Consumer<String> listener) {
            super(x, y, 4);
            field = new TextField(0, 0, 16, 16, font) {
                @Override
                public void setFocused(boolean focused) {
                    super.setFocused(true);
                }
            };
            field.setFocused(true);
            field.setListener(s -> {
                String newString = s.substring(0, Math.min(s.length(), WORD_LENGTH)).trim().toUpperCase();
                if (!newString.equals(s))
                    field.setString(newString);
                listener.accept(newString);
            });
            this.listeners.add(field);
        }

        public void setString(String string) {
            field.setString(string);
        }

        public void append(String s) {
            field.append(s);
        }

        public void remove(int count) {
            field.remove(count);
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
            GeometryHelper.rectangle(
                    VertexConsumer.GUI, matrices,
                    getX(), getY(),
                    getX() + getWidth(), getY() + getHeight(),
                    -1, color.rgba
            );

            if (text != null) {
                Font f = Client.getInstance().font;
                f.render(VertexConsumer.FONT, matrices, getCenterX(), getCenterY() - Math.round(TextUtils.getHeight(text, f) / 2f), text, Alignment.CENTER);
            }
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

    private static class Stats extends ContainerList {
        private static final String SKULL = "\u2620";
        private final Label lastWord, playCount;
        private final ProgressBar[] triesBar;
        private final Label[] triesCount;

        public Stats(int x, int y, int spacing, Font font, int tries) {
            super(x, y, spacing);

            lastWord = new Label(0, 0, Text.of("Last Word\n" + "???"), font);
            addWidget(lastWord);

            playCount = new Label(0, 0, Text.of("Games\n0"), font);
            addWidget(playCount);

            Label triesLabel = new Label(0, 0, Text.of("Stats"), font);
            addWidget(triesLabel);

            ContainerList bars = new ContainerList(0, 0, spacing, 3);

            int length = tries + 1;
            triesBar = new ProgressBar[length];
            triesCount = new Label[length];

            for (int i = 0; i < length; i++) {
                Colors color = COLORS[i == length - 1 ? 5 : i > length / 2 ? 3 : 4];

                bars.addWidget(new Label(0, 0, Text.of(i == length -1 ? SKULL : i + 1), font));

                triesBar[i] = new ProgressBar(0, 0, 40, 8, 0f);
                triesBar[i].setColor(color);
                bars.addWidget(triesBar[i]);

                triesCount[i] = new Label(0, 0, Text.of(0), font);
                bars.addWidget(triesCount[i]);
            }

            addWidget(bars);
        }

        public void update(String lastWord, int playCount, int[] results) {
            this.lastWord.setText(Text.of("Last Word\n" + (lastWord == null ? "???" : lastWord)));

            this.playCount.setText(Text.of("Games\n" + playCount));

            for (int i = 0; i < results.length; i++) {
                triesBar[i].setProgress(playCount == 0 ? 0 : (float) results[i] / playCount);
                triesCount[i].setText(Text.of(results[i]));
            }

            updateDimensions();
        }
    }

    private static class Keyboard extends ContainerList {
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
                ContainerList list = new ContainerList(0, 0, spacing, 10);

                for (int i = 0; i < s.length(); i++) {
                    char c = s.charAt(i);
                    Char ch = new Char(10, 12, c, action);
                    characters.put(c, ch);
                    list.addWidget(ch);
                }

                addWidget(list);
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
            GeometryHelper.rectangle(
                    VertexConsumer.GUI, matrices,
                    getX(), getY(),
                    getX() + getWidth(), getY() + getHeight(),
                    -1, COLORS[isHoveredOrFocused() ? 6 : status].rgba
            );
        }

        public void setStatus(int status) {
            if (status > this.status)
                this.status = status;
        }

        public void reset() {
            this.status = 0;
        }
    }

    private static class KeyboardControls extends ContainerList {
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