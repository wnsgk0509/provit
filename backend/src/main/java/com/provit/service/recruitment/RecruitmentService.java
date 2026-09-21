package com.provit.service.recruitment;

import java.util.List;

import com.provit.dto.recruitment.JobDTO;
import com.provit.dto.recruitment.OccupationDTO;

public interface RecruitmentService {

	public List<OccupationDTO> getSarmainOccupationInfo();

	public List<JobDTO> getJobListByOccupation(String occupationCode);
}
