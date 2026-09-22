package com.provit.dto.interview;

import java.util.List;

import lombok.Data;

@Data
public class LlmQuestionResponseDTO {

    private List<InterviewQuestionDTO> questions;
}
