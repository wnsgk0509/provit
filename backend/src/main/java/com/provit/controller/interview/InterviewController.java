package com.provit.controller.interview;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.provit.dto.interview.InterviewDocumentResponseDTO;
import com.provit.dto.interview.InterviewStartRequestDTO;
import com.provit.dto.interview.LlmQuestionRequestDTO;
import com.provit.dto.response.ApiResponse;
import com.provit.dto.user.ResumeDetailDTO;
import com.provit.service.interview.InterviewService;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService interviewService;

    @Autowired
    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @GetMapping("/documents")
    public ApiResponse<InterviewDocumentResponseDTO> getInterviewDocuments(
            @RequestParam(defaultValue = "1") int userNum) {
        validateUserNum(userNum);
        return ApiResponse.success(interviewService.getInterviewDocuments(userNum));
    }

    @GetMapping("/resumes/{resumeNum}")
    public ApiResponse<ResumeDetailDTO> getResumeDetail(
            @PathVariable int resumeNum,
            @RequestParam(defaultValue = "1") int userNum) {
        validateUserNum(userNum);
        validateResumeNum(resumeNum);
        return ApiResponse.success(interviewService.getResumeDetail(userNum, resumeNum));
    }

    @PostMapping("/context")
    public ApiResponse<LlmQuestionRequestDTO> prepareInterviewContext(
            @RequestParam(defaultValue = "1") int userNum,
            @RequestBody InterviewStartRequestDTO request) {
        validateUserNum(userNum);
        validateStartRequest(request);

        LlmQuestionRequestDTO llmRequest = new LlmQuestionRequestDTO();
        llmRequest.setContext(interviewService.getLlmInterviewContext(
                userNum,
                request.getResumeNum(),
                request.isUsePortfolio(),
                request.isUseCoverLetter()));
        llmRequest.setInterviewStyle(request.getInterviewStyle());
        llmRequest.setInterviewDifficulty(request.getInterviewDifficulty());

        return ApiResponse.success(llmRequest);
    }

    private void validateStartRequest(InterviewStartRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("면접 설정이 필요합니다.");
        }
        validateResumeNum(request.getResumeNum());
        if (isBlank(request.getInterviewStyle())) {
            throw new IllegalArgumentException("면접 스타일을 선택해 주세요.");
        }
        if (isBlank(request.getInterviewDifficulty())) {
            throw new IllegalArgumentException("면접 난이도를 선택해 주세요.");
        }
    }

    private void validateUserNum(int userNum) {
        if (userNum <= 0) {
            throw new IllegalArgumentException("올바른 사용자 번호가 필요합니다.");
        }
    }

    private void validateResumeNum(int resumeNum) {
        if (resumeNum <= 0) {
            throw new IllegalArgumentException("이력서를 선택해 주세요.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
