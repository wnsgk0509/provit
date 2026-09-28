package com.provit.service.interview;

public class InterviewMaintenanceException extends IllegalStateException {
    public InterviewMaintenanceException() {
        super("모의면접 서비스 점검 중입니다. 매일 23:55~00:00(한국시간)에는 이용할 수 없습니다.");
    }
}
