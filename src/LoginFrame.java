import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;

public class LoginFrame extends JFrame {
    private final JTextField txtUsername = Theme.field(18);
    private final JPasswordField txtPassword = Theme.passwordField(18);
    private final JButton btnLogin = Theme.button("Login", Theme.Kind.PRIMARY);

    public LoginFrame() {
        setTitle("PIMS - Login");
        setSize(440, 440);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        setContentPane(root);
        root.add(Theme.header("PIMS", "Pharmacy Inventory Management System"), BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(Theme.BG);
        body.setBorder(new EmptyBorder(24, 32, 24, 32));

        JPanel card = Theme.card(null);
        card.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(4, 0, 4, 0);
        g.gridy = 0; card.add(Theme.label("Username"), g);
        g.gridy = 1; card.add(txtUsername, g);
        g.gridy = 2; g.insets = new Insets(12, 0, 4, 0); card.add(Theme.label("Password"), g);
        g.gridy = 3; g.insets = new Insets(4, 0, 4, 0); card.add(txtPassword, g);
        g.gridy = 4; g.insets = new Insets(20, 0, 0, 0); card.add(btnLogin, g);

        GridBagConstraints bg = new GridBagConstraints();
        bg.weightx = 1;
        bg.weighty = 1;
        bg.fill = GridBagConstraints.BOTH;
        body.add(card, bg);
        root.add(body, BorderLayout.CENTER);

        getRootPane().setDefaultButton(btnLogin); // Enter submits
        btnLogin.addActionListener(e -> handleLogin());
    }

    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            Theme.error(this, "Please enter both username and password.");
            return;
        }

        String sql = "SELECT user_id, password, role, full_name FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && PasswordUtil.verify(password, rs.getString("password"))) {
                    int userId = rs.getInt("user_id");
                    String role = rs.getString("role");
                    String fullName = rs.getString("full_name");

                    // Silently upgrade old plain-text passwords to a hash
                    if (!PasswordUtil.isHashed(rs.getString("password"))) {
                        upgradePassword(conn, userId, password);
                    }

                    if ("Admin".equalsIgnoreCase(role)) {
                        new AdminDashboard(userId, fullName).setVisible(true);
                    } else {
                        new CashierSales(userId, fullName, true).setVisible(true);
                    }
                    dispose();
                } else {
                    txtPassword.setText("");
                    JOptionPane.showMessageDialog(this, "Invalid username or password!", "Access Denied",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException ex) {
            Theme.error(this, "Database Connection Error: " + ex.getMessage());
        }
    }

    private void upgradePassword(Connection conn, int userId, String plain) {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE users SET password = ? WHERE user_id = ?")) {
            ps.setString(1, PasswordUtil.hash(plain));
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException ignored) {
            // Column probably too short; see ALTER TABLE note. Login still works.
        }
    }

    public static void main(String[] args) {
        Theme.install();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}