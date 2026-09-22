package com.provit.controller.interview;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.provit.common.ResponseCode;
import com.provit.dto.interview.InterviewDocumentResponseDTO;
import com.provit.dto.interview.InterviewAnswerRequestDTO;
import com.provit.dto.interview.InterviewAnswerResponseDTO;
import com.provit.dto.interview.InterviewStartRequestDTO;
import com.provit.dto.interview.InterviewStartResponseDTO;
import com.provit.dto.interview.LlmInterviewContextDTO;
import com.provit.dto.response.ApiResponse;
import com.provit.service.interview.InterviewService;
import com.provit.util.jwt.JwtProvider;

@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    private final InterviewService interviewService;
    private final JwtProvider jwtProvider;
    private final Map<Integer, AnswerTimer> answerTimers = new ConcurrentHashMap<>();

    public InterviewController(InterviewService interviewService, JwtProvider jwtProvider) {
        this.interviewService = interviewService;
        this.jwtProvider = jwtProvider;
    }

    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<InterviewDocumentResponseDTO>> getDocuments(HttpServletRequest request) {
        Long userNum = getAuthenticatedUserNum(request);
        if (userNum == null) {
            return unauthorizedResponse();
        }

        return ResponseEntity.ok(ApiResponse.success(
                interviewService.getInterviewDocuments(Math.toIntExact(userNum))));
    }

    @PostMapping("/context")
    public ResponseEntity<ApiResponse<LlmInterviewContextDTO>> getContext(
            HttpServletRequest request, @RequestBody InterviewStartRequestDTO selection) {
        Long userNum = getAuthenticatedUserNum(request);
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
            HttpServletRequest request, @RequestBody InterviewStartRequestDTO startRequest) {
        Long userNum = getAuthenticatedUserNum(request);
        if (userNum == null) {
            return unauthorizedResponse();
        }
        InterviewStartResponseDTO response = interviewService.startInterview(
                Math.toIntExact(userNum), startRequest);
        answerTimers.put(
                response.getHistoryNum(),
                AnswerTimer.start(response.getAnswerTimeLimitSeconds()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{historyNum}/answers")
    public ResponseEntity<ApiResponse<InterviewAnswerResponseDTO>> submitAnswer(
            HttpServletRequest request,
            @PathVariable int historyNum,
            @RequestBody InterviewAnswerRequestDTO answerRequest) {
        Long userNum = getAuthenticatedUserNum(request);
        if (userNum == null) {
            return unauthorizedResponse();
        }

        AnswerTimer answerTimer = answerTimers.get(historyNum);
        if (answerTimer == null) {
            throw new IllegalArgumentException("진행 중인 면접의 답변 시간을 확인할 수 없습니다.");
        }
        if (answerRequest != null) {
            answerRequest.setTimedOut(answerTimer.hasExpired());
        }

        InterviewAnswerResponseDTO response = interviewService.submitAnswer(
                Math.toIntExact(userNum), historyNum, answerRequest);
        if (response.isCompleted()) {
            answerTimers.remove(historyNum);
        } else {
            answerTimers.put(historyNum, answerTimer.restart());
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private record AnswerTimer(long startedAtNanos, long timeLimitNanos) {

        private static AnswerTimer start(int timeLimitSeconds) {
            return new AnswerTimer(
                    System.nanoTime(), TimeUnit.SECONDS.toNanos(timeLimitSeconds));
        }

        private boolean hasExpired() {
            return System.nanoTime() - startedAtNanos >= timeLimitNanos;
        }

        private AnswerTimer restart() {
            return new AnswerTimer(System.nanoTime(), timeLimitNanos);
        }
    }

    private Long getAuthenticatedUserNum(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7).trim();
        if (!jwtProvider.validateToken(token)) {
            return null;
        }
        return jwtProvider.getUserNum(token);
    }

    private <T> ResponseEntity<ApiResponse<T>> unauthorizedResponse() {
        return new ResponseEntity<>(
                new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null), HttpStatus.UNAUTHORIZED);
    }
}
