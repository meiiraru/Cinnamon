package mayo.utils;

public class Region2D {

    private int x1, y1, x2, y2;

    public Region2D(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public boolean intersects(Region2D other) {
        return x2 >= other.x1 && x1 <= other.x2 && y2 >= other.y1 && y1 <= other.y2;
    }

    public void clip(Region2D other) {
        this.x1 = Math.max(x1, other.x1);
        this.y1 = Math.max(y1, other.y1);
        this.x2 = Math.min(x2, other.x2);
        this.y2 = Math.min(y2, other.y2);
    }

    public int getX() {
        return this.x1;
    }

    public int getY() {
        return this.y1;
    }

    public int getX2() {
        return this.x2;
    }

    public int getY2() {
        return this.y2;
    }

    public int getWidth() {
        return x2 - x1;
    }

    public int getHeight() {
        return y2 - y1;
    }
}
