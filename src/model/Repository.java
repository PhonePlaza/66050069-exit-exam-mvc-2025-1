package model;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Repository: จัดการ CSV ทั้งหมด + ให้เมธอดให้ Controller ใช้
 * - รองรับ applications.csv มีคอลัมน์ grade (ถ้าไฟล์เก่ามี 3 คอลัมน์ก็อ่านได้)
 */
public class Repository {

    // ===== ไฟล์ฐานข้อมูล =====
    private final Path companiesCsv;
    private final Path jobsCsv;
    private final Path candidatesCsv;
    private final Path applicationsCsv;
    private final Path adminsCsv;

    // ===== ข้อมูลในหน่วยความจำ =====
    private final Map<String, Company> companies = new HashMap<>();
    private final Map<String, Job> jobs = new HashMap<>();
    private final Map<String, Candidate> candidates = new HashMap<>();

    // applications เก็บในหน่วยความจำเพื่อโชว์ใน Admin
    private final List<Application> applications = new ArrayList<>();

    // อีเมลสำหรับ login
    private final Set<String> candidateEmails = new HashSet<>();
    private final Set<String> adminEmails = new HashSet<>();
    private final Map<String, Candidate> candidateByEmailLower = new HashMap<>();

    // นโยบายสมัคร
    private final ApplicationPolicy coopPolicy = new CoopPolicy();
    private final ApplicationPolicy regularPolicy = new RegularPolicy();

    // Session
    public static class Session {
        public final String email;
        public final String role;
        public Session(String email, String role) { this.email = email; this.role = role; }
    }
    private Session currentSession;
    public void setCurrentUser(String email, String role) { currentSession = new Session(email, role); }
    public Session getCurrentSession() { return currentSession; }

    // Email format
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    public boolean isValidEmail(String email) { return email != null && EMAIL.matcher(email).matches(); }
    public boolean isCandidateEmail(String email) { return email != null && candidateEmails.contains(email.toLowerCase()); }
    public boolean isAdminEmail(String email)     { return email != null && adminEmails.contains(email.toLowerCase()); }

    public Repository(String dbDir) {
        Path base = Paths.get(dbDir);
        this.companiesCsv    = base.resolve("companies.csv");
        this.jobsCsv         = base.resolve("jobs.csv");
        this.candidatesCsv   = base.resolve("candidates.csv");
        this.applicationsCsv = base.resolve("applications.csv");
        this.adminsCsv       = base.resolve("admins.csv");

        try {
            loadCompanies();
            loadJobs();
            loadCandidates();
            if (Files.exists(adminsCsv)) loadAdmins();
            ensureApplicationsFile();
            loadApplications(); // อ่าน applications (รวม grade ถ้ามี)
        } catch (IOException e) {
            throw new RuntimeException("Cannot load database: " + e.getMessage(), e);
        }
    }

    // ===== Queries =====
    public Company findCompany(String id) { return companies.get(id); }
    public Job findJob(String id) { return jobs.get(id); }
    public Candidate findCandidate(String id) { return candidates.get(id); }
    public Candidate findCandidateByEmailLower(String emailLower) { return candidateByEmailLower.get(emailLower); }

    public List<Job> getAllOpenJobs() {
        List<Job> list = new ArrayList<>();
        for (Job j : jobs.values()) if (j.open) list.add(j);
        return list;
    }
    public List<Candidate> getAllCandidates() { return new ArrayList<>(candidates.values()); }

    /** คืนลิสต์ applications (สำเนา) สำหรับโชว์ใน Admin */
    public List<Application> getAllApplications() { return new ArrayList<>(applications); }

    public boolean canApply(Candidate c, Job j) {
        if (j.type == Job.JobType.COOP) return coopPolicy.canApply(c, j);
        return regularPolicy.canApply(c, j);
    }

    /** สร้างแถวสมัครใหม่ (student ใช้งาน) แล้วรีโหลดหน่วยความจำ */
    public void appendApplication(Job job, Candidate candidate, LocalDateTime when) throws IOException {
        boolean headerHasGrade = fileHeaderHasGrade();
        String line = job.id + "," + candidate.id + "," + when + (headerHasGrade ? "," : "") + System.lineSeparator();
        Files.write(applicationsCsv, line.getBytes(), StandardOpenOption.APPEND);
        // อัปเดตหน่วยความจำ (เพิ่มท้าย)
        applications.add(new Application(job.id, candidate.id, when, ""));
    }

