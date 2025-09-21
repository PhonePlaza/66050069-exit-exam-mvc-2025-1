package model;

// Entityสำหรับผู้สมัคร
public class Candidate {
    public enum CandidateStatus { STUDYING, GRADUATED }

    public String id;             // 8 หลัก ตัวแรกไม่เป็น 0
    public String firstName;
    public String lastName;
    public String email;
    public CandidateStatus status;

    public Candidate(String id, String firstName, String lastName,
                     String email, CandidateStatus status) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.status = status;
    }

    public String fullName() { return firstName + " " + lastName; }
}
