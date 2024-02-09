package mayo.gui.screens;

import mayo.gui.ParentedScreen;
import mayo.gui.Screen;
import mayo.gui.widgets.ContainerList;
import mayo.gui.widgets.types.*;
import mayo.text.Text;
import mayo.utils.Colors;

public class WidgetTestScreen extends ParentedScreen {

    public WidgetTestScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void init() {
        ContainerList list = new ContainerList(4, 4, 4);

        //button
        Button b = new Button(0, 0, 60, 12, Text.of("Button"), button -> System.out.println("button!"));
        list.addWidget(b);

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

        b.setContextMenu(ctx);

        //label
        Label l = new Label(Text.of("Label"), font, 0, 0);
        list.addWidget(l);

        //progress bar
        ProgressBar pb = new ProgressBar(0, 0, 0, 60, 12);
        pb.setColor(Colors.PINK);
        list.addWidget(pb);

        //selection box
        SelectionBox sb = new SelectionBox(0, 0, 60, 12);
        list.addWidget(sb);

        sb
                .addEntry(Text.of("Entry 1"), Text.of("1"), button -> System.out.println("1"))
                .addDivider()
                .addEntry(Text.of("Entry 2"), Text.of("2"), button -> System.out.println("2"))
                .addDivider()
                .addEntry(Text.of("Entry 3"), Text.of("3"), button -> System.out.println("3"));

        //slider
        Slider s = new Slider(0, 0, 60);
        list.addWidget(s);

        s.setColor(Colors.PINK);
        s.setChangeListener((f, i) -> {
            l.setText(Text.of("Label " + f));
            pb.setProgress(f);
        });
        s.setMax(1000);
        s.setValue((int) (Math.random() * 500) + 250);

        b.setAction(button -> s.setStepCount(s.getStepCount() + 1));

        //text field
        TextField tf = new TextField(font, 0, 0, 60, 12);
        list.addWidget(tf);

        tf.setHintText("Text Field");
        tf.setListener(s1 -> tf.setColor(Colors.randomRainbow()));

        //toggle button
        ToggleButton tb =  new ToggleButton(0, 0, Text.of("Toggle Button"));
        list.addWidget(tb);

        //add list
        addWidget(list);
        super.init();
    }
}
