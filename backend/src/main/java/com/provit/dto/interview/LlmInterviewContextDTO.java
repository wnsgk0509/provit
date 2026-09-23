package com.provit.dto.interview;

import com.provit.dto.document.CoverLetterDTO;
import com.provit.dto.document.PortfolioDTO;
import com.provit.dto.document.ResumeDetailDTO;
import com.provit.dto.user.UserJobPreferenceDTO;

import lombok.Data;

@Data
public class LlmInterviewContextDTO {

    private UserJobPreferenceDTO jobPreference;
    private ResumeDetailDTO resumeDetail;
    private PortfolioDTO portfolio;
    private String portfolioContent;
    private CoverLetterDTO coverLetter;
}
