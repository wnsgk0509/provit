package com.provit.dto.interview;

import lombok.Data;

@Data
public class InterviewAvailabilityDTO {
    private boolean maintenance;
    private long serverTime;
    private long nextChangeAt;
}
