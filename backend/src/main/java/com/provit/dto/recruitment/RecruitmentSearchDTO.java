package com.provit.dto.recruitment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RecruitmentSearchDTO {

    private String keyword;
    private String location;
    private String experienceLevel;

    // 로그인한 요청자 회원 번호 (스크랩 여부 확인 및 스크랩 필터링용)
    private Long userNum;

    // 스크랩한 공고만 필터링 조회 여부
    @Builder.Default
    private Boolean scrapOnly = false;

    @Builder.Default
    private int page = 1;

    @Builder.Default
    private int size = 10;

    public int getPage() {
        return page < 1 ? 1 : page;
    }

    public int getSize() {
        return size < 1 ? 10 : size;
    }

    public int getOffset() {
        return (getPage() - 1) * getSize();
    }
}
