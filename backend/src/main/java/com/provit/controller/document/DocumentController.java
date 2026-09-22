package com.provit.controller.document;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.provit.common.ResponseCode;
import com.provit.dto.document.PortfolioCreateRequestDTO;
import com.provit.dto.response.ApiResponse;
import com.provit.dto.user.CoverLetterDTO;
import com.provit.dto.user.PortfolioDTO;
import com.provit.dto.user.ResumeDetailDTO;
import com.provit.service.document.DocumentService;
import com.provit.util.jwt.JwtProvider;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final JwtProvider jwtProvider;

    @Autowired
    public DocumentController(DocumentService documentService, JwtProvider jwtProvider) {
        this.documentService = documentService;
        this.jwtProvider = jwtProvider;
    }

    @PostMapping("/resumes")
    public ResponseEntity<ApiResponse<ResumeDetailDTO>> createResume(
            HttpServletRequest request,
            @RequestBody ResumeDetailDTO resumeDetail) {
        Long userNum = getAuthenticatedUserNum(request);
        if (userNum == null) {
            return new ResponseEntity<>(
                    new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null),
                    HttpStatus.UNAUTHORIZED);
        }

        ResumeDetailDTO savedResume = documentService.createResume(
                Math.toIntExact(userNum), resumeDetail);
        return new ResponseEntity<>(
                ApiResponse.success(ResponseCode.CREATED, savedResume),
                HttpStatus.CREATED);
    }

    @PostMapping(value = "/portfolios", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PortfolioDTO>> createPortfolio(
            HttpServletRequest request,
            @ModelAttribute PortfolioCreateRequestDTO portfolioRequest) {
        Long userNum = getAuthenticatedUserNum(request);
        if (userNum == null) {
            return new ResponseEntity<>(
                    new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null),
                    HttpStatus.UNAUTHORIZED);
        }

        PortfolioDTO savedPortfolio = documentService.createPortfolio(
                Math.toIntExact(userNum), portfolioRequest);
        return new ResponseEntity<>(
                ApiResponse.success(ResponseCode.CREATED, savedPortfolio),
                HttpStatus.CREATED);
    }

    @PostMapping("/cover-letters")
    public ResponseEntity<ApiResponse<CoverLetterDTO>> createCoverLetter(
            HttpServletRequest request,
            @RequestBody CoverLetterDTO coverLetter) {
        Long userNum = getAuthenticatedUserNum(request);
        if (userNum == null) {
            return new ResponseEntity<>(
                    new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null),
                    HttpStatus.UNAUTHORIZED);
        }

        CoverLetterDTO savedCoverLetter = documentService.createCoverLetter(
                Math.toIntExact(userNum), coverLetter);
        return new ResponseEntity<>(
                ApiResponse.success(ResponseCode.CREATED, savedCoverLetter),
                HttpStatus.CREATED);
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
}
