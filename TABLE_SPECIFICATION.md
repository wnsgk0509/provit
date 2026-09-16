# 📋 Provit (프로빗) 데이터베이스 테이블 명세서 (Table Specification)

- **Database Engine:** Oracle Database 19c Enterprise Edition
- **Character Set:** AL32UTF8
- **Total Tables:** 18개 테이블
- **Total Sequences:** 11개 시퀀스
- **Last Updated:** 2026-09-16

---

## 📑 목차 (Table of Contents)

1. [회원 및 직무 도메인](#1-회원-및-직무-도메인)
   - [1.1 T_OCCUPATION (대분류 직군)](#11-t_occupation-대분류-직군)
   - [1.2 T_JOB (소분류 직무)](#12-t_job-소분류-직무)
   - [1.3 T_USER (회원 기본 정보)](#13-t_user-회원-기본-정보)
2. [유저 이력 문서 도메인](#2-유저-이력-문서-도메인)
   - [2.1 T_RESUME (이력서 기본 정보)](#21-t_resume-이력서-기본-정보)
   - [2.2 T_EDUCATION (학력 정보)](#22-t_education-학력-정보)
   - [2.3 T_CAREER (경력 정보)](#23-t_career-경력-정보)
   - [2.4 T_CERTIFICATION (자격증 정보)](#24-t_certification-자격증-정보)
   - [2.5 T_COVER_LETTER (자기소개서)](#25-t_cover_letter-자기소개서)
   - [2.6 T_PORTFOLIO (포트폴리오)](#26-t_portfolio-포트폴리오)
3. [AI 모의 면접 도메인](#3-ai-모의-면접-도메인)
   - [3.1 T_INTERVIEW_HISTORY (면접 질문/답변 내역)](#31-t_interview_history-면접-질문답변-내역)
   - [3.2 T_INTERVIEW_RESULT (면접 평가 결과)](#32-t_interview_result-면접-평가-결과)
4. [채용 공고 도메인](#4-채용-공고-도메인)
   - [4.1 T_RECRUITMENT (사람인 채용 공고 API 적재)](#41-t_recruitment-사람인-채용-공고-api-적재)
5. [커뮤니티 및 스터디 도메인](#5-커뮤니티-및-스터디-도메인)
   - [5.1 T_CATEGORY (게시판 종류)](#51-t_category-게시판-종류)
   - [5.2 T_POST (커뮤니티 게시글)](#52-t_post-커뮤니티-게시글)
   - [5.3 T_POST_LIKE (게시글 좋아요)](#53-t_post_like-게시글-좋아요)
   - [5.4 T_COMMENT (게시글 댓글)](#54-t_comment-게시글-댓글)
   - [5.5 T_STUDY (스터디 모집 방)](#55-t_study-스터디-모집-방)
   - [5.6 T_STUDY_MEMBER (스터디 참여자 명단)](#56-t_study_member-스터디-참여자-명단)

---

# 1. 회원 및 직무 도메인

### 1.1 T_OCCUPATION (대분류 직군)
- **설명:** 개발, 기획, 디자인 등 대분류 직군 코드 관리
- **시퀀스:** 없음 (코드형 식별자 사용)

| No | 컬럼 물리명 | 컬럼 논리명 | 데이터 타입 | Null 허용 | Key | 기본값 | 비고 및 제약사항 |
|:--:|:---|:---|:---|:--:|:--:|:---|:---|
| 1 | `OCCUPATION_CODE` | 직군 코드 | VARCHAR2(20) | N | PK | - | 예: 'DEV', 'PLAN' |
| 2 | `OCCUPATION_NAME` | 직군명 | VARCHAR2(200) | N | - | - | 예: '개발', '기획' |

---

### 1.2 T_JOB (소분류 직무)
- **설명:** 백엔드 개발자, 프론트엔드 개발자 등 직군에 종속된 상세 직무 코드 관리
- **시퀀스:** 없음 (코드형 식별자 사용)

| No | 컬럼 물리명 | 컬럼 논리명 | 데이터 타입 | Null 허용 | Key | 기본값 | 비고 및 제약사항 |
|:--:|:---|:---|:---|:--:|:--:|:---|:---|
| 1 | `JOB_CODE` | 직무 코드 | VARCHAR2(20) | N | PK | - | 예: 'BACKEND', 'FRONTEND' |
| 2 | `OCCUPATION_CODE` | 소속 직군 코드 | VARCHAR2(20) | N | FK | - | `T_OCCUPATION(OCCUPATION_CODE)` ON DELETE CASCADE |
| 3 | `JOB_NAME` | 직무명 | VARCHAR2(200) | N | - | - | 예: '백엔드 개발자' |

---

### 1.3 T_USER (회원 기본 정보)
- **설명:** 서비스 전체 회원의 인증 및 기본 프로필 정보
- **시퀀스:** `SEQ_T_USER`

| No | 컬럼 물리명 | 컬럼 논리명 | 데이터 타입 | Null 허용 | Key | 기본값 | 비고 및 제약사항 |
|:--:|:---|:---|:---|:--:|:--:|:---|:---|
| 1 | `USER_NUM` | 회원 번호 | NUMBER(9) | N | PK | `SEQ_T_USER.NEXTVAL` | 회원 고유 식별자 |
| 2 | `USER_NAME` | 회원 실명 | VARCHAR2(100) | N | - | - | 사용자 본명 |
| 3 | `USER_NICKNAME` | 닉네임 | VARCHAR2(100) | N | - | - | 서비스 내 활동명 |
| 4 | `USER_BIRTH_DATE` | 생년월일 | DATE | Y | - | - | 생년월일 (YYYY-MM-DD) |
| 5 | `USER_EMAIL` | 이메일 | VARCHAR2(200) | N | UK | - | 로그인 ID (중복 불가) |
| 6 | `USER_PW` | 비밀번호 | VARCHAR2(255) | N | - | - | SHA-256/BCrypt 암호화 해시 |
| 7 | `USER_REGISTER_DATE` | 가입일시 | DATE | N | - | `SYSDATE` | 최초 가입 시각 |
| 8 | `USER_TYPE` | 권한 구분 | VARCHAR2(30) | Y | - | `'USER'` | 'USER', 'ADMIN' 등 |
| 9 | `JOB_CODE` | 희망 직무 | VARCHAR2(20) | Y | FK | - | `T_JOB(JOB_CODE)` ON DELETE SET NULL |
| 10 | `OCCUPATION_CODE` | 희망 직군 | VARCHAR2(20) | Y | FK | - | `T_OCCUPATION(OCCUPATION_CODE)` ON DELETE SET NULL |
| 11 | `USER_IS_DELETED` | 탈퇴 여부 | NUMBER(1) | N | - | `0` | 0: 정상 회원, 1: 탈퇴 회원 |

---

# 2. 유저 이력 문서 도메인

### 2.1 T_RESUME (이력서 기본 정보)
- **설명:** 회원의 이력서 마스터 테이블 (학력/경력/자격증의 1:N 부모)
- **시퀀스:** `SEQ_T_RESUME` / **인덱스:** `USER_NUM`

| No | 컬럼 물리명 | 컬럼 논리명 | 데이터 타입 | Null 허용 | Key | 기본값 | 비고 및 제약사항 |
|:--:|:---|:---|:---|:--:|:--:|:---|:---|
| 1 | `RESUME_NUM` | 이력서 번호 | NUMBER(18) | N | PK | `SEQ_T_RESUME.NEXTVAL` | 이력서 고유 번호 |
| 2 | `USER_NUM` | 작성자 번호 | NUMBER(9) | N | FK | - | `T_USER(USER_NUM)` ON DELETE CASCADE |
| 3 | `MOTIVATION` | 지원 동기 | CLOB | Y | - | - | 장문 지원 동기 텍스트 |
| 4 | `DESIRED_LOCATION` | 희망 근무지 | VARCHAR2(200) | Y | - | - | 예: '서울 강남구' |
| 5 | `DESIRED_WORK_TYPE` | 희망 고용형태 | VARCHAR2(100) | Y | - | - | 정규직, 계약직, 인턴 등 |
| 6 | `CREATED_AT` | 등록일시 | DATE | N | - | `SYSDATE` | 이력서 생성일시 |
| 7 | `UPDATED_AT` | 수정일시 | DATE | N | - | `SYSDATE` | 이력서 최종수정일시 |

---

### 2.2 T_EDUCATION (학력 정보)
- **설명:** 이력서에 포함되는 최종/세부 학력 정보 (1:N)
- **시퀀스:** `SEQ_T_EDUCATION` / **인덱스:** `RESUME_NUM`, `GRADUATION_DATE`

| No | 컬럼 물리명 | 컬럼 논리명 | 데이터 타입 | Null 허용 | Key | 기본값 | 비고 및 제약사항 |
|:--:|:---|:---|:---|:--:|:--:|:---|:---|
| 1 | `EDU_NUM` | 학력 번호 | NUMBER(18) | N | PK | `SEQ_T_EDUCATION.NEXTVAL` | 학력 항목 고유 번호 |
| 2 | `RESUME_NUM` | 이력서 번호 | NUMBER(18) | N | FK | - | `T_RESUME(RESUME_NUM)` ON DELETE CASCADE |
| 3 | `SCHOOL_NAME` | 학교명 | VARCHAR2(200) | N | - | - | 출신 학교명 |
| 4 | `ADMISSION_DATE` | 입학일자 | DATE | Y | - | - | 입학 연월일 |
| 5 | `GRADUATION_DATE` | 졸업일자 | DATE | Y | - | - | 졸업(예정) 연월일 |
| 6 | `MAJOR` | 전공학과 | VARCHAR2(200) | Y | - | - | 주전공 / 복수전공명 |

---

### 2.3 T_CAREER (경력 정보)
- **설명:** 이력서에 포함되는 직장 경력 및 주요 업무 이력 (1:N)
- **시퀀스:** `SEQ_T_CAREER` / **인덱스:** `RESUME_NUM`, `RESIGN_DATE`

| No | 컬럼 물리명 | 컬럼 논리명 | 데이터 타입 | Null 허용 | Key | 기본값 | 비고 및 제약사항 |
|:--:|:---|:---|:---|:--:|:--:|:---|:---|
| 1 | `CAREER_NUM` | 경력 번호 | NUMBER(18) | N | PK | `SEQ_T_CAREER.NEXTVAL` | 경력 항목 고유 번호 |
| 2 | `RESUME_NUM` | 이력서 번호 | NUMBER(18) | N | FK | - | `T_RESUME(RESUME_NUM)` ON DELETE CASCADE |
| 3 | `COMPANY_NAME` | 회사명 | VARCHAR2(200) | N | - | - | 근무 직장명 |
| 4 | `JOIN_DATE` | 입사일자 | DATE | Y | - | - | 입사 연월일 |
| 5 | `RESIGN_DATE` | 퇴사일자 | DATE | Y | - | - | 퇴사 연월일 (재직 중일 시 NULL) |
| 6 | `MAIN_DUTY` | 주요 담당업무 | VARCHAR2(2000) | Y | - | - | 프로젝트 및 수행 업무 상세 |

---

### 2.4 T_CERTIFICATION (자격증 정보)
- **설명:** 이력서에 포함되는 자격증, 면허, 어학 성적 정보 (1:N)
- **시퀀스:** `SEQ_T_CERTIFICATION` / **인덱스:** `RESUME_NUM`, `ISSUE_DATE`

| No | 컬럼 물리명 | 컬럼 논리명 | 데이터 타입 | Null 허용 | Key | 기본값 | 비고 및 제약사항 |
|:--:|:---|:---|:---|:--:|:--:|:---|:---|
| 1 | `CERT_NUM` | 자격증 번호 | NUMBER(18) | N | PK | `SEQ_T_CERTIFICATION.NEXTVAL` | 자격증 항목 고유 번호 |
| 2 | `RESUME_NUM` | 이력서 번호 | NUMBER(18) | N | FK | - | `T_RESUME(RESUME_NUM)` ON DELETE CASCADE |
| 3 | `CERT_NAME` | 자격증명 | VARCHAR2(200) | N | - | - | 자격증/어학시험 명칭 |
| 4 | `CERT_GRADE` | 등급/점수 | VARCHAR2(100) | Y | - | - | 등급(1급), 점수(TOEIC 850) |
| 5 | `ISSUE_DATE` | 발급(취득)일 | DATE | Y | - | - | 자격증 취득 연월일 |

---

### 2.5 T_COVER_LETTER (자기소개서)
- **설명:** 회원의 4대 핵심 문항 자기소개서 내용 (회원과 1:1 관계)
- **시퀀스:** 없음 (`USER_NUM`을 PK 겸 FK로 사용)

| No | 컬럼 물리명 | 컬럼 논리명 | 데이터 타입 | Null 허용 | Key | 기본값 | 비고 및 제약사항 |
|:--:|:---|:---|:---|:--:|:--:|:---|:---|
| 1 | `USER_NUM` | 회원 번호 | NUMBER(9) | N | PK, FK | - | `T_USER(USER_NUM)` ON DELETE CASCADE |
| 2 | `GROWTH_PROCESS` | 성장 과정 | CLOB | Y | - | - | 1번 문항 본문 |
| 3 | `PERSONALITY_STRENGTHS_WEAKNESSES` | 성격의 장단점 | CLOB | Y | - | - | 2번 문항 본문 |
| 4 | `PROBLEM_SOLVING_EXPERIENCE` | 문제해결/직무경험 | CLOB | Y | - | - | 3번 문항 본문 |
| 5 | `POST_JOINING_ASPIRATION` | 입사 후 포부 | CLOB | Y | - | - | 4번 문항 본문 |
| 6 | `CREATED_AT` | 등록일시 | DATE | N | - | `SYSDATE` | 자소서 생성일시 |
| 7 | `UPDATED_AT` | 수정일시 | DATE | N | - | `SYSDATE` | 자소서 최종수정일시 |

---

### 2.6 T_PORTFOLIO (포트폴리오)
- **설명:** 회원의 외부 포트폴리오 파일 또는 노션/깃허브 링크 (회원과 1:1 관계)
- **시퀀스:** 없음 (`USER_NUM`을 PK 겸 FK로 사용)

| No | 컬럼 물리명 | 컬럼 논리명 | 데이터 타입 | Null 허용 | Key | 기본값 | 비고 및 제약사항 |
|:--:|:---|:---|:---|:--:|:--:|:---|:---|
| 1 | `USER_NUM` | 회원 번호 | NUMBER(9) | N | PK, FK | - | `T_USER(USER_NUM)` ON DELETE CASCADE |
| 2 | `FILE_URL` | 포트폴리오 링크 | VARCHAR2(500) | Y | - | - | 첨부파일 저장 경로 or 웹 링크 |
| 3 | `CREATED_AT` | 등록일시 | DATE | N | - | `SYSDATE` | 포트폴리오 등록일시 |
| 4 | `UPDATED_AT` | 수정일시 | DATE | N | - | `SYSDATE` | 포트폴리오 수정일시 |

---

# 3. AI 모의 면접 도메인

### 3.1 T_INTERVIEW_HISTORY (면접 질문/답변 내역)
- **설명:** 사용자가 진행한 AI 모의면접 5문항 질의응답 세션 기록
- **시퀀스:** `SEQ_T_INTERVIEW_HISTORY`

| No | 컬럼 물리명 | 컬럼 논리명 | 데이터 타입 | Null 허용 | Key | 기본값 | 비고 및 제약사항 |
|:--:|:---|:---|:---|:--:|:--:|:---|:---|
| 1 | `HISTORY_NUM` | 면접 기록 번호 | NUMBER(18) | N | PK | `SEQ_T_INTERVIEW_HISTORY.NEXTVAL` | 모의면접 고유 세션 ID |
| 2 | `USER_NUM` | 응시자 번호 | NUMBER(9) | N | FK | - | `T_USER(USER_NUM)` ON DELETE CASCADE |
| 3 | `QUESTION1` ~ `5` | AI 질문 1~5 | VARCHAR2(1000) | Y | - | - | AI가 생성한 단계별 질문 |
| 4 | `ANSWER1` ~ `5` | 유저 답변 1~5 | VARCHAR2(3000) | Y | - | - | 사용자가 제출한 텍스트 답변 |
| 5 | `INTERVIEW_DATE` | 면접 일시 | DATE | N | - | `SYSDATE` | 모의면접 응시 일시 |

---

### 3.2 T_INTERVIEW_RESULT (면접 평가 결과)
- **설명:** 모의면접 세션 종료 후 LLM이 분석한 5대 핵심 역량 지표 평가 점수
- **시퀀스:** 없음 (복합 기본키: `HISTORY_NUM` + `USER_NUM`)

| No | 컬럼 물리명 | 컬럼 논리명 | 데이터 타입 | Null 허용 | Key | 기본값 | 비고 및 제약사항 |
|:--:|:---|:---|:---|:--:|:--:|:---|:---|
| 1 | `HISTORY_NUM` | 면접 기록 번호 | NUMBER(18) | N | PK, FK | - | `T_INTERVIEW_HISTORY(HISTORY_NUM)` ON DELETE CASCADE |
| 2 | `USER_NUM` | 응시자 번호 | NUMBER(9) | N | PK, FK | - | `T_USER(USER_NUM)` ON DELETE CASCADE |
| 3 | `CONFIDENCE_SCORE` | 자신감 점수 | NUMBER(5,2) | Y | - | `0.00` | 점수 (0.00 ~ 100.00) |
| 4 | `PERSISTENCE_SCORE` | 집요함 점수 | NUMBER(5,2) | Y | - | `0.00` | 점수 (0.00 ~ 100.00) |
| 5 | `EXPERTISE_SCORE` | 전문성 점수 | NUMBER(5,2) | Y | - | `0.00` | 점수 (0.00 ~ 100.00) |
| 6 | `LOGIC_SCORE` | 논리성 점수 | NUMBER(5,2) | Y | - | `0.00` | 점수 (0.00 ~ 100.00) |
| 7 | `DELIVERY_SCORE` | 전달력 점수 | NUMBER(5,2) | Y | - | `0.00` | 점수 (0.00 ~ 100.00) |
| 8 | `TOTAL_SCORE` | 종합 점수 | NUMBER(5,2) | Y | - | `0.00` | 종합 평균 평점 |
| 9 | `INTERVIEW_DATE` | 평가 일시 | DATE | N | - | `SYSDATE` | 분석 완료 일시 |

---

# 4. 채용 공고 도메인 (전담 파트)

### 4.1 T_RECRUITMENT (사람인 채용 공고 API 적재)
- **설명:** 사람인 OpenAPI로 수집된 실시간 채용 공고 (메인 배너 및 공고 탐색용)
- **시퀀스:** `SEQ_T_RECRUITMENT` / **인덱스:** `(IS_ACTIVE, EXPIRATION_DATE)`

| No | 컬럼 물리명 | 컬럼 논리명 | 데이터 타입 | Null 허용 | Key | 기본값 | 비고 및 제약사항 |
|:--:|:---|:---|:---|:--:|:--:|:---|:---|
| 1 | `RECRUITMENT_NUM` | 공고 내부 식별자 | NUMBER(18) | N | PK | `SEQ_T_RECRUITMENT.NEXTVAL` | 시스템 내부 관리용 PK |
| 2 | `SARAMIN_JOB_ID` | 사람인 공고 ID | VARCHAR2(50) | N | UK | - | 사람인 원본 고유 ID (중복방지) |
| 3 | `COMPANY_NAME` | 회사명 | VARCHAR2(200) | N | - | - | 기업 명칭 (배너 노출) |
| 4 | `TITLE` | 공고 제목 | VARCHAR2(400) | N | - | - | 채용 공고 제목 (배너 노출) |
| 5 | `JOB_URL` | 사람인 원본 링크 | VARCHAR2(500) | N | - | - | 클릭 시 바로 이동할 원본 URL |
| 6 | `LOCATION_NAME` | 근무 지역 | VARCHAR2(200) | Y | - | - | 예: '서울 강남구' |
| 7 | `JOB_NAME` | 직무 명칭 | VARCHAR2(300) | Y | - | - | 예: '백엔드/서버개발' |
| 8 | `EXPERIENCE_LEVEL` | 요구 경력 | VARCHAR2(100) | Y | - | - | 예: '신입', '경력 1~3년' |
| 9 | `EXPIRATION_DATE` | 공고 마감일시 | DATE | Y | - | - | D-Day 뱃지 계산용 마감일자 |
| 10 | `CLOSE_TYPE` | 마감 유형 | VARCHAR2(50) | Y | - | - | 접수마감일, 채용시, 상시채용 등 |
| 11 | `IS_ACTIVE` | 진행 상태 | NUMBER(1) | N | - | `1` | 1: 채용 진행중, 0: 채용 마감 |
| 12 | `CREATED_AT` | DB 수집일시 | DATE | N | - | `SYSDATE` | 우리 DB 최초 적재 시각 |

---

# 5. 커뮤니티 및 스터디 도메인

### 5.1 T_CATEGORY (게시판 종류)
- **설명:** 커뮤니티 게시글의 카테고리 분류 (자유게시판, 취업후기 등)
- **시퀀스:** `SEQ_T_CATEGORY`

| No | 컬럼 물리명 | 컬럼 논리명 | 데이터 타입 | Null 허용 | Key | 기본값 | 비고 및 제약사항 |
|:--:|:---|:---|:---|:--:|:--:|:---|:---|
| 1 | `CATEGORY_NUM` | 카테고리 번호 | NUMBER(9) | N | PK | `SEQ_T_CATEGORY.NEXTVAL` | 카테고리 고유 식별자 |
| 2 | `CATEGORY_NAME` | 카테고리 명칭 | VARCHAR2(100) | N | - | - | 예: '자유게시판', '취업후기' |

---

### 5.2 T_POST (커뮤니티 게시글)
- **설명:** 사용자가 작성하는 커뮤니티 게시글 본문 및 메타데이터
- **시퀀스:** `SEQ_T_POST` / **인덱스:** `CATEGORY_NUM`, `USER_NUM`

| No | 컬럼 물리명 | 컬럼 논리명 | 데이터 타입 | Null 허용 | Key | 기본값 | 비고 및 제약사항 |
|:--:|:---|:---|:---|:--:|:--:|:---|:---|
| 1 | `POST_NUM` | 게시글 번호 | NUMBER(18) | N | PK | `SEQ_T_POST.NEXTVAL` | 게시글 고유 식별자 |
| 2 | `CATEGORY_NUM` | 카테고리 번호 | NUMBER(9) | N | FK | - | `T_CATEGORY(CATEGORY_NUM)` ON DELETE CASCADE |
| 3 | `USER_NUM` | 작성자 번호 | NUMBER(9) | N | FK | - | `T_USER(USER_NUM)` ON DELETE CASCADE |
| 4 | `POST_TITLE` | 게시글 제목 | VARCHAR2(300) | N | - | - | 제목 |
| 5 | `POST_CONTENT` | 게시글 본문 | CLOB | Y | - | - | 장문 본문 내용 |
| 6 | `POST_LIKE_COUNT` | 좋아요 수 | NUMBER(9) | N | - | `0` | 누적 추천(좋아요) 수 |
| 7 | `VIEW_COUNT` | 조회수 | NUMBER(9) | N | - | `0` | 누적 조회수 |
| 8 | `POST_FILE` | 첨부파일 | VARCHAR2(500) | Y | - | - | 업로드 파일 경로 |
| 9 | `POST_DATE` | 작성일시 | DATE | N | - | `SYSDATE` | 등록일시 |

---

### 5.3 T_POST_LIKE (게시글 좋아요 이력)
- **설명:** 회원이 게시글에 좋아요를 누른 내역 (중복 추천 방지)
- **시퀀스:** 없음 (복합 기본키: `POST_NUM` + `USER_NUM`)

| No | 컬럼 물리명 | 컬럼 논리명 | 데이터 타입 | Null 허용 | Key | 기본값 | 비고 및 제약사항 |
|:--:|:---|:---|:---|:--:|:--:|:---|:---|
| 1 | `POST_NUM` | 대상 게시글 번호 | NUMBER(18) | N | PK, FK | - | `T_POST(POST_NUM)` ON DELETE CASCADE |
| 2 | `USER_NUM` | 누른 유저 번호 | NUMBER(9) | N | PK, FK | - | `T_USER(USER_NUM)` ON DELETE CASCADE |
| 3 | `CREATED_AT` | 추천 일시 | DATE | N | - | `SYSDATE` | 좋아요 누른 시각 |

---

### 5.4 T_COMMENT (게시글 댓글)
- **설명:** 커뮤니티 게시글에 달리는 댓글 목록
- **시퀀스:** `SEQ_T_COMMENT` / **인덱스:** `POST_NUM`, `USER_NUM`

| No | 컬럼 물리명 | 컬럼 논리명 | 데이터 타입 | Null 허용 | Key | 기본값 | 비고 및 제약사항 |
|:--:|:---|:---|:---|:--:|:--:|:---|:---|
| 1 | `COMMENT_NUM` | 댓글 번호 | NUMBER(18) | N | PK | `SEQ_T_COMMENT.NEXTVAL` | 댓글 고유 번호 |
| 2 | `POST_NUM` | 소속 게시글 번호 | NUMBER(18) | N | FK | - | `T_POST(POST_NUM)` ON DELETE CASCADE |
| 3 | `USER_NUM` | 작성자 번호 | NUMBER(9) | N | FK | - | `T_USER(USER_NUM)` ON DELETE CASCADE |
| 4 | `COMMENT_CONTENT` | 댓글 내용 | VARCHAR2(2000) | N | - | - | 댓글 텍스트 본문 |
| 5 | `COMMENT_DATE` | 작성일시 | DATE | N | - | `SYSDATE` | 등록일시 |

---

### 5.5 T_STUDY (스터디 모집 방)
- **설명:** 취업/면접 스터디 모집 게시판의 개설 스터디 방 정보
- **시퀀스:** `SEQ_T_STUDY`

| No | 컬럼 물리명 | 컬럼 논리명 | 데이터 타입 | Null 허용 | Key | 기본값 | 비고 및 제약사항 |
|:--:|:---|:---|:---|:--:|:--:|:---|:---|
| 1 | `STUDY_NUM` | 스터디 방 번호 | NUMBER(18) | N | PK | `SEQ_T_STUDY.NEXTVAL` | 스터디 고유 번호 |
| 2 | `USER_NUM` | 개설자(방장) 번호 | NUMBER(9) | N | FK | - | `T_USER(USER_NUM)` ON DELETE CASCADE |
| 3 | `STUDY_NAME` | 스터디 방 이름 | VARCHAR2(200) | N | - | - | 스터디 그룹 명칭 |
| 4 | `STUDY_EXPLAIN` | 스터디 소개/설명 | VARCHAR2(2000) | Y | - | - | 스터디 목표, 규칙 등 설명 |
| 5 | `STUDY_CREATE_DATE` | 개설일시 | DATE | N | - | `SYSDATE` | 방 개설 시각 |

---

### 5.6 T_STUDY_MEMBER (스터디 참여자 명단)
- **설명:** 개설된 스터디에 가입/참여한 회원 목록 (N:M 해소)
- **시퀀스:** 없음 (복합 기본키: `STUDY_NUM` + `USER_NUM`)

| No | 컬럼 물리명 | 컬럼 논리명 | 데이터 타입 | Null 허용 | Key | 기본값 | 비고 및 제약사항 |
|:--:|:---|:---|:---|:--:|:--:|:---|:---|
| 1 | `STUDY_NUM` | 참여 스터디 번호 | NUMBER(18) | N | PK, FK | - | `T_STUDY(STUDY_NUM)` ON DELETE CASCADE |
| 2 | `USER_NUM` | 참여 회원 번호 | NUMBER(9) | N | PK, FK | - | `T_USER(USER_NUM)` ON DELETE CASCADE |
| 3 | `STUDY_JOIN_DATE` | 참여(가입)일시 | DATE | N | - | `SYSDATE` | 스터디 참가 시각 |
