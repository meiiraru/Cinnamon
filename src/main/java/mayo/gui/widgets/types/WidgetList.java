package mayo.gui.widgets.types;

import mayo.gui.widgets.Container;
import mayo.gui.widgets.Widget;

public class WidgetList extends Container {

    private final int spacing, columns;

    public WidgetList(int x, int y, int spacing) {
        this(x, y, spacing, 1);
    }

    public WidgetList(int x, int y, int spacing, int columns) {
        super(x, y);
        this.spacing = spacing;
        this.columns = Math.max(columns, 1);
    }

    @Override
    public void updateDimensions() {
        int size = widgets.size();

        //calculate cell dimensions
        int[] minX = new int[columns + 1];
        int[] minY = new int[(int) Math.ceil((float) size / columns) + 1];
        minX[0] = 0; minY[0] = 0;

        for (int i = 0; i < widgets.size(); i++) {
            Widget w = widgets.get(i);

            //x
            int row = i % columns;
            minX[row + 1] = Math.max(minX[row + 1], w.getWidth() + minX[row] + spacing);

            //y
            int column = i / columns;
            minY[column + 1] = Math.max(minY[column + 1], w.getHeight() + minY[column] + spacing);
        }

        //set widget positions
        int x = getX();
        int y = getY();

        for (int i = 0; i < widgets.size(); i++) {
            int row = i % columns;
            int column = i / columns;

            Widget w = widgets.get(i);
            w.setPos(x + minX[row], y + minY[column]);
        }

        //super call
        super.updateDimensions();
    }
}
