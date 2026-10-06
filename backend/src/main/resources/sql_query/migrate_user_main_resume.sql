-- 기존 Oracle DB에 대표이력서 번호를 추가한다. SQL*Plus 또는 SQLcl에서 실행한다.
-- 기존 회원은 미지정(NULL) 상태이며 회원/이력서 데이터와 직군/직무 코드는 유지한다.
-- 이미 추가된 객체는 건너뛰므로 재실행할 수 있다.
-- Oracle DDL은 자동 커밋된다. 오류가 발생하면 원인을 해결한 뒤 다시 실행한다.
DECLARE
    object_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO object_count
    FROM USER_TAB_COLUMNS
    WHERE TABLE_NAME = 'T_USER'
      AND COLUMN_NAME = 'MAIN_RESUME_NUM';

    IF object_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE T_USER ADD (MAIN_RESUME_NUM NUMBER(18))';
    END IF;

    SELECT COUNT(*) INTO object_count
    FROM USER_CONSTRAINTS
    WHERE TABLE_NAME = 'T_USER'
      AND CONSTRAINT_NAME = 'FK_USER_MAIN_RESUME';

    IF object_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE T_USER ADD CONSTRAINT FK_USER_MAIN_RESUME '
            || 'FOREIGN KEY (MAIN_RESUME_NUM) REFERENCES T_RESUME(RESUME_NUM) ON DELETE SET NULL';
    END IF;

    SELECT COUNT(*) INTO object_count
    FROM USER_INDEXES
    WHERE TABLE_NAME = 'T_USER'
      AND INDEX_NAME = 'IDX_USER_MAIN_RESUME_NUM';

    IF object_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX IDX_USER_MAIN_RESUME_NUM ON T_USER(MAIN_RESUME_NUM)';
    END IF;
END;
/
