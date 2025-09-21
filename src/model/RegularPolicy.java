package model;

// นโยบายสมัครงานแบบ REGULAR: รับเฉพาะผู้สมัครที่จบแล้ว
public class RegularPolicy implements ApplicationPolicy {
    @Override
    public boolean canApply(Candidate c, Job j) {
        return j.type == Job.JobType.REGULAR && c.status == Candidate.CandidateStatus.GRADUATED;
    }
}
