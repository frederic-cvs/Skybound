import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Coordinates creation and lifecycle of platforms, enemies, and power-ups.
 * All spawning logic lives here so the game loop can request new content cleanly.
 */
public class ObjectManager {

    // Screen bounds used to decide where new objects may spawn.
    private final int W;
    private final int H;

    // Random source shared by all spawn methods.
    private final Random rng;

    // Active objects owned by the manager.
    public final List<Platform> platforms = new ArrayList<>();
    public final List<Enemy> enemies = new ArrayList<>();
    public final List<Powerup> powerups = new ArrayList<>();

    // Smallest world Y (highest platform so far), used to stack new rows above.
    public double topMostY;

    // Enemy placement rules.
    public static final int MAX_ENEMIES = 2;                 // Hard cap.
    private static final double MIN_ENEMY_GAP = 280;         // Enemy-to-enemy distance (px).
    private static final double MIN_FROM_PLAYER_Y = 180;     // Minimum vertical separation from player.
    private static final double MIN_FROM_PLAYER_X = 90;      // Minimum horizontal separation from player.

    // Platform spacing curve and power-up rules.
    private double baseGap = 90;                          // Easy starting distance.
    private double maxGap = 170;                          // Wider gaps as difficulty increases.
    public static final double POWERUP_SIZE = 72;         // Visual pickup size (px).
    private static final double POWERUP_SPAWN_CHANCE = 0.02; // Chance to spawn a power-up on a platform.

    /**
     * Creates an object manager tied to the screen dimensions and a random source.
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

        // powerup spawn over the main platform
        if (rng.nextDouble() < POWERUP_SPAWN_CHANCE) {
            double px = p.x + p.w / 2.0 - POWERUP_SIZE / 2.0;
            double py = p.y - POWERUP_SIZE - 12;
            powerups.add(new Powerup(px, py, POWERUP_SIZE, POWERUP_SIZE));
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
