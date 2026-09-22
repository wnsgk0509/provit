# 📋 Provit (프로빗) 데이터베이스 테이블 명세서 (Table Specification)

- **Database Engine:** Oracle Database 19c Enterprise Edition
- **Character Set:** AL32UTF8
- **총 테이블 수:** 19개
- **총 시퀀스 수:** 13개
- **최종 수정일:** 2026-09-21

---

## 📑 목차 (Table of Contents)

1. [회원 및 직무 도메인](#1-회원-및-직무-도메인)
   - [1.1 T_OCCUPATION (대분류 직군)](#11-t_occupation-대분류-직군)
   - [1.2 T_JOB (소분류 직무)](#12-t_job-소분류-직무)
   - [1.3 T_USER (회원 기본 정보)](#13-t_user-회원-기본-정보)
2. [유저 이력 문서 도메인](#2-유저-이력-문서-도메인)
   - [2.1 T_EDUCODE (학력 코드 분류)](#21-t_educode-학력-코드-분류)
   - [2.2 T_RESUME (이력서 기본 정보)](#22-t_resume-이력서-기본-정보)
   - [2.3 T_EDUCATION (학력 정보)](#23-t_education-학력-정보)
   - [2.4 T_CAREER (경력 정보)](#24-t_career-경력-정보)
   - [2.5 T_CERTIFICATION (자격증 정보)](#25-t_certification-자격증-정보)
   - [2.6 T_COVER_LETTER (자기소개서)](#26-t_cover_letter-자기소개서)
   - [2.7 T_PORTFOLIO (포트폴리오)](#27-t_portfolio-포트폴리오)
3. [AI 모의 면접 도메인](#3-ai-모의-면접-도메인)
   - [3.1 T_INTERVIEW_HISTORY (면접 Q&A 내역)](#31-t_interview_history-면접-qa-내역)
   - [3.2 T_INTERVIEW_RESULT (면접 평가 결과)](#32-t_interview_result-면접-평가-결과)
4. [채용 공고 도메인](#4-채용-공고-도메인)
   - [4.1 T_RECRUITMENT (사람인 채용 공고)](#41-t_recruitment-사람인-채용-공고)
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
- **테이블 물리명:** `T_OCCUPATION`
- **테이블 논리명:** 대분류 직군
- **설명:** 개발, 기획, 디자인 등 대분류 직군 분류 코드 관리
- **시퀀스:** 없음 (코드형 식별자 사용)

| no | column name | 컬럼명 | type | length | PK | NN | Default | 정의/설명 | 참조테이블 | 비고 |
|:--:|:---|:---|:---|:--:|:--:|:--:|:---|:---|:---|:---|
| 1 | OCCUPATION_CODE | 직군 코드 | VARCHAR2 | 20 | PK | NOT NULL | | 대분류 직군 고유 식별 코드 | | 사람인 표준 코드 (예: '2', '16', '5') |
| 2 | OCCUPATION_NAME | 직군명 | VARCHAR2 | 200 | | NOT NULL | | 대분류 직군 이름 | | 예: 'IT개발·데이터', '기획·전략' (총 21개) |

| no | Index name | Index type | Unique | 구성 컬럼 |
|:--:|:---|:--:|:--:|:---|
| 1 | PK_T_OCCUPATION_IDX | PK | Unique | OCCUPATION_CODE |

---

### 1.2 T_JOB (소분류 직무)
- **테이블 물리명:** `T_JOB`
- **테이블 논리명:** 소분류 직무
- **설명:** 백엔드/서버개발, 웹개발, 기획, 인사 등 직군에 속한 상세 직무 코드 관리 (사람인 표준 2,178개)
- **시퀀스:** 없음 (코드형 식별자 사용)

| no | column name | 컬럼명 | type | length | PK | NN | Default | 정의/설명 | 참조테이블 | 비고 |
|:--:|:---|:---|:---|:--:|:--:|:--:|:---|:---|:---|:---|
| 1 | JOB_CODE | 직무 코드 | VARCHAR2 | 20 | PK | NOT NULL | | 소분류 직무 고유 식별 코드 | | 사람인 표준 코드 (예: '84', '87', '2198') |
| 2 | OCCUPATION_CODE | 소속 직군 코드 | VARCHAR2 | 20 | | NOT NULL | | 소속 대분류 직군 코드 | T_OCCUPATION(OCCUPATION_CODE) | ON DELETE CASCADE |
| 3 | JOB_NAME | 직무명 | VARCHAR2 | 200 | | NOT NULL | | 소분류 직무 이름 | | 예: '백엔드/서버개발', '웹개발' (총 2,178개) |

| no | Index name | Index type | Unique | 구성 컬럼 |
|:--:|:---|:--:|:--:|:---|
| 1 | PK_T_JOB_IDX | PK | Unique | JOB_CODE |

---

### 1.3 T_USER (회원 기본 정보)
- **테이블 물리명:** `T_USER`
- **테이블 논리명:** 회원 기본 정보
- **설명:** 서비스 전체 회원의 계정 인증 및 프로필 기본 정보
- **시퀀스:** `SEQ_T_USER`

| no | column name | 컬럼명 | type | length | PK | NN | Default | 정의/설명 | 참조테이블 | 비고 |
|:--:|:---|:---|:---|:--:|:--:|:--:|:---|:---|:---|:---|
| 1 | USER_NUM | 회원 번호 | NUMBER | 9 | PK | NOT NULL | SEQ_T_USER.NEXTVAL | 회원 고유 식별 번호 | | 시퀀스 자동 채번 |
| 2 | USER_NAME | 회원 실명 | VARCHAR2 | 100 | | NOT NULL | | 회원 실제 이름 | | 사용자 본명 |
| 3 | USER_NICKNAME | 닉네임 | VARCHAR2 | 100 | | NOT NULL | | 서비스 내 활동 닉네임 | | UNIQUE (중복 불가) |
| 4 | USER_BIRTH_DATE | 생년월일 | DATE | | | | | 회원 생년월일 (YYYY-MM-DD) | | |
| 5 | USER_EMAIL | 이메일 | VARCHAR2 | 200 | | NOT NULL | | 로그인 이메일 계정 ID | | UNIQUE (중복 불가) |
| 6 | USER_PW | 비밀번호 | VARCHAR2 | 255 | | NOT NULL | | 암호화된 비밀번호 해시 | | BCrypt 단방향 암호화 |
| 7 | USER_REGISTER_DATE | 가입일시 | DATE | | | NOT NULL | SYSDATE | 최초 회원가입 일시 | | |
| 8 | USER_TYPE | 권한 구분 | VARCHAR2 | 30 | | | 'USER' | 계정 권한 구분 | | 'USER', 'ADMIN' |
| 9 | JOB_CODE | 희망 직무 코드 | VARCHAR2 | 20 | | | | 회원이 희망하는 소분류 직무 | T_JOB(JOB_CODE) | ON DELETE SET NULL |
| 10 | OCCUPATION_CODE | 희망 직군 코드 | VARCHAR2 | 20 | | | | 회원이 희망하는 대분류 직군 | T_OCCUPATION(OCCUPATION_CODE) | ON DELETE SET NULL |
| 11 | USER_IS_DELETED | 탈퇴 여부 | NUMBER | 1 | | NOT NULL | 0 | 회원 탈퇴 플래그 | | 0: 정상, 1: 탈퇴 |

| no | Index name | Index type | Unique | 구성 컬럼 |
|:--:|:---|:--:|:--:|:---|
| 1 | PK_T_USER_IDX | PK | Unique | USER_NUM |
| 2 | UK_T_USER_EMAIL_IDX | Unique | Unique | USER_EMAIL |
| 3 | UK_T_USER_NICKNAME_IDX | Unique | Unique | USER_NICKNAME |

---

# 2. 유저 이력 문서 도메인

### 2.1 T_EDUCODE (학력 코드 분류)
- **테이블 물리명:** `T_EDUCODE`
- **테이블 논리명:** 학력 코드 분류
- **설명:** 사람인 채용 OpenAPI 연동 및 학력 검색 필터링용 표준 학력 코드 관리
- **시퀀스:** 없음 (코드형 식별자 사용)

| no | column name | 컬럼명 | type | length | PK | NN | Default | 정의/설명 | 참조테이블 | 비고 |
|:--:|:---|:---|:---|:--:|:--:|:--:|:---|:---|:---|:---|
| 1 | EDUCATION_CODE | 학력 코드 | NUMBER | 1 | PK | NOT NULL | | 학력 고유 식별 코드 | | 0:학력무관, 1:고졸, 2:초대졸, 3:대졸 등 |
| 2 | EDUCATION_NAME | 학력 이름 | VARCHAR2 | 50 | | NOT NULL | | 학력 코드에 매칭되는 한글 명칭 | | 예: '대학교졸업(4년)' |

| no | Index name | Index type | Unique | 구성 컬럼 |
|:--:|:---|:--:|:--:|:---|
| 1 | PK_T_EDUCODE_IDX | PK | Unique | EDUCATION_CODE |

---

### 2.2 T_RESUME (이력서 기본 정보)
- **테이블 물리명:** `T_RESUME`
- **테이블 논리명:** 이력서 기본 정보
- **설명:** 회원의 이력서 마스터 정보 (학력/경력/자격증의 1:N 부모)
- **시퀀스:** `SEQ_T_RESUME`

| no | column name | 컬럼명 | type | length | PK | NN | Default | 정의/설명 | 참조테이블 | 비고 |
|:--:|:---|:---|:---|:--:|:--:|:--:|:---|:---|:---|:---|
| 1 | RESUME_NUM | 이력서 번호 | NUMBER | 18 | PK | NOT NULL | SEQ_T_RESUME.NEXTVAL | 이력서 고유 식별 번호 | | 시퀀스 자동 채번 |
| 2 | USER_NUM | 작성자 번호 | NUMBER | 9 | | NOT NULL | | 이력서 작성 회원 번호 | T_USER(USER_NUM) | ON DELETE CASCADE |
| 3 | RESUME_TITLE | 이력서 제목 | VARCHAR2 | 200 | | | | 이력서 제목 | | 예: '신입 백엔드 개발자 홍길동의 이력서' |
| 4 | HIGHEST_LEVEL | 최종학력 | VARCHAR2 | 20 | | NOT NULL | | 최종 학력 기재 (고졸, 초대졸, 대졸 등) | | 목록/필터링 최적화 |
| 5 | EDUCATION_CODE | 검색 학력 코드 | NUMBER | 1 | | NOT NULL | | 채용 API 요청 시 전달할 학력 검색 코드 | T_EDUCODE(EDUCATION_CODE) | OpenAPI 연동 |
| 6 | MOTIVATION | 지원 동기 | CLOB | | | | | 장문 지원 동기 텍스트 | | |
| 7 | DESIRED_LOCATION | 희망 근무지 | VARCHAR2 | 200 | | | | 희망 근무 지역 | | 예: '서울 강남구' |
| 8 | DESIRED_WORK_TYPE | 희망 고용형태 | VARCHAR2 | 100 | | | | 희망 고용 형태 | | 정규직, 계약직 등 |
| 9 | CREATED_AT | 등록일시 | DATE | | | NOT NULL | SYSDATE | 이력서 최초 작성 일시 | | |
| 10 | UPDATED_AT | 수정일시 | DATE | | | NOT NULL | SYSDATE | 이력서 최종 수정 일시 | | |

| no | Index name | Index type | Unique | 구성 컬럼 |
|:--:|:---|:--:|:--:|:---|
| 1 | PK_T_RESUME_IDX | PK | Unique | RESUME_NUM |
| 2 | IDX_RESUME_USER_NUM | Normal | Non-Unique | USER_NUM |

---

### 2.3 T_EDUCATION (학력 정보)
- **테이블 물리명:** `T_EDUCATION`
- **테이블 논리명:** 학력 정보
- **설명:** 이력서에 종속되는 회원의 출신 학교 및 학력 정보 (1:N 자식)
- **시퀀스:** `SEQ_T_EDUCATION`

| no | column name | 컬럼명 | type | length | PK | NN | Default | 정의/설명 | 참조테이블 | 비고 |
|:--:|:---|:---|:---|:--:|:--:|:--:|:---|:---|:---|:---|
| 1 | EDU_NUM | 학력 번호 | NUMBER | 18 | PK | NOT NULL | SEQ_T_EDUCATION.NEXTVAL | 학력 사항 고유 식별 번호 | | 시퀀스 자동 채번 |
| 2 | RESUME_NUM | 이력서 번호 | NUMBER | 18 | | NOT NULL | | 소속 이력서 번호 | T_RESUME(RESUME_NUM) | ON DELETE CASCADE |
| 3 | SCHOOL_NAME | 학교명 | VARCHAR2 | 200 | | NOT NULL | | 출신 학교 이름 | | |
| 4 | ADMISSION_DATE | 입학일자 | DATE | | | | | 입학 년월일 | | |
| 5 | GRADUATION_DATE | 졸업일자 | DATE | | | | | 졸업 (예정) 년월일 | | |
| 6 | MAJOR | 전공 | VARCHAR2 | 200 | | | | 전공 학과명 | | |
| 7 | EDUCATION_STATUS | 학력 상태 | VARCHAR2 | 20 | | NOT NULL | | 학력 상태 선택 (졸업, 재학, 수료 등) | | |

| no | Index name | Index type | Unique | 구성 컬럼 |
|:--:|:---|:--:|:--:|:---|
| 1 | PK_T_EDUCATION_IDX | PK | Unique | EDU_NUM |
| 2 | IDX_EDU_RESUME_NUM | Normal | Non-Unique | RESUME_NUM |
| 3 | IDX_EDU_GRADUATION_DATE | Normal | Non-Unique | GRADUATION_DATE |

---

### 2.4 T_CAREER (경력 정보)
- **테이블 물리명:** `T_CAREER`
- **테이블 논리명:** 경력 정보
- **설명:** 이력서에 종속되는 회원의 이전 직장 및 경력 사항 (1:N 자식)
- **시퀀스:** `SEQ_T_CAREER`

| no | column name | 컬럼명 | type | length | PK | NN | Default | 정의/설명 | 참조테이블 | 비고 |
|:--:|:---|:---|:---|:--:|:--:|:--:|:---|:---|:---|:---|
| 1 | CAREER_NUM | 경력 번호 | NUMBER | 18 | PK | NOT NULL | SEQ_T_CAREER.NEXTVAL | 경력 사항 고유 식별 번호 | | 시퀀스 자동 채번 |
| 2 | RESUME_NUM | 이력서 번호 | NUMBER | 18 | | NOT NULL | | 소속 이력서 번호 | T_RESUME(RESUME_NUM) | ON DELETE CASCADE |
| 3 | COMPANY_NAME | 회사명 | VARCHAR2 | 200 | | NOT NULL | | 근무 직장/기업 이름 | | |
| 4 | JOIN_DATE | 입사일자 | DATE | | | | | 입사 년월일 | | |
| 5 | RESIGN_DATE | 퇴사일자 | DATE | | | | | 퇴사 년월일 (재직 중일 경우 NULL) | | |
| 6 | MAIN_DUTY | 담당 업무 | VARCHAR2 | 2000 | | | | 수행 주요 업무 및 역할 설명 | | |

| no | Index name | Index type | Unique | 구성 컬럼 |
|:--:|:---|:--:|:--:|:---|
| 1 | PK_T_CAREER_IDX | PK | Unique | CAREER_NUM |
| 2 | IDX_CAREER_RESUME_NUM | Normal | Non-Unique | RESUME_NUM |
| 3 | IDX_CAREER_RESIGN_DATE | Normal | Non-Unique | RESIGN_DATE |

---

### 2.5 T_CERTIFICATION (자격증 정보)
- **테이블 물리명:** `T_CERTIFICATION`
- **테이블 논리명:** 자격증 정보
- **설명:** 이력서에 종속되는 회원의 보유 자격증 및 어학 점수 (1:N 자식)
- **시퀀스:** `SEQ_T_CERTIFICATION`

| no | column name | 컬럼명 | type | length | PK | NN | Default | 정의/설명 | 참조테이블 | 비고 |
|:--:|:---|:---|:---|:--:|:--:|:--:|:---|:---|:---|:---|
| 1 | CERT_NUM | 자격증 번호 | NUMBER | 18 | PK | NOT NULL | SEQ_T_CERTIFICATION.NEXTVAL | 자격증 사항 고유 식별 번호 | | 시퀀스 자동 채번 |
| 2 | RESUME_NUM | 이력서 번호 | NUMBER | 18 | | NOT NULL | | 소속 이력서 번호 | T_RESUME(RESUME_NUM) | ON DELETE CASCADE |
| 3 | CERT_NAME | 자격증명 | VARCHAR2 | 200 | | NOT NULL | | 취득 자격증/시험 이름 | | 예: '정보처리기사' |
| 4 | CERT_GRADE | 등급/점수 | VARCHAR2 | 100 | | | | 취득 등급 또는 어학 점수 | | 예: 'TOEIC 850' |
| 5 | ISSUE_DATE | 취득일자 | DATE | | | | | 자격증 취득 년월일 | | |

| no | Index name | Index type | Unique | 구성 컬럼 |
|:--:|:---|:--:|:--:|:---|
| 1 | PK_T_CERTIFICATION_IDX | PK | Unique | CERT_NUM |
| 2 | IDX_CERT_RESUME_NUM | Normal | Non-Unique | RESUME_NUM |
| 3 | IDX_CERT_ISSUE_DATE | Normal | Non-Unique | ISSUE_DATE |

---

### 2.6 T_COVER_LETTER (자기소개서)
- **테이블 물리명:** `T_COVER_LETTER`
- **테이블 논리명:** 자기소개서
- **설명:** 회원의 문항별 자기소개서 텍스트 데이터 (회원과 1:N 관계)
- **시퀀스:** `SEQ_T_COVER_LETTER`

| no | column name | 컬럼명 | type | length | PK | NN | Default | 정의/설명 | 참조테이블 | 비고 |
|:--:|:---|:---|:---|:--:|:--:|:--:|:---|:---|:---|:---|
| 1 | LETTER_NUM | 자기소개서 번호 | NUMBER | 18 | PK | NOT NULL | SEQ_T_COVER_LETTER.NEXTVAL | 자기소개서 고유 식별 번호 | | 시퀀스 자동 채번 |
| 2 | USER_NUM | 회원 번호 | NUMBER | 9 | | NOT NULL | | 작성 회원 번호 | T_USER(USER_NUM) | ON DELETE CASCADE |
| 3 | COVER_LETTER_TITLE | 자기소개서 제목 | VARCHAR2 | 200 | | NOT NULL | | 자기소개서 제목 | | 예: '도전하는 백엔드 개발자 자기소개서' |
| 4 | GROWTH_PROCESS | 성장 과정 | VARCHAR2 | 3000 | | | | 성장 과정 및 배경 기술서 | | 한글 약 1,000자 |
| 5 | PERSONALITY_STRENGTHS_WEAKNESSES | 성격의 장단점 | VARCHAR2 | 3000 | | | | 성격의 장단점 기술서 | | 한글 약 1,000자 |
| 6 | PROBLEM_SOLVING_EXPERIENCE | 문제 해결 경험 | VARCHAR2 | 3000 | | | | 위기 극복 및 문제 해결 경험 | | 한글 약 1,000자 |
| 7 | POST_JOINING_ASPIRATION | 입사 후 포부 | VARCHAR2 | 3000 | | | | 입사 후 포부 및 비전 | | 한글 약 1,000자 |
| 8 | CREATED_AT | 등록일시 | DATE | | | NOT NULL | SYSDATE | 자기소개서 최초 등록 일시 | | |
| 9 | UPDATED_AT | 수정일시 | DATE | | | NOT NULL | SYSDATE | 자기소개서 최종 수정 일시 | | |

| no | Index name | Index type | Unique | 구성 컬럼 |
|:--:|:---|:--:|:--:|:---|
| 1 | PK_T_COVER_LETTER_IDX | PK | Unique | LETTER_NUM |
| 2 | IDX_COVER_LETTER_USER_NUM | Normal | Non-Unique | USER_NUM |

---

### 2.7 T_PORTFOLIO (포트폴리오)
- **테이블 물리명:** `T_PORTFOLIO`
- **테이블 논리명:** 포트폴리오
- **설명:** 회원의 포트폴리오 파일 업로드 정보 (회원과 1:N 관계)
- **시퀀스:** `SEQ_T_PORTFOLIO`

| no | column name | 컬럼명 | type | length | PK | NN | Default | 정의/설명 | 참조테이블 | 비고 |
|:--:|:---|:---|:---|:--:|:--:|:--:|:---|:---|:---|:---|
| 1 | PORTFOLIO_NUM | 포트폴리오 번호 | NUMBER | 18 | PK | NOT NULL | SEQ_T_PORTFOLIO.NEXTVAL | 포트폴리오 고유 식별 번호 | | 시퀀스 자동 채번 |
| 2 | USER_NUM | 회원 번호 | NUMBER | 9 | | NOT NULL | | 소유 회원 번호 | T_USER(USER_NUM) | ON DELETE CASCADE |
| 3 | PORTFOLIO_TITLE | 포트폴리오 제목 | VARCHAR2 | 200 | | NOT NULL | | 포트폴리오 제목 | | 예: 'Provit 프로젝트 포트폴리오' |
| 4 | FILE_URL | 파일 URL | VARCHAR2 | 500 | | | | 업로드된 포트폴리오 파일 URL | | PDF/문서 링크 |
| 5 | CREATED_AT | 등록일시 | DATE | | | NOT NULL | SYSDATE | 포트폴리오 최초 등록 일시 | | |
| 6 | UPDATED_AT | 수정일시 | DATE | | | NOT NULL | SYSDATE | 포트폴리오 최종 수정 일시 | | |

| no | Index name | Index type | Unique | 구성 컬럼 |
|:--:|:---|:--:|:--:|:---|
| 1 | PK_T_PORTFOLIO_IDX | PK | Unique | PORTFOLIO_NUM |
| 2 | IDX_PORTFOLIO_USER_NUM | Normal | Non-Unique | USER_NUM |

---

# 3. AI 모의 면접 도메인

### 3.1 T_INTERVIEW_HISTORY (면접 Q&A 내역)
- **테이블 물리명:** `T_INTERVIEW_HISTORY`
- **테이블 논리명:** 면접 질문/답변 내역
- **설명:** AI 모의 면접 진행 시 오간 5문항의 질문 및 답변 텍스트 기록 (순수 문답 내역)
- **시퀀스:** `SEQ_T_INTERVIEW_HISTORY`

| no | column name | 컬럼명 | type | length | PK | NN | Default | 정의/설명 | 참조테이블 | 비고 |
|:--:|:---|:---|:---|:--:|:--:|:--:|:---|:---|:---|:---|
| 1 | HISTORY_NUM | 면접 내역 번호 | NUMBER | 18 | PK | NOT NULL | SEQ_T_INTERVIEW_HISTORY.NEXTVAL | 모의 면접 고유 식별 번호 | | 시퀀스 자동 채번 |
| 2 | USER_NUM | 응시자 번호 | NUMBER | 9 | | NOT NULL | | 면접 응시 회원 번호 | T_USER(USER_NUM) | ON DELETE CASCADE |
| 3 | QUESTION1 | 질문 1 | VARCHAR2 | 1000 | | | | AI 생성 1번 질문 내용 | | |
| 4 | ANSWER1 | 답변 1 | VARCHAR2 | 3000 | | | | 응시자 1번 답변 내용 | | 음성 인식/텍스트 |
| 5 | QUESTION2 | 질문 2 | VARCHAR2 | 1000 | | | | AI 생성 2번 질문 내용 (꼬리질문) | | |
| 6 | ANSWER2 | 답변 2 | VARCHAR2 | 3000 | | | | 응시자 2번 답변 내용 | | |
| 7 | QUESTION3 | 질문 3 | VARCHAR2 | 1000 | | | | AI 생성 3번 질문 내용 | | |
| 8 | ANSWER3 | 답변 3 | VARCHAR2 | 3000 | | | | 응시자 3번 답변 내용 | | |
| 9 | QUESTION4 | 질문 4 | VARCHAR2 | 1000 | | | | AI 생성 4번 질문 내용 | | |
| 10 | ANSWER4 | 답변 4 | VARCHAR2 | 3000 | | | | 응시자 4번 답변 내용 | | |
| 11 | QUESTION5 | 질문 5 | VARCHAR2 | 1000 | | | | AI 생성 5번 질문 내용 | | |
| 12 | ANSWER5 | 답변 5 | VARCHAR2 | 3000 | | | | 응시자 5번 답변 내용 | | |
| 13 | INTERVIEW_DATE | 응시일시 | DATE | | | NOT NULL | SYSDATE | 모의 면접 진행 일시 | | |

| no | Index name | Index type | Unique | 구성 컬럼 |
|:--:|:---|:--:|:--:|:---|
| 1 | PK_T_INTERVIEW_HISTORY_IDX | PK | Unique | HISTORY_NUM |
| 2 | IDX_INTERVIEW_USER_NUM | Normal | Non-Unique | USER_NUM |

---

### 3.2 T_INTERVIEW_RESULT (면접 평가 결과)
- **테이블 물리명:** `T_INTERVIEW_RESULT`
- **테이블 논리명:** 면접 평가 결과
- **설명:** AI 모의 면접 완료 후 산출된 5대 역량 세부 스코어, 총점 및 LLM 역량 피드백 (복합 기본키)
- **시퀀스:** 없음 (`HISTORY_NUM`, `USER_NUM` 복합 PK)

| no | column name | 컬럼명 | type | length | PK | NN | Default | 정의/설명 | 참조테이블 | 비고 |
|:--:|:---|:---|:---|:--:|:--:|:--:|:---|:---|:---|:---|
| 1 | HISTORY_NUM | 면접 내역 번호 | NUMBER | 18 | PK | NOT NULL | | 해당 모의 면접 내역 번호 | T_INTERVIEW_HISTORY(HISTORY_NUM) | 복합 PK, ON DELETE CASCADE |
| 2 | USER_NUM | 응시자 번호 | NUMBER | 9 | PK | NOT NULL | | 면접 응시 회원 번호 | T_USER(USER_NUM) | 복합 PK, ON DELETE CASCADE |
| 3 | CONFIDENCE_SCORE | 자신감 점수 | NUMBER | 5,2 | | | 0.00 | 면접 태도 및 자신감 점수 (100점 만점) | | |
| 4 | PERSISTENCE_SCORE | 끈기/열정 점수 | NUMBER | 5,2 | | | 0.00 | 도전 정신 및 끈기 점수 (100점 만점) | | |
| 5 | EXPERTISE_SCORE | 전문성 점수 | NUMBER | 5,2 | | | 0.00 | 직무 지식 및 기술 역량 점수 (100점 만점) | | |
| 6 | LOGIC_SCORE | 논리력 점수 | NUMBER | 5,2 | | | 0.00 | 논리적 사고 및 답변 전개 점수 (100점 만점) | | |
| 7 | DELIVERY_SCORE | 전달력 점수 | NUMBER | 5,2 | | | 0.00 | 표현력 및 명확한 전달력 점수 (100점 만점) | | |
| 8 | TOTAL_SCORE | 종합 총점 | NUMBER | 5,2 | | | 0.00 | 5개 지표 가중 종합 평점 (100점 만점) | | |
| 9 | STRENGTH | 잘한 점 | VARCHAR2 | 500 | | | | 면접 답변 중 우수한 역량 및 강점 피드백 | | LLM 평가 |
| 10 | WEAKNESS | 아쉬운 점 | VARCHAR2 | 500 | | | | 미흡했던 부분 및 약점 피드백 | | LLM 평가 |
| 11 | PREVIOUS_COMPARISON | 이전 기록 비교 | VARCHAR2 | 500 | | | | 직전 모의면접 대비 변화 및 성장 추이 | | LLM 분석 |
| 12 | IMPROVEMENT_POINT | 개선할 점 | VARCHAR2 | 500 | | | | 차기 면접을 위한 구체적 행동 개선 제안 | | LLM 조언 |
| 13 | INTERVIEW_DATE | 평가일시 | DATE | | | NOT NULL | SYSDATE | 면접 평가 분석 완료 일시 | | |

| no | Index name | Index type | Unique | 구성 컬럼 |
|:--:|:---|:--:|:--:|:---|
| 1 | PK_T_INTERVIEW_RESULT_IDX | PK | Unique | HISTORY_NUM, USER_NUM |

---

# 4. 채용 공고 도메인

### 4.1 T_RECRUITMENT (사람인 채용 공고)
- **테이블 물리명:** `T_RECRUITMENT`
- **테이블 논리명:** 사람인 채용 공고
- **설명:** 사람인 채용 OpenAPI로부터 주기적으로 수집/동기화한 최신 채용 공고 데이터
- **시퀀스:** `SEQ_T_RECRUITMENT`

| no | column name | 컬럼명 | type | length | PK | NN | Default | 정의/설명 | 참조테이블 | 비고 |
|:--:|:---|:---|:---|:--:|:--:|:--:|:---|:---|:---|:---|
| 1 | RECRUITMENT_NUM | 채용공고 번호 | NUMBER | 18 | PK | NOT NULL | SEQ_T_RECRUITMENT.NEXTVAL | 채용 공고 고유 식별 번호 | | 시퀀스 자동 채번 |
| 2 | SARAMIN_JOB_ID | 사람인 공고 ID | VARCHAR2 | 50 | | NOT NULL | | 사람인 OpenAPI 고유 식별 ID | | UNIQUE (중복 적재 방지) |
| 3 | COMPANY_NAME | 기업명 | VARCHAR2 | 200 | | NOT NULL | | 채용 기업/회사 이름 | | |
| 4 | TITLE | 공고 제목 | VARCHAR2 | 400 | | NOT NULL | | 채용 공고 제목 | | |
| 5 | JOB_URL | 사람인 공고 URL | VARCHAR2 | 500 | | NOT NULL | | 클릭 시 연결되는 사람인 원본 URL | | 아웃링크 연동 |
| 6 | LOCATION_NAME | 근무지 | VARCHAR2 | 200 | | | | 근무 지역 (예: 서울 강남구) | | |
| 7 | JOB_NAME | 직무/포지션명 | VARCHAR2 | 300 | | | | 모집 직무 포지션 명칭 | | |
| 8 | EXPERIENCE_LEVEL | 경력 요건 | VARCHAR2 | 100 | | | | 요구 경력 (신입, 경력, 경력무관 등) | | |
| 9 | EXPIRATION_DATE | 마감일자 | DATE | | | | | 공고 마감 일시 (상시 채용 시 NULL) | | |
| 10 | CLOSE_TYPE | 마감 형태 | VARCHAR2 | 50 | | | | 마감 방식 (접수마감일, 채용시 등) | | |
| 11 | IS_ACTIVE | 노출 활성 여부 | NUMBER | 1 | | NOT NULL | 1 | 공고 노출 여부 (1: 노출, 0: 비노출) | | |
| 12 | CREATED_AT | 수집일시 | DATE | | | NOT NULL | SYSDATE | DB 최초 수집 및 적재 일시 | | |

| no | Index name | Index type | Unique | 구성 컬럼 |
|:--:|:---|:--:|:--:|:---|
| 1 | PK_T_RECRUITMENT_IDX | PK | Unique | RECRUITMENT_NUM |
| 2 | UK_T_RECRUIT_JOB_ID_IDX | Unique | Unique | SARAMIN_JOB_ID |
| 3 | IDX_RECRUIT_ACTIVE_EXP | Normal | Non-Unique | IS_ACTIVE, EXPIRATION_DATE |

---

# 5. 커뮤니티 및 스터디 도메인

### 5.1 T_CATEGORY (게시판 종류)
- **테이블 물리명:** `T_CATEGORY`
- **테이블 논리명:** 게시판 종류
- **설명:** 커뮤니티 게시판 종류 구분 (자유게시판, Q&A, 합격후기 등)
- **시퀀스:** `SEQ_T_CATEGORY`

| no | column name | 컬럼명 | type | length | PK | NN | Default | 정의/설명 | 참조테이블 | 비고 |
|:--:|:---|:---|:---|:--:|:--:|:--:|:---|:---|:---|:---|
| 1 | CATEGORY_NUM | 카테고리 번호 | NUMBER | 9 | PK | NOT NULL | SEQ_T_CATEGORY.NEXTVAL | 게시판 종류 식별 번호 | | 시퀀스 자동 채번 |
| 2 | CATEGORY_NAME | 카테고리명 | VARCHAR2 | 100 | | NOT NULL | | 게시판 이름 | | 예: '자유', 'Q&A' |

| no | Index name | Index type | Unique | 구성 컬럼 |
|:--:|:---|:--:|:--:|:---|
| 1 | PK_T_CATEGORY_IDX | PK | Unique | CATEGORY_NUM |

---

### 5.2 T_POST (커뮤니티 게시글)
- **테이블 물리명:** `T_POST`
- **테이블 논리명:** 커뮤니티 게시글
- **설명:** 회원이 작성한 커뮤니티 게시글 본문 및 메타 정보
- **시퀀스:** `SEQ_T_POST`

| no | column name | 컬럼명 | type | length | PK | NN | Default | 정의/설명 | 참조테이블 | 비고 |
|:--:|:---|:---|:---|:--:|:--:|:--:|:---|:---|:---|:---|
| 1 | POST_NUM | 게시글 번호 | NUMBER | 18 | PK | NOT NULL | SEQ_T_POST.NEXTVAL | 게시글 고유 식별 번호 | | 시퀀스 자동 채번 |
| 2 | CATEGORY_NUM | 카테고리 번호 | NUMBER | 9 | | NOT NULL | | 소속 게시판 카테고리 번호 | T_CATEGORY(CATEGORY_NUM) | ON DELETE CASCADE |
| 3 | USER_NUM | 작성자 번호 | NUMBER | 9 | | NOT NULL | | 게시글 작성 회원 번호 | T_USER(USER_NUM) | ON DELETE CASCADE |
| 4 | POST_TITLE | 게시글 제목 | VARCHAR2 | 300 | | NOT NULL | | 게시글 제목 | | |
| 5 | POST_CONTENT | 게시글 본문 | VARCHAR2 | 4000 | | | | 게시글 본문 내용 | | 한글 약 1,300자 |
| 6 | POST_LIKE_COUNT | 좋아요 수 | NUMBER | 9 | | NOT NULL | 0 | 게시글 추천/좋아요 합계 수 | | 집계 캐시 컬럼 |
| 7 | VIEW_COUNT | 조회수 | NUMBER | 9 | | NOT NULL | 0 | 게시글 열람 조회수 | | |
| 8 | POST_FILE | 첨부 파일 | VARCHAR2 | 500 | | | | 첨부 이미지 또는 파일 URL | | |
| 9 | POST_DATE | 작성일시 | DATE | | | NOT NULL | SYSDATE | 게시글 최초 등록 일시 | | |

| no | Index name | Index type | Unique | 구성 컬럼 |
|:--:|:---|:--:|:--:|:---|
| 1 | PK_T_POST_IDX | PK | Unique | POST_NUM |
| 2 | IDX_POST_CATEGORY | Normal | Non-Unique | CATEGORY_NUM |
| 3 | IDX_POST_USER | Normal | Non-Unique | USER_NUM |

---

### 5.3 T_POST_LIKE (게시글 좋아요)
- **테이블 물리명:** `T_POST_LIKE`
- **테이블 논리명:** 게시글 좋아요
- **설명:** 회원의 게시글 중복 추천 방지를 위한 좋아요 이력 테이블 (복합 기본키)
- **시퀀스:** 없음 (`POST_NUM`, `USER_NUM` 복합 PK)

| no | column name | 컬럼명 | type | length | PK | NN | Default | 정의/설명 | 참조테이블 | 비고 |
|:--:|:---|:---|:---|:--:|:--:|:--:|:---|:---|:---|:---|
| 1 | POST_NUM | 게시글 번호 | NUMBER | 18 | PK | NOT NULL | | 추천 대상 게시글 번호 | T_POST(POST_NUM) | 복합 PK, ON DELETE CASCADE |
| 2 | USER_NUM | 회원 번호 | NUMBER | 9 | PK | NOT NULL | | 추천을 누른 회원 번호 | T_USER(USER_NUM) | 복합 PK, ON DELETE CASCADE |
| 3 | CREATED_AT | 추천일시 | DATE | | | NOT NULL | SYSDATE | 추천 클릭 일시 | | |

| no | Index name | Index type | Unique | 구성 컬럼 |
|:--:|:---|:--:|:--:|:---|
| 1 | PK_T_POST_LIKE_IDX | PK | Unique | POST_NUM, USER_NUM |

---

### 5.4 T_COMMENT (게시글 댓글)
- **테이블 물리명:** `T_COMMENT`
- **테이블 논리명:** 게시글 댓글
- **설명:** 커뮤니티 게시글에 등록된 댓글 정보
- **시퀀스:** `SEQ_T_COMMENT`

| no | column name | 컬럼명 | type | length | PK | NN | Default | 정의/설명 | 참조테이블 | 비고 |
|:--:|:---|:---|:---|:--:|:--:|:--:|:---|:---|:---|:---|
| 1 | COMMENT_NUM | 댓글 번호 | NUMBER | 18 | PK | NOT NULL | SEQ_T_COMMENT.NEXTVAL | 댓글 고유 식별 번호 | | 시퀀스 자동 채번 |
| 2 | POST_NUM | 게시글 번호 | NUMBER | 18 | | NOT NULL | | 댓글이 달린 대상 게시글 번호 | T_POST(POST_NUM) | ON DELETE CASCADE |
| 3 | USER_NUM | 작성자 번호 | NUMBER | 9 | | NOT NULL | | 댓글 작성 회원 번호 | T_USER(USER_NUM) | ON DELETE CASCADE |
| 4 | COMMENT_CONTENT | 댓글 내용 | VARCHAR2 | 2000 | | NOT NULL | | 댓글 본문 내용 | | |
| 5 | COMMENT_DATE | 등록일시 | DATE | | | NOT NULL | SYSDATE | 댓글 작성 일시 | | |

| no | Index name | Index type | Unique | 구성 컬럼 |
|:--:|:---|:--:|:--:|:---|
| 1 | PK_T_COMMENT_IDX | PK | Unique | COMMENT_NUM |
| 2 | IDX_COMMENT_POST | Normal | Non-Unique | POST_NUM |
| 3 | IDX_COMMENT_USER | Normal | Non-Unique | USER_NUM |

---

### 5.5 T_STUDY (스터디 모집 방)
- **테이블 물리명:** `T_STUDY`
- **테이블 논리명:** 스터디 모집 방
- **설명:** 회원이 개설한 취업/면접 대비 스터디 모집 방 정보
- **시퀀스:** `SEQ_T_STUDY`

| no | column name | 컬럼명 | type | length | PK | NN | Default | 정의/설명 | 참조테이블 | 비고 |
|:--:|:---|:---|:---|:--:|:--:|:--:|:---|:---|:---|:---|
| 1 | STUDY_NUM | 스터디 방 번호 | NUMBER | 18 | PK | NOT NULL | SEQ_T_STUDY.NEXTVAL | 스터디 모임 고유 식별 번호 | | 시퀀스 자동 채번 |
| 2 | USER_NUM | 개설자 번호 | NUMBER | 9 | | NOT NULL | | 스터디 개설(방장) 회원 번호 | T_USER(USER_NUM) | ON DELETE CASCADE |
| 3 | STUDY_NAME | 스터디 모임명 | VARCHAR2 | 200 | | NOT NULL | | 스터디 방 제목/이름 | | |
| 4 | STUDY_EXPLAIN | 스터디 설명 | VARCHAR2 | 2000 | | | | 스터디 모임 규칙 및 상세 소개 | | |
| 5 | STUDY_CREATE_DATE | 개설일시 | DATE | | | NOT NULL | SYSDATE | 스터디 방 개설 일시 | | |

| no | Index name | Index type | Unique | 구성 컬럼 |
|:--:|:---|:--:|:--:|:---|
| 1 | PK_T_STUDY_IDX | PK | Unique | STUDY_NUM |

---

### 5.6 T_STUDY_MEMBER (스터디 참여자 명단)
- **테이블 물리명:** `T_STUDY_MEMBER`
- **테이블 논리명:** 스터디 참여자 명단
- **설명:** 스터디 방에 가입한 참여자 명단 관리 테이블 (복합 기본키)
- **시퀀스:** 없음 (`STUDY_NUM`, `USER_NUM` 복합 PK)

| no | column name | 컬럼명 | type | length | PK | NN | Default | 정의/설명 | 참조테이블 | 비고 |
|:--:|:---|:---|:---|:--:|:--:|:--:|:---|:---|:---|:---|
| 1 | STUDY_NUM | 스터디 방 번호 | NUMBER | 18 | PK | NOT NULL | | 소속 스터디 모임 번호 | T_STUDY(STUDY_NUM) | 복합 PK, ON DELETE CASCADE |
| 2 | USER_NUM | 참여 회원 번호 | NUMBER | 9 | PK | NOT NULL | | 참여 회원 번호 | T_USER(USER_NUM) | 복합 PK, ON DELETE CASCADE |
| 3 | STUDY_JOIN_DATE | 참여일시 | DATE | | | NOT NULL | SYSDATE | 스터디 방 가입/참여 일시 | | |

| no | Index name | Index type | Unique | 구성 컬럼 |
|:--:|:---|:--:|:--:|:---|
| 1 | PK_T_STUDY_MEMBER_IDX | PK | Unique | STUDY_NUM, USER_NUM |
