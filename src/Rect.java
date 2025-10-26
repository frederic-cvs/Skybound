/**
 * Small helper rectangle used for collision checks in the game.
 * It stores a position and size so we can ask whether two rectangles overlap.
 */
public class Rect {
    public final double x;
    public final double y;
    public final double w;
    public final double h;

    /**
     * Creates a rectangle using world coordinates and dimensions measured in pixels.
     *
     * @param x top-left x-position in pixels
     * @param y top-left y-position in pixels
     * @param w width in pixels
     * @param h height in pixels
     */
    public Rect(double x, double y, double w, double h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    // Returns the y-position of the top edge in pixels.
    public double top() {
        return y;
    }

    // Returns the y-position of the bottom edge in pixels.
    public double bottom() {
        return y + h;
    }

    // Checks if this rectangle overlaps another rectangle.
    public boolean intersects(Rect o) {
        return x < o.x + o.w && x + w > o.x && y < o.y + o.h && y + h > o.y;
    }

    // Convenience overload to check overlap without creating new Rect objects.
    public static boolean intersects(double x1, double y1, double w1, double h1,
                                     double x2, double y2, double w2, double h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
    }
}
