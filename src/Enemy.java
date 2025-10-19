/**
 * Enemy class with all attributes required for functionality.
 */
public class Enemy {
    public double x;   // world x
    public double y;   // world y (fixed)
    public double w = 34; // width
    public double h = 34; // height
    public double vx;  // horizontal speed

    public Enemy(double x, double y, double vx) {
        this.x = x;
        this.y = y;
        this.vx = vx;
    }
}
