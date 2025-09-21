package model;

// นโยบายสมัครงานแบบ CO-OP: รับเฉพาะผู้สมัครที่กำลังศึกษา
public class CoopPolicy implements ApplicationPolicy {
    @Override
    public boolean canApply(Candidate c, Job j) {
        return j.type == Job.JobType.COOP && c.status == Candidate.CandidateStatus.STUDYING;
    }
}
