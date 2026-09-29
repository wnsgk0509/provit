package com.provit.service.document;

import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.Test;
import com.provit.dto.document.DocumentReviewResultDTO.Document;
import com.provit.service.document.generator.DummyCareerPreparationGenerator;

public class DummyCareerPreparationGeneratorTest {
    private final DummyCareerPreparationGenerator generator = new DummyCareerPreparationGenerator();

    @Test
    public void readsResumeTargetAndReturnsSeparatePreparationCategories() {
        var result = generator.generate(List.of(resume("백엔드/서버개발", ""), letter("")));
        assertEquals("2", result.getOccupationCode());
        assertEquals("IT개발·데이터", result.getOccupationName());
        assertEquals("84", result.getJobCode());
        assertEquals("백엔드/서버개발", result.getJobName());
        assertEquals(List.of("experience", "skill", "certification", "qualification"),
                result.getRecommendations().stream().map(item -> item.getCategory()).toList());
        assertTrue(result.getSummary().contains("미보유"));
    }

    @Test
    public void alreadyMentionedResumeAndCoverLetterItemsAreNotRecommended() {
        var resume = resume("백엔드/서버개발", "Docker로 배포했습니다.");
        resume.setSourceSnapshotJson(resume.getSourceSnapshotJson().replace("\"certificationList\":[]",
                "\"certificationList\":[{\"certName\":\"SQLD\"}]"));
        var result = generator.generate(List.of(resume, letter("JUnit 단위 테스트를 작성하고 GitHub에 공개했습니다.")));
        assertTrue(result.getRecommendations().isEmpty());
    }

    @Test
    public void pdfTextSuppressesExistingSkillsAndUnreadablePdfHasCoverageNotice() throws Exception {
        var portfolio = new Document();
        portfolio.setDocumentType("portfolio"); portfolio.setSourceSnapshotJson("{}");
        portfolio.setPdfSnapshot(pdf("Docker SQLD GitHub JUnit"));
        var result = generator.generate(List.of(resume("백엔드/서버개발", ""), letter(""), portfolio));
        assertTrue(result.getRecommendations().isEmpty());
        assertTrue(result.getCoverageNote().contains("이미지"));
        portfolio.setPdfSnapshot(new byte[] { 1, 2, 3 });
        result = generator.generate(List.of(resume("백엔드/서버개발", ""), letter(""), portfolio));
        assertEquals(4, result.getRecommendations().size());
        assertTrue(result.getCoverageNote().contains("읽지 못해"));
    }

    @Test
    public void frontendUsesItsOwnCandidatesAndMissingTargetDoesNotGuess() {
        var result = generator.generate(List.of(resume("프론트엔드", "TypeScript를 사용했습니다."), letter("")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.getTitle().contains("접근성")));
        assertFalse(result.getRecommendations().stream().anyMatch(item -> item.getTitle().contains("Docker")));
        assertFalse(result.getRecommendations().stream().anyMatch(item -> item.getTitle().contains("TypeScript")));
        var missing = new Document();
        missing.setDocumentType("resume");
        missing.setSourceSnapshotJson("{\"resume\":{\"motivation\":\"백엔드 개발\"}}");
        assertTrue(generator.generate(List.of(missing)).getRecommendations().isEmpty());
    }

    @Test
    public void unrelatedOccupationGetsGenericPreparationInsteadOfBackendSkills() {
        var other = resume("회계", "");
        other.setSourceSnapshotJson(other.getSourceSnapshotJson().replace("IT개발·데이터", "회계·세무·재무"));
        var result = generator.generate(List.of(other));
        assertEquals("회계", result.getJobName());
        assertTrue(result.getRecommendations().stream().noneMatch(item -> item.getTitle().contains("Docker") || item.getTitle().contains("SQLD")));
    }

    private Document resume(String job, String motivation) {
        var result = new Document(); result.setDocumentType("resume");
        result.setSourceSnapshotJson("{\"resume\":{\"occupationCode\":\"2\",\"occupationName\":\"IT개발·데이터\","
                + "\"jobCode\":\"84\",\"jobName\":\"" + job + "\",\"motivation\":\"" + motivation
                + "\"},\"educationList\":[],\"careerList\":[],\"certificationList\":[]}");
        return result;
    }

    private Document letter(String content) {
        var result = new Document(); result.setDocumentType("cover-letter");
        result.setSourceSnapshotJson("{\"problemSolvingExperience\":\"" + content + "\"}");
        return result;
    }

    private byte[] pdf(String content) throws Exception {
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
            var page = new PDPage(); document.addPage(page);
            try (var stream = new PDPageContentStream(document, page)) {
                stream.beginText(); stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700); stream.showText(content); stream.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
