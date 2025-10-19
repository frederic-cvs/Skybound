import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ObjectManager class.
 * Owns platforms and enemies. 
 * Handles procedural spawning and difficulty adujstment.
 */
public class ObjectManager {

    // screen bounds for spawn decisions
    private final int W;
    private final int H;

    // random source
    private final Random rng;

    // active objects
    public final List<Platform> platforms = new ArrayList<>();
    public final List<Enemy> enemies = new ArrayList<>();

    // smallest world Y (highest platform so far)
    public double topMostY;

    // enemy rules
    public static final int MAX_ENEMIES = 2;     // hard cap
    private static final double MIN_ENEMY_GAP = 280; // enemy to enemy dist (px)
    private static final double MIN_FROM_PLAYER_Y = 180; // enemy to player vertical sep
    private static final double MIN_FROM_PLAYER_X = 90;  // enemy to player horizontal sep

    // platform spacing curve
    private double baseGap = 90;   // easy start
    private double maxGap = 170;  // harder as player progresses

    /**
     * Creates new object manager.
     */
    public ObjectManager(int screenW, int screenH, Random rng) {
        this.W = screenW;
        this.H = screenH;
        this.rng = rng;
        this.topMostY = H;
    }

    /**
     * Returns target vertical gap between new platforms given current score.
     * Grows linearly up to a max value to keep things manageable.
     */
    public double platformGapForScore(int score) {
        double t = Math.min(1.0, score / 150.0);
        return baseGap + t * (maxGap - baseGap);
    }

    /**
     * Spawns a row of platforms above the current topmost point.
     * Randomly adds a secondary shorter platform for variety.
     */
    public void spawnRowAbove(double gap) {
        topMostY -= gap;

        // main platform
        double x = 20 + rng.nextDouble() * (W - 140);
        Platform p = new Platform(x, topMostY, 120, 16);
        p.boost = rng.nextDouble() < 0.12; // red booster chance
        platforms.add(p);

        // random second platform slightly above
        if (rng.nextDouble() < 0.25) {
            double x2 = 20 + rng.nextDouble() * (W - 100);
            platforms.add(new Platform(x2, topMostY - 18, 80, 14));
        }
    }

    /**
     * Tries to spawn an enemy above the camera.
     * Keeps global cap and avoids clumping or unfair placements.
     */
    public void maybeSpawnEnemy(int score, Player player, double camY) {
        if (enemies.size() >= MAX_ENEMIES) {
            return;
        }

        // simple probability that scales with score
        double p = 0.002 + Math.min(0.008, score * 0.00004);
        if (rng.nextDouble() >= p) {
            return;
        }

        // attempt a few candidate positions. if none are valid, stop
        int attempts = 8;
        while (attempts-- > 0) {
            // fixed y above the camera band
            double y = topMostY - 140 - rng.nextDouble() * 120;
            double x = rng.nextDouble() * (W - 34);
            double vx = rng.nextBoolean() ? 1.8 : -1.8;

            // keep clear of player (horizontally and vertically)
            double playerScreenY = player.y - camY;
            double enemyScreenY = y - camY;

            boolean farFromPlayer = Math.abs(enemyScreenY - playerScreenY) >= MIN_FROM_PLAYER_Y &&
                    Math.abs(x - player.x) >= MIN_FROM_PLAYER_X;
            if (!farFromPlayer) {
                continue;
            }

            // keep clear of other enemies
            boolean isolated = true;
            for (Enemy e : enemies) {
                double dx = (e.x + e.w / 2.0) - (x + 17.0);
                double dy = (e.y - camY + e.h / 2.0) - (enemyScreenY + 17.0);
                double dist = Math.hypot(dx, dy);
                if (dist < MIN_ENEMY_GAP) {
                    isolated = false;
                    break;
                }
            }
            if (!isolated) {
                continue;
            }

            enemies.add(new Enemy(x, y, vx));
            break;
        }
    }
}
