/**
 * Represents a jetpack pick-up that the player can collect.
 * The position and size are stored in world coordinates so the game can draw it
 * and run collision checks against the player.
 */
public class Powerup {
    public double x;   // world x
    public double y;   // world y
    public double w;   // width
    public double h;   // height

    /**
     * Creates a power-up at the given top-left position with the given size.
     *
     * @param x horizontal world position in pixels
     * @param y vertical world position in pixels
     * @param w width in pixels
     * @param h height in pixels
     */
    public Powerup(double x, double y, double w, double h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
}
