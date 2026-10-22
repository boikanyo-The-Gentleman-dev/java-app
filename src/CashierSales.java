import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class CashierSales extends JFrame {
    private JTable tableCart;
    private DefaultTableModel tableModel;
    private JTextField txtMedicineId, txtQuantity;
    private JLabel lblTotal;
    private double grandTotal = 0.0;

    public CashierSales(String cashierName) {
        setTitle("PIMS - Point of Sale (Cashier: " + cashierName + ")");
        setSize(850, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Top Banner
        JLabel lblHeader = new JLabel("Pharmacy Sales Terminal", JLabel.CENTER);
        lblHeader.setFont(new Font("Arial", Font.BOLD, 18));
        lblHeader.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(lblHeader, BorderLayout.NORTH);

        // Cart Table Setup
        String[] columns = {"Medicine ID", "Medicine Name", "Price", "Quantity", "Subtotal"};
        tableModel = new DefaultTableModel(columns, 0);
        tableCart = new JTable(tableModel);
        add(new JScrollPane(tableCart), BorderLayout.CENTER);

        // Bottom Controls Panel
        JPanel panelBottom = new JPanel(new GridLayout(2, 1, 5, 5));

        // Input Panel
        JPanel panelInput = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panelInput.add(new JLabel("Medicine ID:"));
        txtMedicineId = new JTextField(5);
        panelInput.add(txtMedicineId);

        panelInput.add(new JLabel("Quantity:"));
        txtQuantity = new JTextField(5);
        panelInput.add(txtQuantity);

        JButton btnAddToCart = new JButton("Add to Cart");
        panelInput.add(btnAddToCart);
        panelBottom.add(panelInput);

        // Checkout & Total Panel
        JPanel panelCheckout = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        lblTotal = new JLabel("Grand Total: $0.00");
        lblTotal.setFont(new Font("Arial", Font.BOLD, 16));
        panelCheckout.add(lblTotal);

        JButton btnCheckout = new JButton("Complete Sale");
        btnCheckout.setBackground(new Color(46, 204, 113));
        btnCheckout.setForeground(Color.WHITE);
        panelCheckout.add(btnCheckout);

        panelBottom.add(panelCheckout);
        add(panelBottom, BorderLayout.SOUTH);

        // Button Actions
        btnAddToCart.addActionListener(e -> addToCart());
        btnCheckout.addActionListener(e -> completeSale());
    }

    private void addToCart() {
        try {
            int medId = Integer.parseInt(txtMedicineId.getText().trim());
            int qty = Integer.parseInt(txtQuantity.getText().trim());

            try (Connection conn = DBConnection.getConnection()) {
                String sql = "SELECT name, price, quantity_in_stock FROM medicines WHERE medicine_id = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, medId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String name = rs.getString("name");
                    double price = rs.getDouble("price");
                    int stock = rs.getInt("quantity_in_stock");

                    if (qty > stock) {
                        JOptionPane.showMessageDialog(this, "Insufficient stock! Available: " + stock, "Warning", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    double subtotal = price * qty;
                    grandTotal += subtotal;

                    Object[] row = {medId, name, price, qty, subtotal};
                    tableModel.addRow(row);
                    lblTotal.setText(String.format("Grand Total: $%.2f", grandTotal));

                    txtMedicineId.setText("");
                    txtQuantity.setText("");
                } else {
                    JOptionPane.showMessageDialog(this, "Medicine ID not found!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for ID and Quantity.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void completeSale() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Cart is empty!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            // 1. Insert into sales table (Assuming user_id = 1 for now or a general sale record)
            String saleSql = "INSERT INTO sales (total_amount, user_id) VALUES (?, 1)";
            PreparedStatement saleStmt = conn.prepareStatement(saleSql, Statement.RETURN_GENERATED_KEYS);
            saleStmt.setDouble(1, grandTotal);
            saleStmt.executeUpdate();

            ResultSet rsKeys = saleStmt.getGeneratedKeys();
            int saleId = 0;
            if (rsKeys.next()) {
                saleId = rsKeys.getInt(1);
            }

            // 2. Insert items and update stock
            String itemSql = "INSERT INTO sale_items (sale_id, medicine_id, quantity, subtotal) VALUES (?, ?, ?, ?)";
            String updateStockSql = "UPDATE medicines SET quantity_in_stock = quantity_in_stock - ? WHERE medicine_id = ?";

            PreparedStatement itemStmt = conn.prepareStatement(itemSql);
            PreparedStatement updateStockStmt = conn.prepareStatement(updateStockSql);

            for (int i = 0; i < tableModel.getRowCount(); i++) {
                int medId = (int) tableModel.getValueAt(i, 0);
                int qty = (int) tableModel.getValueAt(i, 3);
                double subtotal = (double) tableModel.getValueAt(i, 4);

                // Insert sale item
                itemStmt.setInt(1, saleId);
                itemStmt.setInt(2, medId);
                itemStmt.setInt(3, qty);
                itemStmt.setDouble(4, subtotal);
                itemStmt.executeUpdate();

                // Deduct stock
                updateStockStmt.setInt(1, qty);
                updateStockStmt.setInt(2, medId);
                updateStockStmt.executeUpdate();
            }

            conn.commit(); // Commit transaction
            JOptionPane.showMessageDialog(this, "Sale completed successfully! Receipt generated.");

            // Clear cart
            tableModel.setRowCount(0);
            grandTotal = 0.0;
            lblTotal.setText("Grand Total: $0.00");

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error completing sale: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}