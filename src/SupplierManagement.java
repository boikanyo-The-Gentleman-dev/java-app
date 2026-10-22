import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class SupplierManagement extends JFrame {
    private JTable tableSuppliers;
    private DefaultTableModel tableModel;
    private JTextField txtName, txtPhone, txtEmail, txtAddress;

    public SupplierManagement() {
        setTitle("PIMS - Supplier Management");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Table Setup
        String[] columns = {"ID", "Supplier Name", "Phone", "Email", "Address"};
        tableModel = new DefaultTableModel(columns, 0);
        tableSuppliers = new JTable(tableModel);
        loadSupplierData();

        add(new JScrollPane(tableSuppliers), BorderLayout.CENTER);

        // Input Form Panel at Bottom
        JPanel panelForm = new JPanel(new GridLayout(3, 3, 8, 8));
        panelForm.setBorder(BorderFactory.createTitledBorder("Add New Supplier"));

        panelForm.add(new JLabel("Supplier Name:"));
        txtName = new JTextField();
        panelForm.add(txtName);

        panelForm.add(new JLabel("Phone:"));
        txtPhone = new JTextField();
        panelForm.add(txtPhone);

        panelForm.add(new JLabel("Email:"));
        txtEmail = new JTextField();
        panelForm.add(txtEmail);

        panelForm.add(new JLabel("Address:"));
        txtAddress = new JTextField();
        panelForm.add(txtAddress);

        JButton btnAdd = new JButton("Add Supplier");
        panelForm.add(btnAdd);

        add(panelForm, BorderLayout.SOUTH);

        // Add Button Action
        btnAdd.addActionListener(e -> addSupplier());
    }

    private void loadSupplierData() {
        tableModel.setRowCount(0);
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM suppliers")) {

            while (rs.next()) {
                Object[] row = {
                        rs.getInt("supplier_id"),
                        rs.getString("name"), // Updated to match database column 'name'
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("address")
                };
                tableModel.addRow(row);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading suppliers: " + ex.getMessage());
        }
    }

    private void addSupplier() {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "INSERT INTO suppliers (name, phone, email, address) VALUES (?, ?, ?, ?)"; // Updated to match database column 'name'
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, txtName.getText().trim());
            stmt.setString(2, txtPhone.getText().trim());
            stmt.setString(3, txtEmail.getText().trim());
            stmt.setString(4, txtAddress.getText().trim());

            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Supplier added successfully!");
            loadSupplierData(); // Refresh table
            clearForm();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error adding supplier: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        txtName.setText("");
        txtPhone.setText("");
        txtEmail.setText("");
        txtAddress.setText("");
    }
}