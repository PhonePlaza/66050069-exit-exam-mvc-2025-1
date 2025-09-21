package controller;

import model.*;
import model.Job.JobType;
import view.MainWindow;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Controller: ผูก Login/Jobs/Apply/Admin + บันทึกเกรด + Logout */
public class JobFairController {

    private final Repository repo;
    private final MainWindow window;

    public JobFairController(Repository repo, MainWindow window) {
        this.repo = repo;
        this.window = window;

        // ----- Login -----
        window.loginView.setLoginHandler((email, role) -> {
            if (!repo.isValidEmail(email)) {
                JOptionPaneUtil.error(window, "Invalid email format.");
                return;
            }
            String emailLower = email.toLowerCase();

            if ("student".equalsIgnoreCase(role)) {
                if (!repo.isCandidateEmail(emailLower)) {
                    JOptionPaneUtil.error(window, "This email is not found in candidates.");
                    return;
                }
            } else if ("admin".equalsIgnoreCase(role)) {
                if (!repo.isAdminEmail(emailLower)) {
                    JOptionPaneUtil.error(window, "This email is not authorized as admin.");
                    return;
                }
            } else {
                JOptionPaneUtil.error(window, "Unknown role.");
                return;
            }

            repo.setCurrentUser(emailLower, role.toLowerCase());
            window.setSessionTitle(emailLower, role);

            boolean isStudent = "student".equalsIgnoreCase(role);
            window.jobsView.setApplyEnabled(isStudent); // admin ปิดปุ่ม Apply

            if (isStudent) {
                window.showJobs();
                refreshJobs("BY_TITLE");
            } else {
                // admin: ไปหน้า Applications
                window.adminView.setData(repo.getAllApplications(), repo);
                window.showAdmin();
            }
        });

        // ----- Jobs -----
        window.jobsView.setHandlers(this::refreshJobs, jobId -> {
            Job job = repo.findJob(jobId);
            if (job == null) { JOptionPaneUtil.error(window, "Job not found."); return; }

            Repository.Session s = repo.getCurrentSession();
            boolean isStudent = s != null && "student".equalsIgnoreCase(s.role);

            if (isStudent) {
                Candidate me = repo.findCandidateByEmailLower(s.email);
                if (me == null) { JOptionPaneUtil.error(window, "Your email is not mapped to any candidate."); return; }
                window.applyView.showForStudent(job, me);
            } else {
                JOptionPaneUtil.error(window, "Admins cannot apply.");
                return;
            }
            window.showApply();
        });

        // ---- NEW: Logout จากหน้า Jobs ----
        window.jobsView.setLogoutHandler(() -> {
            // เคลียร์ title ให้กลับเป็นค่าเริ่มต้น และแสดงหน้า Login
            window.setTitle("Job Fair - MVC (Swing)");
            // (ถ้าต้องการเคลียร์ session จริง ๆ สามารถเพิ่มเมธอด logout() ใน Repository แล้วเรียกที่นี่)
            window.showLogin();
        });

        // ----- Apply (Student) -----
        window.applyView.setHandlers((jobId, candId) -> {
            Job job = repo.findJob(jobId);
            Candidate cand = repo.findCandidate(candId);
            if (job == null || cand == null) {
                JOptionPaneUtil.error(window, "Invalid data.");
                return;
            }

            if (!repo.canApply(cand, job)) {
                if (job.type == JobType.COOP) {
                    JOptionPaneUtil.error(window, "CO-OP positions are only for STUDYING candidates.");
                } else {
                    JOptionPaneUtil.error(window, "REGULAR positions are only for GRADUATED candidates.");
                }
                return;
            }

            try {
                repo.appendApplication(job, cand, LocalDateTime.now());
                JOptionPaneUtil.info(window, "Applied successfully.\nCandidate: " + cand.fullName()
                        + "\nJob: " + job.title);
            } catch (Exception ex) {
                JOptionPaneUtil.error(window, "Failed to save application: " + ex.getMessage());
                return;
            }

            window.showJobs();
            refreshJobs(window.jobsView.getSortKey());
        }, () -> window.showJobs());

        // ----- Admin: Save grade -----
        window.adminView.setSaveHandler((rowIndex, grade) -> {
            String candId = window.adminView.getCandidateIdAtRow(rowIndex);
            String jobId  = window.adminView.getJobIdAtRow(rowIndex);

            if (!grade.matches("^$|^[ABCDF]$")) {
                JOptionPaneUtil.error(window, "Grade must be A, B, C, D, F or empty.");
                return;
            }

            try {
                repo.saveGrade(jobId, candId, grade);
                JOptionPaneUtil.info(window, "Saved grade successfully.");
                window.adminView.setData(repo.getAllApplications(), repo);
            } catch (Exception ex) {
                JOptionPaneUtil.error(window, "Failed to save grade: " + ex.getMessage());
            }
        });

        // เริ่มที่หน้า Login
        window.showLogin();
    }

    // โหลดงานที่เปิด + ยังไม่หมด deadline + จัดเรียง
    private void refreshJobs(String sortKey) {
        List<Job> jobs = repo.getAllOpenJobs();

        LocalDate today = LocalDate.now();
        jobs.removeIf(j -> j.deadline != null && j.deadline.isBefore(today));

        switch (sortKey) {
            case "BY_COMPANY":
                jobs.sort(Comparator.comparing(j -> {
                    Company c = repo.findCompany(j.companyId);
                    return c == null ? "" : c.name;
                }));
                break;
            case "BY_DEADLINE":
                jobs.sort(Comparator.comparing(j -> j.deadline));
                break;
            default:
                jobs.sort(Comparator.comparing(j -> j.title));
        }
        window.jobsView.setTableData(jobs, repo);
    }

    private static class JOptionPaneUtil {
        static void error(java.awt.Component parent, String msg) {
            javax.swing.JOptionPane.showMessageDialog(parent, msg, "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
        static void info(java.awt.Component parent, String msg) {
            javax.swing.JOptionPane.showMessageDialog(parent, msg, "Info", javax.swing.JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
