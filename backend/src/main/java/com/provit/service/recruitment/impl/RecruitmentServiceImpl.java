package com.provit.service.recruitment.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.provit.dao.recruitment.RecruitmentDAO;
import com.provit.dto.recruitment.JobDTO;
import com.provit.dto.recruitment.OccupationDTO;
import com.provit.service.recruitment.RecruitmentService;

@Service
public class RecruitmentServiceImpl implements RecruitmentService {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentServiceImpl.class);

    private final RecruitmentDAO recruitmentDAO;

    @Autowired
    public RecruitmentServiceImpl(RecruitmentDAO recruitmentDAO) {
        this.recruitmentDAO = recruitmentDAO;
    }

    @Override
    public List<OccupationDTO> getSarmainOccupationInfo() {
        log.info(">> [Service] 직군 목록 조회 (T_OCCUPATION)");
        return recruitmentDAO.selectOccupationList();
    }

    @Override
    public List<JobDTO> getJobListByOccupation(String occupationCode) {
        log.info(">> [Service] 직무 목록 조회 (T_JOB) - 직군코드: {}", occupationCode);
        return recruitmentDAO.selectJobListByOccupation(occupationCode);
    }
}
