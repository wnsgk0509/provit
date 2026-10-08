package com.provit.dto.interview;

import java.util.List;

import lombok.Data;

@Data
public class InterviewDocumentResponseDTO {

    private List<InterviewDocumentOptionDTO> portfolioList;
    private List<InterviewDocumentOptionDTO> coverLetterList;
    private List<InterviewDocumentOptionDTO> resumeList;
}
