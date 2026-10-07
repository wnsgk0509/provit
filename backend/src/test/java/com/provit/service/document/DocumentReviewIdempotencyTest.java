package com.provit.service.document;

import static org.junit.Assert.*;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import com.provit.dao.document.DocumentReviewDAO;
import com.provit.dto.document.*;
import com.provit.dto.document.DocumentReviewResultDTO.Document;
import com.provit.dto.document.DocumentReviewResultDTO.Feedback;
import com.provit.service.document.generator.DocumentReviewGenerator;
import com.provit.service.document.impl.DocumentReviewServiceImpl;

public class DocumentReviewIdempotencyTest {
    private final Map<String, DocumentReviewDTO> records = new ConcurrentHashMap<>();
    private final Map<Long, DocumentReviewDTO> byNumber = new ConcurrentHashMap<>();
    private final Map<Long, List<Document>> snapshots = new ConcurrentHashMap<>();
    private final AtomicInteger insertCount = new AtomicInteger();
    private final AtomicInteger sourceReads = new AtomicInteger();
    private final AtomicInteger aiCalls = new AtomicInteger();
    private DocumentReviewDAO dao;
    private DocumentService documents;
    private DocumentReviewService service;
    private volatile CyclicBarrier firstLookupBarrier;
    private final AtomicInteger lookups = new AtomicInteger();
    private RuntimeException duplicateInsertFailure;

