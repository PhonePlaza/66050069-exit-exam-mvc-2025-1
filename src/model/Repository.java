package model;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Repository:  Model ที่รับผิดชอบอ่าน/เขียนไฟล์ CSV และเก็บข้อมูลไว้ในหน่วยความจำ
 *
 * หน้าที่หลัก:
 *  - โหลด Entity จากไฟล์ CSV: Company, Job, Candidate, Application
 *  - ให้Methodสำหรับค้นหา/ดึงรายการที่ใช้บ่อย 
 *  - ตรวจอีเมลสำหรับ login (student/admin) 
 *  - ตรวจสิทธิ์การสมัครงานตามนโยบาย (COOP / REGULAR)
 *  - เพิ่มแถวการสมัคร (append) และบันทึกเกรด 
 *  - รองรับไฟล์ applications.csv ทั้งแบบ 3 คอลัมน์ (ไม่มี grade) และ 4 คอลัมน์ (มี grade)
 */
public class Repository {

    // ===== ไฟล์ฐานข้อมูล (ชี้ไปยังไฟล์ในโฟลเดอร์ database/) =====
    private final Path companiesCsv;
    private final Path jobsCsv;
    private final Path candidatesCsv;
    private final Path applicationsCsv;
    private final Path adminsCsv;

    // ===== โครงสร้างข้อมูลในหน่วยความจำ =====
    // เก็บEntityจากไฟล์ CSV เพื่อให้เข้าถึงเร็ว (key = id)
    private final Map<String, Company> companies = new HashMap<>();
    private final Map<String, Job> jobs = new HashMap<>();
    private final Map<String, Candidate> candidates = new HashMap<>();

    // เก็บรายการใบสมัครทั้งหมด เพื่อให้ AdminView แสดงและแก้เกรดได้
    private final List<Application> applications = new ArrayList<>();

    // ===== ชุดข้อมูลช่วยสำหรับการยืนยันตัวตน (Authentication) =====
    // อีเมลของผู้สมัคร (ตัวพิมพ์เล็ก) เพื่อใช้ตรวจว่ามีในระบบไหม
    private final Set<String> candidateEmails = new HashSet<>();
    // อีเมลของผู้ดูแลระบบ (admin) จาก admins.csv
    private final Set<String> adminEmails = new HashSet<>();
    // map อีเมล (ตัวพิมพ์เล็ก) : Candidate สำหรับจับคู่ session กับเอนทิตีผู้สมัคร
    private final Map<String, Candidate> candidateByEmailLower = new HashMap<>();

    // ===== นโยบายสมัครงาน (แยกเป็นคลาสอ่านง่าย) =====
    private final ApplicationPolicy coopPolicy = new CoopPolicy();       // สำหรับงาน CO-OP
    private final ApplicationPolicy regularPolicy = new RegularPolicy(); // สำหรับงาน REGULAR

    // ===== Session แบบง่าย (เก็บเฉพาะอีเมลและบทบาท) =====
    public static class Session {
        public final String email; // อีเมลผู้ใช้ที่ล็อกอิน
        public final String role;  // "student" หรือ "admin"
        public Session(String email, String role) { this.email = email; this.role = role; }
    }
    private Session currentSession; // ตัวแปรเก็บ session ปัจจุบัน

    // ตั้งค่า session ปัจจุบัน
    public void setCurrentUser(String email, String role) { currentSession = new Session(email, role); }
    // ดึง session ปัจจุบัน (อาจเป็น null ถ้ายังไม่ล็อกอิน)
    public Session getCurrentSession() { return currentSession; }

    // ===== ตัวช่วยตรวจรูปแบบอีเมล (regex) =====
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    public boolean isValidEmail(String email) { return email != null && EMAIL.matcher(email).matches(); }

    // ตรวจว่าอีเมลเป็นของผู้สมัครในระบบ (เทียบแบบตัวพิมพ์เล็ก)
    public boolean isCandidateEmail(String email) { return email != null && candidateEmails.contains(email.toLowerCase()); }
    // ตรวจว่าอีเมลเป็นของผู้ดูแลระบบ (admin) ในระบบ
    public boolean isAdminEmail(String email)     { return email != null && adminEmails.contains(email.toLowerCase()); }

