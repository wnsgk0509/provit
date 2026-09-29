package com.provit.controller.document;

import java.net.URI;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.provit.common.ResponseCode;
import com.provit.common.annotation.LoginUser;
import com.provit.dto.document.DocumentReviewDTO;
import com.provit.dto.document.DocumentReviewRequestDTO;
import com.provit.dto.document.DocumentReviewResultDTO;
import com.provit.dto.response.ApiResponse;
import com.provit.service.document.DocumentReviewService;
import com.provit.service.document.DocumentReviewProcessingException;

@RestController
@RequestMapping("/api/document-reviews")
public class DocumentReviewController {
    private static final Logger log = LoggerFactory.getLogger(DocumentReviewController.class);
    private final DocumentReviewService reviewService;

    public DocumentReviewController(DocumentReviewService reviewService) { this.reviewService = reviewService; }

    @PostMapping
    public ResponseEntity<ApiResponse<DocumentReviewResultDTO>> create(
            @LoginUser Long userNum, @RequestBody DocumentReviewRequestDTO request) {
        if (userNum == null) return unauthorized();
        var result = reviewService.createReview(Math.toIntExact(userNum), request);
        return ResponseEntity.created(URI.create("/api/document-reviews/" + result.getReviewNum()))
                .cacheControl(CacheControl.noStore()).body(ApiResponse.success(ResponseCode.CREATED, result));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DocumentReviewDTO>>> list(@LoginUser Long userNum,
            @RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "20") int pageSize) {
        if (userNum == null) return unauthorized();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(reviewService.getReviews(Math.toIntExact(userNum), offset, pageSize)));
    }

    @GetMapping("/{reviewNum}")
    public ResponseEntity<ApiResponse<DocumentReviewResultDTO>> detail(@LoginUser Long userNum, @PathVariable long reviewNum) {
        if (userNum == null) return unauthorized();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(reviewService.getReview(Math.toIntExact(userNum), reviewNum)));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<String>> databaseError(DataAccessException exception) {
        log.error("첨삭 기록 DB 처리 실패: {}", exception.getClass().getSimpleName());
        return ResponseEntity.internalServerError().cacheControl(CacheControl.noStore()).body(ApiResponse.error(
                ResponseCode.INTERNAL_SERVER_ERROR, "첨삭 기록을 저장하거나 조회하지 못했습니다. 잠시 후 다시 시도해 주세요."));
    }

    @ExceptionHandler(DocumentReviewProcessingException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> processingError(DocumentReviewProcessingException exception) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("message", exception.getMessage());
        error.put("reviewNum", exception.getReviewNum());
        return ResponseEntity.status(exception.getHttpStatus()).cacheControl(CacheControl.noStore())
                .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, error));
    }

    private <T> ResponseEntity<ApiResponse<T>> unauthorized() {
        return ResponseEntity.status(401).cacheControl(CacheControl.noStore()).body(ApiResponse.error(ResponseCode.AUTH_UNAUTHORIZED));
    }
}
