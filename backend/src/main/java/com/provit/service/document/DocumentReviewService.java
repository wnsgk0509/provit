package com.provit.service.document;

import java.util.List;
import com.provit.dto.document.DocumentReviewDTO;
import com.provit.dto.document.DocumentReviewRequestDTO;
import com.provit.dto.document.DocumentReviewResultDTO;

public interface DocumentReviewService {
    DocumentReviewResultDTO createReview(int userNum, DocumentReviewRequestDTO request);
    List<DocumentReviewDTO> getReviews(int userNum, int offset, int pageSize);
    DocumentReviewResultDTO getReview(int userNum, long reviewNum);
    DocumentReviewResultDTO getReviewByRequestId(int userNum, String requestId);
}