    @Before
    public void setUp() {
        records.clear(); byNumber.clear(); snapshots.clear();
        insertCount.set(0); sourceReads.set(0); aiCalls.set(0); lookups.set(0);
        duplicateInsertFailure = new DuplicateKeyException("Unique request ID");
        dao = (DocumentReviewDAO) Proxy.newProxyInstance(DocumentReviewDAO.class.getClassLoader(),
                new Class<?>[] { DocumentReviewDAO.class }, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "selectByRequestId":
                            var found = records.get(args[0] + ":" + args[1]);
                            if (firstLookupBarrier != null && lookups.incrementAndGet() <= 2)
                                firstLookupBarrier.await(5, TimeUnit.SECONDS);
                            return found;
                        case "insertReview":
                            var review = (DocumentReviewDTO) args[0];
                            var key = review.getUserNum() + ":" + review.getRequestId();
                            review.setReviewNum((long) review.getUserNum());
                            review.setReviewStatus("PROCESSING");
                            if (records.putIfAbsent(key, review) != null) throw duplicateInsertFailure;
                            byNumber.put(review.getReviewNum(), review);
                            snapshots.put(review.getReviewNum(), new CopyOnWriteArrayList<>());
                            insertCount.incrementAndGet();
                            return 1;
                        case "insertDocument":
                            var document = (Document) args[0];
                            document.setReviewDocumentNum((long) document.getSourceDocumentNum());
                            snapshots.get(document.getReviewNum()).add(document);
                            return 1;
                        case "selectReview":
                            var saved = byNumber.get((Long) args[1]);
                            if (saved == null || saved.getUserNum() != (int) args[0]) return null;
                            var result = new DocumentReviewResultDTO();
                            BeanUtils.copyProperties(saved, result);
                            return result;
                        case "selectDocuments": return List.copyOf(snapshots.get((Long) args[0]));
                        case "lockReview": return byNumber.get((Long) args[1]).getReviewStatus();
                        case "updateDocumentSummary": return 1;
                        case "completeReview":
                            ((DocumentReviewDTO) args[0]).setReviewStatus("COMPLETED"); return 1;
                        case "failReview":
                            ((DocumentReviewDTO) args[0]).setReviewStatus("FAILED"); return 1;
                        case "selectStrengths": case "selectImprovements": case "selectConsistencies": case "selectSources":
                            return List.of();
                        default: throw new AssertionError(method.getName());
                    }
                });
        documents = (DocumentService) Proxy.newProxyInstance(DocumentService.class.getClassLoader(),
                new Class<?>[] { DocumentService.class }, (proxy, method, args) -> {
                    sourceReads.incrementAndGet();
                    if (method.getName().equals("getResume")) {
                        var resume = new ResumeDTO(); resume.setResumeTitle("원본 이력서");
                        var detail = new ResumeDetailDTO(); detail.setResume(resume); return detail;
                    }
                    if (method.getName().equals("getCoverLetter")) {
                        var letter = new CoverLetterDTO(); letter.setCoverLetterTitle("자기소개서"); return letter;
                    }
                    throw new AssertionError(method.getName());
                });
        service = service((request, inputs) -> { aiCalls.incrementAndGet(); return result(); });
    }

    private DocumentReviewService service(DocumentReviewGenerator generator) {
        var transactions = new AbstractPlatformTransactionManager() {
            @Override protected Object doGetTransaction() { return new Object(); }
            @Override protected void doBegin(Object transaction, TransactionDefinition definition) { }
            @Override protected void doCommit(DefaultTransactionStatus status) { }
            @Override protected void doRollback(DefaultTransactionStatus status) { }
        };
        return new DocumentReviewServiceImpl(dao, documents, generator, transactions);
    }

    private DocumentReviewResultDTO result() {
        var result = new DocumentReviewResultDTO(); result.setSummary("저장된 첨삭");
        for (String type : List.of("resume", "coverLetter")) {
            var feedback = new Feedback(); feedback.setSummary(type);
            result.getDocumentReviews().put(type, feedback);
        }
        return result;
    }

    private DocumentReviewRequestDTO request() {
        var request = new DocumentReviewRequestDTO(); request.setRequestId(UUID.randomUUID().toString());
        request.setResumeNum(11); request.setLetterNum(12); request.setReviewMode("comprehensive");
        return request;
    }

    @Test
    public void completedRetryReturnsOriginalWithoutReadingChangedOrDeletedSources() {
        var request = request();
        var first = service.createReview(7, request);
        int reads = sourceReads.get();
        documents = (DocumentService) Proxy.newProxyInstance(DocumentService.class.getClassLoader(),
                new Class<?>[] { DocumentService.class }, (proxy, method, args) -> { throw new NoSuchElementException("Deleted source"); });
        var restartedService = service((ignored, inputs) -> { throw new AssertionError("Duplicate AI call"); });
        var retry = restartedService.createReview(7, request);
        assertEquals(first, retry);
        assertEquals(reads, sourceReads.get()); assertEquals(1, aiCalls.get()); assertEquals(1, insertCount.get());
        assertEquals(first, restartedService.getReviewByRequestId(7, request.getRequestId()));
    }

    @Test
    public void concurrentRequestsAcrossServiceInstancesGenerateAndSaveOnlyOnce() throws Exception {
        assertConcurrentRequestsReuseOneRecord();
    }

    @Test
    public void genericOracleIntegrityExceptionStillReusesTheConcurrentRequest() throws Exception {
        duplicateInsertFailure = new org.springframework.dao.DataIntegrityViolationException("Oracle unique constraint",
                new java.sql.SQLException("Unique request ID", "23000", 1));
        assertConcurrentRequestsReuseOneRecord();
    }

    @Test
    public void uncategorizedOracleUniqueExceptionStillReusesTheConcurrentRequest() throws Exception {
        duplicateInsertFailure = new org.springframework.jdbc.UncategorizedSQLException("insert", "INSERT",
                new java.sql.SQLException("Unique request ID", "23000", 1));
        assertConcurrentRequestsReuseOneRecord();
    }

    private void assertConcurrentRequestsReuseOneRecord() throws Exception {
        firstLookupBarrier = new CyclicBarrier(2);
        var started = new CountDownLatch(1); var release = new CountDownLatch(1);
        DocumentReviewGenerator generator = (request, inputs) -> {
            aiCalls.incrementAndGet(); started.countDown();
            try { assertTrue(release.await(5, TimeUnit.SECONDS)); }
            catch (InterruptedException exception) { throw new AssertionError(exception); }
            return result();
        };
        var first = service(generator); var second = service(generator); var request = request();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var completed = new ExecutorCompletionService<DocumentReviewResultDTO>(executor);
            completed.submit(() -> first.createReview(7, request));
            completed.submit(() -> second.createReview(7, request));
            assertTrue(started.await(5, TimeUnit.SECONDS));
            var processing = completed.poll(5, TimeUnit.SECONDS);
            assertNotNull(processing); assertEquals("PROCESSING", processing.get().getReviewStatus());
            release.countDown();
            var finished = completed.poll(5, TimeUnit.SECONDS);
            assertNotNull(finished); assertEquals("COMPLETED", finished.get().getReviewStatus());
            assertEquals(1, aiCalls.get()); assertEquals(1, insertCount.get());
            assertEquals(2, snapshots.get(7L).size());
        } finally { release.countDown(); executor.shutdownNow(); }
    }

    @Test
    public void otherIntegrityFailuresAreNotTreatedAsSuccessfulDuplicates() {
        var failure = new org.springframework.dao.DataIntegrityViolationException("Too long",
                new java.sql.SQLException("Title exceeds column size", "72000", 12899));
        assertReservationFailureIsPropagated(failure);
    }

    @Test
    public void duplicateWithoutMatchingSavedRequestPropagatesTheOriginalFailure() {
        assertReservationFailureIsPropagated(new DuplicateKeyException("Unrelated unique constraint"));
    }

    private void assertReservationFailureIsPropagated(RuntimeException failure) {
        var originalDAO = dao;
        dao = (DocumentReviewDAO) Proxy.newProxyInstance(DocumentReviewDAO.class.getClassLoader(),
                new Class<?>[] { DocumentReviewDAO.class }, (proxy, method, args) -> {
                    if (method.getName().equals("insertReview")) throw failure;
                    try { return method.invoke(originalDAO, args); }
                    catch (java.lang.reflect.InvocationTargetException exception) { throw exception.getCause(); }
                });
        var failing = service((request, inputs) -> { aiCalls.incrementAndGet(); return result(); });
        assertSame(failure, assertThrows(RuntimeException.class, () -> failing.createReview(7, request())));
        assertEquals(0, aiCalls.get()); assertEquals(0, insertCount.get());
    }

    @Test
    public void failedRequestIsReturnedWithoutRegenerating() {
        var request = request();
        var failing = service((ignored, inputs) -> {
            aiCalls.incrementAndGet(); throw new DocumentReviewProcessingException("AI 장애", 503);
        });
        var error = assertThrows(DocumentReviewProcessingException.class, () -> failing.createReview(7, request));
        var retry = service.createReview(7, request);
        assertEquals(error.getReviewNum(), retry.getReviewNum()); assertEquals("FAILED", retry.getReviewStatus());
        assertEquals("AI 장애", retry.getErrorMessage()); assertEquals(1, aiCalls.get()); assertEquals(1, insertCount.get());
    }

    @Test
    public void changedPayloadWithExistingIdIsRejectedBeforeAnyAiOrSourceReads() {
        var request = request(); service.createReview(7, request);
        int reads = sourceReads.get();
        request.setInstructions("다른 요청");
        assertThrows(DocumentReviewRequestConflictException.class, () -> service.createReview(7, request));
        request.setInstructions(null); request.setResumeNum(99);
        assertThrows(DocumentReviewRequestConflictException.class, () -> service.createReview(7, request));
        request.setResumeNum(11); request.setLetterNum(99);
        assertThrows(DocumentReviewRequestConflictException.class, () -> service.createReview(7, request));
        request.setLetterNum(12); request.setPortfolioNum(99);
        assertThrows(DocumentReviewRequestConflictException.class, () -> service.createReview(7, request));
        request.setPortfolioNum(null); request.setReviewMode("expression");
        assertThrows(DocumentReviewRequestConflictException.class, () -> service.createReview(7, request));
        assertEquals(reads, sourceReads.get()); assertEquals(1, aiCalls.get()); assertEquals(1, insertCount.get());
    }

    @Test
    public void equivalentWhitespaceAndUuidCaseReturnTheSameRequest() {
        var request = request(); request.setReviewMode("custom"); request.setCustomCriteria("  기준  ");
        request.setInstructions("  추가 요청  ");
        var first = service.createReview(7, request);
        request.setRequestId(request.getRequestId().toUpperCase(java.util.Locale.ROOT));
        request.setCustomCriteria("기준"); request.setInstructions("추가 요청");
        assertEquals(first, service.createReview(7, request)); assertEquals(1, aiCalls.get());
    }

    @Test
    public void requestIdsAreScopedToTheAuthenticatedOwner() {
        var request = request(); service.createReview(7, request);
        assertThrows(NoSuchElementException.class, () -> service.getReviewByRequestId(8, request.getRequestId()));
        var other = service.createReview(8, request);
        assertEquals(8, other.getUserNum()); assertEquals(2, aiCalls.get()); assertEquals(2, insertCount.get());
    }

    @Test
    public void missingOrMalformedIdIsRejectedBeforeAnyReadOrWrite() {
        var request = request();
        for (String id : new String[] { null, "", "1-1-1-1-1", "x".repeat(36) }) {
            request.setRequestId(id);
            assertThrows(IllegalArgumentException.class, () -> service.createReview(7, request));
        }
        assertEquals(0, sourceReads.get()); assertEquals(0, aiCalls.get()); assertEquals(0, insertCount.get());
    }

    @Test
    public void controllerReturnsSavedRecordByIdAndConflictForChangedPayload() throws Exception {
        var request = request(); service.createReview(7, request);
        var owner = new org.springframework.web.method.support.HandlerMethodArgumentResolver() {
            @Override public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
                return parameter.hasParameterAnnotation(com.provit.common.annotation.LoginUser.class);
            }
            @Override public Object resolveArgument(org.springframework.core.MethodParameter parameter,
                    org.springframework.web.method.support.ModelAndViewContainer container,
                    org.springframework.web.context.request.NativeWebRequest webRequest,
                    org.springframework.web.bind.support.WebDataBinderFactory binder) { return 7L; }
        };
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(new com.provit.controller.document.DocumentReviewController(service))
                .setCustomArgumentResolvers(owner).setControllerAdvice(new com.provit.common.GlobalExceptionHandler()).build();
        var response = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/document-reviews/requests/" + request.getRequestId())).andReturn().getResponse();
        assertEquals(200, response.getStatus()); assertEquals("no-store", response.getHeader("Cache-Control"));
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        assertEquals("COMPLETED", mapper.readTree(response.getContentAsByteArray()).path("data").path("reviewStatus").asText());
        assertFalse(mapper.readTree(response.getContentAsByteArray()).path("data").has("requestHash"));
        request.setInstructions("다른 요청");
        assertEquals(409, mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/document-reviews").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))).andReturn().getResponse().getStatus());
        assertEquals(1, aiCalls.get()); assertEquals(1, insertCount.get());
    }
}
