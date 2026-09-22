package com.provit.service.document;

import com.provit.dto.document.PortfolioCreateRequestDTO;
import com.provit.dto.user.PortfolioDTO;
import com.provit.dto.user.ResumeDetailDTO;

public interface DocumentService {

    ResumeDetailDTO createResume(int userNum, ResumeDetailDTO resumeDetail);

    PortfolioDTO createPortfolio(int userNum, PortfolioCreateRequestDTO portfolioRequest);
}
