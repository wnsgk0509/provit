package com.provit.dto.document;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentReviewResultDTO extends DocumentReviewDTO {
    private List<Document> documents = new ArrayList<>();
    private List<String> strengths = new ArrayList<>();
    private Map<String, Feedback> documentReviews = new LinkedHashMap<>();
    private List<Consistency> consistencyIssues = new ArrayList<>();
    private CareerPreparationDTO careerPreparation;

    @Data
    public static class Document {
        private Long reviewDocumentNum;
        @JsonIgnore private Long reviewNum;
        private String documentType;
        private int sourceDocumentNum;
        private String documentTitle;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private Date sourceUpdatedAt;
        private String originalFileName;
        @JsonIgnore private String sourceSnapshotJson;
        @JsonIgnore private byte[] pdfSnapshot;
        @JsonIgnore private String summary;
    }

    @Data
    public static class Feedback {
        private String summary;
        private List<String> strengths = new ArrayList<>();
        private List<Improvement> improvements = new ArrayList<>();
    }

    @Data
    public static class Strength {
        private Long reviewNum;
        private Long reviewDocumentNum;
        private int displayOrder;
        private String content;
    }

    @Data
    public static class Improvement {
        @JsonIgnore private Long reviewDocumentNum;
        @JsonIgnore private int displayOrder;
        private String section;
        private String title;
        private String issue;
        private String original;
        private String suggestion;
        private String reason;
    }

    @Data
    public static class Consistency {
        @JsonIgnore private Long consistencyNum;
        @JsonIgnore private Long reviewNum;
        @JsonIgnore private int displayOrder;
        private String type;
        private String title;
        private String recommendation;
        private List<Source> sources = new ArrayList<>();
    }

    @Data
    public static class Source {
        @JsonIgnore private Long consistencyNum;
        @JsonIgnore private Long reviewNum;
        @JsonIgnore private Long reviewDocumentNum;
        @JsonIgnore private int displayOrder;
        private String documentType;
        private String section;
        private String text;
        private Integer pageNumber;
    }
}
