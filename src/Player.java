/**
 * Represents the controllable player character.
 * Stores position and velocity so the game can move, draw, and collide the avatar.
 */
public class Player {
    public double x;   // world x (px)
    public double y;   // world y (px)
    public double w;   // width (px)
    public double h;   // height (px)
    public double vx;  // horizontal speed
    public double vy;  // vertical speed

    /**
     * Creates a player at the given position with the given size.
     *
     * @param x starting horizontal world position in pixels
     * @param y starting vertical world position in pixels
     * @param w width of the player in pixels
     * @param h height of the player in pixels
     */
    public Player(double x, double y, double w, double h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.vx = 0.0;
        this.vy = -1.0; // this is a small upward push at start so first landing feels smooth
    }
}
