package model;

import java.time.LocalDate;

// Entityสำหรับงาน/ตำแหน่ง
public class Job {
    public enum JobType { REGULAR, COOP }

    public String id;             // 8 หลัก ตัวแรกไม่เป็น 0
    public String title;
    public String description;
    public String companyId;      
    public LocalDate deadline;    // วันสุดท้ายรับสมัคร
    public boolean open;          // เปิดรับ/ปิดรับ
    public JobType type;          // REGULAR | COOP

    public Job(String id, String title, String description, String companyId,
               LocalDate deadline, boolean open, JobType type) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.companyId = companyId;
        this.deadline = deadline;
        this.open = open;
        this.type = type;
    }
}
