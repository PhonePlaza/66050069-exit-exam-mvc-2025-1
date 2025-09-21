package model;

// เอนทิตีสำหรับบริษัท (ข้อมูลพื้นฐาน)
// (คอมเมนต์ไทย / โค้ดและข้อความเป็นอังกฤษ)
public class Company {
    public String id;       // 8 หลัก ตัวแรกไม่เป็น 0
    public String name;
    public String email;
    public String location;

    public Company(String id, String name, String email, String location) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.location = location;
    }
}
