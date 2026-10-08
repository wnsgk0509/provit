package com.provit.scheduler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.provit.service.recruitment.RecruitmentService;

/**
 * 사람인 채용공고 일일 정기 수집 스케줄러 및 서버 기동 시점 자동 수집 컴포넌트
 */
@Component
public class RecruitmentScheduler {

	private static final Logger log = LoggerFactory.getLogger(RecruitmentScheduler.class);

	private final RecruitmentService recruitmentService;
	private final AtomicBoolean isStartupExecuted = new AtomicBoolean(false);

	@Autowired
	public RecruitmentScheduler(RecruitmentService recruitmentService) {
		this.recruitmentService = recruitmentService;
	}

	/**
	 * [서버 부팅 시점 1회 자동 실행]
	 * 서버(Tomcat) 구동 완료 시점에 최신 채용 공고를 1회 자동 수집합니다.
	 * 톰캣 메인 스레드 부팅 지연(타임아웃)을 방지하기 위해 CompletableFuture.runAsync() 비동기 스레드로 실행합니다.
	 */
	@EventListener(ContextRefreshedEvent.class)
	public void onApplicationStartup(ContextRefreshedEvent event) {
		// Root WebApplicationContext 초기화 시점에만 단 1회 실행 (Spring MVC 복수 컨텍스트 중복 방어)
		if (event.getApplicationContext().getParent() == null) {
			if (isStartupExecuted.compareAndSet(false, true)) {
				CompletableFuture.runAsync(() -> {
					log.info("===============================================================");
					log.info(">> [Startup] 서버 기동 감지: 채용공고 자동 수집 비동기 태스크 시작");
					log.info("===============================================================");

					try {
						// 톰캣 서버 구동 및 커넥션 풀(HikariCP)이 완전히 안정화될 수 있도록 3초 대기
						Thread.sleep(3000);
						// [부팅 시 1회 실행] 마감 공고 및 장기 미갱신 상시 공고 비활성화
						int deactivated = recruitmentService.deactivateExpiredRecruitments();
						log.info(">> [Startup] 서버 부팅 시점 마감 공고 정리 완료: 총 {}건 비활성화", deactivated);

						int syncCount = recruitmentService.syncSaraminRecruitments(1000);
						log.info(">> [Startup] 서버 기동 초기 동기화 성공: 총 {}건 적재 완료", syncCount);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						log.warn(">> [Startup] 서버 기동 동기화 스레드 인터럽트 발생");
					} catch (Exception e) {
						log.error(">> [Startup] 서버 기동 동기화 중 에러 발생: {}", e.getMessage(), e);
					}

					log.info("===============================================================");
					log.info(">> [Startup] 서버 기동 채용공고 자동 수집 태스크 종료");
					log.info("===============================================================");
				});
			}
		}
	}

	/**
	 * [배치 1] 매일 자정 00:00:00 실행 (대한민국 표준시 KST 기준) 
	 * 1. 마감일(EXPIRATION_DATE) 경과 공고 및 60일 이상 지난 상시채용 공고 비활성화 (IS_ACTIVE = 0)
	 * 2. 아무도 스크랩하지 않은 180일 이상 경과된 만료 공고 영구 삭제 (Purge)
	 */
	@Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Seoul")
	public void scheduleDailyExpiredRecruitmentDeactivation() {
		log.info("===============================================================");
		log.info(">> [Scheduler] 매일 자정 마감 공고 비활성화 & 미스크랩 공고 정리 배치 시작 (00:00:00 KST)");
		log.info("===============================================================");

		try {
			int count = recruitmentService.deactivateExpiredRecruitments();
			log.info(">> [Scheduler] 마감 공고 정리 성공: 총 {}건 비활성화 완료", count);

			int purgedCount = recruitmentService.purgeOldUnscrappedRecruitments();
			log.info(">> [Scheduler] 180일 이상 미스크랩 마감 공고 영구 삭제 성공: 총 {}건 삭제 완료", purgedCount);
		} catch (Exception e) {
			log.error(">> [Scheduler] 마감 공고 정리 중 에러 발생: {}", e.getMessage(), e);
		}

		log.info("===============================================================");
		log.info(">> [Scheduler] 매일 자정 마감 공고 비활성화 & 미스크랩 공고 정리 배치 종료");
		log.info("===============================================================");
	}

	/**
	 * [배치 2] 매일 새벽 03:00:00 실행 (대한민국 표준시 KST 기준) 사람인 실시간 인기 상위 1,000개 공고를 자동 크롤링하여
	 * DB에 최신 상태로 동기화합니다.
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
