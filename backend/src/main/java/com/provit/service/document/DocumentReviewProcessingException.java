package com.provit.service.document;

public class DocumentReviewProcessingException extends RuntimeException {
    private final int httpStatus;
    private Long reviewNum;

    public DocumentReviewProcessingException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() { return httpStatus; }
    public Long getReviewNum() { return reviewNum; }
    public void setReviewNum(Long reviewNum) { this.reviewNum = reviewNum; }
}
