package com.provit.service.document;

public class DocumentReviewRequestConflictException extends RuntimeException {
    public DocumentReviewRequestConflictException() {
        super("이미 사용한 요청 ID의 첨삭 조건을 변경할 수 없습니다. 기존 기록을 확인해 주세요.");
    }
}
