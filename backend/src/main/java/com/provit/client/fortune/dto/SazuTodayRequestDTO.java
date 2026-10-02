package com.provit.client.fortune.dto;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SazuTodayRequestDTO {

    private int birthYear;
    private int birthMonth;
    private int birthDay;
    private Integer birthHour;
    @Builder.Default
    private int birthMinute = 0;
    
    @JsonProperty("isFemale")
    private boolean isFemale;
    
    @JsonProperty("birthCity")
    @Builder.Default
    private String birthCity = "서울";
    
    @JsonProperty("isLunar")
    @Builder.Default
    private boolean isLunar = false;

    /**
     * 회원 생년월일(Date)과 성별 정보를 기반으로 요청 DTO 생성
     */
    public static SazuTodayRequestDTO of(Date birthDate, String gender) {
        if (birthDate == null) {
            return defaultSample();
        }

        LocalDate localDate = birthDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        boolean female = "FEMALE".equalsIgnoreCase(gender) || "F".equalsIgnoreCase(gender) || "여자".equals(gender);

        return SazuTodayRequestDTO.builder()
                .birthYear(localDate.getYear())
                .birthMonth(localDate.getMonthValue())
                .birthDay(localDate.getDayOfMonth())
                .birthHour(12) // 시간 미입력 시 정오(12시) 기본값
                .birthMinute(0)
                .isFemale(female)
                .birthCity("서울")
                .isLunar(false)
                .build();
    }

    /**
     * sazu.app Free 샌드박스 플랜용 공식 샘플 프로필 (신강 남성, 1998-05-19 10:00)
     */
    public static SazuTodayRequestDTO defaultSample() {
        return SazuTodayRequestDTO.builder()
                .birthYear(1998)
                .birthMonth(5)
                .birthDay(19)
                .birthHour(10)
                .birthMinute(0)
                .isFemale(false)
                .birthCity("서울")
                .isLunar(false)
                .build();
    }
}
