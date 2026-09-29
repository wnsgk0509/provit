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
import com.provit.common.GlobalExceptionHandler;
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
    private final byte[] pdf = "%PDF-1.7\nportfolio snapshot".getBytes(StandardCharsets.US_ASCII);
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
    public void storesEntireDummyResponseSnapshotsAndTwoHundredCharacterRequests() throws Exception {
        var request = request(true);
        request.setReviewMode("custom"); request.setCustomCriteria("가".repeat(200)); request.setInstructions("나".repeat(200));
        var saved = transaction.execute(status -> service.createDummyReview(7, request));
        assertNotNull(saved.getReviewNum()); assertEquals("COMPLETED", saved.getReviewStatus());
        assertEquals("DUMMY", saved.getResultSource()); assertNotNull(saved.getFinishedAt());
        assertEquals(request.getCustomCriteria(), saved.getCustomCriteria());
        assertEquals(request.getInstructions(), saved.getInstructions());
        assertEquals(3, saved.getDocuments().size());
        assertCounts(1, 3, 8, 3, 2, 4);
        var mapper = new ObjectMapper();
        var expected = mapper.readTree(resource("/document-review/dummy-result.json"));
        var actual = mapper.valueToTree(saved);
        for (String field : List.of("summary", "strengths", "documentReviews", "consistencyIssues")) {
            removeNullPageNumbers(actual.path(field));
            assertEquals(field, expected.path(field), actual.path(field));
        }
        String json = mapper.writeValueAsString(saved);
        assertFalse(json.contains("sourceSnapshotJson")); assertFalse(json.contains("pdfSnapshot"));
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
        var saved = transaction.execute(status -> service.createDummyReview(7, request));
        assertNull(saved.getCustomCriteria()); assertEquals("추가 요청", saved.getInstructions());
        assertNull(saved.getDocumentReviews().get("portfolio"));
        assertCounts(1, 2, 6, 2, 1, 2);
        resume.getResume().setResumeTitle("수정된 이력서"); documentsDeleted = true;
        var reread = transaction.execute(status -> service.getReview(7, saved.getReviewNum()));
        assertEquals("저장 당시 이력서", reread.getDocuments().get(0).getDocumentTitle());
        assertEquals(saved.getSummary(), reread.getSummary());
        assertEquals(1, service.getReviews(7, 0, 20).size()); assertEquals(0, service.getReviews(7, 1, 20).size());
        assertEquals(0, service.getReviews(8, 0, 20).size());
        assertThrows(NoSuchElementException.class, () -> service.getReview(8, saved.getReviewNum()));
    }

    @Test
    public void childInsertFailureRollsBackEveryTableAndDoesNotLeaveProcessingRecord() {
        var failingDAO = (DocumentReviewDAO) Proxy.newProxyInstance(DocumentReviewDAO.class.getClassLoader(),
                new Class<?>[] { DocumentReviewDAO.class }, (proxy, method, args) -> {
                    if (method.getName().equals("insertSource")) throw new IllegalStateException("Injected failure");
                    try { return method.invoke(dao, args); }
                    catch (java.lang.reflect.InvocationTargetException exception) { throw exception.getCause(); }
                });
        var failingService = transactionalService(failingDAO);
        assertThrows(IllegalStateException.class, () -> failingService.createDummyReview(7, request(true)));
        assertCounts(0, 0, 0, 0, 0, 0);
    }

    @Test
    public void authenticatedRestRequestReturnsCreatedAndSavedRecordCanBeReadAgain() throws Exception {
        JwtProvider jwt = new JwtProvider(null) {
            @Override public boolean validateToken(String token) { return "owner".equals(token); }
            @Override public Long getUserNum(String token) { return 7L; }
        };
        var mvc = MockMvcBuilders.standaloneSetup(new DocumentReviewController(service))
                .setCustomArgumentResolvers(new LoginUserArgumentResolver(jwt))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        var mapper = new ObjectMapper();
        var response = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/document-reviews")
                .header("Authorization", "Bearer owner").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request(false)))).andReturn().getResponse();
        assertEquals(response.getContentAsString(), 201, response.getStatus());
        assertEquals("no-store", response.getHeader("Cache-Control"));
        var data = mapper.readTree(response.getContentAsByteArray()).path("data");
        assertEquals("DUMMY", data.path("resultSource").asText());
        String url = "/api/document-reviews/" + data.path("reviewNum").asLong();
        assertEquals(url, response.getHeader("Location"));
        var reread = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url)
                .header("Authorization", "Bearer owner")).andReturn().getResponse();
        assertEquals(200, reread.getStatus());
        assertEquals(data, mapper.readTree(reread.getContentAsByteArray()).path("data"));
        assertCounts(1, 2, 6, 2, 1, 2);
    }

    private DocumentReviewService transactionalService(DocumentReviewDAO reviewDAO) {
        var proxy = new ProxyFactory(new DocumentReviewServiceImpl(reviewDAO, documents));
        proxy.addAdvice(new TransactionInterceptor(transactionManager, new AnnotationTransactionAttributeSource()));
        return (DocumentReviewService) proxy.getProxy();
    }

    private void assertCounts(int... expected) {
        var tables = List.of("T_DOCUMENT_REVIEW", "T_REVIEW_DOCUMENT", "T_REVIEW_STRENGTH", "T_REVIEW_IMPROVEMENT", "T_REVIEW_CONSISTENCY", "T_REVIEW_SOURCE");
        for (int i = 0; i < tables.size(); i++) {
            try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT COUNT(*) FROM " + names.get(tables.get(i)))) {
                rows.next(); assertEquals(tables.get(i), expected[i], rows.getInt(1));
            } catch (Exception exception) { throw new AssertionError(exception); }
        }
    }

    private void removeNullPageNumbers(com.fasterxml.jackson.databind.JsonNode node) {
        if (node.isObject() && node.has("pageNumber") && node.path("pageNumber").isNull())
            ((com.fasterxml.jackson.databind.node.ObjectNode) node).remove("pageNumber");
        node.forEach(this::removeNullPageNumbers);
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
