import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * Main game surface for Skybound.
 * Runs the update loop, handles input, and draws every frame.
 */
public class GamePanel extends JPanel implements ActionListener, KeyListener {

    // Screen width and height in pixels.
    private final int W;
    private final int H;

    // 60 FPS loop timer (fires on the Swing thread).
    private final javax.swing.Timer timer;

    // Keys that are currently pressed.
    private final Set<Integer> keys = new HashSet<>();

    // Shared random source for procedural events.
    private final Random rng = new Random();

    // Dynamic game objects.
    private Player player;
    private ObjectManager objects;

    // Camera offset in world space (how far we've scrolled up).
    private double camY = 0.0;

    // Most negative camY reached (used for score).
    private double maxCamY = 0.0;

    // Score and persistent high score.
    private int score = 0;
    private static int highScore = 0;

    // Tracks whether the player has died.
    private boolean dead = false;
    // Tracks whether gameplay is paused.
    private boolean paused = false;

    // Jetpack state and sprites.
    private boolean jetpackActive = false;
    private int jetpackTimer = 0;
    private BufferedImage ureCarSprite;
    private BufferedImage backdropImage;
    private BufferedImage enemySprite;

    // Physics and movement tuning.
    private static final double GRAVITY = 0.45; // px/tick^2
    private static final double H_ACC = 0.80; // px/tick^2 (key held)
    private static final double H_FRICTION = 0.90; // vx *= friction per tick
    private static final double MAX_H_SPEED = 10.0; // clamp for vx
    private static final double JUMP_VY = -13.5; // upward impulse
    private static final int TARGET_FPS = 60;
    private static final int JETPACK_DURATION_TICKS = TARGET_FPS * 3;
    private static final double JETPACK_ASCENT_SPEED = -18.0;
    private static final double JETPACK_H_DECAY = 0.92;
    private static final double JETPACK_DRAW_SCALE = 1.6; // visual scale for car sprite
    private static final double BACKDROP_PARALLAX_Y = 0.35;

    /**
     * Creates the panel with a fixed size and initialises state.
     */
    public GamePanel(int width, int height) {
        this.W = width;
        this.H = height;

        setPreferredSize(new Dimension(W, H));
        setBackground(new Color(18, 23, 33));
        setFocusable(true);
        addKeyListener(this);

        timer = new javax.swing.Timer(1000 / TARGET_FPS, this);
        loadAssets();
        initWorld();
    }

    /**
     * Resets world to a new fresh state (for start and restart).
     */
    public final void initWorld() {
        dead = false;
        paused = false;
        score = 0;
        camY = 0.0;
        maxCamY = 0.0;
        jetpackActive = false;
        jetpackTimer = 0;

        // Spawn the player near the bottom of the screen.
        player = new Player(W / 2.0 - 18, H - 100, 36, 48);

        // Create a fresh object manager for this run.
        objects = new ObjectManager(W, H, rng);

        // Build a starting ladder of staggered platforms.
        double y = H - 40;
        for (int i = 0; i < 8; i++) {
            double x = 40 + (i % 2) * (W - 160);
            objects.platforms.add(new Platform(x, y, 100, 16));
            y -= 90;
        }

        // Add a wide safety platform right above the floor.
        objects.platforms.add(new Platform(W * 0.2, H - 20, W * 0.6, 16));
        objects.topMostY = y; // Track the highest point for future spawns.

        // Drop an early power-up so new players see the mechanic quickly.
        if (!objects.platforms.isEmpty()) {
            Platform base = objects.platforms.get(0);
            double px = base.x + base.w / 2.0 - ObjectManager.POWERUP_SIZE / 2.0;
            double py = base.y - ObjectManager.POWERUP_SIZE - 12;
            objects.powerups.add(new Powerup(px, py, ObjectManager.POWERUP_SIZE, 
                                 ObjectManager.POWERUP_SIZE));
        }
    }

    // Starts the Swing timer that drives the game loop.
    public void start() {
        timer.start();
    }

