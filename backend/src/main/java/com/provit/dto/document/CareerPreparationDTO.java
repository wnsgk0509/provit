package com.provit.dto.document;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class CareerPreparationDTO {
    private String occupationCode;
    private String occupationName;
    private String jobCode;
    private String jobName;
    private String summary;
    private String coverageNote;
    private List<Recommendation> recommendations = new ArrayList<>();

    @Data
    public static class Recommendation {
        private String category;
        private String title;
        private String reason;
        private String action;
        private List<DocumentReviewResultDTO.Source> sources = new ArrayList<>();
    }
}
