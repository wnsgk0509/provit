-- ================================================================================
-- Provit (프로빗) 전체 테이블 및 시퀀스 초기화/삭제 스크립트 (Oracle 19c)
-- 
-- 📌 주의사항:
-- 1. CASCADE CONSTRAINTS: 외래키(FK) 참조 관계를 자동으로 함께 해제하여 삭제 에러 방지
-- 2. PURGE: Oracle 휴지통(RecycleBin, BIN$...)에 임시 보관하지 않고 완전 영구 삭제
-- 3. 테이블은 자식(참조하는) 테이블 -> 부모(참조되는) 테이블 순으로 삭제
-- 4. 이미 없는 테이블/시퀀스만 건너뛰며, 권한 등 다른 오류는 그대로 보고
-- ================================================================================

DECLARE
    PROCEDURE drop_table(table_name IN VARCHAR2) IS
    BEGIN
        EXECUTE IMMEDIATE 'DROP TABLE ' || table_name || ' CASCADE CONSTRAINTS PURGE';
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE <> -942 THEN -- ORA-00942: 테이블이 없음
                RAISE;
            END IF;
    END;

    PROCEDURE drop_sequence(sequence_name IN VARCHAR2) IS
    BEGIN
        EXECUTE IMMEDIATE 'DROP SEQUENCE ' || sequence_name;
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE <> -2289 THEN -- ORA-02289: 시퀀스가 없음
                RAISE;
            END IF;
    END;
BEGIN
    -- ============================================================================
    -- 1. 테이블 삭제 (현재 스키마 27개 + 이전 버전 잔여 테이블 2개)
    -- ============================================================================

    -- 커뮤니티 및 스터디 도메인 (자식 -> 부모 순)
    drop_table('T_REPORT');
    drop_table('T_STUDY_MEMBER');
    drop_table('T_STUDY');
    drop_table('T_COMMENT');
    drop_table('T_POST_LIKE');
    drop_table('T_POST');
    drop_table('T_CATEGORY');

    -- 채용 공고 도메인
    drop_table('T_JOB_SCRAP');
    drop_table('T_RECRUITMENT');

    -- AI 모의면접 도메인
    drop_table('T_INTERVIEW_RESULT');
    drop_table('T_INTERVIEW_HISTORY');

    -- AI 첨삭 도메인 (자식 -> 부모 순)
    drop_table('T_REVIEW_SOURCE');
    drop_table('T_REVIEW_IMPROVEMENT');
    drop_table('T_REVIEW_STRENGTH');
    drop_table('T_REVIEW_CONSISTENCY');
    drop_table('T_REVIEW_DOCUMENT');
    drop_table('T_DOCUMENT_REVIEW');

    -- 유저 이력 문서 도메인 (자식 -> 부모 순)
    drop_table('T_PORTFOLIO');
    drop_table('T_COVER_LETTER');
    drop_table('T_CERTIFICATION');
    drop_table('T_CAREER');
    drop_table('T_EDUCATION');
    drop_table('T_RESUME');

    -- 이전 버전 미사용 테이블 잔여물 정리
    drop_table('T_EDUCODE');
    drop_table('T_FORTUNE_RECOMMEND');

    -- 회원 및 직무 도메인 (자식 -> 부모 순)
    drop_table('T_REFRESH_TOKEN');
    drop_table('T_USER');
    drop_table('T_JOB');
    drop_table('T_OCCUPATION');

    -- ============================================================================
    -- 2. 시퀀스 삭제 (총 19개, 모든 테이블 삭제 후 실행)
    -- ============================================================================
    drop_sequence('SEQ_T_REVIEW_CONSISTENCY');
    drop_sequence('SEQ_T_REVIEW_IMPROVEMENT');
    drop_sequence('SEQ_T_REVIEW_STRENGTH');
    drop_sequence('SEQ_T_REVIEW_DOCUMENT');
    drop_sequence('SEQ_T_DOCUMENT_REVIEW');
    drop_sequence('SEQ_T_REPORT');
    drop_sequence('SEQ_T_STUDY');
    drop_sequence('SEQ_T_COMMENT');
    drop_sequence('SEQ_T_POST');
    drop_sequence('SEQ_T_CATEGORY');
    drop_sequence('SEQ_T_RECRUITMENT');
    drop_sequence('SEQ_T_INTERVIEW_HISTORY');
    drop_sequence('SEQ_T_PORTFOLIO');
    drop_sequence('SEQ_T_COVER_LETTER');
    drop_sequence('SEQ_T_CERTIFICATION');
    drop_sequence('SEQ_T_CAREER');
    drop_sequence('SEQ_T_EDUCATION');
    drop_sequence('SEQ_T_RESUME');
    drop_sequence('SEQ_T_USER');
END;
/
