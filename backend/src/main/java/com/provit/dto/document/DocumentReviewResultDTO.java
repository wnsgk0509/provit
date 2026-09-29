package com.provit.dto.document;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentReviewResultDTO extends DocumentReviewDTO {
    private List<Document> documents = new ArrayList<>();
    private List<Strength> strengths = new ArrayList<>();
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
        private List<Strength> strengths = new ArrayList<>();
        private List<Improvement> improvements = new ArrayList<>();
    }

    @Data
    public static class Strength {
        @JsonIgnore private Long reviewNum;
        @JsonIgnore private Long reviewDocumentNum;
        @JsonIgnore private int displayOrder;
        @JsonIgnore private String sourcesJson;
        private String title;
        private String reason;
        private String suggestion;
        private List<Source> sources = new ArrayList<>();

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static Strength fromJson(JsonNode node) {
            var strength = new Strength();
            if (node.isTextual()) {
                strength.setTitle(node.asText());
                return strength;
            }
            if (!node.isObject()) throw new IllegalArgumentException("강점 형식이 올바르지 않습니다.");
            strength.setTitle(node.path("title").asText(null));
            strength.setReason(node.path("reason").asText(null));
            strength.setSuggestion(node.path("suggestion").asText(null));
            for (JsonNode evidence : node.path("sources")) {
                var source = new Source();
                source.setDocumentType(evidence.path("documentType").asText());
                source.setSection(evidence.path("section").asText());
                source.setText(evidence.path("text").asText());
                source.setPageNumber(evidence.path("pageNumber").isNumber() ? evidence.path("pageNumber").asInt() : null);
                strength.getSources().add(source);
            }
            return strength;
        }
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
