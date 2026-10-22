import javax.swing.*;
import java.awt.*;

public class AdminDashboard extends JFrame {

    public AdminDashboard(String adminName) {
        setTitle("PIMS - Admin Dashboard (" + adminName + ")");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Top Banner / Welcome
        JLabel lblHeader = new JLabel("Welcome, Admin: " + adminName, JLabel.CENTER);
        lblHeader.setFont(new Font("Arial", Font.BOLD, 18));
        lblHeader.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(lblHeader, BorderLayout.NORTH);

        // Center Panel for Management Buttons
        JPanel panelButtons = new JPanel(new GridLayout(2, 3, 15, 15));
        panelButtons.setBorder(BorderFactory.createEmptyBorder(20, 50, 50, 50));
        JButton btnManageUsers = new JButton("Manage Staff Users");
        panelButtons.add(btnManageUsers);
        JButton btnManageMedicines = new JButton("Manage Medicines");
        JButton btnManageSuppliers = new JButton("Manage Suppliers");
        JButton btnViewReports = new JButton("View Sales & Stock Reports");
        JButton btnLogout = new JButton("Logout");

        panelButtons.add(btnManageMedicines);
        panelButtons.add(btnManageSuppliers);
        panelButtons.add(btnViewReports);
        panelButtons.add(btnLogout);

        add(panelButtons, BorderLayout.CENTER);

        // Button Actions
        btnManageMedicines.addActionListener(e -> new MedicineManagement().setVisible(true));
        btnManageSuppliers.addActionListener(e -> new SupplierManagement().setVisible(true));
        btnViewReports.addActionListener(e -> new CashierSales("System Administrator").setVisible(true));
        btnViewReports.addActionListener(e -> new SalesStockReports().setVisible(true));
        btnManageUsers.addActionListener(e -> new UserManagement().setVisible(true));
        btnLogout.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
    }
}