    /** บันทึกเกรดให้แถวที่ match jobId+candidateId (บันทึกกลับทั้งไฟล์แบบง่าย) */
    public void saveGrade(String jobId, String candidateId, String grade) throws IOException {
        // อัปเดตในหน่วยความจำก่อน
        for (Application a : applications) {
            if (a.jobId.equals(jobId) && a.candidateId.equals(candidateId)) {
                a.grade = grade == null ? "" : grade;
                break;
            }
        }
        // เขียนกลับไฟล์: ถ้ายังเป็นไฟล์ 3 คอลัมน์ ให้แปลงเป็น 4 คอลัมน์พร้อมหัวใหม่
        writeApplicationsToFile();
    }

    // ===== Loaders =====
    private void loadCompanies() throws IOException {
        for (String[] r : readCsv(companiesCsv, true)) {
            companies.put(r[0], new Company(r[0], r[1], r[2], r[3]));
        }
    }
    private void loadJobs() throws IOException {
        for (String[] r : readCsv(jobsCsv, true)) {
            String id = r[0], title = r[1], desc = r[2], companyId = r[3];
            LocalDate deadline = r[4].isBlank() ? null : LocalDate.parse(r[4]);
            boolean open = Boolean.parseBoolean(r[5]);
            Job.JobType type = Job.JobType.valueOf(r[6].toUpperCase());
            jobs.put(id, new Job(id, title, desc, companyId, deadline, open, type));
        }
    }
    private void loadCandidates() throws IOException {
        for (String[] r : readCsv(candidatesCsv, true)) {
            String id = r[0], first = r[1], last = r[2], email = r[3];
            Candidate.CandidateStatus st = Candidate.CandidateStatus.valueOf(r[4].toUpperCase());
            Candidate c = new Candidate(id, first, last, email, st);
            candidates.put(id, c);
            String lower = email.toLowerCase();
            candidateEmails.add(lower);
            candidateByEmailLower.put(lower, c);
        }
    }
    private void loadAdmins() throws IOException {
        for (String[] r : readCsv(adminsCsv, true)) {
            if (r.length > 0) {
                String email = r[0].trim();
                if (!email.isEmpty()) adminEmails.add(email.toLowerCase());
            }
        }
    }

    private void ensureApplicationsFile() throws IOException {
        if (!Files.exists(applicationsCsv)) {
            Files.createDirectories(applicationsCsv.getParent());
            // สร้างไฟล์ใหม่พร้อมหัวคอลัมน์เป็น 4 คอลัมน์
            Files.write(applicationsCsv, "job_id,candidate_id,applied_at,grade\n".getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        }
    }

    private boolean fileHeaderHasGrade() throws IOException {
        List<String> lines = Files.readAllLines(applicationsCsv);
        if (lines.isEmpty()) return false;
        return lines.get(0).toLowerCase().contains("grade");
    }

    private void loadApplications() throws IOException {
        List<String> lines = Files.readAllLines(applicationsCsv);
        if (lines.isEmpty()) return;
        // ตรวจหัวคอลัมน์ ว่ามี grade หรือไม่
        boolean hasGrade = lines.get(0).toLowerCase().contains("grade");
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(",", -1);
            // รองรับ 3 หรือ 4 คอลัมน์
            String jobId = parts[0];
            String candId = parts[1];
            LocalDateTime ts = parts.length > 2 && !parts[2].isBlank() ? LocalDateTime.parse(parts[2]) : null;
            String grade = (hasGrade && parts.length > 3) ? parts[3] : "";
            applications.add(new Application(jobId, candId, ts, grade));
        }
    }

    private void writeApplicationsToFile() throws IOException {
        // เขียนหัว 4 คอลัมน์เสมอ (อัปเกรดไฟล์เดิมถ้าเคยมีแค่ 3)
        try (BufferedWriter bw = Files.newBufferedWriter(applicationsCsv)) {
            bw.write("job_id,candidate_id,applied_at,grade");
            bw.newLine();
            for (Application a : applications) {
                String ts = a.appliedAt == null ? "" : a.appliedAt.toString();
                String g  = a.grade == null ? "" : a.grade;
                bw.write(a.jobId + "," + a.candidateId + "," + ts + "," + g);
                bw.newLine();
            }
        }
    }

    // CSV แบบง่าย
    private List<String[]> readCsv(Path p, boolean skipHeader) throws IOException {
        if (!Files.exists(p)) throw new FileNotFoundException("Missing file: " + p.toAbsolutePath());
        List<String> lines = Files.readAllLines(p);
        List<String[]> out = new ArrayList<>();
        int start = skipHeader ? 1 : 0;
        for (int i = start; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            out.add(line.split(",", -1));
        }
        return out;
    }
}
