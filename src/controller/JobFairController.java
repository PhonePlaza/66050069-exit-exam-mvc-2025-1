package controller;

import model.*;
import model.Job.JobType;
import view.MainWindow;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Controller หลักของระบบ
 * หน้าที่:
 *  - ผูก event จาก View ทั้งหมด (Login / Jobs / Apply / Admin)
 *  - ประสานงานกับ Repository (Model) เพื่ออ่าน/เขียนข้อมูล CSV
 *  - navigation ระหว่างหน้า และตรวจ business rules
 *  - จัดเรียง/กรองข้อมูลสำหรับหน้า Jobs
 *  - บันทึกผลการสมัคร (student) และบันทึกเกรด (admin)
 */
public class JobFairController {

    // อ้างอิง Model (Repository: อ่าน/เขียน CSV และให้เมธอดช่วยเหลือ)
    private final Repository repo;

    // อ้างอิง View หลัก (มี 4 หน้า: Login / Jobs / Apply / Admin)
    private final MainWindow window;

    /**
     * ส่วนสร้าง Controller:
     *  - รับ Repository และ MainWindow 
     *  - ตั้งหน้าเริ่มต้นเป็นหน้า Login
     */
    public JobFairController(Repository repo, MainWindow window) {
        this.repo = repo;
        this.window = window;

        // ====== Login ======
        // เมื่อ user กด "Login" จากหน้า LoginView
        window.loginView.setLoginHandler((email, role) -> {
            // 1) ตรวจรูปแบบอีเมล (regex) ให้ถูกเบื้องต้นก่อน
            if (!repo.isValidEmail(email)) {
                JOptionPaneUtil.error(window, "Invalid email format.");
                return;
            }
            // เก็บอีเมลเป็นตัวพิมพ์เล็ก 
            String emailLower = email.toLowerCase();

            // 2) ตรวจสิทธิ์ตาม role
            if ("student".equalsIgnoreCase(role)) {
                // student ต้องมีอีเมลอยู่ใน candidates.csv
                if (!repo.isCandidateEmail(emailLower)) {
                    JOptionPaneUtil.error(window, "This email is not found in candidates.");
                    return;
                }
            } else if ("admin".equalsIgnoreCase(role)) {
                // admin ต้องมีอีเมลอยู่ใน admins.csv
                if (!repo.isAdminEmail(emailLower)) {
                    JOptionPaneUtil.error(window, "This email is not authorized as admin.");
                    return;
                }
            } else {
                // ป้องกัน role แปลก ๆ
                JOptionPaneUtil.error(window, "Unknown role.");
                return;
            }

            // 3) ตั้งค่า session และอัปเดต title หน้าต่างเพื่อบอกสถานะผู้ใช้
            repo.setCurrentUser(emailLower, role.toLowerCase());
            window.setSessionTitle(emailLower, role);

            // 4) คุมสิทธิ์การกดปุ่ม Apply ที่หน้า Jobs:
            //    - student: เปิดให้กด
            //    - admin: ปิด/ซ่อนปุ่ม (การสมัครงานเป็นหน้าที่ของ student)
            boolean isStudent = "student".equalsIgnoreCase(role);
            window.jobsView.setApplyEnabled(isStudent);

            // 5) navigation:
            //    - student → ไปหน้า Jobs แล้วโหลดข้อมูล (เรียง BY_TITLE)
            //    - admin   → ไปหน้า Admin แล้วแสดงรายการ applications
            if (isStudent) {
                window.showJobs();
                refreshJobs("BY_TITLE");
            } else {
                window.adminView.setData(repo.getAllApplications(), repo);
                window.showAdmin();
            }
        });

        // ====== Jobs ======
        // callback:
        //  - เปลี่ยนวิธีเรียง (sort) ตารางงาน แล้วเรียก refreshJobs
        //  - กด Apply ที่แถวงาน แล้วไปหน้า Apply (เฉพาะ student)
        window.jobsView.setHandlers(this::refreshJobs, jobId -> {
            // ดึงงานที่เลือกจาก Repository
            Job job = repo.findJob(jobId);
            if (job == null) {
                JOptionPaneUtil.error(window, "Job not found.");
                return;
            }

            // ตรวจ role จาก session
            Repository.Session s = repo.getCurrentSession();
            boolean isStudent = s != null && "student".equalsIgnoreCase(s.role);

            if (isStudent) {
                // student: เปิดหน้า Apply โดยล็อกผู้สมัครเป็นตัวเอง
                Candidate me = repo.findCandidateByEmailLower(s.email);
                if (me == null) {
                    // กรณีอีเมลแม็พกับ candidate ไม่ได้ (ข้อมูลเพี้ยน) ทำการกันพังไว้
                    JOptionPaneUtil.error(window, "Your email is not mapped to any candidate.");
                    return;
                }
                window.applyView.showForStudent(job, me);
            } else {
                // admin: ในสเปคนี้ไม่ให้สมัครงาน
                JOptionPaneUtil.error(window, "Admins cannot apply.");
                return;
            }
            // เปลี่ยนหน้าเป็น Apply
            window.showApply();
        });

        // ====== Logout จากหน้า Jobs ======
        // ปุ่ม Logout: พากลับไปหน้า Login
        window.jobsView.setLogoutHandler(() -> {
            window.setTitle("Job Fair - MVC (Swing)");
            window.showLogin();
        });

        // ====== Apply (Student) ======
        // callback ที่หน้า Apply:
        //  - ยืนยันสมัคร (Confirm apply)
        //  - ย้อนกลับไปหน้า Jobs (Back to jobs)
        window.applyView.setHandlers((jobId, candId) -> {
            // ตรวจว่ามี job/candidate ตามรหัสที่ส่งมาจริง
            Job job = repo.findJob(jobId);
            Candidate cand = repo.findCandidate(candId);
            if (job == null || cand == null) {
                JOptionPaneUtil.error(window, "Invalid data.");
                return;
            }

            // ตรวจ business rule: COOP เฉพาะ STUDYING ส่วน REGULAR เฉพาะ GRADUATED
            if (!repo.canApply(cand, job)) {
                if (job.type == JobType.COOP) {
                    JOptionPaneUtil.error(window, "CO-OP positions are only for STUDYING candidates.");
                } else {
                    JOptionPaneUtil.error(window, "REGULAR positions are only for GRADUATED candidates.");
                }
                return;
            }

            try {
                // บันทึกการสมัคร:
                //  - append ลง applications.csv
                //  - เก็บเวลาเครื่อง LocalDateTime.now()
                repo.appendApplication(job, cand, LocalDateTime.now());

                // แจ้งผลสำเร็จ
                JOptionPaneUtil.info(window, "Applied successfully.\nCandidate: " + cand.fullName()
                        + "\nJob: " + job.title);
            } catch (Exception ex) {
                // กรณีเขียนไฟล์ล้มเหลว
                JOptionPaneUtil.error(window, "Failed to save application: " + ex.getMessage());
                return;
            }

            //กลับไปหน้า Jobs และรีเฟรชตารางด้วย sort ปัจจุบัน
            window.showJobs();
            refreshJobs(window.jobsView.getSortKey());
        }, () -> window.showJobs());

        // ====== Admin: Save grade ======
        // callback ที่หน้า Admin:
        //  - เมื่อกด "Save selected grade" จะบันทึกเกรด A-F (หรือว่าง)
        window.adminView.setSaveHandler((rowIndex, grade) -> {
            // แปลงข้อมูลจากแถวในตารางกลับเป็น id จริง (job_id, candidate_id)
            String candId = window.adminView.getCandidateIdAtRow(rowIndex);
            String jobId  = window.adminView.getJobIdAtRow(rowIndex);

            // ตรวจรูปแบบเกรด: "" หรือ A/B/C/D/F เท่านั้น
            if (!grade.matches("^$|^[ABCDF]$")) {
                JOptionPaneUtil.error(window, "Grade must be A, B, C, D, F or empty.");
                return;
            }

            try {
                // บันทึกเกรดลง Repository:
                //  - อัปเดตในหน่วยความจำ
                //  - เขียนกลับ applications.csv 
                repo.saveGrade(jobId, candId, grade);

                // แจ้งผล + รีโหลดตารางบนหน้า Admin
                JOptionPaneUtil.info(window, "Saved grade successfully.");
                window.adminView.setData(repo.getAllApplications(), repo);
            } catch (Exception ex) {
                JOptionPaneUtil.error(window, "Failed to save grade: " + ex.getMessage());
            }
        });

        // ====== หน้าเริ่มต้น ======
        window.showLogin();
    }

