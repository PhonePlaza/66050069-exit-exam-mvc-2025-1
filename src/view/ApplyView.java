package view;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import model.Candidate;
import model.Job;

public class ApplyView extends JPanel {

    public interface SubmitHandler { void handle(String jobId, String candidateId); }
    public interface BackHandler { void handle(); }

    private SubmitHandler onSubmit;
    private BackHandler onBack;

    private JLabel lblJob;
    private JComboBox<CandItem> cbCandidate;
    private JLabel lblFixedCandidate; // แสดงชื่อกรณี student mode
    private String currentJobId;

    // โหมดแสดงผล
    private boolean studentMode = false;
    private String fixedCandidateId = null;

    public ApplyView() {
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8,8,8,8);
        gc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Apply");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        lblJob = new JLabel("-");

        cbCandidate = new JComboBox<>();
        lblFixedCandidate = new JLabel("-"); // ใช้เฉพาะ student mode

        JButton btnBack = new JButton("Back to jobs");
        btnBack.addActionListener(e -> { if (onBack != null) onBack.handle(); });

        JButton btnSubmit = new JButton("Confirm apply");
        btnSubmit.addActionListener(e -> {
            String candIdToUse;
            if (studentMode) {
                candIdToUse = fixedCandidateId; // ล็อกเป็นตัวเอง
            } else {
                CandItem it = (CandItem) cbCandidate.getSelectedItem();
                candIdToUse = (it == null) ? null : it.id;
            }
            if (candIdToUse == null || currentJobId == null) {
                JOptionPane.showMessageDialog(this, "Incomplete selection.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (onSubmit != null) onSubmit.handle(currentJobId, candIdToUse);
        });

        int r=0;
        gc.gridx=0; gc.gridy=r++; gc.gridwidth=2; add(title, gc);
        gc.gridwidth=1;
        gc.gridy=r; add(new JLabel("Job:"), gc);
        gc.gridx=1; add(lblJob, gc);

        // แถว Candidate จะสลับระหว่าง label (student) กับ combobox (admin)
        gc.gridx=0; gc.gridy=++r; add(new JLabel("Candidate:"), gc);
        gc.gridx=1; add(cbCandidate, gc);
        gc.gridx=1; // label ทับตำแหน่งเดียวกัน แต่จะ hide/show ตามโหมด
        add(lblFixedCandidate, gc);

        gc.gridx=0; gc.gridy=++r; add(btnBack, gc);
        gc.gridx=1; add(btnSubmit, gc);

        setStudentMode(false, null); // ค่าเริ่มต้นเป็น admin mode
    }

    public void setHandlers(SubmitHandler s, BackHandler b) { this.onSubmit = s; this.onBack = b; }

    // โหมด student: ล็อกผู้สมัครเป็นคนปัจจุบัน, ซ่อน ComboBox, แสดง label ชื่อ
    public void showForStudent(Job job, Candidate me) {
        this.currentJobId = job.id;
        lblJob.setText(job.title + " (" + job.id + ")");
        setStudentMode(true, me);
    }

    // โหมด admin: เลือกผู้สมัครได้จากรายการทั้งหมด
    public void showForAdmin(Job job, List<Candidate> candidates) {
        this.currentJobId = job.id;
        lblJob.setText(job.title + " (" + job.id + ")");
        cbCandidate.removeAllItems();
        for (Candidate c : candidates) cbCandidate.addItem(new CandItem(c.id, c.fullName()));
        setStudentMode(false, null);
    }

    private void setStudentMode(boolean isStudent, Candidate me) {
        this.studentMode = isStudent;
        if (isStudent) {
            fixedCandidateId = (me == null ? null : me.id);
            lblFixedCandidate.setText(me == null ? "-" : me.fullName() + " (" + me.id + ")");
        } else {
            fixedCandidateId = null;
            lblFixedCandidate.setText("-");
        }
        // สลับการมองเห็น
        cbCandidate.setVisible(!isStudent);
        lblFixedCandidate.setVisible(isStudent);
        revalidate(); repaint();
    }

    // item สำหรับ ComboBox
    private static class CandItem {
        String id; String label;
        CandItem(String id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return label + " (" + id + ")"; }
    }
}
