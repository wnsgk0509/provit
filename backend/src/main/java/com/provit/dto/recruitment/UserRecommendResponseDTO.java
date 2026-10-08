package com.provit.dto.recruitment;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRecommendResponseDTO {

    /**
     * 추천 유형
     * - JOB_MATCH : 유저의 소분류 직무(JOB_NAME) 키워드 매칭 성공
     * - OCCUPATION_MATCH : 대분류 직군(OCCUPATION_NAME) 키워드 매칭
     * - POPULAR_FALLBACK : 직무 미설정 또는 매칭 공고 부족으로 인한 인기 공고 대체
     */
    private String recommendType;

    /**
     * 추천 기준이 된 대상 직무 또는 직군명 (예: "웹개발", "백엔드/서버", "IT개발·데이터")
     * 폴백인 경우 null
     */
    private String targetJobName;

    /**
     * 회원 닉네임 (프론트엔드 안내 배너 문구용)
     */
    private String userNickname;

    /**
     * 추천된 채용 공고 목록
     */
    private List<RecruitmentDTO> recruitments;
}
