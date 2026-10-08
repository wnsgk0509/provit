package com.provit.dto.recruitment;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 관심 채용 공고 스크랩(북마크) 토글 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class JobScrapResponseDTO {

    private Long recruitmentNum;
    private Long userNum;

    @JsonProperty("isScrapped")
    private Boolean isScrapped;

    private String message;
}
