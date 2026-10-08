package com.provit.service.interview.generator;

import com.provit.dto.interview.InterviewQuestionDTO;
import com.provit.dto.interview.LlmEvaluationRequestDTO;
import com.provit.dto.interview.LlmEvaluationResponseDTO;
import com.provit.dto.interview.LlmFollowUpRequestDTO;
import com.provit.dto.interview.LlmQuestionRequestDTO;
import com.provit.dto.interview.LlmQuestionResponseDTO;

public interface InterviewGenerator {

    LlmQuestionResponseDTO generateDocumentQuestions(LlmQuestionRequestDTO request);

    InterviewQuestionDTO generateFollowUpQuestion(int questionOrder, LlmFollowUpRequestDTO request);

    LlmEvaluationResponseDTO evaluate(LlmEvaluationRequestDTO request);
}
