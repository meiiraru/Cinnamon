package cinnamon.gui.widgets;

import cinnamon.utils.Alignment;

public interface AlignedWidget {
    void setAlignment(Alignment alignment);
    Alignment getAlignment();
    int getAlignedX();
    int getAlignedY();
}
