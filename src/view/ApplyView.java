package view;

import javax.swing.*;
import java.awt.*;

import model.Candidate;
import model.Job;

/**
 * ApplyView (Student-only):
 * - หน้านี้ใช้สำหรับ "นักเรียน" สมัครงานของตัวเองเท่านั้น
 * - ล็อกผู้สมัครเป็นคนที่ล็อกอินอยู่ (รับมาจาก Controller)
 */
public class ApplyView extends JPanel {

    // callback ให้ Controller ใส่
    public interface SubmitHandler { void handle(String jobId, String candidateId); }
    public interface BackHandler   { void handle(); }

    private SubmitHandler onSubmit; // เรียกเมื่อกดยืนยัน
    private BackHandler onBack;     // เรียกเมื่อกดย้อนกลับ

    // UI หลัก
    private JLabel lblJob;              // แสดงงานที่กำลังจะสมัคร
    private JLabel lblCandidate;        // แสดงชื่อผู้สมัครที่ล็อกไว้ (ตัวเอง)
    private String currentJobId;        // job_id ปัจจุบัน
    private String fixedCandidateId;    // candidate_id ของผู้สมัคร (ล็อก)

    public ApplyView() {
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8,8,8,8);
        gc.fill = GridBagConstraints.HORIZONTAL;

        // หัวข้อ
        JLabel title = new JLabel("Apply");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

        lblJob = new JLabel("-");
        lblCandidate = new JLabel("-");

        JButton btnBack = new JButton("Back to jobs");
        btnBack.addActionListener(e -> { if (onBack != null) onBack.handle(); });

        JButton btnSubmit = new JButton("Confirm apply");
        btnSubmit.addActionListener(e -> {
            // สมัครให้ "ตัวเอง" เท่านั้น
            if (fixedCandidateId == null || currentJobId == null) {
                JOptionPane.showMessageDialog(this, "Incomplete selection.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (onSubmit != null) onSubmit.handle(currentJobId, fixedCandidateId);
        });

        // วางlayout
        int r = 0;
        gc.gridx=0; gc.gridy=r++; gc.gridwidth=2; add(title, gc);

        gc.gridwidth=1;
        gc.gridy=r; gc.gridx=0; add(new JLabel("Job:"), gc);
        gc.gridx=1; add(lblJob, gc);

        gc.gridx=0; gc.gridy=++r; add(new JLabel("Candidate:"), gc);
        gc.gridx=1; add(lblCandidate, gc);

        gc.gridx=0; gc.gridy=++r; add(btnBack, gc);
        gc.gridx=1; add(btnSubmit, gc);
    }

    /** ให้ Controller ใส่ handler สำหรับปุ่มต่าง ๆ */
    public void setHandlers(SubmitHandler s, BackHandler b) {
        this.onSubmit = s;
        this.onBack = b;
    }

    /**
     * ใช้แสดงหน้าในโหมด student:
     * - กำหนด job ที่จะสมัคร
     * - ล็อกผู้สมัครเป็นตัวเอง และโชว์ชื่อที่ label
     */
    public void showForStudent(Job job, Candidate me) {
        this.currentJobId = job.id;
        this.fixedCandidateId = (me == null ? null : me.id);
        lblJob.setText(job.title + " (" + job.id + ")");
        lblCandidate.setText(me == null ? "-" : me.fullName() + " (" + me.id + ")");
        revalidate();
        repaint();
    }
}
