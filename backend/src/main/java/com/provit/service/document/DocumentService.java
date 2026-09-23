package com.provit.service.document;

import org.springframework.core.io.Resource;

import com.provit.dto.document.CoverLetterDTO;
import com.provit.dto.document.PortfolioCreateRequestDTO;
import com.provit.dto.document.PortfolioDTO;
import com.provit.dto.document.ResumeDetailDTO;

public interface DocumentService {

    ResumeDetailDTO createResume(int userNum, ResumeDetailDTO resumeDetail);

    PortfolioDTO createPortfolio(int userNum, PortfolioCreateRequestDTO portfolioRequest);

    CoverLetterDTO createCoverLetter(int userNum, CoverLetterDTO coverLetter);

    ResumeDetailDTO getResume(int userNum, int resumeNum);

    CoverLetterDTO getCoverLetter(int userNum, int letterNum);

    PortfolioDTO getPortfolio(int userNum, int portfolioNum);

    Resource getPortfolioFile(int userNum, int portfolioNum);
}
