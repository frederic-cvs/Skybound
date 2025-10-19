/**
 * AABB helper class. Simple top landing checks
 */
public class Rect {
    public final double x;
    public final double y;
    public final double w;
    public final double h;

    public Rect(double x, double y, double w, double h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public double top() {
        return y;
    }

    public double bottom() {
        return y + h;
    }

    /*
     * Overlap test against another rectangle
     */
    public boolean intersects(Rect o) {
        return x < o.x + o.w && x + w > o.x && y < o.y + o.h && y + h > o.y;
    }

    /*
     * Static version to check without creating new rects
     */
    public static boolean intersects(double x1, double y1, double w1, double h1,
                                     double x2, double y2, double w2, double h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
    }
}
