package com.provit.service.interview;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.provit.dto.document.CoverLetterDTO;
import com.provit.dto.document.PortfolioDTO;
import com.provit.dto.document.ResumeDTO;
import com.provit.dto.document.ResumeDetailDTO;
import com.provit.dto.interview.LlmInterviewContextDTO;
import com.provit.service.document.storage.PortfolioFileStorage;

public class InterviewDocumentInputBuilderTest {

    @Test
    public void requiredDocumentsArePreparedWithoutPortfolio() {
        PortfolioFileStorage unusedStorage = storage(null);
        LlmInterviewContextDTO context = requiredContext();

        new InterviewDocumentInputBuilder(unusedStorage).prepare(context);

        assertTrue(context.getDocumentText().contains("[이력서]"));
        assertTrue(context.getDocumentText().contains("지원 동기: 서버 개발 경험"));
        assertTrue(context.getDocumentText().contains("[자기소개서]"));
        assertTrue(context.getDocumentText().contains("문제 해결 경험: 병목을 분석하고 개선"));
        assertFalse(context.getDocumentText().contains("[포트폴리오]"));
    }

    @Test
    public void selectedPortfolioPdfIsIncludedAsText() throws Exception {
        Path pdf = Files.createTempFile("interview-document-", ".pdf");
        try {
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(50, 700);
                    content.showText("Reduced API latency by 30 percent");
                    content.endText();
                }
                document.save(pdf.toFile());
            }
            LlmInterviewContextDTO context = requiredContext();
            PortfolioDTO portfolio = new PortfolioDTO();
            portfolio.setPortfolioTitle("서비스 프로젝트");
            portfolio.setFileUrl(pdf.toString());
            context.setPortfolio(portfolio);

            new InterviewDocumentInputBuilder(storage(pdf)).prepare(context);

            assertTrue(context.getPortfolioContent().contains("Reduced API latency"));
            assertTrue(context.getDocumentText().contains("[포트폴리오]"));
            assertTrue(context.getDocumentText().contains("Reduced API latency by 30 percent"));
            assertFalse(context.getDocumentText().contains(pdf.toString()));
        } finally {
            Files.deleteIfExists(pdf);
        }
    }

    private LlmInterviewContextDTO requiredContext() {
        ResumeDTO resume = new ResumeDTO();
        resume.setResumeTitle("백엔드 이력서");
        resume.setMotivation("서버\n개발  경험");
        ResumeDetailDTO resumeDetail = new ResumeDetailDTO();
        resumeDetail.setResume(resume);
        CoverLetterDTO coverLetter = new CoverLetterDTO();
        coverLetter.setProblemSolvingExperience("병목을 분석하고 개선");
        LlmInterviewContextDTO context = new LlmInterviewContextDTO();
        context.setResumeDetail(resumeDetail);
        context.setCoverLetter(coverLetter);
        return context;
    }

    private PortfolioFileStorage storage(Path pdf) {
        return new PortfolioFileStorage() {
            @Override
            public String store(int portfolioNum, MultipartFile file) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void deleteIfExists(String fileUrl) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Resource loadAsResource(String fileUrl) {
                if (pdf == null) {
                    throw new AssertionError("포트폴리오를 선택하지 않았는데 파일을 읽었습니다.");
                }
                return new FileSystemResource(pdf);
            }
        };
    }
}
