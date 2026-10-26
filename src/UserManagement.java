import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class UserManagement extends JFrame {
    private final int currentUserId;
    private final DefaultTableModel tableModel = Theme.model("User ID", "Username", "Full Name", "Role");
    private final JTable tableUsers = new JTable(tableModel);
    private final JTextField txtUsername = Theme.field(15);
    private final JPasswordField txtPassword = Theme.passwordField(15);
    private final JTextField txtFullName = Theme.field(15);
    private final JComboBox<String> cmbRole = new JComboBox<>(new String[]{"Cashier", "Admin"});
    private int selectedUserId = -1;
    private String selectedRole = "";

    public UserManagement(int currentUserId) {
        this.currentUserId = currentUserId;
        setTitle("PIMS - Staff & User Management");
        setSize(900, 620);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        setContentPane(root);
        root.add(Theme.header("Staff Accounts", "Create and manage staff logins"), BorderLayout.NORTH);

        JPanel body = Theme.body();
        root.add(body, BorderLayout.CENTER);

        Theme.styleTable(tableUsers);
        body.add(Theme.scroll(tableUsers), BorderLayout.CENTER);

        Theme.styleCombo(cmbRole);
        JPanel form = Theme.card("Account Details");
        form.setLayout(new BorderLayout(0, 8));
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        Theme.addField(fields, "Username:", txtUsername, 0, 0);
        Theme.addField(fields, "Password:", txtPassword, 1, 0);
        Theme.addField(fields, "Full Name:", txtFullName, 0, 1);
        Theme.addField(fields, "Role:", cmbRole, 1, 1);
        form.add(fields, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        JPanel buttons = Theme.buttonRow();
        JButton btnAdd = Theme.button("Add User", Theme.Kind.PRIMARY);
        JButton btnUpdate = Theme.button("Update Selected", Theme.Kind.SUCCESS);
        JButton btnDelete = Theme.button("Delete Selected", Theme.Kind.DANGER);
        JButton btnClear = Theme.button("Clear", Theme.Kind.NEUTRAL);
        buttons.add(btnAdd);
        buttons.add(btnUpdate);
        buttons.add(btnDelete);
        buttons.add(btnClear);
        south.add(buttons, BorderLayout.WEST);
        south.add(Theme.mutedLabel("When updating, leave Password blank to keep the current one."), BorderLayout.EAST);
        form.add(south, BorderLayout.SOUTH);
        body.add(form, BorderLayout.SOUTH);

        loadUserData();

        tableUsers.getSelectionModel().addListSelectionListener(e -> {
            int row = tableUsers.getSelectedRow();
            if (e.getValueIsAdjusting() || row == -1) return;
            selectedUserId = (int) tableModel.getValueAt(row, 0);
            txtUsername.setText(Theme.str(tableModel.getValueAt(row, 1)));
            txtFullName.setText(Theme.str(tableModel.getValueAt(row, 2)));
            selectedRole = Theme.str(tableModel.getValueAt(row, 3));
            cmbRole.setSelectedItem(selectedRole);
            txtPassword.setText("");
        });

        btnAdd.addActionListener(e -> addUser());
        btnUpdate.addActionListener(e -> updateUser());
        btnDelete.addActionListener(e -> deleteUser());
        btnClear.addActionListener(e -> clearForm());
    }

    private void loadUserData() {
        tableModel.setRowCount(0);
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT user_id, username, full_name, role FROM users ORDER BY username")) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{rs.getInt("user_id"), rs.getString("username"),
                        rs.getString("full_name"), rs.getString("role")});
            }
        } catch (SQLException ex) {
            Theme.error(this, "Error loading users: " + ex.getMessage());
        }
    }

    private boolean validateCommon() {
        if (txtUsername.getText().trim().isEmpty() || txtFullName.getText().trim().isEmpty()) {
            Theme.warn(this, "Username and full name are required.");
            return false;
        }
        return true;
    }

    private boolean usernameTaken(Connection conn, String username, int excludeId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM users WHERE username = ? AND user_id <> ?")) {
            ps.setString(1, username);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /** True if no OTHER admin exists besides this user. */
    private boolean isOnlyAdmin(Connection conn, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM users WHERE role = 'Admin' AND user_id <> ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) == 0;
            }
        }
    }

    private void addUser() {
        if (!validateCommon()) return;
        String password = new String(txtPassword.getPassword()).trim();
        if (password.length() < 6) {
            Theme.warn(this, "Password must be at least 6 characters.");
            return;
        }
        String username = txtUsername.getText().trim();

        try (Connection conn = DBConnection.getConnection()) {
            if (usernameTaken(conn, username, -1)) {
                Theme.warn(this, "That username already exists.");
                return;
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO users (username, password, full_name, role) VALUES (?, ?, ?, ?)")) {
                stmt.setString(1, username);
                stmt.setString(2, PasswordUtil.hash(password));
                stmt.setString(3, txtFullName.getText().trim());
                stmt.setString(4, cmbRole.getSelectedItem().toString());
                stmt.executeUpdate();
            }
            Theme.info(this, "User added successfully!");
            loadUserData();
            clearForm();
        } catch (SQLException ex) {
            Theme.error(this, "Error adding user: " + ex.getMessage());
        }
    }

    private void updateUser() {
        if (selectedUserId == -1) {
            Theme.warn(this, "Please select a user from the table to update.");
            return;
        }
        if (!validateCommon()) return;
        String username = txtUsername.getText().trim();
        String role = cmbRole.getSelectedItem().toString();
        String password = new String(txtPassword.getPassword()).trim();
        if (!password.isEmpty() && password.length() < 6) {
            Theme.warn(this, "Password must be at least 6 characters.");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            if (usernameTaken(conn, username, selectedUserId)) {
                Theme.warn(this, "That username already exists.");
                return;
            }
            boolean demoting = "Admin".equalsIgnoreCase(selectedRole) && !"Admin".equalsIgnoreCase(role);
            if (demoting && selectedUserId == currentUserId) {
                Theme.warn(this, "You can't remove your own Admin role.");
                return;
            }
            if (demoting && isOnlyAdmin(conn, selectedUserId)) {
                Theme.warn(this, "This is the last Admin account, so its role can't be changed.");
                return;
            }

            String sql = password.isEmpty()
                    ? "UPDATE users SET username=?, full_name=?, role=? WHERE user_id=?"
                    : "UPDATE users SET username=?, full_name=?, role=?, password=? WHERE user_id=?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, txtFullName.getText().trim());
                stmt.setString(3, role);
                if (password.isEmpty()) {
                    stmt.setInt(4, selectedUserId);
                } else {
                    stmt.setString(4, PasswordUtil.hash(password));
                    stmt.setInt(5, selectedUserId);
                }
                stmt.executeUpdate();
            }
            Theme.info(this, "User updated successfully!");
            loadUserData();
            clearForm();
        } catch (SQLException ex) {
            Theme.error(this, "Error updating user: " + ex.getMessage());
        }
    }

    private void deleteUser() {
        if (selectedUserId == -1) {
            Theme.warn(this, "Please select a user from the table to delete.");
            return;
        }
        if (selectedUserId == currentUserId) {
            Theme.warn(this, "You can't delete the account you are logged in with.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this user account?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DBConnection.getConnection()) {
            if ("Admin".equalsIgnoreCase(selectedRole) && isOnlyAdmin(conn, selectedUserId)) {
                Theme.warn(this, "This is the last Admin account and can't be deleted.");
                return;
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE user_id=?")) {
                stmt.setInt(1, selectedUserId);
                stmt.executeUpdate();
            }
            Theme.info(this, "User deleted successfully!");
            loadUserData();
            clearForm();
        } catch (SQLIntegrityConstraintViolationException ex) {
            Theme.warn(this, "This user has recorded sales and can't be deleted.");
        } catch (SQLException ex) {
            Theme.error(this, "Error deleting user: " + ex.getMessage());
        }
    }

    private void clearForm() {
        selectedUserId = -1;
        selectedRole = "";
        txtUsername.setText("");
        txtPassword.setText("");
        txtFullName.setText("");
        cmbRole.setSelectedIndex(0);
        tableUsers.clearSelection();
    }
}