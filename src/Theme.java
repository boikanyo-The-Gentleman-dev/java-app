import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;

/** Shared look & feel helpers so every screen has the same design. */
public final class Theme {
    private Theme() {}

    /** Change to "R", "€", etc. to switch currency everywhere. */
    public static final String CURRENCY = "$";

    public static final Color PRIMARY = new Color(13, 148, 136);
    public static final Color PRIMARY_DARK = new Color(15, 118, 110);
    public static final Color DANGER = new Color(220, 38, 38);
    public static final Color SUCCESS = new Color(22, 163, 74);
    public static final Color NEUTRAL = new Color(100, 116, 139);
    public static final Color BG = new Color(241, 245, 249);
    public static final Color CARD = Color.WHITE;
    public static final Color TEXT = new Color(15, 23, 42);
    public static final Color MUTED = new Color(100, 116, 139);
    public static final Color BORDER = new Color(203, 213, 225);
    public static final Color STRIPE = new Color(248, 250, 252);
    public static final Color SELECTION = new Color(204, 251, 241);
    public static final Color WARN_ROW = new Color(254, 243, 199);
    public static final Color DANGER_ROW = new Color(254, 226, 226);

    public static final Font BASE = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font BOLD = BASE.deriveFont(Font.BOLD);
    public static final Font TITLE = BASE.deriveFont(Font.BOLD, 22f);

    public enum Kind { PRIMARY, SUCCESS, DANGER, NEUTRAL }

    /** Call once at startup. */
    public static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }
        for (String k : new String[]{"Label.font", "TextField.font", "PasswordField.font", "ComboBox.font",
                "Table.font", "TableHeader.font", "TabbedPane.font", "Button.font",
                "OptionPane.messageFont", "OptionPane.buttonFont", "TitledBorder.font"}) {
            UIManager.put(k, BASE);
        }
    }

    // ---------- dialogs ----------
    public static void warn(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    public static void error(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "PIMS", JOptionPane.INFORMATION_MESSAGE);
    }

    public static String money(BigDecimal v) {
        return CURRENCY + String.format("%,.2f", v);
    }

    public static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    // ---------- layout pieces ----------
    public static JPanel header(String title, String subtitle) {
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 2));
        p.setBackground(PRIMARY);
        p.setBorder(new EmptyBorder(16, 24, 16, 24));
        JLabel t = new JLabel(title);
        t.setFont(TITLE);
        t.setForeground(Color.WHITE);
        p.add(t);
        if (subtitle != null) {
            JLabel s = new JLabel(subtitle);
            s.setFont(BASE);
            s.setForeground(new Color(204, 251, 241));
            p.add(s);
        }
        return p;
    }

    public static JPanel card(String title) {
        JPanel p = new JPanel();
        p.setBackground(CARD);
        Border line = new LineBorder(BORDER, 1, true);
        if (title == null) {
            p.setBorder(new CompoundBorder(line, new EmptyBorder(16, 16, 16, 16)));
        } else {
            p.setBorder(new CompoundBorder(
                    new TitledBorder(line, " " + title + " ", TitledBorder.LEFT, TitledBorder.TOP, BOLD, MUTED),
                    new EmptyBorder(6, 12, 12, 12)));
        }
        return p;
    }

    public static JPanel body() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));
        return p;
    }

    public static JPanel buttonRow() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        return p;
    }

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BASE);
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel mutedLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BASE);
        l.setForeground(MUTED);
        return l;
    }

    /** Adds "label: field" to a GridBagLayout panel. pair = 0 (left) or 1 (right). */
    public static void addField(JPanel p, String label, JComponent c, int pair, int row) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = row;
        g.gridx = pair * 2;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(5, pair == 0 ? 0 : 24, 5, 10);
        p.add(label(label), g);
        g = new GridBagConstraints();
        g.gridy = row;
        g.gridx = pair * 2 + 1;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(5, 0, 5, 0);
        p.add(c, g);
    }

    // ---------- inputs ----------
    public static JTextField field(int cols) {
        JTextField f = new JTextField(cols);
        styleField(f);
        return f;
    }

    public static JPasswordField passwordField(int cols) {
        JPasswordField f = new JPasswordField(cols);
        styleField(f);
        return f;
    }

    public static void styleField(JTextComponent f) {
        f.setFont(BASE);
        f.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(6, 8, 6, 8)));
    }

    public static void styleCombo(JComboBox<?> c) {
        c.setFont(BASE);
        c.setBackground(Color.WHITE);
    }

    public static JButton button(String text, Kind kind) {
        Color base = switch (kind) {
            case PRIMARY -> PRIMARY;
            case SUCCESS -> SUCCESS;
            case DANGER -> DANGER;
            case NEUTRAL -> NEUTRAL;
        };
        return new RoundButton(text, base);
    }

    private static class RoundButton extends JButton {
        private final Color base;
        private boolean hover;

        RoundButton(String text, Color base) {
            super(text);
            this.base = base;
            setFont(BOLD);
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(8, 16, 8, 16));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = !isEnabled() ? BORDER
                    : getModel().isPressed() ? base.darker().darker()
                    : hover ? base.darker() : base;
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ---------- tables ----------
    public static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
    }

    public static void styleTable(JTable t) {
        styleTable(t, new RowRenderer());
    }

    public static void styleTable(JTable t, RowRenderer renderer) {
        t.setRowHeight(30);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setFont(BASE);
        t.setSelectionBackground(SELECTION);
        t.setSelectionForeground(TEXT);
        t.setFillsViewportHeight(true);
        t.setDefaultRenderer(Object.class, renderer);

        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(0, 34));
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(tb, v, false, false, r, c);
                setOpaque(true);
                setBackground(PRIMARY_DARK);
                setForeground(Color.WHITE);
                setFont(BOLD);
                setHorizontalAlignment(LEFT);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        });
    }

    public static JScrollPane scroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(new LineBorder(BORDER));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    /** Striped rows, padded cells, money formatting. Override rowColor() to highlight rows. */
    public static class RowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, false, row, col);
            setBorder(new EmptyBorder(0, 10, 0, 10));
            setOpaque(true);
            setForeground(TEXT);
            if (v instanceof BigDecimal b) {
                setText(String.format("%,.2f", b));
            }
            setHorizontalAlignment(v instanceof Number ? RIGHT : LEFT);
            if (sel) {
                setBackground(SELECTION);
            } else {
                Color c = rowColor(t, t.convertRowIndexToModel(row));
                setBackground(c != null ? c : (row % 2 == 0 ? Color.WHITE : STRIPE));
            }
            return this;
        }

        protected Color rowColor(JTable t, int modelRow) { return null; }
    }
}