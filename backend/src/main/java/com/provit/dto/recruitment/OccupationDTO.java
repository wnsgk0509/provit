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
public class OccupationDTO {
    private String occupationCode;
    private String occupationName;

    public String getOccupationCode() {
        return occupationCode;
    }

    public void setOccupationCode(String occupationCode) {
        this.occupationCode = occupationCode;
    }

    public String getOccupationName() {
        return occupationName;
    }

    public void setOccupationName(String occupationName) {
        this.occupationName = occupationName;
    }

    @Override
    public String toString() {
        return "OccupationDTO{" +
                "occupationCode='" + occupationCode + '\'' +
                ", occupationName='" + occupationName + '\'' +
                '}';
    }
}
