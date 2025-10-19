/**
 * Platform class. 
 * Currently contains "standard" and "boost" platforms.
 * The latter provides an extra boost in upwards velocity.
 */
public class Platform {
    public double x;     // world x
    public double y;     // world y
    public double w;     // width
    public double h;     // height
    public boolean boost; // whether is a boost platform

    public Platform(double x, double y, double w, double h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.boost = false;
    }
}
