import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.regex.Pattern;

public class MedicineManagement extends JFrame {
    // Model column indexes (supplier id is kept in the model but hidden in the view)
    private static final int C_ID = 0, C_NAME = 1, C_COMPANY = 2, C_TYPE = 3, C_SUPPLIER_ID = 4,
            C_SUPPLIER = 5, C_PRICE = 6, C_STOCK = 7, C_REORDER = 8, C_EXPIRY = 9;

    private record SupplierItem(int id, String name) {
        @Override public String toString() { return name; }
    }

    private record Form(String name, String company, String type, BigDecimal price, int stock,
                        int reorder, Date expiry, int supplierId) { }

    private final DefaultTableModel tableModel = Theme.model("ID", "Name", "Company", "Type", "SupplierID",
            "Supplier", "Price", "Stock", "Reorder", "Expiry");
    private final JTable tableMedicines = new JTable(tableModel);
    private final TableRowSorter<DefaultTableModel> rowSorter = new TableRowSorter<>(tableModel);
    private final JTextField txtName = Theme.field(15);
    private final JTextField txtCompany = Theme.field(15);
    private final JComboBox<String> cmbType = new JComboBox<>(
            new String[]{"Tablet", "Capsule", "Syrup", "Injection", "Cream", "Drops", "Other"});
    private final JComboBox<SupplierItem> cmbSupplier = new JComboBox<>();
    private final JTextField txtPrice = Theme.field(15);
    private final JTextField txtStock = Theme.field(15);
    private final JTextField txtReorder = Theme.field(15);
    private final JTextField txtExpiry = Theme.field(15);
    private final JTextField txtSearch = Theme.field(25);
    private int selectedMedicineId = -1;

