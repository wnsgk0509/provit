package com.provit.service.fortune;

import com.provit.dto.fortune.TodayFortuneDTO;

/**
 * 오늘의 취업 운세 및 행운 추천 공고 비즈니스 서비스 인터페이스
 */
public interface FortuneService {

    /**
     * 오늘의 취업 운세 및 맞춤 추천 채용 공고 조회
     * 
     * @param userNum 로그인한 사용자 고유 번호 (미로그인 시 null)
     * @return 사주 명리학 해석 결과 및 행운 직무 채용 공고가 결합된 TodayFortuneDTO
     */
    TodayFortuneDTO getTodayFortune(Long userNum);
}
