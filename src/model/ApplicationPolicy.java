package model;

// อินเทอร์เฟซนโยบายการสมัคร (แยกให้ชัดเจน)
public interface ApplicationPolicy {
    boolean canApply(Candidate c, Job j);
}
