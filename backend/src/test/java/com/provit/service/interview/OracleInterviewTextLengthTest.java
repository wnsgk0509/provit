package com.provit.service.interview;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.provit.dto.interview.InterviewHistoryDTO;
import com.provit.dto.interview.InterviewResultDTO;

public class OracleInterviewTextLengthTest {
    private Connection connection;
    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    private final String historyTable = "ITXT_H_" + suffix;
    private final String resultTable = "ITXT_R_" + suffix;
    private boolean historyCreated;
    private boolean resultCreated;
    private Configuration configuration;

    @Before
    public void createIsolatedTables() throws Exception {
        Assume.assumeTrue("Opt in with -Dprovit.oracle.lengthTests=true",
                Boolean.getBoolean("provit.oracle.lengthTests"));
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources/database.properties"), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        var credentials = new Properties();
        credentials.setProperty("user", properties.getProperty("db.username"));
        credentials.setProperty("password", properties.getProperty("db.password"));
        credentials.setProperty("oracle.net.CONNECT_TIMEOUT", "5000");
        credentials.setProperty("oracle.jdbc.ReadTimeout", "10000");
        connection = DriverManager.getConnection(properties.getProperty("db.url"), credentials);
        try (var statement = connection.createStatement()) {
            var columns = new StringBuilder("HISTORY_NUM NUMBER, USER_NUM NUMBER, INTERVIEW_DATE DATE");
            for (int i = 1; i <= 5; i++) {
                columns.append(", QUESTION").append(i).append(" VARCHAR2(1000 BYTE), ANSWER")
                        .append(i).append(" VARCHAR2(3000 BYTE)");
            }
            statement.execute("CREATE TABLE " + historyTable + " (" + columns + ")");
            historyCreated = true;
            statement.execute("CREATE TABLE " + resultTable + " (HISTORY_NUM NUMBER, USER_NUM NUMBER, "
                    + "DOCUMENT_CONSISTENCY_SCORE NUMBER, EXPERTISE_SCORE NUMBER, PROBLEM_SOLVING_SCORE NUMBER, "
                    + "LOGIC_SCORE NUMBER, COMMUNICATION_SCORE NUMBER, TOTAL_SCORE NUMBER, "
                    + "STRENGTH VARCHAR2(500 BYTE), WEAKNESS VARCHAR2(500 BYTE), "
                    + "PREVIOUS_COMPARISON VARCHAR2(500 BYTE), IMPROVEMENT_POINT VARCHAR2(500 BYTE), INTERVIEW_DATE DATE)");
            resultCreated = true;
        }
        configuration = new Configuration();
        for (String resource : List.of("/mappers/interview/interview_history_mapper.xml",
                "/mappers/interview/interview_result_mapper.xml")) {
            try (var stream = getClass().getResourceAsStream(resource)) {
                assertNotNull(stream);
                String xml = isolatedSql(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                try (var reader = new java.io.StringReader(xml)) {
                    new XMLMapperBuilder(reader, configuration, resource, configuration.getSqlFragments()).parse();
                }
            }
        }
    }

    @After
    public void dropOnlyCreatedTestTables() throws Exception {
        if (connection == null) return;
        try (var testConnection = connection; var statement = testConnection.createStatement()) {
            try {
                if (resultCreated) statement.execute("DROP TABLE " + resultTable + " PURGE");
            } finally {
                if (historyCreated) statement.execute("DROP TABLE " + historyTable + " PURGE");
            }
        }
    }

    @Test
    public void migrationPreservesDataAndStoresKoreanEnglishMixedAndSupplementaryText() throws Exception {
        var oversizedBeforeMigration = result(1, "가".repeat(250));
        assertEquals(12899, assertThrows(SQLException.class, () -> insertResult(oversizedBeforeMigration)).getErrorCode());
        insertResult(result(1, "기존 평가 내용"));
        migrate();
        assertEquals("기존 평가 내용", storedText(resultTable, "STRENGTH", 1));
        assertSemantics("C");

        int id = 2;
        for (String feedback : List.of("가".repeat(250), "A".repeat(250),
                "가A1!".repeat(62) + "가A", "가A😀".repeat(50))) {
            var result = result(id, feedback);
            insertResult(result);
            for (String column : List.of("STRENGTH", "WEAKNESS", "PREVIOUS_COMPARISON", "IMPROVEMENT_POINT")) {
                assertEquals(feedback, storedText(resultTable, column, id));
            }
            var history = new InterviewHistoryDTO();
            history.setHistoryNum(id);
            history.setUserNum(7);
            String question = "질문".repeat(60);
            String answer = id == 2 ? "가".repeat(1000) : "가A1!😀".repeat(166) + "가A1!";
            assertEquals(1000, answer.length());
            for (int i = 1; i <= 5; i++) {
                InterviewHistoryDTO.class.getMethod("setQuestion" + i, String.class).invoke(history, question);
                InterviewHistoryDTO.class.getMethod("setAnswer" + i, String.class).invoke(history, answer);
            }
            insert("com.provit.mapper.interview.InterviewHistoryMapper.insertInterviewHistory", history);
            for (int i = 1; i <= 5; i++) {
                assertEquals(question, storedText(historyTable, "QUESTION" + i, id));
                assertEquals(answer, storedText(historyTable, "ANSWER" + i, id));
            }
            id++;
        }
    }

    @Test
    public void preflightStopsBeforeAnyAlterWhenLegacyDataExceedsNewLimit() throws Exception {
        insertResult(result(1, "A".repeat(251)));
        assertEquals(20002, assertThrows(SQLException.class, this::migrate).getErrorCode());
        assertSemantics("B");
        assertEquals("A".repeat(251), storedText(resultTable, "STRENGTH", 1));
    }

    @Test
    public void migrationCanRunTwiceAndDatabaseRejectsTheNextCharacter() throws Exception {
        migrate();
        migrate();
        assertSemantics("C");
        assertEquals(12899, assertThrows(SQLException.class, () -> insertResult(result(1, "A".repeat(251)))).getErrorCode());
    }

    private void migrate() throws Exception {
        try (var stream = getClass().getResourceAsStream("/sql_query/normalize_interview_text_lengths.sql")) {
            assertNotNull(stream);
            String sql = isolatedSql(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).replace("\r\n", "\n");
            String block = sql.substring(sql.indexOf("DECLARE"), sql.indexOf("\n/"));
            assertFalse(block.contains("T_INTERVIEW_HISTORY"));
            assertFalse(block.contains("T_INTERVIEW_RESULT"));
            try (var statement = connection.createStatement()) {
                statement.execute(block);
            }
        }
    }

    private String isolatedSql(String sql) {
        return sql.replace("T_INTERVIEW_HISTORY", historyTable).replace("T_INTERVIEW_RESULT", resultTable);
    }

    private void insertResult(InterviewResultDTO result) throws Exception {
        insert("com.provit.mapper.interview.InterviewResultMapper.insertInterviewResult", result);
    }

    private void insert(String id, Object dto) throws Exception {
        var mappedStatement = configuration.getMappedStatement(id);
        var boundSql = mappedStatement.getBoundSql(dto);
        try (var statement = connection.prepareStatement(boundSql.getSql())) {
            new DefaultParameterHandler(mappedStatement, dto, boundSql).setParameters(statement);
            assertEquals(1, statement.executeUpdate());
        }
    }

    private InterviewResultDTO result(int id, String feedback) {
        var result = new InterviewResultDTO();
        result.setHistoryNum(id);
        result.setUserNum(7);
        result.setStrengths(feedback);
        result.setWeaknesses(feedback);
        result.setComparison(feedback);
        result.setImprovements(feedback);
        return result;
    }

    private String storedText(String table, String column, int id) throws Exception {
        try (var statement = connection.prepareStatement("SELECT " + column + " FROM " + table + " WHERE HISTORY_NUM=?")) {
            statement.setInt(1, id);
            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getString(1);
            }
        }
    }

    private void assertSemantics(String semantics) throws Exception {
        try (var statement = connection.prepareStatement("SELECT COLUMN_NAME, CHAR_USED, CHAR_LENGTH "
                + "FROM USER_TAB_COLUMNS WHERE TABLE_NAME IN (?,?) AND DATA_TYPE='VARCHAR2'")) {
            statement.setString(1, historyTable);
            statement.setString(2, resultTable);
            try (var result = statement.executeQuery()) {
                int count = 0;
                while (result.next()) {
                    count++;
                    assertEquals(semantics, result.getString(2));
                    if ("C".equals(semantics)) {
                        String column = result.getString(1);
                        int limit = column.startsWith("QUESTION") ? InterviewTextLimits.QUESTION
                                : column.startsWith("ANSWER") ? InterviewTextLimits.ANSWER : InterviewTextLimits.FEEDBACK;
                        assertEquals(limit, result.getInt(3));
                    }
                }
                assertEquals(14, count);
            }
        }
    }
}
