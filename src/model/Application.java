package model;

import java.time.LocalDateTime;

/** เอนทิตีสำหรับแถวข้อมูลใบสมัคร (อ่านจาก applications.csv) */
public class Application {
    public String jobId;
    public String candidateId;
    public LocalDateTime appliedAt;
    public String grade; // A-F หรือค่าว่าง

    public Application(String jobId, String candidateId, LocalDateTime appliedAt, String grade) {
        this.jobId = jobId;
        this.candidateId = candidateId;
        this.appliedAt = appliedAt;
        this.grade = grade == null ? "" : grade;
    }
}
