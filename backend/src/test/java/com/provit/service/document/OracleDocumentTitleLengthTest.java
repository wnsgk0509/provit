package com.provit.service.document;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class OracleDocumentTitleLengthTest {
    private Connection connection;
    private final Map<String, String> tables = new LinkedHashMap<>();
    private final List<String> created = new ArrayList<>();

    @Before
    public void createIsolatedTables() throws Exception {
        Assume.assumeTrue("Opt in with -Dprovit.oracle.documentTitleTests=true",
                Boolean.getBoolean("provit.oracle.documentTitleTests"));
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
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        tables.put("T_RESUME", "DTXT_R_" + suffix);
        tables.put("T_COVER_LETTER", "DTXT_C_" + suffix);
        tables.put("T_PORTFOLIO", "DTXT_P_" + suffix);
        try (var statement = connection.createStatement()) {
            for (var entry : tables.entrySet()) {
                // A larger legacy letter column also exercises preflight before any DDL.
                int capacity = entry.getKey().equals("T_COVER_LETTER") ? 1000 : 200;
                statement.execute("CREATE TABLE " + entry.getValue() + " (ID NUMBER, "
                        + column(entry.getKey()) + " VARCHAR2(" + capacity + " BYTE))");
                created.add(entry.getValue());
            }
        }
    }

    @After
    public void dropOnlyCreatedTestTables() throws Exception {
        if (connection == null) return;
        try (var testConnection = connection; var statement = testConnection.createStatement()) {
            for (String table : created) statement.execute("DROP TABLE " + table + " PURGE");
        }
    }

    @Test
    public void migrationPreservesRowsAndStoresKoreanEnglishMixedAndSupplementaryTitles() throws Exception {
        for (String table : tables.keySet()) insert(table, 1, "기존 제목");
        migrate();
        assertSemantics("C");
        for (String table : tables.keySet()) {
            assertEquals("기존 제목", title(table, 1));
            int id = 2;
            for (String text : List.of("가".repeat(200), "A".repeat(200),
                    "가A1!".repeat(50), "가A😀".repeat(50))) {
                assertEquals(200, text.length());
                insert(table, id, text);
                assertEquals(text, title(table, id++));
            }
        }
    }

    @Test
    public void preflightStopsBeforeAnyAlterWhenAnotherTableHasOversizedData() throws Exception {
        insert("T_COVER_LETTER", 1, "A".repeat(201));
        assertEquals(20002, assertThrows(SQLException.class, this::migrate).getErrorCode());
        assertSemantics("B");
        assertEquals("A".repeat(201), title("T_COVER_LETTER", 1));
    }

    @Test
    public void migrationCanRunTwiceAndDatabaseRejects201Characters() throws Exception {
        migrate();
        migrate();
        assertSemantics("C");
        for (String table : tables.keySet()) {
            for (String text : List.of("가".repeat(201), "A".repeat(201))) {
                assertEquals(12899, assertThrows(SQLException.class, () -> insert(table, 1, text)).getErrorCode());
            }
        }
    }

    private void migrate() throws Exception {
        try (var stream = getClass().getResourceAsStream("/sql_query/normalize_document_title_lengths.sql")) {
            assertNotNull(stream);
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
            for (var entry : tables.entrySet()) sql = sql.replace(entry.getKey(), entry.getValue());
            String block = sql.substring(sql.indexOf("DECLARE"), sql.indexOf("\n/"));
            try (var statement = connection.createStatement()) { statement.execute(block); }
        }
    }

    private String column(String table) {
        return switch (table) {
            case "T_RESUME" -> "RESUME_TITLE";
            case "T_COVER_LETTER" -> "COVER_LETTER_TITLE";
            case "T_PORTFOLIO" -> "PORTFOLIO_TITLE";
            default -> throw new AssertionError(table);
        };
    }

    private void insert(String table, int id, String title) throws Exception {
        try (var statement = connection.prepareStatement("INSERT INTO " + tables.get(table)
                + " (ID," + column(table) + ") VALUES (?,?)")) {
            statement.setInt(1, id); statement.setString(2, title); statement.executeUpdate();
        }
    }

    private String title(String table, int id) throws Exception {
        try (var statement = connection.prepareStatement("SELECT " + column(table) + " FROM "
                + tables.get(table) + " WHERE ID=?")) {
            statement.setInt(1, id);
            try (var result = statement.executeQuery()) { assertTrue(result.next()); return result.getString(1); }
        }
    }

    private void assertSemantics(String semantics) throws Exception {
        for (String table : tables.values()) {
            try (var statement = connection.prepareStatement("SELECT CHAR_USED, CHAR_LENGTH FROM USER_TAB_COLUMNS "
                    + "WHERE TABLE_NAME=? AND DATA_TYPE='VARCHAR2'")) {
                statement.setString(1, table);
                try (var result = statement.executeQuery()) {
                    assertTrue(result.next()); assertEquals(semantics, result.getString(1));
                    if (semantics.equals("C")) assertEquals(200, result.getInt(2));
                    assertFalse(result.next());
                }
            }
        }
    }
}
