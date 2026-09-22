package com.provit.dao.document;

import com.provit.dto.user.CareerDTO;
import com.provit.dto.user.CertificationDTO;
import com.provit.dto.user.CoverLetterDTO;
import com.provit.dto.user.EducationDTO;
import com.provit.dto.user.PortfolioDTO;
import com.provit.dto.user.ResumeDTO;

public interface DocumentDAO {

    int countEducationCode(int educationCode);

    int insertResume(ResumeDTO resume);

    int insertEducation(EducationDTO education);

    int insertCareer(CareerDTO career);

    int insertCertification(CertificationDTO certification);

    int selectNextPortfolioNum();

    int insertPortfolio(PortfolioDTO portfolio);

    int insertCoverLetter(CoverLetterDTO coverLetter);
}
