package model;

// interfaceนโยบายการสมัคร (แยกเพื่อความชัดเจน)
public interface ApplicationPolicy {
    boolean canApply(Candidate c, Job j);
}
