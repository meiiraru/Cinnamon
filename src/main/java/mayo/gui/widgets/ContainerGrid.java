package mayo.gui.widgets;

import mayo.utils.Alignment;

public class ContainerGrid extends Container implements AlignedWidget {

    private final int spacing, columns;
    protected Alignment alignment = Alignment.LEFT;

    public ContainerGrid(int x, int y, int spacing) {
        this(x, y, spacing, 1);
    }

    public ContainerGrid(int x, int y, int spacing, int columns) {
        super(x, y);
        this.spacing = spacing;
        this.columns = Math.max(columns, 1);
    }

    @Override
    public void updateDimensions() {
        //prepare variables
        int size = widgets.size();

        int totalWidth = -spacing;
        int[] xPos = new int[columns + 1];

        int[] widths = new int[xPos.length];
        int[] yPos = new int[(int) Math.ceil((float) size / columns) + 1];
        widths[0] = 0;
        yPos[0] = 0;

        //grab cells size
        for (int i = 0; i < size; i++) {
            Widget w = widgets.get(i);

            //x
            int row = i % columns;
            widths[row + 1] = Math.max(widths[row + 1], w.getWidth());

            //y
            int column = i / columns;
            yPos[column + 1] = Math.max(yPos[column + 1], w.getHeight() + yPos[column] + spacing);
        }

        //fix for widths
        for (int i = 0; i < widths.length; i++) {
            totalWidth += widths[i] + spacing;
            xPos[i] = totalWidth;
        }

        if (size < columns)
            totalWidth -= spacing * (columns - size);

        //set dimensions (same as super)
        updateDimensions(totalWidth - spacing, yPos[yPos.length - 1] - spacing);

        //set widget positions
        int x = getX() + Math.round(alignment.getOffset(getWidth()));
        int y = getY();

        for (int i = 0; i < widgets.size(); i++) {
            int row = i % columns;
            int column = i / columns;

            Widget w = widgets.get(i);
            int ww = w.getWidth();
            w.setPos(x + xPos[row] + Math.round(alignment.getOffset(ww - widths[row + 1])), y + yPos[column]);

            if (w instanceof AlignedWidget aw) {
                aw.setAlignment(alignment);
                w.translate(Math.round(-alignment.getOffset(ww)), 0);
            }
        }
    }

    @Override
    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
        updateDimensions();
    }

    @Override
    public int getAlignedX() {
        return getX() + Math.round(alignment.getOffset(getWidth()));
    }
}
