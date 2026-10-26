import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class AdminDashboard extends JFrame {
    private final int userId;
    private final String adminName;
    private final Map<String, JFrame> openWindows = new HashMap<>();
    private final JLabel lblMeds = new JLabel("-");
    private final JLabel lblLow = new JLabel("-");
    private final JLabel lblToday = new JLabel("-");

    public AdminDashboard(int userId, String adminName) {
        this.userId = userId;
        this.adminName = adminName;

        setTitle("PIMS - Admin Dashboard (" + adminName + ")");
        setSize(860, 580);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        setContentPane(root);
        root.add(Theme.header("Welcome, " + adminName, "Administrator dashboard"), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 20));
        body.setBackground(Theme.BG);
        body.setBorder(new EmptyBorder(20, 24, 24, 24));

        // Quick stats
        JPanel stats = new JPanel(new GridLayout(1, 3, 16, 0));
        stats.setOpaque(false);
        stats.add(tile("Medicines in inventory", lblMeds));
        stats.add(tile("Low-stock alerts", lblLow));
        stats.add(tile("Sales today", lblToday));
        body.add(stats, BorderLayout.NORTH);

        // Menu buttons
        JPanel grid = new JPanel(new GridLayout(2, 3, 16, 16));
        grid.setOpaque(false);
        grid.add(menuButton("Manage Staff Users", Theme.Kind.PRIMARY,
                () -> showWindow("users", () -> new UserManagement(userId))));
        grid.add(menuButton("Manage Medicines", Theme.Kind.PRIMARY,
                () -> showWindow("medicines", MedicineManagement::new)));
        grid.add(menuButton("Manage Suppliers", Theme.Kind.PRIMARY,
                () -> showWindow("suppliers", SupplierManagement::new)));
        grid.add(menuButton("Sales & Stock Reports", Theme.Kind.PRIMARY,
                () -> showWindow("reports", SalesStockReports::new)));
        grid.add(menuButton("Point of Sale", Theme.Kind.SUCCESS,
                () -> showWindow("pos", () -> new CashierSales(userId, adminName, false))));
        grid.add(menuButton("Logout", Theme.Kind.DANGER, this::logout));
        body.add(grid, BorderLayout.CENTER);

        root.add(body, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override public void windowActivated(WindowEvent e) { refreshStats(); }
        });
    }

    private JPanel tile(String caption, JLabel value) {
        JPanel p = Theme.card(null);
        p.setLayout(new GridLayout(0, 1, 0, 2));
        value.setFont(Theme.BASE.deriveFont(Font.BOLD, 26f));
        value.setForeground(Theme.PRIMARY_DARK);
        p.add(value);
        p.add(Theme.mutedLabel(caption));
        return p;
    }

    private JButton menuButton(String text, Theme.Kind kind, Runnable action) {
        JButton b = Theme.button(text, kind);
        b.setFont(Theme.BASE.deriveFont(Font.BOLD, 15f));
        b.addActionListener(e -> action.run());
        return b;
    }

    /** Opens a window once; clicking again brings the existing one to the front. */
    private void showWindow(String key, Supplier<JFrame> factory) {
        JFrame f = openWindows.get(key);
        if (f == null || !f.isDisplayable()) {
            f = factory.get();
            openWindows.put(key, f);
            f.setVisible(true);
        } else {
            f.setState(Frame.NORMAL);
            f.toFront();
            f.requestFocus();
        }
    }

    private void logout() {
        for (JFrame f : openWindows.values()) f.dispose();
        dispose();
        new LoginFrame().setVisible(true);
    }

    private void refreshStats() {
        lblMeds.setText(scalar("SELECT COUNT(*) FROM medicines"));
        lblLow.setText(scalar("SELECT COUNT(*) FROM medicines WHERE quantity_in_stock <= reorder_level"));
        String today = scalar("SELECT COALESCE(SUM(total_amount), 0) FROM sales WHERE DATE(sale_date) = CURDATE()");
        lblToday.setText("-".equals(today) ? "-" : Theme.CURRENCY + today);
    }

    private String scalar(String sql) {
        try (Connection c = DBConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) {
                Object v = rs.getObject(1);
                if (v instanceof BigDecimal b) return String.format("%,.2f", b);
                return String.valueOf(v);
            }
        } catch (SQLException ignored) { }
        return "-";
    }
}