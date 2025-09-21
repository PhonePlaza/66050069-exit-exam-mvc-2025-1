package view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

import model.Job;
import model.Repository;

/** หน้าแสดง ตำแหน่งงานที่เปิด */
public class JobsView extends JPanel {

    // callback ให้ Controller ใส่
    public interface SortHandler   { void handle(String key); }
    public interface ApplyHandler  { void handle(String jobId); }
    public interface LogoutHandler { void handle(); } 

    private SortHandler onSort;
    private ApplyHandler onApply;
    private LogoutHandler onLogout; 

    private JTable table;
    private JComboBox<String> cbSort;
    private JButton btnApply;
    private JButton btnLogout;      

    public JobsView() {
        setLayout(new BorderLayout(10,10));

        // แถบด้านบน: ซ้าย = Sort/Apply, ขวา = Logout
        JPanel top = new JPanel(new BorderLayout());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        left.add(new JLabel("Sort by:"));
        cbSort = new JComboBox<>(new String[]{"BY_TITLE","BY_COMPANY","BY_DEADLINE"});
        cbSort.addActionListener(e -> { if (onSort != null) onSort.handle(getSortKey()); });
        left.add(cbSort);

        btnApply = new JButton("Apply selected");
        btnApply.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Please select a job.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String jobId = (String) table.getValueAt(row, 0);
            if (onApply != null) onApply.handle(jobId);
        });
        left.add(btnApply);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnLogout = new JButton("Logout"); // <-- ปุ่ม Logout
        btnLogout.addActionListener(e -> { if (onLogout != null) onLogout.handle(); });
        right.add(btnLogout);

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // ตารางแสดงงาน
        table = new JTable(new DefaultTableModel(new Object[]{"Job ID","Title","Company","Deadline","Type"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    // ให้ Controller ใส่ handler
    public void setHandlers(SortHandler s, ApplyHandler a) {
        this.onSort = s; this.onApply = a;
    }
    public void setLogoutHandler(LogoutHandler h) { this.onLogout = h; } // <-- setter ใหม่

    public String getSortKey() { return (String) cbSort.getSelectedItem(); }

    // ให้ Controller เติมข้อมูลตาราง
    public void setTableData(List<Job> jobs, Repository repo) {
        DefaultTableModel dtm = (DefaultTableModel) table.getModel();
        dtm.setRowCount(0);
        for (Job j : jobs) {
            String companyName = "(Unknown Company)";
            var c = repo.findCompany(j.companyId);
            if (c != null) companyName = c.name;
            dtm.addRow(new Object[]{
                    j.id, j.title, companyName,
                    (j.deadline == null ? "" : j.deadline.toString()),
                    j.type.name()
            });
        }
    }

    // เรียกจาก Controller เพื่อปิดปุ่ม Apply เมื่อเป็น admin
    public void setApplyEnabled(boolean enabled) {
        btnApply.setEnabled(enabled);
        btnApply.setVisible(enabled);
    }
}
