package com.provit.dao.document;

import java.util.List;

import com.provit.dto.document.CareerDTO;
import com.provit.dto.document.CertificationDTO;
import com.provit.dto.document.CoverLetterDTO;
import com.provit.dto.document.EducationDTO;
import com.provit.dto.document.PortfolioDTO;
import com.provit.dto.document.ResumeDTO;

public interface DocumentDAO {

    int countEducationCode(int educationCode);

    int insertResume(ResumeDTO resume);

    int insertEducation(EducationDTO education);

    int insertCareer(CareerDTO career);

    int insertCertification(CertificationDTO certification);

    int selectNextPortfolioNum();

    int insertPortfolio(PortfolioDTO portfolio);

    int insertCoverLetter(CoverLetterDTO coverLetter);

    ResumeDTO selectResume(int userNum, int resumeNum);

    List<EducationDTO> selectEducationList(int resumeNum);

    List<CareerDTO> selectCareerList(int resumeNum);

    List<CertificationDTO> selectCertificationList(int resumeNum);

    CoverLetterDTO selectCoverLetter(int userNum, int letterNum);

    PortfolioDTO selectPortfolio(int userNum, int portfolioNum);
}