    /**
     * ส่วนสร้าง Repository
     * - รับpathโฟลเดอร์ฐานข้อมูล (database)
     * - ตั้งค่าpathไฟล์ CSV ต่าง ๆ
     * - ถ้าไฟล์ applications.csv ไม่มี ให้สร้างใหม่พร้อมหัวคอลัมน์ 4 ช่อง
     * - จากนั้นโหลด applications (รองรับทั้งกรณีที่ไฟล์มี/ไม่มีคอลัมน์ grade)
     */
    public Repository(String dbDir) {
        Path base = Paths.get(dbDir);
        this.companiesCsv    = base.resolve("companies.csv");
        this.jobsCsv         = base.resolve("jobs.csv");
        this.candidatesCsv   = base.resolve("candidates.csv");
        this.applicationsCsv = base.resolve("applications.csv");
        this.adminsCsv       = base.resolve("admins.csv");

        try {
            loadCompanies();      // อ่าน companies.csv 
            loadJobs();           // อ่าน jobs.csv 
            loadCandidates();     // อ่าน candidates.csv 
            if (Files.exists(adminsCsv)) loadAdmins(); // ถ้ามี admins.csv ให้โหลดรายชื่อผู้ดูแล
            ensureApplicationsFile(); // ถ้าไม่มีไฟล์ applications.csv ให้สร้างหัว 4 คอลัมน์
            loadApplications();   // โหลดใบสมัครทั้งหมด (อ่าน grade ถ้ามี)
        } catch (IOException e) {
            // โยน RuntimeException เพื่อให้โปรแกรมหลักหยุดพร้อมข้อความชัดเจน
            throw new RuntimeException("Cannot load database: " + e.getMessage(), e);
        }
    }

    // ===== Queries / ฟังก์ชันดึงข้อมูลที่ใช้บ่อย =====

    // หา Company ตาม id (คืน null ถ้าไม่พบ)
    public Company findCompany(String id) { return companies.get(id); }

    // หา Job ตาม id
    public Job findJob(String id) { return jobs.get(id); }

    // หา Candidate ตาม id
    public Candidate findCandidate(String id) { return candidates.get(id); }

    // หา Candidate จากอีเมล (ตัวพิมพ์เล็ก) — ใช้ตอนล็อกอิน student เพื่อผูกตัวเองกับผู้สมัคร
    public Candidate findCandidateByEmailLower(String emailLower) { return candidateByEmailLower.get(emailLower); }

    // ดึงรายการงานที่ open=true ทั้งหมด (ยังไม่กรอง deadline)
    public List<Job> getAllOpenJobs() {
        List<Job> list = new ArrayList<>();
        for (Job j : jobs.values()) if (j.open) list.add(j);
        return list;
    }

    // ดึงผู้สมัครทั้งหมด (ทำสำเนาใหม่เพื่อกันการแก้จากภายนอก)
    public List<Candidate> getAllCandidates() { return new ArrayList<>(candidates.values()); }

    /** คืนรายการใบสมัครทั้งหมด (ทำสำเนาใหม่สำหรับโชว์ใน AdminView) */
    public List<Application> getAllApplications() { return new ArrayList<>(applications); }

    // ตรวจสิทธิ์สมัครตามนโยบาย: COOP → ต้อง STUDYING, REGULAR → ต้อง GRADUATED
    public boolean canApply(Candidate c, Job j) {
        if (j.type == Job.JobType.COOP) return coopPolicy.canApply(c, j);
        return regularPolicy.canApply(c, j);
    }

    /**
     * เพิ่มแถวใบสมัครใหม่ลงไฟล์ applications.csv (append 1 บรรทัด)
     * - ถ้าไฟล์มีหัวคอลัมน์ grade อยู่แล้ว → เขียนคอมม่า "," ปลายบรรทัดเผื่อคอลัมน์เกรดว่าง
     * - อัปเดตรายการในหน่วยความจำ (applications) ต่อท้าย
     */
    public void appendApplication(Job job, Candidate candidate, LocalDateTime when) throws IOException {
        boolean headerHasGrade = fileHeaderHasGrade(); 
        String line = job.id + "," + candidate.id + "," + when + (headerHasGrade ? "," : "") + System.lineSeparator();
        Files.write(applicationsCsv, line.getBytes(), StandardOpenOption.APPEND);
        // อัปเดตในหน่วยความจำ
        applications.add(new Application(job.id, candidate.id, when, ""));
    }

    /**
     * บันทึกเกรด (A-F หรือค่าว่าง) สำหรับใบสมัครที่เจาะจงด้วย (jobId, candidateId)
     * ขั้นตอน:
     *  1) อัปเดตในหน่วยความจำก่อน
     *  2) เขียนกลับไฟล์ทั้งไฟล์เป็นหัว 4 คอลัมน์เสมอ (อัปเกรดไฟล์ถ้าเดิมเป็น 3 คอลัมน์)
     */
    public void saveGrade(String jobId, String candidateId, String grade) throws IOException {
        // อัปเดตเกรดในหน่วยความจำ
        for (Application a : applications) {
            if (a.jobId.equals(jobId) && a.candidateId.equals(candidateId)) {
                a.grade = grade == null ? "" : grade;
                break;
            }
        }
        // เขียนกลับไฟล์ทั้งไฟล์
        writeApplicationsToFile();
    }

    // ===== ส่วนโหลดไฟล์ CSV  =====

    // โหลด companies.csv 
    private void loadCompanies() throws IOException {
        for (String[] r : readCsv(companiesCsv, true)) {
            // รูปแบบ: company_id,name,email,location
            companies.put(r[0], new Company(r[0], r[1], r[2], r[3]));
        }
    }

