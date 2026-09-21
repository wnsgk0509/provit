package com.provit.controller.interview;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.provit.common.ResponseCode;
import com.provit.dto.interview.InterviewDocumentResponseDTO;
import com.provit.dto.interview.InterviewStartRequestDTO;
import com.provit.dto.interview.LlmInterviewContextDTO;
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
    public ResponseEntity<ApiResponse<InterviewDocumentResponseDTO>> getDocuments(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return new ResponseEntity<>(new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null), HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7).trim();
        if (!jwtProvider.validateToken(token)) {
            ResponseCode code = jwtProvider.isTokenExpired(token)
                    ? ResponseCode.AUTH_TOKEN_EXPIRED : ResponseCode.AUTH_TOKEN_INVALID;
            return new ResponseEntity<>(new ApiResponse<>(code, null), HttpStatus.UNAUTHORIZED);
        }

        int userNum = Math.toIntExact(jwtProvider.getUserNum(token));
        return ResponseEntity.ok(ApiResponse.success(interviewService.getInterviewDocuments(userNum)));
    }

    @PostMapping("/context")
    public ResponseEntity<ApiResponse<LlmInterviewContextDTO>> getContext(
            HttpServletRequest request, @RequestBody InterviewStartRequestDTO selection) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return new ResponseEntity<>(new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null), HttpStatus.UNAUTHORIZED);
        }
        String token = authHeader.substring(7).trim();
        if (!jwtProvider.validateToken(token)) {
            ResponseCode code = jwtProvider.isTokenExpired(token)
                    ? ResponseCode.AUTH_TOKEN_EXPIRED : ResponseCode.AUTH_TOKEN_INVALID;
            return new ResponseEntity<>(new ApiResponse<>(code, null), HttpStatus.UNAUTHORIZED);
        }
        int userNum = Math.toIntExact(jwtProvider.getUserNum(token));
        if (selection.getResumeNum() <= 0) {
            return ResponseEntity.badRequest().build();
        }
        try {
            LlmInterviewContextDTO context = interviewService.getLlmInterviewContext(
                    userNum, selection.getResumeNum(), selection.isUsePortfolio(), selection.isUseCoverLetter());
            return ResponseEntity.ok(ApiResponse.success(context));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        }
    }
}
