/**
 * Player class. All atrributes required for funcionality. Velocities in px/tick
 */
public class Player {
    public double x;   // world x (px)
    public double y;   // world y (px)
    public double w;   // width (px)
    public double h;   // height (px)
    public double vx;  // horizontal speed
    public double vy;  // vertical speed

    public Player(double x, double y, double w, double h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.vx = 0.0;
        this.vy = -1.0; // this is a small upward push at start so first landing feels smooth
    }
}
