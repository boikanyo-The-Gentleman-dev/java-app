import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class UserManagement extends JFrame {
    private JTable tableUsers;
    private DefaultTableModel tableModel;
    private JTextField txtUsername, txtPassword, txtFullName;
    private JComboBox<String> cmbRole;
    private int selectedUserId = -1;

    public UserManagement() {
        setTitle("PIMS - Staff & User Management");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Table Setup
        String[] columns = {"User ID", "Username", "Full Name", "Role"};
        tableModel = new DefaultTableModel(columns, 0);
        tableUsers = new JTable(tableModel);
        loadUserData();

        add(new JScrollPane(tableUsers), BorderLayout.CENTER);

        // Input Form Panel at Bottom
        JPanel panelForm = new JPanel(new GridLayout(3, 3, 8, 8));
        panelForm.setBorder(BorderFactory.createTitledBorder("Manage Staff Accounts"));

        panelForm.add(new JLabel("Username:"));
        txtUsername = new JTextField();
        panelForm.add(txtUsername);

        panelForm.add(new JLabel("Password:"));
        txtPassword = new JTextField();
        panelForm.add(txtPassword);

        panelForm.add(new JLabel("Full Name:"));
        txtFullName = new JTextField();
        panelForm.add(txtFullName);

        panelForm.add(new JLabel("Role:"));
        cmbRole = new JComboBox<>(new String[]{"Cashier", "Admin"});
        panelForm.add(cmbRole);

        // Buttons Panel
        JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JButton btnAdd = new JButton("Add User");
        JButton btnDelete = new JButton("Delete Selected");
        JButton btnClear = new JButton("Clear");

        panelButtons.add(btnAdd);
        panelButtons.add(btnDelete);
        panelButtons.add(btnClear);

        panelForm.add(panelButtons);

        add(panelForm, BorderLayout.SOUTH);

        // Table Row Selection
        tableUsers.getSelectionModel().addListSelectionListener(e -> {
            int row = tableUsers.getSelectedRow();
            if (row != -1) {
                selectedUserId = (int) tableModel.getValueAt(row, 0);
                txtUsername.setText(tableModel.getValueAt(row, 1).toString());
                txtFullName.setText(tableModel.getValueAt(row, 2).toString());
                cmbRole.setSelectedItem(tableModel.getValueAt(row, 3).toString());
                txtPassword.setText(""); // Leave password blank for security on selection
            }
        });

        // Button Actions
        btnAdd.addActionListener(e -> addUser());
        btnDelete.addActionListener(e -> deleteUser());
        btnClear.addActionListener(e -> clearForm());
    }

    private void loadUserData() {
        tableModel.setRowCount(0);
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT user_id, username, full_name, role FROM users")) {

            while (rs.next()) {
                Object[] row = {
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("role")
                };
                tableModel.addRow(row);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading users: " + ex.getMessage());
        }
    }

    private void addUser() {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "INSERT INTO users (username, password, full_name, role) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, txtUsername.getText().trim());
            stmt.setString(2, txtPassword.getText().trim());
            stmt.setString(3, txtFullName.getText().trim());
            stmt.setString(4, cmbRole.getSelectedItem().toString());

            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "User added successfully!");
            loadUserData();
            clearForm();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error adding user: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteUser() {
        if (selectedUserId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user from the table to delete.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this user account?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DBConnection.getConnection()) {
                String sql = "DELETE FROM users WHERE user_id=?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, selectedUserId);

                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "User deleted successfully!");
                loadUserData();
                clearForm();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error deleting user: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearForm() {
        txtUsername.setText("");
        txtPassword.setText("");
        txtFullName.setText("");
        cmbRole.setSelectedIndex(0);
        selectedUserId = -1;
        tableUsers.clearSelection();
    }
}