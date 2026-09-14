package com.provit.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 팀원 참고용 스케줄러 샘플 클래스 (배치 작업, 주기적 DB 동기화 등에 활용)
 */
@Component
public class TestScheduler {

    private static final Logger log = LoggerFactory.getLogger(TestScheduler.class);

    /**
     * 주기적으로 실행할 작업 예시 (실제 활성화 시 root-context.xml에 task 설정 필요)
     */
    public void runScheduledTask() {
        log.info("스케줄러 작업 실행 예시: 백그라운드 배치 작업");
    }
}
