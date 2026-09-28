package com.provit.dto.interview;

import com.provit.dto.document.CoverLetterDTO;
import com.provit.dto.document.PortfolioDTO;
import com.provit.dto.document.ResumeDetailDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.ToString;

@Data
public class LlmInterviewContextDTO {

    private ResumeDetailDTO resumeDetail;
    private PortfolioDTO portfolio;
    @JsonIgnore
    @ToString.Exclude
    private byte[] portfolioPdf;
    private CoverLetterDTO coverLetter;
    private String documentText;
    private InterviewRecruitmentDTO recruitment;
}
