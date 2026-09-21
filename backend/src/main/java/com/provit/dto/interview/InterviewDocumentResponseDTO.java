package com.provit.dto.interview;

import java.util.List;

import com.provit.dto.user.CoverLetterDTO;
import com.provit.dto.user.PortfolioDTO;
import com.provit.dto.user.ResumeDTO;
import com.provit.dto.user.UserJobPreferenceDTO;

import lombok.Data;

@Data
public class InterviewDocumentResponseDTO {

    private List<PortfolioDTO> portfolioList;
    private List<CoverLetterDTO> coverLetterList;
    private List<ResumeDTO> resumeList;
    private UserJobPreferenceDTO jobPreference;
}
