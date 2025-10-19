import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Skybound class.
 * Creates the window and boots the game panel.
 */
public class Skybound {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("skybound");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setResizable(false);

            GamePanel panel = new GamePanel(480, 720);
            f.setContentPane(panel);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);

            panel.start();
        });
    }
}
