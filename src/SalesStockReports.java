import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class SalesStockReports extends JFrame {
    private JTable tableSales, tableLowStock;
    private DefaultTableModel salesModel, lowStockModel;

    public SalesStockReports() {
        setTitle("PIMS - Sales & Stock Reports");
        setSize(850, 550);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Tabbed Pane for separate reports
        JTabbedPane tabbedPane = new JTabbedPane();

        // Tab 1: Sales History
        JPanel panelSales = new JPanel(new BorderLayout());
        String[] salesCols = {"Sale ID", "Date / Time", "Total Amount ($)"};
        salesModel = new DefaultTableModel(salesCols, 0);
        tableSales = new JTable(salesModel);
        panelSales.add(new JScrollPane(tableSales), BorderLayout.CENTER);
        loadSalesData();

        // Tab 2: Low Stock Alerts
        JPanel panelLowStock = new JPanel(new BorderLayout());
        String[] stockCols = {"Medicine ID", "Medicine Name", "Current Stock", "Reorder Level"};
        lowStockModel = new DefaultTableModel(stockCols, 0);
        tableLowStock = new JTable(lowStockModel);
        panelLowStock.add(new JScrollPane(tableLowStock), BorderLayout.CENTER);
        loadLowStockData();

        tabbedPane.addTab("Sales History", panelSales);
        tabbedPane.addTab("Low Stock Alerts", panelLowStock);

        add(tabbedPane, BorderLayout.CENTER);

        // Refresh Button at Bottom
        JButton btnRefresh = new JButton("Refresh Reports");
        JPanel panelBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBottom.add(btnRefresh);
        add(panelBottom, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> {
            loadSalesData();
            loadLowStockData();
            JOptionPane.showMessageDialog(this, "Reports refreshed successfully!");
        });
    }

    private void loadSalesData() {
        salesModel.setRowCount(0);
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM sales ORDER BY sale_date DESC")) {

            while (rs.next()) {
                Object[] row = {
                        rs.getInt("sale_id"),
                        rs.getTimestamp("sale_date"),
                        rs.getDouble("total_amount")
                };
                salesModel.addRow(row);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading sales history: " + ex.getMessage());
        }
    }

    private void loadLowStockData() {
        lowStockModel.setRowCount(0);
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT medicine_id, name, quantity_in_stock, reorder_level FROM medicines WHERE quantity_in_stock <= reorder_level")) {

            while (rs.next()) {
                Object[] row = {
                        rs.getInt("medicine_id"),
                        rs.getString("name"),
                        rs.getInt("quantity_in_stock"),
                        rs.getInt("reorder_level")
                };
                lowStockModel.addRow(row);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading low stock alerts: " + ex.getMessage());
        }
    }
}