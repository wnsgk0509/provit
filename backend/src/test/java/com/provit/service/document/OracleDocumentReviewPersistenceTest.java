package com.provit.service.document;

import static org.junit.Assert.*;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import javax.servlet.http.Cookie;
import org.springframework.test.util.ReflectionTestUtils;
import com.provit.common.GlobalExceptionHandler;
import com.provit.common.auth.AuthCookieService;
import com.provit.common.resolver.LoginUserArgumentResolver;
import com.provit.controller.document.DocumentReviewController;
import com.provit.util.jwt.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.provit.dao.document.DocumentReviewDAO;
import com.provit.dao.document.impl.DocumentReviewDAOImpl;
import com.provit.dto.document.*;
import com.provit.service.document.impl.DocumentReviewServiceImpl;

public class OracleDocumentReviewPersistenceTest {
    private final String prefix = "DRT" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    private final Map<String, String> names = new LinkedHashMap<>();
    private final List<String[]> objects = new ArrayList<>();
    private Connection connection;
    private TransactionTemplate transaction;
    private DocumentReviewDAO dao;
    private DocumentReviewService service;
    private DocumentService documents;
    private DataSourceTransactionManager transactionManager;
    private ResumeDetailDTO resume;
    private byte[] pdf = "%PDF-1.7\nportfolio snapshot".getBytes(StandardCharsets.US_ASCII);
    private boolean documentsDeleted;

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue("Opt in with -Dprovit.oracle.documentReviewTests=true",
                Boolean.getBoolean("provit.oracle.documentReviewTests"));
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources/database.properties"), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        var dataSource = new DriverManagerDataSource();
        dataSource.setUrl(properties.getProperty("db.url"));
        dataSource.setUsername(properties.getProperty("db.username"));
        dataSource.setPassword(properties.getProperty("db.password"));
        var settings = new Properties();
        settings.setProperty("oracle.net.CONNECT_TIMEOUT", "5000");
        settings.setProperty("oracle.jdbc.ReadTimeout", "10000");
        dataSource.setConnectionProperties(settings);
        connection = dataSource.getConnection();
        String ddl = "CREATE TABLE T_USER (USER_NUM NUMBER(9) PRIMARY KEY);\n" + resource("/sql_query/document_review_schema.sql");
        var matcher = Pattern.compile("\\b(?:SEQ_T_|T_|FK_|CK_|UQ_|IDX_|PK_)[A-Z_]+\\b").matcher(ddl);
        while (matcher.find()) names.computeIfAbsent(matcher.group(), key -> prefix + "_" + names.size());
        ddl = isolatedSql(ddl);
        matcher = Pattern.compile("CREATE (TABLE|SEQUENCE) (\\w+)").matcher(ddl);
        while (matcher.find()) objects.add(new String[] { matcher.group(1), matcher.group(2) });
        ScriptUtils.executeSqlScript(connection,
                new EncodedResource(new ByteArrayResource(ddl.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO " + names.get("T_USER") + " (USER_NUM) VALUES (7)");
            statement.executeUpdate("INSERT INTO " + names.get("T_USER") + " (USER_NUM) VALUES (8)");
        }
        var configuration = new Configuration(new Environment("review-test", new SpringManagedTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(true);
        String xml = isolatedSql(resource("/mappers/document/document_review_mapper.xml"));
        try (var reader = new java.io.StringReader(xml)) {
            new XMLMapperBuilder(reader, configuration, "review-test-mapper", configuration.getSqlFragments()).parse();
        }
        dao = new DocumentReviewDAOImpl(new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(configuration)));
        transactionManager = new DataSourceTransactionManager(dataSource);
        transaction = new TransactionTemplate(transactionManager);
        resume = new ResumeDetailDTO();
        var resumeInfo = new ResumeDTO();
        resumeInfo.setUserNum(7); resumeInfo.setResumeNum(11); resumeInfo.setResumeTitle("저장 당시 이력서");
        resumeInfo.setMotivation("원본 지원 동기");
        resumeInfo.setOccupationCode("2"); resumeInfo.setOccupationName("IT개발·데이터");
        resumeInfo.setJobCode("84"); resumeInfo.setJobName("백엔드/서버개발");
        resumeInfo.setUpdatedAt(new java.util.Date(1_750_000_000_000L));
        resume.setResume(resumeInfo); resume.setCareerList(List.of());
        resume.setEducationList(List.of()); resume.setCertificationList(List.of());
        documents = (DocumentService) Proxy.newProxyInstance(DocumentService.class.getClassLoader(),
                new Class<?>[] { DocumentService.class }, (proxy, method, args) -> {
                    if (documentsDeleted) throw new NoSuchElementException("원본 삭제됨");
                    assertEquals(7, args[0]);
                    switch (method.getName()) {
                        case "getResume": return resume;
                        case "getCoverLetter":
                            var letter = new CoverLetterDTO();
                            letter.setUserNum(7); letter.setLetterNum(12); letter.setCoverLetterTitle("저장 당시 자기소개서");
                            letter.setProblemSolvingExperience("실제 사용자 경험");
                            return letter;
                        case "getPortfolio":
                            var portfolio = new PortfolioDTO();
                            portfolio.setUserNum(7); portfolio.setPortfolioNum(13); portfolio.setPortfolioTitle("포트폴리오");
                            portfolio.setOriginalFileName("내 포트폴리오.pdf");
                            portfolio.setFileUrl("D:/private/path.pdf"); portfolio.setSavedFileName("private.pdf");
                            return portfolio;
                        case "getPortfolioFile": return new ByteArrayResource(pdf);
                        default: throw new AssertionError(method.getName());
                    }
                });
        service = transactionalService(dao);
    }

    @After
    public void dropOnlyIsolatedObjects() throws Exception {
        if (connection == null) return;
        try (var testConnection = connection) {
            var reverse = new ArrayList<>(objects);
            Collections.reverse(reverse);
            for (String[] object : reverse) {
                assertTrue(object[1].startsWith(prefix + "_"));
                try (var query = testConnection.prepareStatement("SELECT COUNT(*) FROM USER_OBJECTS WHERE OBJECT_NAME = ? AND OBJECT_TYPE = ?")) {
                    query.setString(1, object[1]); query.setString(2, object[0]);
                    try (var rows = query.executeQuery()) {
                        rows.next();
                        if (rows.getInt(1) == 0) continue;
                    }
                }
                try (var statement = testConnection.createStatement()) {
                    statement.execute("DROP " + object[0] + " " + object[1] + ("TABLE".equals(object[0]) ? " PURGE" : ""));
                }
            }
        }
    }

    @Test
    public void storesEntireResponseSnapshotsAndTwoHundredCharacterRequests() throws Exception {
        var request = request(true);
        request.setReviewMode("custom"); request.setCustomCriteria("가".repeat(200)); request.setInstructions("나".repeat(200));
        var saved = transaction.execute(status -> service.createReview(7, request));
        assertNotNull(saved.getReviewNum()); assertEquals("COMPLETED", saved.getReviewStatus());
        assertEquals("AI", saved.getResultSource()); assertNotNull(saved.getFinishedAt());
        assertEquals(request.getCustomCriteria(), saved.getCustomCriteria());
        assertEquals(request.getInstructions(), saved.getInstructions());
        assertEquals(3, saved.getDocuments().size());
        assertEquals(3, saved.getResponseVersion());
        assertEquals("백엔드/서버개발", saved.getCareerPreparation().getJobName());
        assertEquals(3, saved.getCareerPreparation().getRecommendations().size());
        assertCounts(1, 3, 0, 4, 2, 5);
        var mapper = new ObjectMapper();
        var expected = mapper.valueToTree(reviewFixture(true));
        var actual = mapper.valueToTree(saved);
        for (String field : List.of("summary", "strengths", "documentReviews", "consistencyIssues", "careerPreparation")) {
            assertEquals(field, expected.path(field), actual.path(field));
        }
        String json = mapper.writeValueAsString(saved);
        assertFalse(json.contains("sourceSnapshotJson")); assertFalse(json.contains("pdfSnapshot"));
        assertFalse(json.contains("careerPreparationJson"));
        assertFalse(json.contains("D:/private")); assertFalse(json.contains("private.pdf"));
        try (var query = connection.prepareStatement("SELECT SOURCE_SNAPSHOT_JSON, PDF_SNAPSHOT FROM "
                + names.get("T_REVIEW_DOCUMENT") + " WHERE DOCUMENT_TYPE = ?")) {
            query.setString(1, "portfolio");
            try (var rows = query.executeQuery()) {
                assertTrue(rows.next()); assertArrayEquals(pdf, rows.getBytes(2));
                assertFalse(rows.getString(1).contains("fileUrl"));
            }
            query.setString(1, "resume");
            try (var rows = query.executeQuery()) {
                assertTrue(rows.next());
                assertEquals("원본 지원 동기", mapper.readTree(rows.getString(1)).path("resume").path("motivation").asText());
            }
        }
    }

    @Test
    public void withoutPortfolioPersistsOnlySelectedDocumentsAndSurvivesOriginalChanges() {
        var request = request(false);
        request.setCustomCriteria("선택하지 않은 이전 입력"); request.setInstructions("  추가 요청  ");
        var saved = transaction.execute(status -> service.createReview(7, request));
        assertNull(saved.getCustomCriteria()); assertEquals("추가 요청", saved.getInstructions());
        assertNull(saved.getDocumentReviews().get("portfolio"));
        assertCounts(1, 2, 0, 3, 1, 3);
        resume.getResume().setResumeTitle("수정된 이력서"); documentsDeleted = true;
        var reread = transaction.execute(status -> service.getReview(7, saved.getReviewNum()));
        assertEquals("저장 당시 이력서", reread.getDocuments().get(0).getDocumentTitle());
        assertEquals(saved.getSummary(), reread.getSummary());
        assertEquals(saved.getCareerPreparation(), reread.getCareerPreparation());
        assertEquals(1, service.getReviews(7, 0, 20).size()); assertEquals(0, service.getReviews(7, 1, 20).size());
        assertEquals(0, service.getReviews(8, 0, 20).size());
        assertThrows(NoSuchElementException.class, () -> service.getReview(8, saved.getReviewNum()));
    }

    @Test
    public void childInsertFailureRollsBackResultButRetainsFailedRequestAndSnapshots() {
        var failingDAO = (DocumentReviewDAO) Proxy.newProxyInstance(DocumentReviewDAO.class.getClassLoader(),
                new Class<?>[] { DocumentReviewDAO.class }, (proxy, method, args) -> {
                    if (method.getName().equals("insertSource")) throw new IllegalStateException("Injected failure");
                    try { return method.invoke(dao, args); }
                    catch (java.lang.reflect.InvocationTargetException exception) { throw exception.getCause(); }
                });
        var failingService = transactionalService(failingDAO);
        var failure = assertThrows(DocumentReviewProcessingException.class, () -> failingService.createReview(7, request(true)));
        assertCounts(1, 3, 0, 0, 0, 0);
        var failed = service.getReview(7, failure.getReviewNum());
        assertEquals("FAILED", failed.getReviewStatus());
        assertNotNull(failed.getFinishedAt()); assertNotNull(failed.getErrorMessage());
        assertNull(failed.getCareerPreparation()); assertTrue(failed.getDocumentReviews().isEmpty());
    }

    @Test
    public void preparationIsStoredAsJsonAndLegacyRecordsRemainReadable() throws Exception {
        var saved = service.createReview(7, request(false));
        try (var query = connection.prepareStatement("SELECT CAREER_PREPARATION_JSON FROM "
                + names.get("T_DOCUMENT_REVIEW") + " WHERE REVIEW_NUM = ?")) {
            query.setLong(1, saved.getReviewNum());
            try (var rows = query.executeQuery()) {
                assertTrue(rows.next());
                var json = new ObjectMapper().readTree(rows.getString(1));
                assertEquals("84", json.path("jobCode").asText());
                assertEquals(3, json.path("recommendations").size());
            }
        }
        resume.getResume().setJobName("프론트엔드");
        assertEquals("백엔드/서버개발", service.getReview(7, saved.getReviewNum()).getCareerPreparation().getJobName());
        try (var query = connection.prepareStatement("UPDATE " + names.get("T_DOCUMENT_REVIEW")
                + " SET CAREER_PREPARATION_JSON = NULL, RESPONSE_VERSION = 1 WHERE REVIEW_NUM = ?")) {
            query.setLong(1, saved.getReviewNum()); query.executeUpdate();
        }
        assertNull(service.getReview(7, saved.getReviewNum()).getCareerPreparation());
    }

    @Test
    public void authenticatedRestRequestReturnsCreatedAndSavedRecordCanBeReadAgain() throws Exception {
        JwtProvider jwt = new JwtProvider(null) {
            @Override public boolean validateToken(String token) { return "owner".equals(token); }
            @Override public Long getUserNum(String token) { return 7L; }
        };
        var cookies = new AuthCookieService();
        ReflectionTestUtils.setField(cookies, "cookieName", "provit_access");
        var mvc = MockMvcBuilders.standaloneSetup(new DocumentReviewController(service))
                .setCustomArgumentResolvers(new LoginUserArgumentResolver(jwt, cookies))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        var mapper = new ObjectMapper();
        var response = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/document-reviews")
                .cookie(new Cookie("provit_access", "owner")).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request(false)))).andReturn().getResponse();
        assertEquals(response.getContentAsString(), 201, response.getStatus());
        assertEquals("no-store", response.getHeader("Cache-Control"));
        var data = mapper.readTree(response.getContentAsByteArray()).path("data");
        assertEquals("AI", data.path("resultSource").asText());
        String url = "/api/document-reviews/" + data.path("reviewNum").asLong();
        assertEquals(url, response.getHeader("Location"));
        var reread = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url)
                .cookie(new Cookie("provit_access", "owner"))).andReturn().getResponse();
        assertEquals(200, reread.getStatus());
        assertEquals(data, mapper.readTree(reread.getContentAsByteArray()).path("data"));
        assertCounts(1, 2, 0, 3, 1, 3);
    }

    private DocumentReviewService transactionalService(DocumentReviewDAO reviewDAO) {
        return transactionalService(reviewDAO, (request, snapshots) -> {
            assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());
            try {
                return reviewFixture(request.getPortfolioNum() != null);
            } catch (Exception exception) { throw new AssertionError(exception); }
        });
    }

    private DocumentReviewResultDTO reviewFixture(boolean includePortfolio) throws Exception {
        var result = new ObjectMapper().readValue(Files.readString(Path.of("docs/examples/document-review/response.example.json")),
                DocumentReviewResultDTO.class);
        var preparation = result.getCareerPreparation();
        preparation.setOccupationCode(resume.getResume().getOccupationCode());
        preparation.setOccupationName(resume.getResume().getOccupationName());
        preparation.setJobCode(resume.getResume().getJobCode());
        preparation.setJobName(resume.getResume().getJobName());
        if (includePortfolio) {
            var feedback = new DocumentReviewResultDTO.Feedback();
            feedback.setSummary("포트폴리오의 역할·검증 결과 보완");
            var improvement = new DocumentReviewResultDTO.Improvement();
            improvement.setSection("1페이지"); improvement.setTitle("담당 역할 보완");
            improvement.setOriginal("portfolio snapshot"); improvement.setIssue("담당 역할이 설명되지 않았습니다.");
            improvement.setSuggestion("Portfolio snapshot: [실제 담당 역할과 확인한 결과]");
            improvement.setReason("실제 담당 역할과 검증 결과를 추가해 주세요.");
            feedback.setImprovements(List.of(improvement));
            result.getDocumentReviews().put("portfolio", feedback);
            var issue = new DocumentReviewResultDTO.Consistency();
            issue.setType("needsConfirmation"); issue.setTitle("포트폴리오와 지원 동기의 역할 확인");
            issue.setRecommendation("같은 경험을 설명하는지 확인하고 실제 담당 역할을 구분해 주세요.");
            var portfolioSource = new DocumentReviewResultDTO.Source();
            portfolioSource.setDocumentType("portfolio"); portfolioSource.setSection("1페이지");
            portfolioSource.setText("portfolio snapshot"); portfolioSource.setPageNumber(1);
            var resumeSource = new DocumentReviewResultDTO.Source();
            resumeSource.setDocumentType("resume"); resumeSource.setSection("지원 동기");
            resumeSource.setText("원본 지원 동기");
            issue.setSources(List.of(portfolioSource, resumeSource));
            result.getConsistencyIssues().add(issue);
        }
        return result;
    }

    private DocumentReviewService transactionalService(DocumentReviewDAO reviewDAO,
            com.provit.service.document.generator.DocumentReviewGenerator generator) {
        var proxy = new ProxyFactory(new DocumentReviewServiceImpl(reviewDAO, documents, generator, transactionManager));
        proxy.addAdvice(new TransactionInterceptor(transactionManager, new AnnotationTransactionAttributeSource()));
        return (DocumentReviewService) proxy.getProxy();
    }

    @Test
    public void generationFailureIsCalledOnceAndSavedAsFailedRestRecord() throws Exception {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var failing = transactionalService(dao, (request, snapshots) -> {
            assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());
            calls.incrementAndGet();
            throw new DocumentReviewProcessingException("AI 요청 한도에 도달했습니다.", 503);
        });
        JwtProvider jwt = new JwtProvider(null) {
            @Override public boolean validateToken(String token) { return "owner".equals(token); }
            @Override public Long getUserNum(String token) { return 7L; }
        };
        var cookies = new AuthCookieService();
        ReflectionTestUtils.setField(cookies, "cookieName", "provit_access");
        var mvc = MockMvcBuilders.standaloneSetup(new DocumentReviewController(failing))
                .setCustomArgumentResolvers(new LoginUserArgumentResolver(jwt, cookies))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        var response = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/document-reviews")
                .cookie(new Cookie("provit_access", "owner")).contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request(false)))).andReturn().getResponse();
        assertEquals(1, calls.get()); assertEquals(503, response.getStatus());
        assertEquals("no-store", response.getHeader("Cache-Control"));
        var data = new ObjectMapper().readTree(response.getContentAsByteArray()).path("data");
        var saved = service.getReview(7, data.path("reviewNum").asLong());
        assertEquals("FAILED", saved.getReviewStatus());
        assertEquals(data.path("message").asText(), saved.getErrorMessage());
        assertCounts(1, 2, 0, 0, 0, 0);
    }

    @Test
    public void strengthEvidenceAndPreparationSourcesSurviveSaveAndLegacyColumnsRemainReadable() throws Exception {
        var mapper = new ObjectMapper();
        var example = mapper.readValue(Files.readString(Path.of("docs/examples/document-review/strength.example.json")),
                DocumentReviewResultDTO.Strength.class);
        var strength = new DocumentReviewResultDTO.Strength();
        strength.setTitle(example.getTitle()); strength.setReason(example.getReason()); strength.setSuggestion(example.getSuggestion());
        var source = new DocumentReviewResultDTO.Source();
        source.setDocumentType("resume"); source.setSection("지원 동기"); source.setText("원본 지원 동기");
        strength.setSources(List.of(source));
        var fixture = transactionalService(dao, (request, snapshots) -> {
            assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());
            var result = new DocumentReviewResultDTO(); result.setSummary("근거 저장 검증");
            result.setStrengths(List.of(strength));
            var resumeFeedback = new DocumentReviewResultDTO.Feedback(); resumeFeedback.setSummary("이력서 검토");
            resumeFeedback.setStrengths(List.of(mapper.convertValue(strength, DocumentReviewResultDTO.Strength.class)));
            var letterFeedback = new DocumentReviewResultDTO.Feedback(); letterFeedback.setSummary("자기소개서 검토");
            result.getDocumentReviews().put("resume", resumeFeedback); result.getDocumentReviews().put("coverLetter", letterFeedback);
            try { result.setCareerPreparation(reviewFixture(false).getCareerPreparation()); }
            catch (Exception exception) { throw new AssertionError(exception); }
            result.getCareerPreparation().getRecommendations().get(0).setSources(List.of(source));
            return result;
        });
        var saved = fixture.createReview(7, request(false));
        var reread = service.getReview(7, saved.getReviewNum());
        assertEquals(saved, reread);
        assertEquals(example.getReason(), reread.getStrengths().get(0).getReason());
        assertEquals(example.getSuggestion(), reread.getStrengths().get(0).getSuggestion());
        assertEquals("원본 지원 동기", reread.getStrengths().get(0).getSources().get(0).getText());
        assertEquals(reread.getStrengths().get(0).getSources(), reread.getCareerPreparation().getRecommendations().get(0).getSources());
        assertCounts(1, 2, 2, 0, 0, 0);
        try (var query = connection.prepareStatement("SELECT STRENGTH_REASON, STRENGTH_SUGGESTION, STRENGTH_SOURCES_JSON FROM "
                + names.get("T_REVIEW_STRENGTH") + " WHERE REVIEW_NUM = ? AND REVIEW_DOCUMENT_NUM IS NULL")) {
            query.setLong(1, saved.getReviewNum());
            try (var rows = query.executeQuery()) {
                assertTrue(rows.next()); assertEquals(example.getReason(), rows.getString(1));
                assertEquals(example.getSuggestion(), rows.getString(2));
                assertEquals("원본 지원 동기", mapper.readTree(rows.getString(3)).get(0).path("text").asText());
            }
        }
        try (var query = connection.prepareStatement("UPDATE " + names.get("T_REVIEW_STRENGTH")
                + " SET STRENGTH_REASON = NULL, STRENGTH_SUGGESTION = NULL, STRENGTH_SOURCES_JSON = NULL WHERE REVIEW_NUM = ?")) {
            query.setLong(1, saved.getReviewNum()); query.executeUpdate();
        }
        var legacy = service.getReview(7, saved.getReviewNum()).getStrengths().get(0);
        assertEquals(example.getTitle(), legacy.getTitle()); assertNull(legacy.getReason());
        assertNull(legacy.getSuggestion()); assertTrue(legacy.getSources().isEmpty());
    }

    @Test
    public void liveOpenAiResponseIsStoredAndRereadWithPortfolioInOneRequest() throws Exception {
        Assume.assumeTrue("Opt in separately for one paid synthetic request",
                Boolean.getBoolean("provit.openai.documentReviewTests"));
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources/api.properties"), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        var actualGenerator = new com.provit.service.document.generator.OpenAiDocumentReviewGenerator(properties.getProperty("api.openai.key"));
        var live = transactionalService(dao, actualGenerator);
        var strengthExample = new ObjectMapper().readTree(Files.readString(Path.of("docs/examples/document-review/strength.example.json")));
        resume.getResume().setMotivation(strengthExample.path("sources").get(0).path("text").asText());
        try (var pdfDocument = new org.apache.pdfbox.pdmodel.PDDocument(); var bytes = new java.io.ByteArrayOutputStream()) {
            var page = new org.apache.pdfbox.pdmodel.PDPage();
            pdfDocument.addPage(page);
            try (var content = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdfDocument, page)) {
                content.beginText();
                content.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(40, 700);
                content.showText("Synthetic portfolio: developed member input validation with Spring MVC and MyBatis.");
                content.endText();
            }
            pdfDocument.save(bytes);
            pdf = bytes.toByteArray();
        }
        var saved = live.createReview(7, request(true));
        assertEquals("COMPLETED", saved.getReviewStatus()); assertEquals("AI", saved.getResultSource());
        assertEquals("gpt-6.1-sol", saved.getModelName()); assertEquals(3, saved.getDocuments().size());
        assertNotNull(saved.getDocumentReviews().get("portfolio"));
        assertEquals("84", saved.getCareerPreparation().getJobCode());
        var strengths = new ArrayList<>(saved.getStrengths());
        saved.getDocumentReviews().values().stream().filter(java.util.Objects::nonNull)
                .forEach(feedback -> strengths.addAll(feedback.getStrengths()));
        assertFalse("Concrete synthetic evidence should support at least one strength", strengths.isEmpty());
        for (var strength : strengths) {
            assertFalse(strength.getSources().isEmpty()); assertFalse(strength.getReason().isBlank());
            assertFalse(strength.getSuggestion().isBlank());
        }
        assertEquals(saved, service.getReview(7, saved.getReviewNum()));
        System.out.println("Live OpenAI + Oracle: completed, saved, reread; one synthetic request.");
    }

    private void assertCounts(int... expected) {
        var tables = List.of("T_DOCUMENT_REVIEW", "T_REVIEW_DOCUMENT", "T_REVIEW_STRENGTH", "T_REVIEW_IMPROVEMENT", "T_REVIEW_CONSISTENCY", "T_REVIEW_SOURCE");
        for (int i = 0; i < tables.size(); i++) {
            try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT COUNT(*) FROM " + names.get(tables.get(i)))) {
                rows.next(); assertEquals(tables.get(i), expected[i], rows.getInt(1));
            } catch (Exception exception) { throw new AssertionError(exception); }
        }
    }

    private DocumentReviewRequestDTO request(boolean portfolio) {
        var request = new DocumentReviewRequestDTO();
        request.setResumeNum(11); request.setLetterNum(12); request.setPortfolioNum(portfolio ? 13 : null);
        request.setReviewMode("comprehensive");
        return request;
    }

    private String isolatedSql(String sql) {
        var sorted = new ArrayList<>(names.keySet()); sorted.sort((left, right) -> Integer.compare(right.length(), left.length()));
        for (String name : sorted) sql = sql.replaceAll("\\b" + name + "\\b", names.get(name));
        return sql;
    }

    private String resource(String path) throws Exception {
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(path, stream);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