    // Timer callback: advance the game state and then request a repaint.
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!dead && !paused) {
            update();
        }
        repaint();
    }

    // Handles input, physics, collisions, camera movement, spawning, and scoring.
    private void update() {
        // Apply horizontal acceleration based on the keys that are held.
        if (keys.contains(KeyEvent.VK_A) || keys.contains(KeyEvent.VK_LEFT)) {
            player.vx -= H_ACC;
        }
        if (keys.contains(KeyEvent.VK_D) || keys.contains(KeyEvent.VK_RIGHT)) {
            player.vx += H_ACC;
        }

        // Clamp horizontal speed and apply friction or jetpack drift.
        player.vx = clamp(player.vx, -MAX_H_SPEED, MAX_H_SPEED);
        if (jetpackActive) {
            player.vx *= JETPACK_H_DECAY;
        } else {
            player.vx *= H_FRICTION;
        }

        // Apply gravity, or forced upward speed while the jetpack is active.
        if (jetpackActive) {
            player.vy = JETPACK_ASCENT_SPEED;
        } else {
            player.vy += GRAVITY;
        }

        // Wrap horizontally so the player reappears on the other side.
        player.x += player.vx;
        if (player.x < -player.w) {
            player.x = W;
        }
        if (player.x > W) {
            player.x = -player.w;
        }

        // Move vertically and look for platform landings.
        double oldY = player.y;
        player.y += player.vy;

        // Only land when falling (prevents collisions from underneath).
        if (player.vy > 0) {
            for (Platform p : objects.platforms) {
                double pScreenY = p.y - camY;

                Rect pr = new Rect(p.x, pScreenY, p.w, p.h);
                Rect r = new Rect(player.x, player.y - camY, player.w, player.h);
                Rect rOld = new Rect(player.x, oldY - camY, player.w, player.h);

                // top hit check (old bottom was above platform top)
                if (r.intersects(pr) && rOld.bottom() <= pr.top() + 2) {
                    player.y = pr.top() - player.h + camY;
                    player.vy = JUMP_VY;
                    if (p.boost) {
                        player.vy = JUMP_VY * 1.4; // stronger jump for boost platform
                    }
                }
            }
        }

        // Let the camera follow when the player climbs near the top of the screen.
        double screenY = player.y - camY;
        double threshold = H * 0.35;
        if (screenY < threshold) {
            camY -= (threshold - screenY);
            if (camY < maxCamY) {
                maxCamY = camY;
            }
        }

        // Check for death by falling far below the camera.
        if (player.y - camY > H + 120) {
            dead = true;
            highScore = Math.max(highScore, score);
            jetpackActive = false;
            jetpackTimer = 0;
        }

        // Keep generating platforms slightly above the visible area.
        double desiredGap = objects.platformGapForScore(score);
        while (objects.topMostY - camY > -200) {
            objects.spawnRowAbove(desiredGap);
        }

        // Spawn enemies according to difficulty rules.
        objects.maybeSpawnEnemy(score, player, camY);

        // Power-up pickup checks (ignored during a jetpack burst).
        if (!jetpackActive) {
            Iterator<Powerup> pit = objects.powerups.iterator();
            while (pit.hasNext()) {
                Powerup pu = pit.next();
                if (Rect.intersects(player.x, player.y, player.w, player.h,
                                    pu.x, pu.y, pu.w, pu.h)) {
                    pit.remove();
                    activateJetpack();
                    break;
                }
            }
        }

        // Update enemies: move, bounce off walls, and handle collisions.
        Iterator<Enemy> it = objects.enemies.iterator();
        while (it.hasNext()) {
            Enemy en = it.next();

            // Move horizontally.
            en.x += en.vx;

            // Bounce off the left and right edges.
            if (en.x < 0) {
                en.x = 0;
                en.vx = -en.vx;
            }
            if (en.x + en.w > W) {
                en.x = W - en.w;
                en.vx = -en.vx;
            }

            // Remove enemies that fell far below the camera.
            if (en.y - camY > H + 60) {
                it.remove();
                continue;
            }

            // Jetpack-free collisions still cause death.
            if (!dead && !jetpackActive && Rect.intersects(player.x, player.y - camY,
                    player.w, player.h, en.x, en.y - camY, en.w, en.h)) {
                dead = true;
                highScore = Math.max(highScore, score);
            }
        }

        // Drop platforms and power-ups that are far below the camera.
        objects.platforms.removeIf(p -> p.y - camY > H + 80);
        objects.powerups.removeIf(p -> p.y - camY > H + 80);

        // Update the score based on the highest point reached.
        score = Math.max(score, (int) Math.round((-maxCamY) / 5.0));

        // Tick down the jetpack timer and switch back to normal physics.
        if (jetpackActive) {
            jetpackTimer--;
            if (jetpackTimer <= 0) {
                jetpackActive = false;
                jetpackTimer = 0;
                player.vy = -4.0;
            }
        }
    }

    // Draw the background, actors, HUD, and optional death overlay.
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackdrop(g);

        // Draw platforms.
        for (Platform p : objects.platforms) {
            int x = (int) p.x;
            int y = (int) Math.round(p.y - camY);
            g.setColor(p.boost ? new Color(20, 220, 244) : new Color(104, 204, 141));
            g.fillRoundRect(x, y, (int) p.w, (int) p.h, 10, 10);
            g.setColor(new Color(0, 0, 0, 90));
            g.drawRoundRect(x, y, (int) p.w, (int) p.h, 10, 10);
        }

        // Draw enemies.
        for (Enemy en : objects.enemies) {
            int x = (int) en.x;
            int y = (int) Math.round(en.y - camY);
            int w = (int) Math.round(en.w * 2.0);
            int h = (int) Math.round(en.h * 2.0);
            if (enemySprite != null) {
                g.drawImage(enemySprite, x, y, w, h, null);
            } else {
                g.setColor(new Color(252, 73, 73));
                g.fillOval(x, y, w, h);
                g.setColor(Color.BLACK);
                g.drawOval(x, y, w, h);
            }
        }

        // Draw power-ups.
        for (Powerup pu : objects.powerups) {
            int x = (int) Math.round(pu.x);
            int y = (int) Math.round(pu.y - camY);
            int w = (int) Math.round(pu.w);
            int h = (int) Math.round(pu.h);
            if (ureCarSprite != null) {
                g.drawImage(ureCarSprite, x, y, w, h, null);
            } else {
                g.setColor(new Color(255, 214, 94));
                g.fillOval(x, y, w, h);
                g.setColor(new Color(194, 148, 54));
                g.drawOval(x, y, w, h);
            }
        }

        // Draw the player last so it sits above pickups.
        drawPlayer(g);

        // Draw the on-screen HUD.
        g.setColor(Color.WHITE);
        g.setFont(getFont().deriveFont(java.awt.Font.BOLD, 18f));
        g.drawString("Score: " + score, 12, 22);
        g.drawString("High: " + highScore, W - 120, 22);

        // Draw pause overlay when the game is paused.
        if (paused && !dead) {
            g.setColor(new Color(0, 0, 0, 140));
            g.fillRect(0, 0, W, H);
            g.setColor(Color.WHITE);
            g.setFont(getFont().deriveFont(java.awt.Font.BOLD, 32f));
            String t = "Paused";
            int tw = g.getFontMetrics().stringWidth(t);
            g.drawString(t, (W - tw) / 2, H / 2 - 10);
            g.setFont(getFont().deriveFont(java.awt.Font.PLAIN, 16f));
            String hint = "Press P to resume";
            g.drawString(hint,
                    (W - g.getFontMetrics().stringWidth(hint)) / 2,
                    H / 2 + 18);
        }

        // Draw the death overlay with restart prompt.
        if (dead) {
            g.setColor(new Color(0, 0, 0, 140));
            g.fillRect(0, 0, W, H);
            g.setColor(Color.WHITE);
            g.setFont(getFont().deriveFont(java.awt.Font.BOLD, 32f));
            String t = "Game Over";
            int tw = g.getFontMetrics().stringWidth(t);
            g.drawString(t, (W - tw) / 2, H / 2 - 20);
            g.setFont(getFont().deriveFont(java.awt.Font.PLAIN, 18f));
            String s1 = "Final score: " + score;
            String s2 = "Press SPACE to restart";
            g.drawString(s1, (W - g.getFontMetrics().stringWidth(s1)) / 2, H / 2 + 10);
            g.drawString(s2, (W - g.getFontMetrics().stringWidth(s2)) / 2, H / 2 + 35);
        }
    }

    // Draw the parallax backdrop image, or a gradient if it failed to load.
    private void drawBackdrop(Graphics2D g) {
        if (backdropImage == null || backdropImage.getWidth() <= 0 
            || backdropImage.getHeight() <= 0) {
            Paint old = g.getPaint();
            g.setPaint(new GradientPaint(0, 0, new Color(45, 56, 81), 0, H, new Color(20, 22, 34)));
            g.fillRect(0, 0, W, H);
            g.setPaint(old);
            return;
        }

        int imgW = backdropImage.getWidth();
        int imgH = backdropImage.getHeight();

        double scrollY = camY * BACKDROP_PARALLAX_Y;
        int offsetY = wrapOffset(scrollY, imgH);

        int startY = -offsetY;
        if (startY > 0) {
            startY -= imgH;
        }

        for (int y = startY; y < H; y += imgH) {
            for (int x = 0; x < W; x += imgW) {
                g.drawImage(backdropImage, x, y, null);
            }
        }
    }

    // Draw the player character or the jetpack sprite when active.
    private void drawPlayer(Graphics2D g) {
        int x = (int) Math.round(player.x);
        double screenYd = player.y - camY;
        int y = (int) Math.round(screenYd);

        if (jetpackActive && ureCarSprite != null) {
            int drawW = (int) Math.round(player.w * JETPACK_DRAW_SCALE);
            int drawH = (int) Math.round(player.h * JETPACK_DRAW_SCALE);
            int drawX = (int) Math.round(player.x + player.w / 2.0 - drawW / 2.0);
            int drawY = (int) Math.round(screenYd + player.h / 2.0 - drawH / 2.0);
            g.drawImage(ureCarSprite, drawX, drawY, drawW, drawH, null);
        } else {
            Color body = jetpackActive ? new Color(255, 214, 94) : new Color(108, 160, 255);
            g.setColor(body);
            g.fillRoundRect(x, y, (int) player.w, (int) player.h, 10, 10);

            Stroke oldStroke = g.getStroke();
            g.setStroke(new BasicStroke(2f));
            g.setColor(new Color(20, 30, 50, 200));
            g.drawRoundRect(x - 2, y - 2, (int) player.w + 4, (int) player.h + 4, 12, 12);
            g.setStroke(oldStroke);

            g.setColor(Color.WHITE);
            g.fillOval(x + 7, y + 12, 8, 8);
            g.fillOval(x + 21, y + 12, 8, 8);
            g.setColor(Color.BLACK);
            g.fillOval(x + 10, y + 15, 3, 3);
            g.fillOval(x + 24, y + 15, 3, 3);

            g.setColor(new Color(42, 52, 75));
            g.fillRoundRect(x + 4, y + (int) player.h - 8, (int) player.w - 8, 6, 6, 6);

            if (jetpackActive) {
                g.setColor(new Color(255, 145, 64, 180));
                g.fillOval(x + 6, y + (int) player.h - 2, 8, 16);
                g.fillOval(x + (int) player.w - 16, y + (int) player.h - 2, 8, 16);
            }
        }
    }

    // KeyListener implementation.

    @Override
    public void keyPressed(KeyEvent e) {
        keys.add(e.getKeyCode());
        if (!dead && e.getKeyCode() == KeyEvent.VK_P) {
            paused = !paused;
            keys.remove(KeyEvent.VK_P);
            return;
        }
        // Space restarts the run after death.
        if (dead && e.getKeyCode() == KeyEvent.VK_SPACE) {
            initWorld();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keys.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used, but the interface requires it.
    }

    // Utility helpers.

    // Clamp a value between the given limits.
    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // Load sprites from either the classpath or the src directory as a fallback.
    private void loadAssets() {
        ureCarSprite = loadFromResource("/Assets/URECarSprite.png");
        if (ureCarSprite == null) {
            ureCarSprite = loadFromFile("src/Assets/URECarSprite.png");
        }

        backdropImage = loadFromResource("/Assets/BackdropSky.png");
        if (backdropImage == null) {
            backdropImage = loadFromFile("src/Assets/BackdropSky.png");
        }

        enemySprite = loadFromResource("/Assets/EnemyDrawnSparCroc.png");
        if (enemySprite == null) {
            enemySprite = loadFromFile("src/Assets/EnemyDrawnSparCroc.png");
        }
    }

    // Activate the jetpack power-up and prime its timer.
    private void activateJetpack() {
        jetpackActive = true;
        jetpackTimer = JETPACK_DURATION_TICKS;
        player.vy = JETPACK_ASCENT_SPEED;
        player.vx *= 0.5;
    }

    private BufferedImage loadFromResource(String resourcePath) {
        try (InputStream in = GamePanel.class.getResourceAsStream(resourcePath)) {
            if (in != null) {
                return ImageIO.read(in);
            }
        } catch (IOException ignore) {
        }
        return null;
    }

    private BufferedImage loadFromFile(String filePath) {
        try {
            return ImageIO.read(new File(filePath));
        } catch (IOException ignore) {
            return null;
        }
    }

    // Keep background scrolling offsets within the image bounds.
    private int wrapOffset(double scroll, int size) {
        if (size <= 0) {
            return 0;
        }
        double mod = scroll % size;
        if (mod < 0) {
            mod += size;
        }
        return (int) Math.floor(mod);
    }
}
