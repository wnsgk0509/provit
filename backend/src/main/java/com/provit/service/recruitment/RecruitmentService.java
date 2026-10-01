package com.provit.service.recruitment;

import java.util.List;

import com.provit.dto.recruitment.JobDTO;
import com.provit.dto.recruitment.OccupationDTO;
import com.provit.dto.recruitment.RecruitmentDTO;
import com.provit.dto.recruitment.RecruitmentSearchDTO;
import com.provit.dto.recruitment.UserRecommendResponseDTO;
import com.provit.dto.response.PageResponse;

public interface RecruitmentService {

	public List<OccupationDTO> getSarmainOccupationInfo();

	public List<JobDTO> getJobListByOccupation(String occupationCode);

	public int syncSaraminRecruitments(int limit);

	public PageResponse<RecruitmentDTO> getRecruitmentList(RecruitmentSearchDTO searchDTO);

	public int deactivateExpiredRecruitments();

	public int purgeOldUnscrappedRecruitments();

	public com.provit.dto.recruitment.JobScrapResponseDTO toggleJobScrap(long recruitmentNum, long userNum);

	public UserRecommendResponseDTO getUserJobRecommendations(Long userNum);
}
