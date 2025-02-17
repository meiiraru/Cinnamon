package cinnamon.gui.widgets;

import cinnamon.utils.Alignment;

public class ContainerGrid extends Container implements AlignedWidget {

    private final int spacing, columns;
    protected Alignment alignment = Alignment.TOP_LEFT;
    protected boolean forceChildAlignment = false;

    public ContainerGrid(int x, int y, int spacing) {
        this(x, y, spacing, 1);
    }

    public ContainerGrid(int x, int y, int spacing, int columns) {
        super(x, y);
        this.spacing = spacing;
        this.columns = Math.max(columns, 1);
    }

    @Override
    protected void updateDimensions() {
        if (forceChildAlignment)
            for (Widget w : widgets)
                if (w instanceof AlignedWidget aw)
                    aw.setAlignment(alignment);

        //prepare variables
        int size = widgets.size();
        int[] widths = new int[Math.min(columns, size)];
        int[] heights = new int[(int) Math.ceil((float) size / columns)];

        //grab cells size
        for (int i = 0; i < size; i++) {
            Widget w = widgets.get(i);

            int row = i % columns;
            int column = i / columns;

            widths[row] = Math.max(widths[row], w.getWidth());
            heights[column] = Math.max(heights[column], w.getHeight());
        }

        //calculate total sizes
        int totalWidth = spacing * (widths.length - 1);
        int totalHeight = spacing * (heights.length - 1);

        for (int width : widths)
            totalWidth += width;
        for (int height : heights)
            totalHeight += height;

        //set dimensions (same as super)
        updateDimensions(totalWidth, totalHeight);

        Widget parent = getParent();
        if (parent != null)
            parent.updateDimensions();

        //set widget positions
        int x = getAlignedX() - Math.round(alignment.getWidthOffset(getWidth() - totalWidth));
        int y = getAlignedY() - Math.round(alignment.getHeightOffset(getHeight() - totalHeight));

        int xx = x;
        int yy = y;

        for (int i = 0; i < widgets.size(); i++) {
            int row = i % columns;
            int column = i / columns;

            Widget w = widgets.get(i);
            int ww = w.getWidth();
            int wh = w.getHeight();

            w.setPos(xx - Math.round(alignment.getWidthOffset(widths[row] - ww)), yy - Math.round(alignment.getHeightOffset(heights[column] - wh)));
            if (w instanceof AlignedWidget aw)
                w.translate(Math.round(-aw.getAlignment().getWidthOffset(ww)), Math.round(-aw.getAlignment().getHeightOffset(wh)));

            xx += widths[row] + spacing;
            if (row == columns - 1) {
                xx = x;
                yy += heights[column] + spacing;
            }
        }
    }

    @Override
    public void setAlignment(Alignment alignment) {
        if (this.alignment == alignment)
            return;

        this.alignment = alignment;
        updateDimensions();
    }

    @Override
    public Alignment getAlignment() {
        return alignment;
    }

    @Override
    public int getAlignedX() {
        return getX() + Math.round(alignment.getWidthOffset(getWidth()));
    }

    @Override
    public int getAlignedY() {
        return getY() + Math.round(alignment.getHeightOffset(getHeight()));
    }

    public int getSpacing() {
        return spacing;
    }

    public void forceChildAlignment(boolean bool) {
        this.forceChildAlignment = bool;
    }
}
