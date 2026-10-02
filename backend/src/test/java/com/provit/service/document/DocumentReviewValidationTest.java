package com.provit.service.document;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.http.Cookie;
import org.springframework.test.util.ReflectionTestUtils;
import com.provit.common.GlobalExceptionHandler;
import com.provit.common.auth.AuthCookieService;
import com.provit.common.resolver.LoginUserArgumentResolver;
import com.provit.controller.document.DocumentReviewController;
import com.provit.dao.document.DocumentDAO;
import com.provit.dao.document.DocumentReviewDAO;
import com.provit.dto.document.*;
import com.provit.service.document.impl.DocumentReviewServiceImpl;
import com.provit.service.document.impl.DocumentServiceImpl;
import com.provit.util.jwt.JwtProvider;

public class DocumentReviewValidationTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicInteger reads = new AtomicInteger();
    private final AtomicInteger writes = new AtomicInteger();
    private DocumentReviewService service;
    private MockMvc mvc;

    @Test
    public void currentAndPreviousModelRecordsAreRecognizedAsAi() {
        var review = new DocumentReviewDTO();
        for (String model : List.of("gpt-6.1-sol", "gpt-6-sol")) {
            review.setModelName(model);
            assertEquals("AI", review.getResultSource());
        }
        review.setModelName("dummy-document-review-v2");
        assertEquals("DUMMY", review.getResultSource());
        review.setModelName(null);
        assertEquals("UNKNOWN", review.getResultSource());
    }

    @Before
    public void setUp() {
        reads.set(0);
        writes.set(0);
        var documents = (DocumentDAO) Proxy.newProxyInstance(DocumentDAO.class.getClassLoader(),
                new Class<?>[] { DocumentDAO.class }, (proxy, method, args) -> {
                    reads.incrementAndGet();
                    switch (method.getName()) {
                        case "selectResume":
                            if (!args[0].equals(7) || !args[1].equals(11)) return null;
                            var resume = new ResumeDTO();
                            resume.setResumeNum(11); resume.setUserNum(7); resume.setResumeTitle("내 이력서");
                            return resume;
                        case "selectCoverLetter":
                            if (!args[0].equals(7) || !args[1].equals(12)) return null;
                            var letter = new CoverLetterDTO();
                            letter.setLetterNum(12); letter.setUserNum(7); letter.setCoverLetterTitle("내 자기소개서");
                            return letter;
                        case "selectPortfolioByPortfolioNum":
                            var portfolio = new PortfolioDTO();
                            portfolio.setPortfolioNum(13); portfolio.setUserNum(8);
                            return portfolio;
                        default:
                            if (method.getName().endsWith("List")) return List.of();
                            throw new AssertionError(method.getName());
                    }
                });
        var reviews = (DocumentReviewDAO) Proxy.newProxyInstance(DocumentReviewDAO.class.getClassLoader(),
                new Class<?>[] { DocumentReviewDAO.class }, (proxy, method, args) -> {
                    if (method.getName().equals("selectReview")) {
                        assertEquals(8, args[0]);
                        reads.incrementAndGet();
                        return null;
                    }
                    writes.incrementAndGet();
                    throw new AssertionError("Unexpected persistence: " + method.getName());
                });
        var transactionManager = (PlatformTransactionManager) Proxy.newProxyInstance(
                PlatformTransactionManager.class.getClassLoader(),
                new Class<?>[] { PlatformTransactionManager.class }, (proxy, method, args) -> {
                    throw new AssertionError("Unexpected transaction: " + method.getName());
                });
        service = new DocumentReviewServiceImpl(reviews, new DocumentServiceImpl(documents, null),
                (request, snapshots) -> { throw new AssertionError("Unexpected AI request"); },
                transactionManager);
        JwtProvider jwt = new JwtProvider(null) {
            @Override public boolean validateToken(String token) { return "owner".equals(token) || "other".equals(token); }
            @Override public Long getUserNum(String token) { return "owner".equals(token) ? 7L : 8L; }
        };
        var cookies = new AuthCookieService();
        ReflectionTestUtils.setField(cookies, "cookieName", "provit_access");
        mvc = MockMvcBuilders.standaloneSetup(new DocumentReviewController(service))
                .setCustomArgumentResolvers(new LoginUserArgumentResolver(jwt, cookies))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    public void unauthenticatedCallsDoNotReadDocumentsOrWriteRecords() throws Exception {
        String body = mapper.writeValueAsString(request());
        assertEquals(401, mvc.perform(post("/api/document-reviews").contentType(MediaType.APPLICATION_JSON)
                .content(body)).andReturn().getResponse().getStatus());
        assertEquals(401, mvc.perform(get("/api/document-reviews")).andReturn().getResponse().getStatus());
        assertEquals(401, mvc.perform(get("/api/document-reviews/1").cookie(new Cookie("provit_access", "invalid")))
                .andReturn().getResponse().getStatus());
        assertEquals(0, reads.get()); assertEquals(0, writes.get());
    }

    @Test
    public void serverRejectsInvalidCriteriaAndLengthsBeforeDocumentReads() {
        var request = request();
        request.setReviewMode("unknown");
        assertThrows(IllegalArgumentException.class, () -> service.createReview(7, request));
        request.setReviewMode("custom");
        for (String text : new String[] { null, "", " \t\n　", "가".repeat(201) }) {
            request.setCustomCriteria(text);
            assertThrows(IllegalArgumentException.class, () -> service.createReview(7, request));
        }
        request.setCustomCriteria("실제 역할을 확인해 주세요.");
        request.setInstructions("요".repeat(201));
        assertThrows(IllegalArgumentException.class, () -> service.createReview(7, request));
        assertEquals(0, reads.get()); assertEquals(0, writes.get());
    }

    @Test
    public void twoHundredCharactersAndEveryPresetPassValidation() {
        var request = request();
        request.setResumeNum(99);
        request.setCustomCriteria("가".repeat(200));
        request.setInstructions("나".repeat(200));
        for (String mode : List.of("comprehensive", "expression", "consistency", "jobFit", "evidence", "custom")) {
            request.setReviewMode(mode);
            assertThrows(NoSuchElementException.class, () -> service.createReview(7, request));
        }
        assertEquals(6, reads.get()); assertEquals(0, writes.get());
    }

    @Test
    public void requiredDocumentIdsAndOptionalPortfolioIdsMustBePositive() {
        var request = request();
        request.setResumeNum(null);
        assertThrows(IllegalArgumentException.class, () -> service.createReview(7, request));
        request.setResumeNum(11); request.setLetterNum(0);
        assertThrows(IllegalArgumentException.class, () -> service.createReview(7, request));
        request.setLetterNum(12); request.setPortfolioNum(-1);
        assertThrows(IllegalArgumentException.class, () -> service.createReview(7, request));
        assertEquals(0, reads.get()); assertEquals(0, writes.get());
    }

    @Test
    public void foreignResumeLetterOrPortfolioCannotCreateAnyReviewRows() {
        var request = request();
        assertThrows(NoSuchElementException.class, () -> service.createReview(8, request));
        request.setLetterNum(99);
        assertThrows(NoSuchElementException.class, () -> service.createReview(7, request));
        request.setLetterNum(12); request.setPortfolioNum(13);
        assertThrows(SecurityException.class, () -> service.createReview(7, request));
        assertEquals(0, writes.get());
    }

    @Test
    public void spoofedUserIdCannotBypassRecordOwnershipAndInvalidPagingIsRejected() throws Exception {
        assertEquals(404, mvc.perform(get("/api/document-reviews/1").param("userNum", "7")
                .cookie(new Cookie("provit_access", "other"))).andReturn().getResponse().getStatus());
        assertEquals(400, mvc.perform(get("/api/document-reviews").param("pageSize", "101")
                .cookie(new Cookie("provit_access", "owner"))).andReturn().getResponse().getStatus());
        assertEquals(0, writes.get());
    }

    private DocumentReviewRequestDTO request() {
        var request = new DocumentReviewRequestDTO();
        request.setResumeNum(11); request.setLetterNum(12); request.setReviewMode("comprehensive");
        return request;
    }
}
