import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class CashierSales extends JFrame {
    private final int userId;
    private final String cashierName;

    // Products (left)
    private final DefaultTableModel productModel = Theme.model("ID", "Medicine", "Price", "Stock", "Expiry");
    private final JTable tableProducts = new JTable(productModel);
    private final TableRowSorter<DefaultTableModel> productSorter = new TableRowSorter<>(productModel);
    private final JTextField txtSearch = Theme.field(18);

    // Cart (right)
    private final DefaultTableModel cartModel = Theme.model("ID", "Medicine", "Price", "Qty", "Subtotal");
    private final JTable tableCart = new JTable(cartModel);
    private final JTextField txtMedicineId = Theme.field(5);
    private final JTextField txtQuantity = Theme.field(4);
    private final JLabel lblTotal = new JLabel();
    private BigDecimal grandTotal = BigDecimal.ZERO;

    public CashierSales(int userId, String cashierName, boolean standalone) {
        this.userId = userId;
        this.cashierName = cashierName;

        setTitle("PIMS - Point of Sale (Cashier: " + cashierName + ")");
        setSize(1150, 680);
        setDefaultCloseOperation(standalone ? JFrame.EXIT_ON_CLOSE : JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        setContentPane(root);
        root.add(Theme.header("Pharmacy Sales Terminal", "Cashier: " + cashierName), BorderLayout.NORTH);

        JPanel body = Theme.body();
        root.add(body, BorderLayout.CENTER);

        // ----- left: product list -----
        JPanel left = Theme.card("Medicines");
        left.setLayout(new BorderLayout(0, 8));
        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        search.setOpaque(false);
        search.add(Theme.label("Search:"));
        search.add(txtSearch);
        left.add(search, BorderLayout.NORTH);

        Theme.styleTable(tableProducts, new Theme.RowRenderer() {
            @Override protected Color rowColor(JTable t, int row) {
                Object exp = productModel.getValueAt(row, 4);
                if (exp instanceof Date d && d.toLocalDate().isBefore(LocalDate.now())) return Theme.DANGER_ROW;
                int stock = ((Number) productModel.getValueAt(row, 3)).intValue();
                return stock <= 0 ? Theme.DANGER_ROW : null;
            }
        });
        tableProducts.setRowSorter(productSorter);
        left.add(Theme.scroll(tableProducts), BorderLayout.CENTER);

        // ----- right: cart -----
        JPanel right = Theme.card("Current Sale");
        right.setLayout(new BorderLayout(0, 8));

        JPanel input = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        input.setOpaque(false);
        input.add(Theme.label("Medicine ID:"));
        input.add(txtMedicineId);
        input.add(Theme.label("Quantity:"));
        input.add(txtQuantity);
        JButton btnAdd = Theme.button("Add to Cart", Theme.Kind.PRIMARY);
        input.add(btnAdd);
        right.add(input, BorderLayout.NORTH);

        Theme.styleTable(tableCart);
        right.add(Theme.scroll(tableCart), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JPanel leftButtons = Theme.buttonRow();
        JButton btnRemove = Theme.button("Remove Selected", Theme.Kind.NEUTRAL);
        JButton btnClearCart = Theme.button("Clear Cart", Theme.Kind.NEUTRAL);
        leftButtons.add(btnRemove);
        leftButtons.add(btnClearCart);
        if (standalone) {
            JButton btnLogout = Theme.button("Logout", Theme.Kind.DANGER);
            btnLogout.addActionListener(e -> {
                dispose();
                new LoginFrame().setVisible(true);
            });
            leftButtons.add(btnLogout);
        }
        bottom.add(leftButtons, BorderLayout.WEST);

        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        totalPanel.setOpaque(false);
        lblTotal.setFont(Theme.BASE.deriveFont(Font.BOLD, 18f));
        lblTotal.setForeground(Theme.PRIMARY_DARK);
        JButton btnCheckout = Theme.button("Complete Sale", Theme.Kind.SUCCESS);
        totalPanel.add(lblTotal);
        totalPanel.add(btnCheckout);
        bottom.add(totalPanel, BorderLayout.EAST);
        right.add(bottom, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.42);
        split.setDividerSize(12);
        split.setBorder(null);
        split.setBackground(Theme.BG);
        body.add(split, BorderLayout.CENTER);

        updateTotal();
        loadProducts();

        // ----- behaviour -----
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterProducts(); }
            @Override public void removeUpdate(DocumentEvent e) { filterProducts(); }
            @Override public void changedUpdate(DocumentEvent e) { filterProducts(); }
        });
        tableProducts.getSelectionModel().addListSelectionListener(e -> {
            int row = tableProducts.getSelectedRow();
            if (e.getValueIsAdjusting() || row == -1) return;
            txtMedicineId.setText(Theme.str(productModel.getValueAt(tableProducts.convertRowIndexToModel(row), 0)));
            txtQuantity.requestFocusInWindow();
        });
        txtMedicineId.addActionListener(e -> txtQuantity.requestFocusInWindow());
        txtQuantity.addActionListener(e -> addToCart());
        btnAdd.addActionListener(e -> addToCart());
        btnRemove.addActionListener(e -> removeSelected());
        btnClearCart.addActionListener(e -> clearCart(true));
        btnCheckout.addActionListener(e -> completeSale());
    }

    private void filterProducts() {
        String text = txtSearch.getText().trim();
        productSorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
    }

    private void loadProducts() {
        productModel.setRowCount(0);
        String sql = "SELECT medicine_id, name, price, quantity_in_stock, expiry_date FROM medicines ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                productModel.addRow(new Object[]{rs.getInt("medicine_id"), rs.getString("name"),
                        rs.getBigDecimal("price"), rs.getInt("quantity_in_stock"), rs.getDate("expiry_date")});
            }
        } catch (SQLException ex) {
            Theme.error(this, "Error loading medicines: " + ex.getMessage());
        }
    }

    private int findCartRow(int medId) {
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            if ((int) cartModel.getValueAt(i, 0) == medId) return i;
        }
        return -1;
    }

    private void addToCart() {
        int medId, qty;
        try {
            medId = Integer.parseInt(txtMedicineId.getText().trim());
            qty = Integer.parseInt(txtQuantity.getText().trim());
        } catch (NumberFormatException ex) {
            Theme.error(this, "Please enter valid numbers for ID and Quantity.");
            return;
        }
        if (qty <= 0) {
            Theme.warn(this, "Quantity must be at least 1.");
            return;
        }

        String sql = "SELECT name, price, quantity_in_stock, expiry_date FROM medicines WHERE medicine_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, medId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    Theme.error(this, "Medicine ID not found!");
                    return;
                }
                String name = rs.getString("name");
                BigDecimal price = rs.getBigDecimal("price");
                int stock = rs.getInt("quantity_in_stock");
                Date expiry = rs.getDate("expiry_date");

                if (expiry != null && expiry.toLocalDate().isBefore(LocalDate.now())) {
                    Theme.warn(this, name + " has expired (" + expiry + ") and can't be sold.");
                    return;
                }

                int existingRow = findCartRow(medId);
                int inCart = existingRow >= 0 ? (int) cartModel.getValueAt(existingRow, 3) : 0;
                if (inCart + qty > stock) {
                    Theme.warn(this, "Insufficient stock! Available: " + stock + ", already in cart: " + inCart);
                    return;
                }

                int newQty = inCart + qty;
                BigDecimal subtotal = price.multiply(BigDecimal.valueOf(newQty));
                if (existingRow >= 0) {
                    cartModel.setValueAt(newQty, existingRow, 3);
                    cartModel.setValueAt(subtotal, existingRow, 4);
                } else {
                    cartModel.addRow(new Object[]{medId, name, price, newQty, subtotal});
                }
                updateTotal();
                txtMedicineId.setText("");
                txtQuantity.setText("");
                txtMedicineId.requestFocusInWindow();
            }
        } catch (SQLException ex) {
            Theme.error(this, "Database error: " + ex.getMessage());
        }
    }

    private void removeSelected() {
        int row = tableCart.getSelectedRow();
        if (row == -1) {
            Theme.warn(this, "Select an item in the cart to remove.");
            return;
        }
        cartModel.removeRow(row);
        updateTotal();
    }

    private void clearCart(boolean confirm) {
        if (confirm && cartModel.getRowCount() > 0) {
            int c = JOptionPane.showConfirmDialog(this, "Remove all items from the cart?", "Clear Cart",
                    JOptionPane.YES_NO_OPTION);
            if (c != JOptionPane.YES_OPTION) return;
        }
        cartModel.setRowCount(0);
        updateTotal();
    }

    private void updateTotal() {
        grandTotal = BigDecimal.ZERO;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            grandTotal = grandTotal.add((BigDecimal) cartModel.getValueAt(i, 4));
        }
        lblTotal.setText("Grand Total: " + Theme.money(grandTotal));
    }

    private void completeSale() {
        if (cartModel.getRowCount() == 0) {
            Theme.warn(this, "Cart is empty!");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            int saleId = 0;
            try {
                // 1. Sale header
                try (PreparedStatement saleStmt = conn.prepareStatement(
                        "INSERT INTO sales (total_amount, user_id) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    saleStmt.setBigDecimal(1, grandTotal);
                    saleStmt.setInt(2, userId);
                    saleStmt.executeUpdate();
                    try (ResultSet keys = saleStmt.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Could not create the sale record.");
                        saleId = keys.getInt(1);
                    }
                }

                // 2. Items + stock (stock is only reduced if enough is still available)
                String itemSql = "INSERT INTO sale_items (sale_id, medicine_id, quantity, subtotal) VALUES (?, ?, ?, ?)";
                String stockSql = "UPDATE medicines SET quantity_in_stock = quantity_in_stock - ? "
                        + "WHERE medicine_id = ? AND quantity_in_stock >= ? "
                        + "AND (expiry_date IS NULL OR expiry_date >= CURDATE())";
                try (PreparedStatement itemStmt = conn.prepareStatement(itemSql);
                     PreparedStatement stockStmt = conn.prepareStatement(stockSql)) {
                    for (int i = 0; i < cartModel.getRowCount(); i++) {
                        int medId = (int) cartModel.getValueAt(i, 0);
                        String name = (String) cartModel.getValueAt(i, 1);
                        int qty = (int) cartModel.getValueAt(i, 3);
                        BigDecimal subtotal = (BigDecimal) cartModel.getValueAt(i, 4);

                        stockStmt.setInt(1, qty);
                        stockStmt.setInt(2, medId);
                        stockStmt.setInt(3, qty);
                        if (stockStmt.executeUpdate() == 0) {
                            throw new SQLException("Not enough stock (or item expired) for \"" + name
                                    + "\". Sale cancelled.");
                        }

                        itemStmt.setInt(1, saleId);
                        itemStmt.setInt(2, medId);
                        itemStmt.setInt(3, qty);
                        itemStmt.setBigDecimal(4, subtotal);
                        itemStmt.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }

            showReceipt(saleId);
            clearCart(false);
            loadProducts();
        } catch (SQLException ex) {
            Theme.error(this, "Error completing sale: " + ex.getMessage());
            loadProducts();
        }
    }

    private void showReceipt(int saleId) {
        StringBuilder sb = new StringBuilder();
        sb.append("        PIMS PHARMACY\n");
        sb.append(String.format("Receipt #%d%n", saleId));
        sb.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append('\n');
        sb.append("Cashier: ").append(cashierName).append('\n');
        sb.append("-".repeat(44)).append('\n');
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            sb.append(String.format("%-24.24s %3d x %7.2f = %9.2f%n",
                    cartModel.getValueAt(i, 1), cartModel.getValueAt(i, 3),
                    cartModel.getValueAt(i, 2), cartModel.getValueAt(i, 4)));
        }
        sb.append("-".repeat(44)).append('\n');
        sb.append(String.format("TOTAL: %s%n", Theme.money(grandTotal)));
        sb.append("\n     Thank you!\n");

        JTextArea area = new JTextArea(sb.toString());
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Sale completed", JOptionPane.PLAIN_MESSAGE);
    }
}