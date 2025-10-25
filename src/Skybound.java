import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Skybound class.
 * Creates the window and shows the main menu, then boots the game panel on start.
 */
public class Skybound {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("skybound");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setResizable(false);

            // create game panel (but don't start it yet)
            GamePanel gamePanel = new GamePanel(480, 720);
            
            // create main menu with callback to switch to game
            MainMenu menu = new MainMenu(480, 720, () -> {
                f.setContentPane(gamePanel);
                f.revalidate();
                gamePanel.requestFocusInWindow();
                gamePanel.start();
            });
            
            // show menu first
            f.setContentPane(menu);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}