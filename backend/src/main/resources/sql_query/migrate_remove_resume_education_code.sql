-- 기존 DB에 애플리케이션 변경을 배포하기 전에 실행한다.
-- 이력서의 기존 필수 컬럼을 제거하여 해당 값을 보내지 않아도 저장할 수 있게 한다.
-- 최종 학력(HIGHEST_LEVEL), 학교별 학력(T_EDUCATION), 이력서 행은 유지한다.
-- 이미 변경한 DB에서는 건너뛰므로 재실행할 수 있다.
DECLARE
    object_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO object_count
    FROM USER_TAB_COLUMNS
    WHERE TABLE_NAME = 'T_RESUME'
      AND COLUMN_NAME = 'EDUCATION_CODE';

    IF object_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE T_RESUME DROP COLUMN EDUCATION_CODE CASCADE CONSTRAINTS';
    END IF;

    SELECT COUNT(*) INTO object_count
    FROM USER_TABLES
    WHERE TABLE_NAME = 'T_EDUCODE';

    IF object_count > 0 THEN
        EXECUTE IMMEDIATE 'DROP TABLE T_EDUCODE PURGE';
    END IF;
END;
/
