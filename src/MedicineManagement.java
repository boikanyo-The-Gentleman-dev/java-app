import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.sql.*;

public class MedicineManagement extends JFrame {
    private JTable tableMedicines;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JTextField txtName, txtCompany, txtType, txtPrice, txtStock, txtReorder, txtExpiry, txtSearch;
    private int selectedMedicineId = -1;

    public MedicineManagement() {
        setTitle("PIMS - Medicine Inventory Management");
        setSize(950, 650);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Top Search Panel
        JPanel panelSearch = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panelSearch.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
        panelSearch.add(new JLabel("Search Inventory:"));
        txtSearch = new JTextField(25);
        panelSearch.add(txtSearch);
        add(panelSearch, BorderLayout.NORTH);

        // Table Setup
        String[] columns = {"ID", "Name", "Company", "Type", "Price", "Stock", "Reorder", "Expiry"};
        tableModel = new DefaultTableModel(columns, 0);
        tableMedicines = new JTable(tableModel);

        // Initialize RowSorter for live filtering
        rowSorter = new TableRowSorter<>(tableModel);
        tableMedicines.setRowSorter(rowSorter);

        loadMedicineData();

        add(new JScrollPane(tableMedicines), BorderLayout.CENTER);

        // Input & Action Form Panel at Bottom
        JPanel panelForm = new JPanel(new GridLayout(4, 4, 8, 8));
        panelForm.setBorder(BorderFactory.createTitledBorder("Manage / Edit Medicine"));

        panelForm.add(new JLabel("Name:"));
        txtName = new JTextField();
        panelForm.add(txtName);

        panelForm.add(new JLabel("Company:"));
        txtCompany = new JTextField();
        panelForm.add(txtCompany);

        panelForm.add(new JLabel("Type (Tablet/Syrup):"));
        txtType = new JTextField();
        panelForm.add(txtType);

        panelForm.add(new JLabel("Price:"));
        txtPrice = new JTextField();
        panelForm.add(txtPrice);

        panelForm.add(new JLabel("Stock Quantity:"));
        txtStock = new JTextField();
        panelForm.add(txtStock);

        panelForm.add(new JLabel("Reorder Level:"));
        txtReorder = new JTextField();
        panelForm.add(txtReorder);

        panelForm.add(new JLabel("Expiry (YYYY-MM-DD):"));
        txtExpiry = new JTextField();
        panelForm.add(txtExpiry);

        // Buttons Panel inside the form
        JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JButton btnAdd = new JButton("Add New");
        JButton btnUpdate = new JButton("Update Selected");
        JButton btnDelete = new JButton("Delete Selected");
        JButton btnClear = new JButton("Clear");

        panelButtons.add(btnAdd);
        panelButtons.add(btnUpdate);
        panelButtons.add(btnDelete);
        panelButtons.add(btnClear);

        panelForm.add(panelButtons);
        add(panelForm, BorderLayout.SOUTH);

        // Live Search Document Listener
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterTable(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterTable(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterTable(); }

            private void filterTable() {
                String text = txtSearch.getText().trim();
                if (text.length() == 0) {
                    rowSorter.setRowFilter(null);
                } else {
                    // (?i) makes the search case-insensitive
                    rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                }
            }
        });

        // Table Row Click Event to populate form
        tableMedicines.getSelectionModel().addListSelectionListener(e -> {
            int row = tableMedicines.getSelectedRow();
            if (row != -1) {
                // Convert view index to model index in case the table is filtered/sorted
                int modelRow = tableMedicines.convertRowIndexToModel(row);
                selectedMedicineId = (int) tableModel.getValueAt(modelRow, 0);
                txtName.setText(tableModel.getValueAt(modelRow, 1).toString());
                txtCompany.setText(tableModel.getValueAt(modelRow, 2).toString());
                txtType.setText(tableModel.getValueAt(modelRow, 3).toString());
                txtPrice.setText(tableModel.getValueAt(modelRow, 4).toString());
                txtStock.setText(tableModel.getValueAt(modelRow, 5).toString());
                txtReorder.setText(tableModel.getValueAt(modelRow, 6).toString());
                txtExpiry.setText(tableModel.getValueAt(modelRow, 7).toString());
            }
        });

        // Button Actions
        btnAdd.addActionListener(e -> addMedicine());
        btnUpdate.addActionListener(e -> updateMedicine());
        btnDelete.addActionListener(e -> deleteMedicine());
        btnClear.addActionListener(e -> clearForm());
    }

    private void loadMedicineData() {
        tableModel.setRowCount(0);
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM medicines")) {

            while (rs.next()) {
                Object[] row = {
                        rs.getInt("medicine_id"),
                        rs.getString("name"),
                        rs.getString("company"),
                        rs.getString("medicine_type"),
                        rs.getDouble("price"),
                        rs.getInt("quantity_in_stock"),
                        rs.getInt("reorder_level"),
                        rs.getDate("expiry_date")
                };
                tableModel.addRow(row);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading medicines: " + ex.getMessage());
        }
    }

    private void addMedicine() {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "INSERT INTO medicines (name, company, medicine_type, price, quantity_in_stock, reorder_level, expiry_date, supplier_id) VALUES (?, ?, ?, ?, ?, ?, ?, 1)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, txtName.getText().trim());
            stmt.setString(2, txtCompany.getText().trim());
            stmt.setString(3, txtType.getText().trim());
            stmt.setDouble(4, Double.parseDouble(txtPrice.getText().trim()));
            stmt.setInt(5, Integer.parseInt(txtStock.getText().trim()));
            stmt.setInt(6, Integer.parseInt(txtReorder.getText().trim()));
            stmt.setString(7, txtExpiry.getText().trim());

            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Medicine added successfully!");
            loadMedicineData();
            clearForm();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error adding medicine: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateMedicine() {
        if (selectedMedicineId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a medicine from the table to update.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "UPDATE medicines SET name=?, company=?, medicine_type=?, price=?, quantity_in_stock=?, reorder_level=?, expiry_date=? WHERE medicine_id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, txtName.getText().trim());
            stmt.setString(2, txtCompany.getText().trim());
            stmt.setString(3, txtType.getText().trim());
            stmt.setDouble(4, Double.parseDouble(txtPrice.getText().trim()));
            stmt.setInt(5, Integer.parseInt(txtStock.getText().trim()));
            stmt.setInt(6, Integer.parseInt(txtReorder.getText().trim()));
            stmt.setString(7, txtExpiry.getText().trim());
            stmt.setInt(8, selectedMedicineId);

            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Medicine updated successfully!");
            loadMedicineData();
            clearForm();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error updating medicine: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteMedicine() {
        if (selectedMedicineId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a medicine from the table to delete.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this medicine?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DBConnection.getConnection()) {
                String sql = "DELETE FROM medicines WHERE medicine_id=?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, selectedMedicineId);

                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Medicine deleted successfully!");
                loadMedicineData();
                clearForm();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error deleting medicine: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearForm() {
        txtName.setText("");
        txtCompany.setText("");
        txtType.setText("");
        txtPrice.setText("");
        txtStock.setText("");
        txtReorder.setText("");
        txtExpiry.setText("");
        selectedMedicineId = -1;
        tableMedicines.clearSelection();
    }
}