    // โหลด jobs.csv 
    private void loadJobs() throws IOException {
        for (String[] r : readCsv(jobsCsv, true)) {
            // รูปแบบ: job_id,title,description,company_id,deadline,open,type
            String id = r[0], title = r[1], desc = r[2], companyId = r[3];
            LocalDate deadline = r[4].isBlank() ? null : LocalDate.parse(r[4]);
            boolean open = Boolean.parseBoolean(r[5]);
            Job.JobType type = Job.JobType.valueOf(r[6].toUpperCase());
            jobs.put(id, new Job(id, title, desc, companyId, deadline, open, type));
        }
    }

    // โหลด candidates.csv 
    private void loadCandidates() throws IOException {
        for (String[] r : readCsv(candidatesCsv, true)) {
            //รูปแบบ: candidate_id,first,last,email,status
            String id = r[0], first = r[1], last = r[2], email = r[3];
            Candidate.CandidateStatus st = Candidate.CandidateStatus.valueOf(r[4].toUpperCase());
            Candidate c = new Candidate(id, first, last, email, st);
            candidates.put(id, c);
            String lower = email.toLowerCase();
            candidateEmails.add(lower);
            candidateByEmailLower.put(lower, c);
        }
    }

    // โหลด admins.csv 
    private void loadAdmins() throws IOException {
        for (String[] r : readCsv(adminsCsv, true)) {
            if (r.length > 0) {
                String email = r[0].trim();
                if (!email.isEmpty()) adminEmails.add(email.toLowerCase());
            }
        }
    }

    /**
     * ถ้าไม่มีไฟล์ applications.csv → สร้างใหม่พร้อมหัวคอลัมน์ 4 ช่อง:
     *   job_id,candidate_id,applied_at,grade
     * เหตุผลที่ทำ:จากโจทย์ให้แอดมินใส่เกรด จึงทำให้รองรับคอลัมน์ grade ตั้งแต่ต้น
     */
    private void ensureApplicationsFile() throws IOException {
        if (!Files.exists(applicationsCsv)) {
            Files.createDirectories(applicationsCsv.getParent());
            Files.write(applicationsCsv, "job_id,candidate_id,applied_at,grade\n".getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        }
    }

    // ตรวจหัวไฟล์ applications.csv ว่ามีคำว่า "grade" ไหม 
    private boolean fileHeaderHasGrade() throws IOException {
        List<String> lines = Files.readAllLines(applicationsCsv);
        if (lines.isEmpty()) return false;
        return lines.get(0).toLowerCase().contains("grade");
    }

    /**
     * โหลด applications.csv ทั้งหมดเข้าหน่วยความจำ
     * - รองรับทั้งหัวแบบ 3 คอลัมน์ (ไม่มี grade) และแบบ 4 คอลัมน์ (มี grade)
     * - แปลงบรรทัดละแถวเป็น Application object
     */
    private void loadApplications() throws IOException {
        List<String> lines = Files.readAllLines(applicationsCsv);
        if (lines.isEmpty()) return;

        // ตรวจจากหัวบรรทัดแรกว่ามีคอลัมน์ grade ไหม
        boolean hasGrade = lines.get(0).toLowerCase().contains("grade");

        // ไล่โหลดทีละบรรทัด 
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split(",", -1); // split แบบง่าย ๆ
            // job_id, candidate_id, applied_at
            String jobId = parts[0];
            String candId = parts[1];

            // แปลงเวลา (อาจว่างได้)
            LocalDateTime ts = (parts.length > 2 && !parts[2].isBlank())
                    ? LocalDateTime.parse(parts[2])
                    : null;

            // ถ้ามีคอลัมน์ grade และจำนวนคอลัมน์พอ โดยใช้ค่าที่อ่านได้ ไม่งั้นให้เป็นว่าง
            String grade = (hasGrade && parts.length > 3) ? parts[3] : "";

            applications.add(new Application(jobId, candId, ts, grade));
        }
    }

    /**
     * เขียน applications ทั้งหมดกลับลงไฟล์ (ทับทั้งไฟล์)
     * - บังคับหัวคอลัมน์ให้เป็น 4 คอลัมน์เสมอ: job_id,candidate_id,applied_at,grade
     */
    private void writeApplicationsToFile() throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(applicationsCsv)) {
            bw.write("job_id,candidate_id,applied_at,grade");
            bw.newLine();
            for (Application a : applications) {
                String ts = (a.appliedAt == null) ? "" : a.appliedAt.toString();
                String g  = (a.grade == null) ? "" : a.grade;
                bw.write(a.jobId + "," + a.candidateId + "," + ts + "," + g);
                bw.newLine();
            }
        }
    }

    // ===== ตัวช่วยอ่าน CSV  =====
    private List<String[]> readCsv(Path p, boolean skipHeader) throws IOException {
        if (!Files.exists(p)) throw new FileNotFoundException("Missing file: " + p.toAbsolutePath());
        List<String> lines = Files.readAllLines(p);
        List<String[]> out = new ArrayList<>();
        int start = skipHeader ? 1 : 0; 
        for (int i = start; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            out.add(line.split(",", -1)); // split ตามคอมม่า (ไม่ตัดช่องว่าง, รองรับค่าว่าง)
        }
        return out;
    }
}
