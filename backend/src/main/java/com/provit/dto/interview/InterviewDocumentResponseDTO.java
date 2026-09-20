package com.provit.dto.interview;

import java.util.List;

import com.provit.dto.user.CoverLetterDTO;
import com.provit.dto.user.PortfolioDTO;
import com.provit.dto.user.ResumeDTO;
import com.provit.dto.user.UserJobPreferenceDTO;

import lombok.Data;

@Data
public class InterviewDocumentResponseDTO {

    private PortfolioDTO portfolio;
    private CoverLetterDTO coverLetter;
    private List<ResumeDTO> resumeList;
    private UserJobPreferenceDTO jobPreference;
}
