package com.provit.controller.document;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.provit.common.ResponseCode;
import com.provit.dto.document.CoverLetterDTO;
import com.provit.dto.document.DocumentSummaryDTO;
import com.provit.dto.document.PortfolioCreateRequestDTO;
import com.provit.dto.document.PortfolioDTO;
import com.provit.dto.document.ResumeDetailDTO;
import com.provit.dto.response.ApiResponse;
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

    @PutMapping("/resumes/{resumeNum}")
    public ResponseEntity<ApiResponse<ResumeDetailDTO>> updateResume(
            HttpServletRequest request,
            @PathVariable int resumeNum,
            @RequestBody ResumeDetailDTO resumeDetail) {
        Long userNum = getAuthenticatedUserNum(request);
        if (userNum == null) {
            return new ResponseEntity<>(
                    new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null),
                    HttpStatus.UNAUTHORIZED);
        }

        ResumeDetailDTO updatedResume = documentService.updateResume(
                Math.toIntExact(userNum), resumeNum, resumeDetail);
        return new ResponseEntity<>(
                ApiResponse.success(ResponseCode.SUCCESS, updatedResume), HttpStatus.OK);
    }

    @PutMapping("/cover-letters/{letterNum}")
    public ResponseEntity<ApiResponse<CoverLetterDTO>> updateCoverLetter(
            HttpServletRequest request,
            @PathVariable int letterNum,
            @RequestBody CoverLetterDTO coverLetter) {
        Long userNum = getAuthenticatedUserNum(request);
        if (userNum == null) {
            return new ResponseEntity<>(
                    new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null),
                    HttpStatus.UNAUTHORIZED);
        }

        CoverLetterDTO updatedCoverLetter = documentService.updateCoverLetter(
                Math.toIntExact(userNum), letterNum, coverLetter);
        return new ResponseEntity<>(
                ApiResponse.success(ResponseCode.SUCCESS, updatedCoverLetter), HttpStatus.OK);
    }

    @GetMapping("/resumes/{resumeNum}")
    public ResponseEntity<ApiResponse<ResumeDetailDTO>> getResume(
            HttpServletRequest request,
            @PathVariable int resumeNum) {
        Long userNum = getAuthenticatedUserNum(request);
        if (userNum == null) {
            return new ResponseEntity<>(
                    new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null),
                    HttpStatus.UNAUTHORIZED);
        }

        ResumeDetailDTO resume = documentService.getResume(Math.toIntExact(userNum), resumeNum);
        return new ResponseEntity<>(ApiResponse.success(ResponseCode.SUCCESS, resume), HttpStatus.OK);
    }

    @GetMapping("/resumes")
    public ResponseEntity<ApiResponse<List<DocumentSummaryDTO>>> getResumeList(
            HttpServletRequest request) {
        Long userNum = getAuthenticatedUserNum(request);
        if (userNum == null) {
            return new ResponseEntity<>(
                    new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null),
                    HttpStatus.UNAUTHORIZED);
        }

        List<DocumentSummaryDTO> resumes = documentService.getResumeSummaryList(
                Math.toIntExact(userNum));
        return new ResponseEntity<>(ApiResponse.success(ResponseCode.SUCCESS, resumes), HttpStatus.OK);
    }

    @GetMapping("/cover-letters/{letterNum}")
    public ResponseEntity<ApiResponse<CoverLetterDTO>> getCoverLetter(
            HttpServletRequest request,
            @PathVariable int letterNum) {
        Long userNum = getAuthenticatedUserNum(request);
        if (userNum == null) {
            return new ResponseEntity<>(
                    new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null),
                    HttpStatus.UNAUTHORIZED);
        }

        CoverLetterDTO coverLetter = documentService.getCoverLetter(
                Math.toIntExact(userNum), letterNum);
        return new ResponseEntity<>(
                ApiResponse.success(ResponseCode.SUCCESS, coverLetter), HttpStatus.OK);
    }

    @GetMapping("/cover-letters")
    public ResponseEntity<ApiResponse<List<DocumentSummaryDTO>>> getCoverLetterList(
            HttpServletRequest request) {
        Long userNum = getAuthenticatedUserNum(request);
        if (userNum == null) {
            return new ResponseEntity<>(
                    new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null),
                    HttpStatus.UNAUTHORIZED);
        }

        List<DocumentSummaryDTO> coverLetters = documentService.getCoverLetterSummaryList(
                Math.toIntExact(userNum));
        return new ResponseEntity<>(
                ApiResponse.success(ResponseCode.SUCCESS, coverLetters), HttpStatus.OK);
    }

    @GetMapping("/portfolios/{portfolioNum}")
    public ResponseEntity<ApiResponse<PortfolioDTO>> getPortfolio(
            HttpServletRequest request,
            @PathVariable int portfolioNum) {
        Long userNum = getAuthenticatedUserNum(request);
        if (userNum == null) {
            return new ResponseEntity<>(
                    new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null),
                    HttpStatus.UNAUTHORIZED);
        }

        PortfolioDTO portfolio = documentService.getPortfolio(
                Math.toIntExact(userNum), portfolioNum);
        portfolio.setFileUrl(null);
        return new ResponseEntity<>(
                ApiResponse.success(ResponseCode.SUCCESS, portfolio), HttpStatus.OK);
    }

    @GetMapping("/portfolios")
    public ResponseEntity<ApiResponse<List<DocumentSummaryDTO>>> getPortfolioList(
            HttpServletRequest request) {
        Long userNum = getAuthenticatedUserNum(request);
        if (userNum == null) {
            return new ResponseEntity<>(
                    new ApiResponse<>(ResponseCode.AUTH_UNAUTHORIZED, null),
                    HttpStatus.UNAUTHORIZED);
        }

        List<DocumentSummaryDTO> portfolios = documentService.getPortfolioSummaryList(
                Math.toIntExact(userNum));
        return new ResponseEntity<>(
                ApiResponse.success(ResponseCode.SUCCESS, portfolios), HttpStatus.OK);
    }

    @GetMapping(value = "/portfolios/{portfolioNum}/file", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> downloadPortfolio(
            HttpServletRequest request,
            @PathVariable int portfolioNum) {
        Long userNum = getAuthenticatedUserNum(request);
        if (userNum == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Resource file = documentService.getPortfolioFile(Math.toIntExact(userNum), portfolioNum);
        String filename = portfolioNum + ".pdf";
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(file);
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
