package com.provit.service.document.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.provit.dao.document.DocumentReviewDAO;
import com.provit.dto.document.DocumentReviewDTO;
import com.provit.dto.document.DocumentReviewRequestDTO;
import com.provit.dto.document.DocumentReviewResultDTO;
import com.provit.dto.document.DocumentReviewResultDTO.*;
import com.provit.dto.document.CareerPreparationDTO;
import com.provit.service.document.DocumentReviewService;
import com.provit.service.document.DocumentService;
import com.provit.service.document.generator.DocumentReviewGenerator;
import com.provit.service.document.generator.OpenAiDocumentReviewGenerator;
import com.provit.service.document.DocumentReviewProcessingException;

@Service
public class DocumentReviewServiceImpl implements DocumentReviewService {
    private static final Set<String> MODES = Set.of("comprehensive", "expression", "consistency", "jobFit", "evidence", "custom");
    private static final int MAX_TEXT_LENGTH = 200;
    private static final int MAX_PDF_SIZE = 20_000_000;
    private final DocumentReviewDAO reviewDAO;
    private final DocumentService documentService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DocumentReviewGenerator generator;
    private final TransactionTemplate transaction;

    public DocumentReviewServiceImpl(DocumentReviewDAO reviewDAO, DocumentService documentService,
            DocumentReviewGenerator generator, PlatformTransactionManager transactionManager) {
        this.reviewDAO = reviewDAO;
        this.documentService = documentService;
        this.generator = generator;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public DocumentReviewResultDTO createReview(int userNum, DocumentReviewRequestDTO request) {
        validateRequest(request);
        var documents = snapshotDocuments(userNum, request);
        var review = new DocumentReviewDTO();
        review.setUserNum(userNum);
        String title = "AI 첨삭 · " + documents.get(0).getDocumentTitle();
        review.setReviewTitle(title.substring(0, Math.min(title.length(), MAX_TEXT_LENGTH)));
        review.setReviewMode(request.getReviewMode());
        review.setCustomCriteria("custom".equals(request.getReviewMode()) ? request.getCustomCriteria().strip() : null);
        review.setInstructions(normalizeOptionalText(request.getInstructions()));
        review.setModelName(OpenAiDocumentReviewGenerator.MODEL);
        review.setPromptVersion(OpenAiDocumentReviewGenerator.PROMPT_VERSION);
        review.setResponseVersion(OpenAiDocumentReviewGenerator.RESPONSE_VERSION);
        transaction.executeWithoutResult(status -> {
            reviewDAO.insertReview(review);
            for (Document document : documents) {
                document.setReviewNum(review.getReviewNum());
                reviewDAO.insertDocument(document);
            }
        });
        try {
            var result = generator.generate(request, documents);
            transaction.executeWithoutResult(status -> saveResult(userNum, review, documents, result));
        } catch (RuntimeException exception) {
            var failure = exception instanceof DocumentReviewProcessingException
                    ? (DocumentReviewProcessingException) exception
                    : new DocumentReviewProcessingException("첨삭 결과를 저장하지 못했습니다. 기록을 확인해 주세요.", 500);
            failure.setReviewNum(review.getReviewNum());
            review.setErrorMessage(failure.getMessage());
            try {
                transaction.executeWithoutResult(status -> {
                    if (reviewDAO.failReview(review) != 1) throw new IllegalStateException("첨삭 실패 상태 저장 실패");
                });
            } catch (RuntimeException persistenceFailure) {
                org.slf4j.LoggerFactory.getLogger(getClass()).error("첨삭 실패 상태 저장 실패: reviewNum={}, exception={}",
                        review.getReviewNum(), persistenceFailure.getClass().getSimpleName());
            }
            throw failure;
        }
        return getReview(userNum, review.getReviewNum());
    }

    private void saveResult(int userNum, DocumentReviewDTO review, List<Document> documents, DocumentReviewResultDTO result) {
        if (!"PROCESSING".equals(reviewDAO.lockReview(userNum, review.getReviewNum())))
            throw new IllegalStateException("처리 중인 첨삭 기록이 없습니다.");
        review.setSummary(result.getSummary());
        try { review.setCareerPreparationJson(objectMapper.writeValueAsString(result.getCareerPreparation())); }
        catch (IOException exception) { throw new IllegalStateException("취업 준비 추천 저장 실패", exception); }
        Map<String, Document> documentsByType = new LinkedHashMap<>();
        for (Document document : documents) {
            Feedback feedback = result.getDocumentReviews().get(resultKey(document.getDocumentType()));
            document.setSummary(feedback.getSummary());
            if (reviewDAO.updateDocumentSummary(document) != 1) throw new IllegalStateException("문서 요약 저장 실패");
            documentsByType.put(document.getDocumentType(), document);
            saveStrengths(review.getReviewNum(), document.getReviewDocumentNum(), feedback.getStrengths());
            int order = 1;
            for (Improvement improvement : feedback.getImprovements()) {
                improvement.setReviewDocumentNum(document.getReviewDocumentNum());
                improvement.setDisplayOrder(order++);
                reviewDAO.insertImprovement(improvement);
            }
        }
        saveStrengths(review.getReviewNum(), null, result.getStrengths());
        int order = 1;
        for (Consistency issue : result.getConsistencyIssues()) {
            issue.setReviewNum(review.getReviewNum());
            issue.setDisplayOrder(order++);
            reviewDAO.insertConsistency(issue);
            int sourceOrder = 1;
            for (Source source : issue.getSources()) {
                Document document = documentsByType.get(source.getDocumentType());
                if (document == null) throw new IllegalStateException("결과의 비교 문서가 선택한 서류와 다릅니다.");
                source.setReviewNum(review.getReviewNum());
                source.setConsistencyNum(issue.getConsistencyNum());
                source.setReviewDocumentNum(document.getReviewDocumentNum());
                source.setDisplayOrder(sourceOrder++);
                reviewDAO.insertSource(source);
            }
        }
        if (reviewDAO.completeReview(review) != 1) throw new IllegalStateException("첨삭 결과 저장을 완료하지 못했습니다.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentReviewDTO> getReviews(int userNum, int offset, int pageSize) {
        if (offset < 0 || pageSize < 1 || pageSize > 100) throw new IllegalArgumentException("조회 범위가 올바르지 않습니다.");
        return reviewDAO.selectReviews(userNum, offset, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentReviewResultDTO getReview(int userNum, long reviewNum) {
        if (reviewNum < 1) throw new IllegalArgumentException("첨삭 기록 번호가 올바르지 않습니다.");
        var result = reviewDAO.selectReview(userNum, reviewNum);
        if (result == null) throw new NoSuchElementException("첨삭 기록을 찾을 수 없습니다.");
        result.setDocuments(reviewDAO.selectDocuments(reviewNum));
        if (!"COMPLETED".equals(result.getReviewStatus())) return result;
        if (result.getCareerPreparationJson() != null) {
            try {
                result.setCareerPreparation(objectMapper.readValue(result.getCareerPreparationJson(), CareerPreparationDTO.class));
            } catch (IOException exception) {
                throw new IllegalStateException("저장된 취업 준비 추천 결과를 읽지 못했습니다.", exception);
            }
        }
        result.setStrengths(new ArrayList<>());
        result.setDocumentReviews(new LinkedHashMap<>());
        Map<Long, Feedback> feedbackByDocument = new LinkedHashMap<>();
        for (Document document : result.getDocuments()) {
            var feedback = new Feedback();
            feedback.setSummary(document.getSummary());
            result.getDocumentReviews().put(resultKey(document.getDocumentType()), feedback);
            feedbackByDocument.put(document.getReviewDocumentNum(), feedback);
        }
        result.getDocumentReviews().putIfAbsent("portfolio", null);
        for (Strength strength : reviewDAO.selectStrengths(reviewNum)) {
            if (strength.getSourcesJson() != null) {
                try {
                    strength.setSources(objectMapper.readValue(strength.getSourcesJson(),
                            new com.fasterxml.jackson.core.type.TypeReference<List<Source>>() {}));
                } catch (IOException exception) { throw new IllegalStateException("저장된 강점 근거를 읽지 못했습니다.", exception); }
            }
            var strengths = strength.getReviewDocumentNum() == null ? result.getStrengths()
                    : feedbackByDocument.get(strength.getReviewDocumentNum()).getStrengths();
            strengths.add(strength);
        }
        for (Improvement improvement : reviewDAO.selectImprovements(reviewNum)) {
            feedbackByDocument.get(improvement.getReviewDocumentNum()).getImprovements().add(improvement);
        }
        var issues = reviewDAO.selectConsistencies(reviewNum);
        Map<Long, Consistency> issuesById = new LinkedHashMap<>();
        for (Consistency issue : issues) {
            issue.setSources(new ArrayList<>());
            issuesById.put(issue.getConsistencyNum(), issue);
        }
        for (Source source : reviewDAO.selectSources(reviewNum)) {
            issuesById.get(source.getConsistencyNum()).getSources().add(source);
        }
        result.setConsistencyIssues(issues);
        return result;
    }

    private void validateRequest(DocumentReviewRequestDTO request) {
        if (request == null) throw new IllegalArgumentException("첨삭 요청이 필요합니다.");
        validateDocumentNum(request.getResumeNum(), "이력서");
        validateDocumentNum(request.getLetterNum(), "자기소개서");
        if (request.getPortfolioNum() != null) validateDocumentNum(request.getPortfolioNum(), "포트폴리오");
        if (request.getReviewMode() == null || !MODES.contains(request.getReviewMode()))
            throw new IllegalArgumentException("올바른 첨삭 기준을 선택해 주세요.");
        validateTextLength(request.getCustomCriteria(), "직접 입력 기준");
        validateTextLength(request.getInstructions(), "추가 요청");
        if ("custom".equals(request.getReviewMode()) &&
                (request.getCustomCriteria() == null || request.getCustomCriteria().isBlank()))
            throw new IllegalArgumentException("직접 입력 기준을 입력해 주세요.");
    }

    private void validateDocumentNum(Integer number, String label) {
        if (number == null || number < 1) throw new IllegalArgumentException(label + "를 선택해 주세요.");
    }

    private void validateTextLength(String text, String label) {
        if (text != null && text.length() > MAX_TEXT_LENGTH)
            throw new IllegalArgumentException(label + "은 200자 이내로 입력해 주세요.");
    }

    private String normalizeOptionalText(String text) { return text == null || text.isBlank() ? null : text.strip(); }

    private List<Document> snapshotDocuments(int userNum, DocumentReviewRequestDTO request) {
        var resume = documentService.getResume(userNum, request.getResumeNum());
        var letter = documentService.getCoverLetter(userNum, request.getLetterNum());
        List<Document> documents = new ArrayList<>();
        documents.add(snapshot("resume", request.getResumeNum(), resume.getResume().getResumeTitle(),
                resume.getResume().getUpdatedAt(), resume));
        documents.add(snapshot("cover-letter", request.getLetterNum(), letter.getCoverLetterTitle(), letter.getUpdatedAt(), letter));
        if (request.getPortfolioNum() != null) {
            var portfolio = documentService.getPortfolio(userNum, request.getPortfolioNum());
            var document = snapshot("portfolio", request.getPortfolioNum(), portfolio.getPortfolioTitle(), portfolio.getUpdatedAt(), portfolio);
            document.setOriginalFileName(portfolio.getOriginalFileName());
            try (var stream = documentService.getPortfolioFile(userNum, request.getPortfolioNum()).getInputStream()) {
                byte[] bytes = stream.readNBytes(MAX_PDF_SIZE + 1);
                if (bytes.length > MAX_PDF_SIZE) throw new IllegalArgumentException("포트폴리오 파일은 20MB 이하여야 합니다.");
                document.setPdfSnapshot(bytes);
            } catch (IOException exception) {
                throw new IllegalStateException("포트폴리오 보관본을 읽지 못했습니다.", exception);
            }
            documents.add(document);
        }
        return documents;
    }

    private Document snapshot(String type, int number, String title, Date updatedAt, Object value) {
        var document = new Document();
        document.setDocumentType(type);
        document.setSourceDocumentNum(number);
        document.setDocumentTitle(title);
        document.setSourceUpdatedAt(updatedAt);
        try { document.setSourceSnapshotJson(objectMapper.writeValueAsString(value)); }
        catch (IOException exception) { throw new IllegalStateException("서류 보관본을 만들지 못했습니다.", exception); }
        return document;
    }

    private void saveStrengths(Long reviewNum, Long documentNum, List<Strength> strengths) {
        int order = 1;
        for (Strength strength : strengths) {
            strength.setReviewNum(reviewNum);
            strength.setReviewDocumentNum(documentNum);
            strength.setDisplayOrder(order++);
            try { strength.setSourcesJson(objectMapper.writeValueAsString(strength.getSources())); }
            catch (IOException exception) { throw new IllegalStateException("강점 근거를 저장하지 못했습니다.", exception); }
            reviewDAO.insertStrength(strength);
        }
    }

    private String resultKey(String type) { return "cover-letter".equals(type) ? "coverLetter" : type; }
}
