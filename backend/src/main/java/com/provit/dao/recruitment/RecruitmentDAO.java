package com.provit.dao.recruitment;

import java.util.List;
import com.provit.dto.recruitment.JobDTO;
import com.provit.dto.recruitment.OccupationDTO;
import com.provit.dto.recruitment.RecruitmentDTO;
import com.provit.dto.recruitment.RecruitmentSearchDTO;

public interface RecruitmentDAO {

    public List<OccupationDTO> selectOccupationList();

    public List<JobDTO> selectJobListByOccupation(String occupationCode);

    public int mergeRecruitment(RecruitmentDTO recruitmentDTO);

    public List<RecruitmentDTO> selectRecruitmentList(RecruitmentSearchDTO searchDTO);

    public long selectRecruitmentCount(RecruitmentSearchDTO searchDTO);

    public int deactivateExpiredRecruitments();

    public int insertJobScrap(long recruitmentNum, long userNum);

    public int deleteJobScrap(long recruitmentNum, long userNum);

    public int checkJobScrap(long recruitmentNum, long userNum);
}
