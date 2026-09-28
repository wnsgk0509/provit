package com.provit.service.interview;

import java.io.IOException;
import java.io.InputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import com.provit.dto.document.CareerDTO;
import com.provit.dto.document.CertificationDTO;
import com.provit.dto.document.CoverLetterDTO;
import com.provit.dto.document.EducationDTO;
import com.provit.dto.document.PortfolioDTO;
import com.provit.dto.document.ResumeDTO;
import com.provit.dto.document.ResumeDetailDTO;
import com.provit.dto.interview.LlmInterviewContextDTO;
import com.provit.service.document.storage.PortfolioFileStorage;

@Component
public class InterviewDocumentInputBuilder {

    private static final int MAX_PORTFOLIO_BYTES = 20 * 1024 * 1024;
    private final PortfolioFileStorage portfolioFileStorage;

    public InterviewDocumentInputBuilder(PortfolioFileStorage portfolioFileStorage) {
        this.portfolioFileStorage = portfolioFileStorage;
    }

    public void prepare(LlmInterviewContextDTO context) {
        if (context.getPortfolio() != null) {
            context.setPortfolioPdf(readPortfolio(context.getPortfolio()));
        }
        context.setDocumentText(buildText(context));
    }

    private byte[] readPortfolio(PortfolioDTO portfolio) {
        try (InputStream stream = portfolioFileStorage.loadAsResource(portfolio.getFileUrl()).getInputStream()) {
            byte[] pdf = stream.readNBytes(MAX_PORTFOLIO_BYTES + 1);
            if (pdf.length > MAX_PORTFOLIO_BYTES) {
                throw new IllegalArgumentException("포트폴리오 PDF가 20MB를 초과합니다.");
            }
            try (PDDocument document = Loader.loadPDF(pdf)) {
                if (document.isEncrypted()) {
                    throw new IllegalArgumentException("암호화되지 않은 포트폴리오 PDF를 첨부해 주세요.");
                }
                if (document.getNumberOfPages() == 0) {
                    throw new IllegalArgumentException("포트폴리오 PDF에 페이지가 없습니다.");
                }
                return pdf;
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("포트폴리오 PDF를 읽을 수 없습니다.", exception);
        }
    }

    private String buildText(LlmInterviewContextDTO context) {
        StringBuilder text = new StringBuilder();

        text.append("[이력서]\n");
        ResumeDetailDTO detail = context.getResumeDetail();
        ResumeDTO resume = detail.getResume();
        append(text, "지원 직군", resume.getOccupationName());
        append(text, "지원 직군 코드", resume.getOccupationCode());
        append(text, "지원 직무", resume.getJobName());
        append(text, "지원 직무 코드", resume.getJobCode());
        append(text, "제목", resume.getResumeTitle());
        append(text, "최종 학력", resume.getHighestLevel());
        append(text, "지원 동기", resume.getMotivation());
        if (detail.getEducationList() != null) {
            for (EducationDTO education : detail.getEducationList()) {
                append(text, "교육", join(education.getSchoolName(), education.getMajor(),
                        education.getEducationStatus()));
            }
        }
        if (detail.getCareerList() != null) {
            for (CareerDTO career : detail.getCareerList()) {
                append(text, "경력", join(career.getCompanyName(), career.getMainDuty()));
            }
        }
        if (detail.getCertificationList() != null) {
            for (CertificationDTO certification : detail.getCertificationList()) {
                append(text, "자격", join(certification.getCertName(), certification.getCertGrade()));
            }
        }

        text.append("[자기소개서]\n");
        CoverLetterDTO coverLetter = context.getCoverLetter();
        append(text, "제목", coverLetter.getCoverLetterTitle());
        append(text, "성장 과정", coverLetter.getGrowthProcess());
        append(text, "성격의 장단점", coverLetter.getPersonalityStrengthsWeaknesses());
        append(text, "문제 해결 경험", coverLetter.getProblemSolvingExperience());
        append(text, "입사 후 포부", coverLetter.getPostJoiningAspiration());

        if (context.getPortfolio() != null) {
            text.append("[포트폴리오]\n");
            append(text, "제목", context.getPortfolio().getPortfolioTitle());
            text.append("본문과 이미지는 함께 첨부한 portfolio.pdf 원본을 확인하세요.\n");
        }
        return text.toString().trim();
    }

    private void append(StringBuilder text, String label, String value) {
        String normalized = clean(value);
        if (!normalized.isEmpty()) {
            text.append(label).append(": ").append(normalized).append('\n');
        }
    }

    private String join(String... values) {
        StringBuilder joined = new StringBuilder();
        for (String value : values) {
            String normalized = clean(value);
            if (!normalized.isEmpty()) {
                if (joined.length() > 0) {
                    joined.append(" / ");
                }
                joined.append(normalized);
            }
        }
        return joined.toString();
    }

    private String clean(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }
}