    /**
     * refreshJobs:
     *  - ดึงรายการงานที่ open=true
     *  - กรองงานที่ deadline หมดอายุ (ก่อนวันนี้) ออก
     *  - จัดเรียงตามคีย์ที่ผู้ใช้เลือก (BY_TITLE / BY_COMPANY / BY_DEADLINE)
     *  - ส่งผลลัพธ์ให้ JobsView แสดงในตาราง
     */
    private void refreshJobs(String sortKey) {
        // ดึงงานที่เปิดอยู่ทั้งหมด (ยังไม่กรอง deadline)
        List<Job> jobs = repo.getAllOpenJobs();

        // กรองงานที่หมดเขตแล้ว: ถ้ามี deadline และ < วันนี้ ให้ตัดออก
        LocalDate today = LocalDate.now();
        jobs.removeIf(j -> j.deadline != null && j.deadline.isBefore(today));

        // จัดเรียงตามตัวเลือก
        switch (sortKey) {
            case "BY_COMPANY":
                // เรียงตามชื่อบริษัท 
                jobs.sort(Comparator.comparing(j -> {
                    Company c = repo.findCompany(j.companyId);
                    return c == null ? "" : c.name;
                }));
                break;
            case "BY_DEADLINE":
                // เรียงตามวันหมดเขต 
                jobs.sort(Comparator.comparing(j -> j.deadline));
                break;
            default:
                // ค่าdefault: เรียงตามชื่อตำแหน่งงาน
                jobs.sort(Comparator.comparing(j -> j.title));
        }

        // ส่งข้อมูลให้ View แสดงผล
        window.jobsView.setTableData(jobs, repo);
    }

    /**
     * utility สำหรับแสดง dialog (ลดการเขียนซ้ำ)
     *  - error(...)   : กล่องข้อความแบบ Error
     *  - info(...)    : กล่องข้อความแบบ Info
     */
    private static class JOptionPaneUtil {
        static void error(java.awt.Component parent, String msg) {
            javax.swing.JOptionPane.showMessageDialog(parent, msg, "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
        static void info(java.awt.Component parent, String msg) {
            javax.swing.JOptionPane.showMessageDialog(parent, msg, "Info", javax.swing.JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
