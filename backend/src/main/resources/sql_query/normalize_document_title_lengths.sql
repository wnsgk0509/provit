-- DOC-014: align all document titles with the application's 200-character limit.
-- Run the complete script while document writes are paused. Oracle DDL commits
-- implicitly; all columns and existing lengths are checked before the first ALTER.
-- Existing rows are preserved and an already normalized database is a no-op.
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

DECLARE
    PROCEDURE preflight(p_table VARCHAR2, p_column VARCHAR2) IS
        v_type USER_TAB_COLUMNS.DATA_TYPE%TYPE;
        v_long_rows NUMBER;
    BEGIN
        SELECT DATA_TYPE INTO v_type FROM USER_TAB_COLUMNS
        WHERE TABLE_NAME = p_table AND COLUMN_NAME = p_column;
        IF v_type <> 'VARCHAR2' THEN
            RAISE_APPLICATION_ERROR(-20001, p_table || '.' || p_column || ' must be VARCHAR2');
        END IF;
        EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ' || p_table ||
            ' WHERE LENGTH(' || p_column || ') > 200' INTO v_long_rows;
        IF v_long_rows > 0 THEN
            RAISE_APPLICATION_ERROR(-20002, p_table || '.' || p_column || ' contains titles over 200 characters');
        END IF;
    END;

    PROCEDURE normalize(p_table VARCHAR2, p_column VARCHAR2) IS
        v_semantics USER_TAB_COLUMNS.CHAR_USED%TYPE;
        v_length USER_TAB_COLUMNS.CHAR_LENGTH%TYPE;
    BEGIN
        SELECT CHAR_USED, CHAR_LENGTH INTO v_semantics, v_length
        FROM USER_TAB_COLUMNS WHERE TABLE_NAME = p_table AND COLUMN_NAME = p_column;
        IF v_semantics <> 'C' OR v_length <> 200 THEN
            EXECUTE IMMEDIATE 'ALTER TABLE ' || p_table ||
                ' MODIFY (' || p_column || ' VARCHAR2(200 CHAR))';
        END IF;
    END;
BEGIN
    preflight('T_RESUME', 'RESUME_TITLE');
    preflight('T_COVER_LETTER', 'COVER_LETTER_TITLE');
    preflight('T_PORTFOLIO', 'PORTFOLIO_TITLE');

    normalize('T_RESUME', 'RESUME_TITLE');
    normalize('T_COVER_LETTER', 'COVER_LETTER_TITLE');
    normalize('T_PORTFOLIO', 'PORTFOLIO_TITLE');
END;
/

SELECT TABLE_NAME, COLUMN_NAME, CHAR_USED, CHAR_LENGTH, DATA_LENGTH
FROM USER_TAB_COLUMNS
WHERE (TABLE_NAME = 'T_RESUME' AND COLUMN_NAME = 'RESUME_TITLE')
   OR (TABLE_NAME = 'T_COVER_LETTER' AND COLUMN_NAME = 'COVER_LETTER_TITLE')
   OR (TABLE_NAME = 'T_PORTFOLIO' AND COLUMN_NAME = 'PORTFOLIO_TITLE')
ORDER BY TABLE_NAME;
