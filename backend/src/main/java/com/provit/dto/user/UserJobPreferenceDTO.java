package com.provit.dto.user;

import lombok.Data;

@Data
public class UserJobPreferenceDTO {

    private int userNum;
    private String occupationCode;
    private String occupationName;
    private String jobCode;
    private String jobName;
}
