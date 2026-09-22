package com.provit.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.provit.service.recruitment.RecruitmentService;

/**
 * 사람인 채용공고 일일 정기 수집 스케줄러
 * 매일 새벽 03:00에 사람인 실시간 인기 상위 1,000개 공고를 자동 크롤링하여 DB에 적재
 */
@Component
public class RecruitmentScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentScheduler.class);

    private final RecruitmentService recruitmentService;

    @Autowired
    public RecruitmentScheduler(RecruitmentService recruitmentService) {
        this.recruitmentService = recruitmentService;
    }

    /**
     * [배치 1] 매일 자정 00:00:00 실행 (대한민국 표준시 KST 기준)
     * 마감일(EXPIRATION_DATE)이 경과한 채용 공고를 자동으로 비활성화(IS_ACTIVE = 0) 처리합니다.
     */
    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Seoul")
    public void scheduleDailyExpiredRecruitmentDeactivation() {
        log.info("===============================================================");
        log.info(">> [Scheduler] 매일 자정 마감 공고 비활성화 배치 시작 (00:00:00 KST)");
        log.info("===============================================================");

        try {
            int count = recruitmentService.deactivateExpiredRecruitments();
            log.info(">> [Scheduler] 마감 공고 정리 성공: 총 {}건 비활성화 완료", count);
        } catch (Exception e) {
            log.error(">> [Scheduler] 마감 공고 정리 중 에러 발생: {}", e.getMessage(), e);
        }

        log.info("===============================================================");
        log.info(">> [Scheduler] 매일 자정 마감 공고 비활성화 배치 종료");
        log.info("===============================================================");
    }

    /**
     * [배치 2] 매일 새벽 03:00:00 실행 (대한민국 표준시 KST 기준)
     * 사람인 실시간 인기 상위 1,000개 공고를 자동 크롤링하여 DB에 최신 상태로 동기화합니다.
     */
    @Scheduled(cron = "0 0 3 * * ?", zone = "Asia/Seoul")
    public void scheduleDailyRecruitmentSync() {
        log.info("===============================================================");
        log.info(">> [Scheduler] 매일 새벽 채용공고 자동 동기화 배치 시작 (03:00:00 KST)");
        log.info("===============================================================");

        try {
            int syncCount = recruitmentService.syncSaraminRecruitments(1000);
            log.info(">> [Scheduler] 정기 동기화 성공: 총 {}건 적재 완료", syncCount);
        } catch (Exception e) {
            log.error(">> [Scheduler] 정기 동기화 중 에러 발생: {}", e.getMessage(), e);
        }

        log.info("===============================================================");
        log.info(">> [Scheduler] 매일 새벽 채용공고 자동 동기화 배치 종료");
        log.info("===============================================================");
    }
}
