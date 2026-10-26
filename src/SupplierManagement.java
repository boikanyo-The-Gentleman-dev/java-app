import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class SupplierManagement extends JFrame {
    private final DefaultTableModel tableModel = Theme.model("ID", "Supplier Name", "Phone", "Email", "Address");
    private final JTable tableSuppliers = new JTable(tableModel);
    private final JTextField txtName = Theme.field(15);
    private final JTextField txtPhone = Theme.field(15);
    private final JTextField txtEmail = Theme.field(15);
    private final JTextField txtAddress = Theme.field(15);
    private int selectedId = -1;

    public SupplierManagement() {
        setTitle("PIMS - Supplier Management");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        setContentPane(root);
        root.add(Theme.header("Suppliers", "Add, edit and remove medicine suppliers"), BorderLayout.NORTH);

        JPanel body = Theme.body();
        root.add(body, BorderLayout.CENTER);

        Theme.styleTable(tableSuppliers);
        body.add(Theme.scroll(tableSuppliers), BorderLayout.CENTER);

        // Form
        JPanel form = Theme.card("Supplier Details");
        form.setLayout(new BorderLayout(0, 8));
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        Theme.addField(fields, "Supplier Name:", txtName, 0, 0);
        Theme.addField(fields, "Phone:", txtPhone, 1, 0);
        Theme.addField(fields, "Email:", txtEmail, 0, 1);
        Theme.addField(fields, "Address:", txtAddress, 1, 1);
        form.add(fields, BorderLayout.CENTER);

        JPanel buttons = Theme.buttonRow();
        JButton btnAdd = Theme.button("Add New", Theme.Kind.PRIMARY);
        JButton btnUpdate = Theme.button("Update Selected", Theme.Kind.SUCCESS);
        JButton btnDelete = Theme.button("Delete Selected", Theme.Kind.DANGER);
        JButton btnClear = Theme.button("Clear", Theme.Kind.NEUTRAL);
        buttons.add(btnAdd);
        buttons.add(btnUpdate);
        buttons.add(btnDelete);
        buttons.add(btnClear);
        form.add(buttons, BorderLayout.SOUTH);
        body.add(form, BorderLayout.SOUTH);

        loadSupplierData();

        tableSuppliers.getSelectionModel().addListSelectionListener(e -> {
            int row = tableSuppliers.getSelectedRow();
            if (e.getValueIsAdjusting() || row == -1) return;
            selectedId = (int) tableModel.getValueAt(row, 0);
            txtName.setText(Theme.str(tableModel.getValueAt(row, 1)));
            txtPhone.setText(Theme.str(tableModel.getValueAt(row, 2)));
            txtEmail.setText(Theme.str(tableModel.getValueAt(row, 3)));
            txtAddress.setText(Theme.str(tableModel.getValueAt(row, 4)));
        });

        btnAdd.addActionListener(e -> addSupplier());
        btnUpdate.addActionListener(e -> updateSupplier());
        btnDelete.addActionListener(e -> deleteSupplier());
        btnClear.addActionListener(e -> clearForm());
    }

    private void loadSupplierData() {
        tableModel.setRowCount(0);
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM suppliers ORDER BY name")) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("supplier_id"), rs.getString("name"), rs.getString("phone"),
                        rs.getString("email"), rs.getString("address")});
            }
        } catch (SQLException ex) {
            Theme.error(this, "Error loading suppliers: " + ex.getMessage());
        }
    }

    private boolean validateForm() {
        if (txtName.getText().trim().isEmpty()) {
            Theme.warn(this, "Supplier name is required.");
            return false;
        }
        String email = txtEmail.getText().trim();
        if (!email.isEmpty() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            Theme.warn(this, "Please enter a valid email address.");
            return false;
        }
        return true;
    }

    private void addSupplier() {
        if (!validateForm()) return;
        String sql = "INSERT INTO suppliers (name, phone, email, address) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bind(stmt);
            stmt.executeUpdate();
            Theme.info(this, "Supplier added successfully!");
            loadSupplierData();
            clearForm();
        } catch (SQLException ex) {
            Theme.error(this, "Error adding supplier: " + ex.getMessage());
        }
    }

    private void updateSupplier() {
        if (selectedId == -1) {
            Theme.warn(this, "Please select a supplier from the table to update.");
            return;
        }
        if (!validateForm()) return;
        String sql = "UPDATE suppliers SET name=?, phone=?, email=?, address=? WHERE supplier_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bind(stmt);
            stmt.setInt(5, selectedId);
            stmt.executeUpdate();
            Theme.info(this, "Supplier updated successfully!");
            loadSupplierData();
            clearForm();
        } catch (SQLException ex) {
            Theme.error(this, "Error updating supplier: " + ex.getMessage());
        }
    }

    private void deleteSupplier() {
        if (selectedId == -1) {
            Theme.warn(this, "Please select a supplier from the table to delete.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this supplier?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM suppliers WHERE supplier_id=?")) {
            stmt.setInt(1, selectedId);
            stmt.executeUpdate();
            Theme.info(this, "Supplier deleted successfully!");
            loadSupplierData();
            clearForm();
        } catch (SQLIntegrityConstraintViolationException ex) {
            Theme.warn(this, "This supplier is linked to medicines and can't be deleted.\n"
                    + "Reassign those medicines to another supplier first.");
        } catch (SQLException ex) {
            Theme.error(this, "Error deleting supplier: " + ex.getMessage());
        }
    }

    private void bind(PreparedStatement stmt) throws SQLException {
        stmt.setString(1, txtName.getText().trim());
        stmt.setString(2, txtPhone.getText().trim());
        stmt.setString(3, txtEmail.getText().trim());
        stmt.setString(4, txtAddress.getText().trim());
    }

    private void clearForm() {
        selectedId = -1;
        txtName.setText("");
        txtPhone.setText("");
        txtEmail.setText("");
        txtAddress.setText("");
        tableSuppliers.clearSelection();
    }
}