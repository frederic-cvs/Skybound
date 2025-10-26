/**
 * Represents the ledges the player can land on.
 * Platforms can be regular or boosters, which give the player a stronger jump.
 */
public class Platform {
    public double x;     // world x
    public double y;     // world y
    public double w;     // width
    public double h;     // height
    public boolean boost; // whether is a boost platform

    /**
     * Creates a platform at the given position with a given size.
     *
     * @param x horizontal world position in pixels
     * @param y vertical world position in pixels
     * @param w width of the platform in pixels
     * @param h height of the platform in pixels
     */
    public Platform(double x, double y, double w, double h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.boost = false;
    }
}
