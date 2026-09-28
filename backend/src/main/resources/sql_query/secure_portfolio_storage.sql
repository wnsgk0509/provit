-- 기존 DB에 애플리케이션 배포 전에 한 번 적용합니다.
ALTER TABLE T_PORTFOLIO ADD (
    ORIGINAL_FILE_NAME VARCHAR2(255 CHAR),
    SAVED_FILE_NAME VARCHAR2(255 CHAR)
);

UPDATE T_PORTFOLIO
SET SAVED_FILE_NAME = REGEXP_SUBSTR(REPLACE(FILE_URL, CHR(92), '/'), '[^/]+$')
WHERE FILE_URL IS NOT NULL;

-- 과거 순번 파일의 원본 이름은 복원할 수 없어 NULL로 유지합니다.
-- 기존 FILE_URL과 실제 파일은 유지하며, 인증된 API에서만 읽고 삭제합니다.
COMMIT;
