-- OPS-010: normalize legacy interview columns to the current text limits.
-- Run the complete script with interview writes paused and a database backup.
-- Oracle DDL commits implicitly: preflight every column before the first ALTER.
-- Never truncate existing data. Already normalized columns are left unchanged.
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

DECLARE
    PROCEDURE preflight(p_table VARCHAR2, p_column VARCHAR2, p_limit PLS_INTEGER) IS
        v_type USER_TAB_COLUMNS.DATA_TYPE%TYPE;
        v_long_rows NUMBER;
    BEGIN
        SELECT DATA_TYPE INTO v_type FROM USER_TAB_COLUMNS
        WHERE TABLE_NAME = p_table AND COLUMN_NAME = p_column;
        IF v_type <> 'VARCHAR2' THEN
            RAISE_APPLICATION_ERROR(-20001, p_table || '.' || p_column || ' must be VARCHAR2');
        END IF;
        EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ' || p_table ||
            ' WHERE LENGTH(' || p_column || ') > :limit' INTO v_long_rows USING p_limit;
        IF v_long_rows > 0 THEN
            RAISE_APPLICATION_ERROR(-20002, p_table || '.' || p_column ||
                ' contains text over ' || p_limit || ' characters');
        END IF;
    END;

    PROCEDURE normalize(p_table VARCHAR2, p_column VARCHAR2, p_limit PLS_INTEGER) IS
        v_semantics USER_TAB_COLUMNS.CHAR_USED%TYPE;
        v_length USER_TAB_COLUMNS.CHAR_LENGTH%TYPE;
    BEGIN
        SELECT CHAR_USED, CHAR_LENGTH INTO v_semantics, v_length
        FROM USER_TAB_COLUMNS WHERE TABLE_NAME = p_table AND COLUMN_NAME = p_column;
        IF v_semantics <> 'C' OR v_length <> p_limit THEN
            EXECUTE IMMEDIATE 'ALTER TABLE ' || p_table || ' MODIFY (' || p_column ||
                ' VARCHAR2(' || p_limit || ' CHAR))';
        END IF;
    END;
BEGIN
    FOR i IN 1..5 LOOP
        preflight('T_INTERVIEW_HISTORY', 'QUESTION' || i, 120);
        preflight('T_INTERVIEW_HISTORY', 'ANSWER' || i, 1000);
    END LOOP;
    preflight('T_INTERVIEW_RESULT', 'STRENGTH', 250);
    preflight('T_INTERVIEW_RESULT', 'WEAKNESS', 250);
    preflight('T_INTERVIEW_RESULT', 'PREVIOUS_COMPARISON', 250);
    preflight('T_INTERVIEW_RESULT', 'IMPROVEMENT_POINT', 250);

    FOR i IN 1..5 LOOP
        normalize('T_INTERVIEW_HISTORY', 'QUESTION' || i, 120);
        normalize('T_INTERVIEW_HISTORY', 'ANSWER' || i, 1000);
    END LOOP;
    normalize('T_INTERVIEW_RESULT', 'STRENGTH', 250);
    normalize('T_INTERVIEW_RESULT', 'WEAKNESS', 250);
    normalize('T_INTERVIEW_RESULT', 'PREVIOUS_COMPARISON', 250);
    normalize('T_INTERVIEW_RESULT', 'IMPROVEMENT_POINT', 250);
END;
/

SELECT TABLE_NAME, COLUMN_NAME, CHAR_USED, CHAR_LENGTH, DATA_LENGTH
FROM USER_TAB_COLUMNS
WHERE TABLE_NAME IN ('T_INTERVIEW_HISTORY', 'T_INTERVIEW_RESULT')
  AND DATA_TYPE = 'VARCHAR2'
ORDER BY TABLE_NAME, COLUMN_ID;
