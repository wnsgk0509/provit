package com.provit.dto.interview;

import com.provit.dto.user.CoverLetterDTO;
import com.provit.dto.user.ResumeDetailDTO;
import com.provit.dto.user.UserJobPreferenceDTO;

import lombok.Data;

@Data
public class LlmInterviewContextDTO {

    private UserJobPreferenceDTO jobPreference;
    private ResumeDetailDTO resumeDetail;
    private String portfolioContent;
    private CoverLetterDTO coverLetter;
}
