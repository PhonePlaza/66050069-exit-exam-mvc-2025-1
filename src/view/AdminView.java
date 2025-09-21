package view;

import model.Application;
import model.Company;
import model.Job;
import model.Candidate;
import model.Repository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.List;

/** หน้าสำหรับแอดมิน: ดูรายการสมัคร + ใส่เกรด A-F */
public class AdminView extends JPanel {

    public interface SaveHandler {
        // ส่ง index แถวและเกรดที่เลือกให้ Controller ไปบันทึก
        void handleSave(int rowIndex, String grade);
    }

    private JTable table;
    private DefaultTableModel dtm;
    private SaveHandler onSave;

    public AdminView() {
        setLayout(new BorderLayout(10,10));

        JLabel title = new JLabel("Applications (Admin)");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        add(title, BorderLayout.NORTH);

        dtm = new DefaultTableModel(new Object[]{
                "Candidate", "Job", "Company", "Applied At", "Grade"
        }, 0) {
            @Override public boolean isCellEditable(int r, int c) {
                // อนุญาตแก้ไขเฉพาะคอลัมน์ Grade
                return c == 4;
            }
        };
        table = new JTable(dtm);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // ทำให้คอลัมน์ Grade เป็น ComboBox A-F
        JComboBox<String> gradeCombo = new JComboBox<>(new String[]{"", "A","B","C","D","F"});
        TableColumn gradeCol = table.getColumnModel().getColumn(4);
        gradeCol.setCellEditor(new DefaultCellEditor(gradeCombo));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("Save selected grade");
        btnSave.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Please select a row.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String grade = (String) table.getValueAt(row, 4);
            if (onSave != null) onSave.handleSave(row, grade == null ? "" : grade.trim());
        });
        bottom.add(btnSave);
        add(bottom, BorderLayout.SOUTH);
    }

    /** Controller ใส่ handler บันทึกเกรด */
    public void setSaveHandler(SaveHandler h) { this.onSave = h; }

    /** เติมตารางจากรายการ applications โดย map ชื่อผ่าน repository */
    public void setData(List<Application> applications, Repository repo) {
        dtm.setRowCount(0);
        for (Application a : applications) {
            Candidate cand = repo.findCandidate(a.candidateId);
            Job job = repo.findJob(a.jobId);
            Company comp = (job == null) ? null : repo.findCompany(job.companyId);
            String candName = cand == null ? "(Unknown)" : cand.fullName() + " (" + a.candidateId + ")";
            String jobTitle = job == null ? "(Unknown)" : job.title + " (" + a.jobId + ")";
            String company = comp == null ? "(Unknown)" : comp.name;
            String appliedAt = a.appliedAt == null ? "" : a.appliedAt.toString();
            dtm.addRow(new Object[]{ candName, jobTitle, company, appliedAt, a.grade });
        }
    }

    /** ดึงข้อมูลแอปพลิเคชันของแถว (สำหรับ Controller ใช้หา id ต้นทาง) */
    public String getCandidateIdAtRow(int rowIdx) {
        // รูปแบบ: "Name (ID)" → ตัด () ด้านท้าย
        String v = (String) dtm.getValueAt(rowIdx, 0);
        int s = v.lastIndexOf('('), e = v.lastIndexOf(')');
        return (s >= 0 && e > s) ? v.substring(s+1, e) : "";
    }
    public String getJobIdAtRow(int rowIdx) {
        String v = (String) dtm.getValueAt(rowIdx, 1);
        int s = v.lastIndexOf('('), e = v.lastIndexOf(')');
        return (s >= 0 && e > s) ? v.substring(s+1, e) : "";
    }
}
