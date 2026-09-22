package com.provit.service.interview.generator;

import com.provit.dto.interview.InterviewQuestionDTO;
import com.provit.dto.interview.LlmEvaluationRequestDTO;
import com.provit.dto.interview.LlmEvaluationResponseDTO;
import com.provit.dto.interview.LlmFollowUpRequestDTO;
import com.provit.dto.interview.LlmQuestionRequestDTO;

public interface InterviewGenerator {

    InterviewQuestionDTO generateDocumentQuestion(int questionOrder, LlmQuestionRequestDTO request);

    InterviewQuestionDTO generateFollowUpQuestion(int questionOrder, LlmFollowUpRequestDTO request);

    LlmEvaluationResponseDTO evaluate(LlmEvaluationRequestDTO request);
}
