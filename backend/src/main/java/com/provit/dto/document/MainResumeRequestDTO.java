package com.provit.dto.document;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class MainResumeRequestDTO {
    private Long resumeNum;

    @JsonCreator
    public static MainResumeRequestDTO fromJson(@JsonProperty("resumeNum") JsonNode number) {
        var request = new MainResumeRequestDTO();
        if (number == null || number.isNull()) return request;
        if (!number.isIntegralNumber() || !number.canConvertToLong()) {
            throw new IllegalArgumentException("이력서 번호는 정수여야 합니다.");
        }
        request.setResumeNum(number.longValue());
        return request;
    }
}