    public MedicineManagement() {
        setTitle("PIMS - Medicine Inventory Management");
        setSize(1050, 740);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        setContentPane(root);
        root.add(Theme.header("Medicine Inventory", "Red = expired  |  Amber = low stock or expires within 30 days"),
                BorderLayout.NORTH);

        JPanel body = Theme.body();
        root.add(body, BorderLayout.CENTER);

        // Search bar
        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        search.setOpaque(false);
        search.add(Theme.label("Search inventory:"));
        search.add(txtSearch);
        body.add(search, BorderLayout.NORTH);

        // Table
        Theme.styleTable(tableMedicines, new Theme.RowRenderer() {
            @Override protected Color rowColor(JTable t, int row) {
                Object exp = tableModel.getValueAt(row, C_EXPIRY);
                if (exp instanceof Date d) {
                    LocalDate ld = d.toLocalDate();
                    if (ld.isBefore(LocalDate.now())) return Theme.DANGER_ROW;
                    if (ld.isBefore(LocalDate.now().plusDays(30))) return Theme.WARN_ROW;
                }
                int stock = ((Number) tableModel.getValueAt(row, C_STOCK)).intValue();
                int reorder = ((Number) tableModel.getValueAt(row, C_REORDER)).intValue();
                return stock <= reorder ? Theme.WARN_ROW : null;
            }
        });
        tableMedicines.setRowSorter(rowSorter);
        tableMedicines.removeColumn(tableMedicines.getColumnModel().getColumn(C_SUPPLIER_ID));
        body.add(Theme.scroll(tableMedicines), BorderLayout.CENTER);

        // Form
        Theme.styleCombo(cmbType);
        cmbType.setEditable(true);
        Theme.styleCombo(cmbSupplier);
        JPanel form = Theme.card("Medicine Details");
        form.setLayout(new BorderLayout(0, 8));
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        Theme.addField(fields, "Name:", txtName, 0, 0);
        Theme.addField(fields, "Company:", txtCompany, 1, 0);
        Theme.addField(fields, "Type:", cmbType, 0, 1);
        Theme.addField(fields, "Supplier:", cmbSupplier, 1, 1);
        Theme.addField(fields, "Price:", txtPrice, 0, 2);
        Theme.addField(fields, "Stock Quantity:", txtStock, 1, 2);
        Theme.addField(fields, "Reorder Level:", txtReorder, 0, 3);
        Theme.addField(fields, "Expiry (YYYY-MM-DD):", txtExpiry, 1, 3);
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

        loadSuppliers();
        loadMedicineData();

        // Live search (text is quoted, so characters like ( or [ are safe)
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterTable(); }
            @Override public void removeUpdate(DocumentEvent e) { filterTable(); }
            @Override public void changedUpdate(DocumentEvent e) { filterTable(); }
        });

        // Row click fills the form
        tableMedicines.getSelectionModel().addListSelectionListener(e -> {
            int row = tableMedicines.getSelectedRow();
            if (e.getValueIsAdjusting() || row == -1) return;
            int m = tableMedicines.convertRowIndexToModel(row);
            selectedMedicineId = (int) tableModel.getValueAt(m, C_ID);
            txtName.setText(Theme.str(tableModel.getValueAt(m, C_NAME)));
            txtCompany.setText(Theme.str(tableModel.getValueAt(m, C_COMPANY)));
            cmbType.setSelectedItem(Theme.str(tableModel.getValueAt(m, C_TYPE)));
            Object sid = tableModel.getValueAt(m, C_SUPPLIER_ID);
            if (sid != null) selectSupplier(((Number) sid).intValue());
            txtPrice.setText(Theme.str(tableModel.getValueAt(m, C_PRICE)));
            txtStock.setText(Theme.str(tableModel.getValueAt(m, C_STOCK)));
            txtReorder.setText(Theme.str(tableModel.getValueAt(m, C_REORDER)));
            txtExpiry.setText(Theme.str(tableModel.getValueAt(m, C_EXPIRY)));
        });

        // Pick up suppliers added in the Supplier window
        addWindowListener(new WindowAdapter() {
            @Override public void windowActivated(WindowEvent e) { loadSuppliers(); }
        });

        btnAdd.addActionListener(e -> addMedicine());
        btnUpdate.addActionListener(e -> updateMedicine());
        btnDelete.addActionListener(e -> deleteMedicine());
        btnClear.addActionListener(e -> clearForm());
    }

    private void filterTable() {
        String text = txtSearch.getText().trim();
        rowSorter.setRowFilter(text.isEmpty() ? null
                : RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
    }

    private void loadSuppliers() {
        SupplierItem selected = (SupplierItem) cmbSupplier.getSelectedItem();
        cmbSupplier.removeAllItems();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT supplier_id, name FROM suppliers ORDER BY name")) {
            while (rs.next()) {
                SupplierItem item = new SupplierItem(rs.getInt("supplier_id"), rs.getString("name"));
                cmbSupplier.addItem(item);
                if (selected != null && selected.id() == item.id()) cmbSupplier.setSelectedItem(item);
            }
        } catch (SQLException ex) {
            Theme.error(this, "Error loading suppliers: " + ex.getMessage());
        }
    }

    private void selectSupplier(int id) {
        for (int i = 0; i < cmbSupplier.getItemCount(); i++) {
            if (cmbSupplier.getItemAt(i).id() == id) {
                cmbSupplier.setSelectedIndex(i);
                return;
            }
        }
    }

    private void loadMedicineData() {
        tableModel.setRowCount(0);
        String sql = "SELECT m.medicine_id, m.name, m.company, m.medicine_type, m.supplier_id, "
                + "s.name AS supplier_name, m.price, m.quantity_in_stock, m.reorder_level, m.expiry_date "
                + "FROM medicines m LEFT JOIN suppliers s ON s.supplier_id = m.supplier_id ORDER BY m.name";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("medicine_id"), rs.getString("name"), rs.getString("company"),
                        rs.getString("medicine_type"), rs.getObject("supplier_id"), rs.getString("supplier_name"),
                        rs.getBigDecimal("price"), rs.getInt("quantity_in_stock"), rs.getInt("reorder_level"),
                        rs.getDate("expiry_date")});
            }
        } catch (SQLException ex) {
            Theme.error(this, "Error loading medicines: " + ex.getMessage());
        }
    }

    /** Validates the form; shows a message and returns null if something is wrong. */
    private Form readForm() {
        String name = txtName.getText().trim();
        if (name.isEmpty()) { Theme.warn(this, "Medicine name is required."); return null; }

        SupplierItem supplier = (SupplierItem) cmbSupplier.getSelectedItem();
        if (supplier == null) { Theme.warn(this, "Please add a supplier first, then select it."); return null; }

        BigDecimal price;
        try {
            price = new BigDecimal(txtPrice.getText().trim());
        } catch (NumberFormatException e) {
            Theme.warn(this, "Price must be a number, e.g. 12.50");
            return null;
        }
        if (price.signum() < 0) { Theme.warn(this, "Price can't be negative."); return null; }

        int stock = parseNonNegative(txtStock, "Stock quantity");
        if (stock < 0) return null;
        int reorder = parseNonNegative(txtReorder, "Reorder level");
        if (reorder < 0) return null;

        Date expiry;
        try {
            expiry = Date.valueOf(txtExpiry.getText().trim());
        } catch (IllegalArgumentException e) {
            Theme.warn(this, "Expiry date must be in the format YYYY-MM-DD, e.g. 2027-03-31");
            return null;
        }

        Object typeObj = cmbType.getSelectedItem();
        String type = typeObj == null ? "" : typeObj.toString().trim();
        if (type.isEmpty()) { Theme.warn(this, "Medicine type is required."); return null; }

        return new Form(name, txtCompany.getText().trim(), type, price, stock, reorder, expiry, supplier.id());
    }

    /** Returns the number, or -1 after showing a message. */
    private int parseNonNegative(JTextField f, String label) {
        try {
            int v = Integer.parseInt(f.getText().trim());
            if (v >= 0) return v;
        } catch (NumberFormatException ignored) { }
        Theme.warn(this, label + " must be a whole number of 0 or more.");
        return -1;
    }

    private void bind(PreparedStatement s, Form f) throws SQLException {
        s.setString(1, f.name());
        s.setString(2, f.company());
        s.setString(3, f.type());
        s.setBigDecimal(4, f.price());
        s.setInt(5, f.stock());
        s.setInt(6, f.reorder());
        s.setDate(7, f.expiry());
        s.setInt(8, f.supplierId());
    }

    private void addMedicine() {
        Form f = readForm();
        if (f == null) return;
        String sql = "INSERT INTO medicines (name, company, medicine_type, price, quantity_in_stock, "
                + "reorder_level, expiry_date, supplier_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bind(stmt, f);
            stmt.executeUpdate();
            Theme.info(this, "Medicine added successfully!");
            loadMedicineData();
            clearForm();
        } catch (SQLException ex) {
            Theme.error(this, "Error adding medicine: " + ex.getMessage());
        }
    }

    private void updateMedicine() {
        if (selectedMedicineId == -1) {
            Theme.warn(this, "Please select a medicine from the table to update.");
            return;
        }
        Form f = readForm();
        if (f == null) return;
        String sql = "UPDATE medicines SET name=?, company=?, medicine_type=?, price=?, quantity_in_stock=?, "
                + "reorder_level=?, expiry_date=?, supplier_id=? WHERE medicine_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bind(stmt, f);
            stmt.setInt(9, selectedMedicineId);
            stmt.executeUpdate();
            Theme.info(this, "Medicine updated successfully!");
            loadMedicineData();
            clearForm();
        } catch (SQLException ex) {
            Theme.error(this, "Error updating medicine: " + ex.getMessage());
        }
    }

    private void deleteMedicine() {
        if (selectedMedicineId == -1) {
            Theme.warn(this, "Please select a medicine from the table to delete.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this medicine?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM medicines WHERE medicine_id=?")) {
            stmt.setInt(1, selectedMedicineId);
            stmt.executeUpdate();
            Theme.info(this, "Medicine deleted successfully!");
            loadMedicineData();
            clearForm();
        } catch (SQLIntegrityConstraintViolationException ex) {
            Theme.warn(this, "This medicine appears in past sales and can't be deleted.\n"
                    + "Set its stock to 0 instead.");
        } catch (SQLException ex) {
            Theme.error(this, "Error deleting medicine: " + ex.getMessage());
        }
    }

    private void clearForm() {
        selectedMedicineId = -1;
        txtName.setText("");
        txtCompany.setText("");
        cmbType.setSelectedIndex(0);
        txtPrice.setText("");
        txtStock.setText("");
        txtReorder.setText("");
        txtExpiry.setText("");
        tableMedicines.clearSelection();
    }
}