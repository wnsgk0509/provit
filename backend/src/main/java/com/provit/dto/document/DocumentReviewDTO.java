package com.provit.dto.document;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class DocumentReviewDTO {
    private Long reviewNum;
    @JsonIgnore
    private int userNum;
    private String reviewTitle;
    private String reviewStatus;
    private String reviewMode;
    private String customCriteria;
    private String instructions;
    private String summary;
    @JsonIgnore
    private String careerPreparationJson;
    @JsonIgnore
    private String modelName;
    private String promptVersion;
    private int responseVersion;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date finishedAt;

    public String getResultSource() {
        return modelName != null && modelName.startsWith("dummy-") ? "DUMMY" : "UNKNOWN";
    }
}
