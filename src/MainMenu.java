import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;

/**
 * MainMenu class.
 * Displays start and quit buttons. On start, switches to game panel.
 */
public class MainMenu extends JPanel {
    
    private final int W;
    private final int H;
    
    // button dimensions and positions
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 60;
    
    private int startButtonX;
    private int startButtonY;
    private int quitButtonX;
    private int quitButtonY;
    
    // hover states
    private boolean startHovered = false;
    private boolean quitHovered = false;
    
    // callback to switch to game
    private final Runnable onStart;
    
    /**
     * Creates the main menu panel.
     * @param width screen width
     * @param height screen height
     * @param onStart callback to invoke when start is clicked
     */
    public MainMenu(int width, int height, Runnable onStart) {
        this.W = width;
        this.H = height;
        this.onStart = onStart;
        
        setPreferredSize(new Dimension(W, H));
        setBackground(new Color(18, 23, 33));
        
        // center buttons
        startButtonX = (W - BUTTON_WIDTH) / 2;
        startButtonY = H / 2 - 80;
        quitButtonX = (W - BUTTON_WIDTH) / 2;
        quitButtonY = H / 2 + 20;
        
        // mouse listener for clicks and hovers
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int mx = e.getX();
                int my = e.getY();
                
                if (isInside(mx, my, startButtonX, startButtonY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                    onStart.run();
                } else if (isInside(mx, my, quitButtonX, quitButtonY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                    System.exit(0);
                }
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                int mx = e.getX();
                int my = e.getY();
                
                startHovered = isInside(mx, my, startButtonX, startButtonY, BUTTON_WIDTH, BUTTON_HEIGHT);
                quitHovered = isInside(mx, my, quitButtonX, quitButtonY, BUTTON_WIDTH, BUTTON_HEIGHT);
                repaint();
            }
        };
        
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }
    
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // gradient background (same as game)
        Paint old = g.getPaint();
        g.setPaint(new GradientPaint(0, 0, new Color(45, 56, 81), 0, H, new Color(20, 22, 34)));
        g.fillRect(0, 0, W, H);
        g.setPaint(old);
        
        // title
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 56));
        String title = "SKYBOUND";
        int titleWidth = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (W - titleWidth) / 2, H / 2 - 160);
        
        // start button
        drawButton(g, "START", startButtonX, startButtonY, startHovered, new Color(104, 204, 141));
        
        // quit button
        drawButton(g, "QUIT", quitButtonX, quitButtonY, quitHovered, new Color(252, 73, 73));
    }
    
    /**
     * Draw a button with hover effect.
     */
    private void drawButton(Graphics2D g, String text, int x, int y, boolean hovered, Color baseColor) {
        // button background
        if (hovered) {
            g.setColor(baseColor.brighter());
        } else {
            g.setColor(baseColor);
        }
        g.fillRoundRect(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, 15, 15);
        
        // border of the button
        g.setColor(new Color(0, 0, 0, 90));
        g.drawRoundRect(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, 15, 15);
        
        // text in the button
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        int textWidth = g.getFontMetrics().stringWidth(text);
        int textX = x + (BUTTON_WIDTH - textWidth) / 2;
        int textY = y + (BUTTON_HEIGHT + g.getFontMetrics().getAscent()) / 2 - 2;
        g.drawString(text, textX, textY);
    }
    
    /**
     * Check if point is inside rectangle.
     */
    private boolean isInside(int px, int py, int rx, int ry, int rw, int rh) {
        return px >= rx && px <= rx + rw && py >= ry && py <= ry + rh;
    }
}
