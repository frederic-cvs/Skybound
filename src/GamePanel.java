import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import javax.swing.JPanel;

/**
 * GamePanel class. 
 * Acts as main game surface. Includes main loop, input, physics, camera, and rendering.
 */
public class GamePanel extends JPanel implements ActionListener, KeyListener {

    // screen size
    private final int W;
    private final int H;

    // 60 FPS loop timer (Swing thread)
    private final javax.swing.Timer timer;

    // pressed keys
    private final Set<Integer> keys = new HashSet<>();

    // variable for random source
    private final Random rng = new Random();

    // world state
    private Player player;
    private ObjectManager objects;

    // camera offset in world space (how far we've scrolled up)
    private double camY = 0.0;

    // most negative camY reached (used for score)
    private double maxCamY = 0.0;

    // score and persistent high score
    private int score = 0;
    private static int highScore = 0;

    // death flag
    private boolean dead = false;

    // physics and feel
    private static final double GRAVITY = 0.45; // px/tick^2
    private static final double H_ACC = 0.80; // px/tick^2 (key held)
    private static final double H_FRICTION = 0.90; // vx *= friction per tick
    private static final double MAX_H_SPEED = 10.0; // clamp for vx
    private static final double JUMP_VY = -13.5; // upward impulse
    private static final int TARGET_FPS = 60;

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
        initWorld();
    }

    /**
     * Resets world to a new fresh state (for start and restart).
     */
    public final void initWorld() {
        dead = false;
        score = 0;
        camY = 0.0;
        maxCamY = 0.0;

        // player near bottom
        player = new Player(W / 2.0 - 18, H - 100, 36, 48);

        // object manager with initial platforms
        objects = new ObjectManager(W, H, rng);

        // initial stack of platforms (simple staggered layout)
        double y = H - 40;
        for (int i = 0; i < 8; i++) {
            double x = 40 + (i % 2) * (W - 160);
            objects.platforms.add(new Platform(x, y, 100, 16));
            y -= 90;
        }

        // wide safety platform at the bottom
        objects.platforms.add(new Platform(W * 0.2, H - 20, W * 0.6, 16));
        objects.topMostY = y; // track topmost world Y for future spawns
    }

    /*
     * Starts the Swing timer.
     */
    public void start() {
        timer.start();
    }

    /*
     * Loop tick: first update, then repaint.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!dead) {
            update();
        }
        repaint();
    }

    /*
     * Core update: input, physics, collisions, camera, spawning, scoring.
     */
    private void update() {
        // input : horizontal acceleration
        if (keys.contains(KeyEvent.VK_A) || keys.contains(KeyEvent.VK_LEFT)) {
            player.vx -= H_ACC;
        }
        if (keys.contains(KeyEvent.VK_D) || keys.contains(KeyEvent.VK_RIGHT)) {
            player.vx += H_ACC;
        }

        // clamp and friction
        player.vx = clamp(player.vx, -MAX_H_SPEED, MAX_H_SPEED);
        player.vx *= H_FRICTION;

        // gravity
        player.vy += GRAVITY;

        // horizontal wrap
        player.x += player.vx;
        if (player.x < -player.w) {
            player.x = W;
        }
        if (player.x > W) {
            player.x = -player.w;
        }

        // vertical move and landing checks
        double oldY = player.y;
        player.y += player.vy;

        // only land when falling (no colliding from below)
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

        // camera follows when player climbs into upper band
        double screenY = player.y - camY;
        double threshold = H * 0.35;
        if (screenY < threshold) {
            camY -= (threshold - screenY);
            if (camY < maxCamY) {
                maxCamY = camY;
            }
        }

        // death: fell below camera by large margin
        if (player.y - camY > H + 120) {
            dead = true;
            highScore = Math.max(highScore, score);
        }

        // generating platforms above view
        double desiredGap = objects.platformGapForScore(score);
        while (objects.topMostY - camY > -200) {
            objects.spawnRowAbove(desiredGap);
        }

        // spawn enemies and powerups when added
        objects.maybeSpawnEnemy(score, player, camY);

        // enemy updates: horizontal movement back and forth, fixed y
        Iterator<Enemy> it = objects.enemies.iterator();
        while (it.hasNext()) {
            Enemy en = it.next();

            // move
            en.x += en.vx;

            // bounce
            if (en.x < 0) {
                en.x = 0;
                en.vx = -en.vx;
            }
            if (en.x + en.w > W) {
                en.x = W - en.w;
                en.vx = -en.vx;
            }

            // remove far below camera
            if (en.y - camY > H + 60) {
                it.remove();
                continue;
            }

            // hit = death
            if (!dead && rectsIntersect(player.x, player.y - camY, 
                                        player.w, player.h, en.x, en.y - camY, en.w, en.h)) {
                dead = true;
                highScore = Math.max(highScore, score);
            }
        }

        // drop platforms far below camera
        objects.platforms.removeIf(p -> p.y - camY > H + 80);

        // score from height climbed (pixels → coarse units)
        score = Math.max(score, (int) Math.round((-maxCamY) / 5.0));
    }

    /*
     * Draws background, platforms, enemies, player, HUD, and death overlay.
     */
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // bg gradient
        Paint old = g.getPaint();
        g.setPaint(new GradientPaint(0, 0, new Color(45, 56, 81), 0, H, new Color(20, 22, 34)));
        g.fillRect(0, 0, W, H);
        g.setPaint(old);

        // platforms
        for (Platform p : objects.platforms) {
            int x = (int) p.x;
            int y = (int) Math.round(p.y - camY);
            g.setColor(p.boost ? new Color(20, 220, 244) : new Color(104, 204, 141));
            g.fillRoundRect(x, y, (int) p.w, (int) p.h, 10, 10);
            g.setColor(new Color(0, 0, 0, 90));
            g.drawRoundRect(x, y, (int) p.w, (int) p.h, 10, 10);
        }

        // enemies
        g.setColor(new Color(252, 73, 73));
        for (Enemy en : objects.enemies) {
            int x = (int) en.x;
            int y = (int) Math.round(en.y - camY);
            g.fillOval(x, y, (int) en.w, (int) en.h);
            g.setColor(Color.BLACK);
            g.drawOval(x, y, (int) en.w, (int) en.h);
            g.setColor(new Color(252, 73, 73));
        }

        // player
        drawPlayer(g);

        // HUD
        g.setColor(Color.WHITE);
        g.setFont(getFont().deriveFont(java.awt.Font.BOLD, 18f));
        g.drawString("Score: " + score, 12, 22);
        g.drawString("High: " + highScore, W - 120, 22);

        // death overlay
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

    /*
     * Simple player sprite
     */
    private void drawPlayer(Graphics2D g) {
        int x = (int) Math.round(player.x);
        int y = (int) Math.round(player.y - camY);

        g.setColor(new Color(108, 160, 255));
        g.fillRoundRect(x, y, (int) player.w, (int) player.h, 10, 10);

        g.setColor(Color.WHITE);
        g.fillOval(x + 7, y + 12, 8, 8);
        g.fillOval(x + 21, y + 12, 8, 8);
        g.setColor(Color.BLACK);
        g.fillOval(x + 10, y + 15, 3, 3);
        g.fillOval(x + 24, y + 15, 3, 3);

        g.setColor(new Color(42, 52, 75));
        g.fillRoundRect(x + 4, y + (int) player.h - 8, (int) player.w - 8, 6, 6, 6);
    }

    // KeyListener

    @Override
    public void keyPressed(KeyEvent e) {
        keys.add(e.getKeyCode());
        // restart on space
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
    }

    // misc helpers

    private static boolean rectsIntersect(double x1, double y1, 
                                          double w1, double h1, double x2, double y2, double w2,
                                          double h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
