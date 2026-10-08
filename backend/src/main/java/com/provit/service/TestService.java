package com.provit.service;

import java.util.Map;

/**
 * 팀원 참고용 샘플 서비스 인터페이스
 */
public interface TestService {

    /**
     * 시스템 상태 및 서버 정보 조회
     */
    Map<String, Object> getSystemStatus();
}
