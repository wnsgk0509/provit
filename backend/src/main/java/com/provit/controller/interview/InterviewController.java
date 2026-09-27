package com.provit.controller.interview;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.provit.common.annotation.LoginUser;

import com.provit.common.ResponseCode;
import com.provit.dto.interview.InterviewDocumentResponseDTO;
import com.provit.dto.interview.InterviewAnswerRequestDTO;
import com.provit.dto.interview.InterviewAnswerResponseDTO;
import com.provit.dto.interview.InterviewStartRequestDTO;
import com.provit.dto.interview.InterviewStartResponseDTO;
import com.provit.dto.interview.LlmInterviewContextDTO;
import com.provit.dto.interview.InterviewHistoryDTO;
import com.provit.dto.interview.InterviewResultDTO;
import com.provit.dto.interview.InterviewRecordDTO;
import com.provit.dto.response.ApiResponse;
import com.provit.service.interview.InterviewService;
import com.provit.util.jwt.JwtProvider;

@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    private final InterviewService interviewService;
    private final JwtProvider jwtProvider;

    public InterviewController(InterviewService interviewService, JwtProvider jwtProvider) {
        this.interviewService = interviewService;
        this.jwtProvider = jwtProvider;
    }

    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<InterviewDocumentResponseDTO>> getDocuments(@LoginUser Long userNum) {
        if (userNum == null) {
            return unauthorizedResponse();
        }

        return ResponseEntity.ok(ApiResponse.success(
                interviewService.getInterviewDocuments(Math.toIntExact(userNum))));
    }

    @PostMapping("/context")
    public ResponseEntity<ApiResponse<LlmInterviewContextDTO>> getContext(
            @LoginUser Long userNum, @RequestBody InterviewStartRequestDTO selection) {
        if (userNum == null) {
            return unauthorizedResponse();
        }
        if (selection.getResumeNum() <= 0) {
            return ResponseEntity.badRequest().build();
        }
        try {
            LlmInterviewContextDTO context = interviewService.getLlmInterviewContext(
                    Math.toIntExact(userNum), selection.getResumeNum(),
                    selection.getPortfolioNum(), selection.getLetterNum());
            return ResponseEntity.ok(ApiResponse.success(context));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<InterviewStartResponseDTO>> startInterview(
            @LoginUser Long userNum, @RequestBody InterviewStartRequestDTO startRequest) {
        if (userNum == null) {
            return unauthorizedResponse();
        }
        InterviewStartResponseDTO response = interviewService.startInterview(
                Math.toIntExact(userNum), startRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{historyNum}/answers")
    public ResponseEntity<ApiResponse<InterviewAnswerResponseDTO>> submitAnswer(
            @LoginUser Long userNum,
            @PathVariable int historyNum,
            @RequestBody InterviewAnswerRequestDTO answerRequest) {
        if (userNum == null) {
            return unauthorizedResponse();
        }

        InterviewAnswerResponseDTO response = interviewService.submitAnswer(
                Math.toIntExact(userNum), historyNum, answerRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }



    @GetMapping("/results")
    public ResponseEntity<ApiResponse<List<InterviewResultDTO>>> getResults(@LoginUser Long userNum) {
        if (userNum == null) return unauthorizedResponse();
        return ResponseEntity.ok(ApiResponse.success(
                interviewService.getInterviewResultList(Math.toIntExact(userNum))));
    }

    @GetMapping("/{historyNum}/record")
    public ResponseEntity<ApiResponse<InterviewRecordDTO>> getRecord(
            @LoginUser Long userNum, @PathVariable("historyNum") int historyNum) {
        if (userNum == null) return unauthorizedResponse();
        int owner = Math.toIntExact(userNum);
        InterviewHistoryDTO history = interviewService.getInterviewHistory(historyNum, owner);
        InterviewResultDTO result = interviewService.getInterviewResult(historyNum, owner);
        if (history == null || result == null) {
            return new ResponseEntity<>(new ApiResponse<>(ResponseCode.NOT_FOUND, null), HttpStatus.NOT_FOUND);
        }
        InterviewRecordDTO record = new InterviewRecordDTO();
        record.setHistory(history);
        record.setResult(result);
        return ResponseEntity.ok(ApiResponse.success(record));
    }

    private <T> ResponseEntity<ApiResponse<T>> unauthorizedResponse() {
        return new ResponseEntity<>(
                new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null), HttpStatus.UNAUTHORIZED);
    }
}
