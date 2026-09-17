package com.provit.dto.community;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 커뮤니티 게시글 목록 조회 시 페이징 및 검색 조건을 담는 DTO
 */
@Getter
@Setter
@ToString
public class PostSearchDto {
    // 1. 검색 필터
    private Integer categoryNum; // 카테고리 필터 (null일 경우 전체 조회)
    private String searchType;   // 검색 타입 (예: TITLE, CONTENT, WRITER)
    private String keyword;      // 검색 키워드

    // 2. 페이징 처리 변수
    private int page = 1;        // 요청 페이지 (기본 1페이지)
    private int pageSize = 10;   // 한 페이지당 노출할 게시글 수 (기본 10개)

    /**
     * Oracle 12c+ 페이징 처리를 위한 OFFSET 자동 계산
     * 쿼리: OFFSET #{offset} ROWS FETCH NEXT #{pageSize} ROWS ONLY 에 사용됩니다.
     */
    public int getOffset() {
        return (this.page - 1) * this.pageSize;
    }
}
