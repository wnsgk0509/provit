package com.provit.dto.user;

import lombok.Data;

@Data
public class UserJobPreferenceDTO {

    private int userNum;
    private String jobCode;
    private String jobName;
    private String occupationCode;
    private String occupationName;
}
