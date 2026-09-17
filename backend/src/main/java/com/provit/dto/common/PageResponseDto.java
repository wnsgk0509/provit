package com.provit.dto.common;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * 프론트엔드 통신용 공통 페이징 응답 DTO
 * 게시판, 스터디 모집, 채용 공고 등 모든 페이징 목록 응답 시 재사용 가능합니다.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class PageResponseDto<T> {
    private List<T> list;        // 현재 페이지의 데이터 목록
    private int totalCount;      // 전체 데이터 개수
    private int currentPage;     // 현재 페이지 번호
    private int pageSize;        // 페이지당 출력 개수
    private int totalPages;      // 전체 페이지 수
    private boolean hasNext;     // 다음 페이지 존재 여부
    private boolean hasPrev;     // 이전 페이지 존재 여부

    /**
     * 데이터를 받아 페이징 메타데이터를 자동 계산하는 생성자
     */
    public PageResponseDto(List<T> list, int totalCount, int currentPage, int pageSize) {
        this.list = list;
        this.totalCount = totalCount;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        
        // 전체 페이지 수 계산 (나머지가 있으면 1페이지 추가)
        this.totalPages = (int) Math.ceil((double) totalCount / pageSize);
        
        // 이전, 다음 페이지 존재 여부 계산
        this.hasPrev = currentPage > 1;
        this.hasNext = currentPage < this.totalPages;
    }
}
