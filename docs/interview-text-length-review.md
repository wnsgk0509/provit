# 모의면접 문자열 길이 수정 결과

사용자의 범위 제한에 따라 모의면접만 수정했다. 작업 중 추가했던 회원·서류·커뮤니티·댓글·스터디 코드와 해당 스키마 변경은 모두 원복했다. 아래 내용은 `T_INTERVIEW_HISTORY`, `T_INTERVIEW_RESULT` 및 모의면접 코드에만 적용된다.

## 원인과 확인된 상태

현재 연결된 Oracle DB의 문자셋은 `AL32UTF8`, `NLS_LENGTH_SEMANTICS`는 `BYTE`다. 면접의 문자열 컬럼 14개는 실제 DB에서도 모두 BYTE 기준이었다.

AI 평가 문구는 최대 250자이지만 DB의 각 평가 문구 컬럼은 500 BYTE여서 한글 250자(750 BYTE)를 저장할 수 없었다. Service는 500자까지 허용하여 AI의 250자 제한과도 달랐다.

질문은 AI/프롬프트에서 120자, Service에서는 1,000자를 허용했다. 답변의 프론트와 Service 제한은 모두 1,000자다. 기존 질문·답변 컬럼은 현재 정상 생성/입력 제한 내 한글을 저장할 수 있었지만, DB와 애플리케이션의 길이 단위를 명시적으로 통일하기 위해 실제 계약에 맞췄다.

길이 계산은 기존 Java `String.length()`, JavaScript `.length`, 브라우저 `maxLength`의 UTF-16 단위를 유지했다. 일반 한글·영문·숫자·기호는 1, 일부 이모지는 2로 센다. Oracle CHAR 용량은 이 입력 범위의 전체 문자열을 수용한다.

## 컬럼별 정의와 기존 데이터 최대 길이

2026-09-28 기존 DB에서 문자열 자체를 출력하지 않고 `MAX(LENGTH(...))`, `MAX(LENGTHB(...))`만 조회했다.

| 테이블 | 컬럼 | 변경 전: 실제 BYTE 기준 | 변경 후 | 기존 최대 문자 수 |
| --- | --- | --- | --- | --- |
| T_INTERVIEW_HISTORY | QUESTION1 | VARCHAR2(1000 BYTE) | VARCHAR2(120 CHAR) | 63 |
| T_INTERVIEW_HISTORY | QUESTION2 | VARCHAR2(1000 BYTE) | VARCHAR2(120 CHAR) | 66 |
| T_INTERVIEW_HISTORY | QUESTION3 | VARCHAR2(1000 BYTE) | VARCHAR2(120 CHAR) | 75 |
| T_INTERVIEW_HISTORY | QUESTION4 | VARCHAR2(1000 BYTE) | VARCHAR2(120 CHAR) | 84 |
| T_INTERVIEW_HISTORY | QUESTION5 | VARCHAR2(1000 BYTE) | VARCHAR2(120 CHAR) | 56 |
| T_INTERVIEW_HISTORY | ANSWER1 | VARCHAR2(3000 BYTE) | VARCHAR2(1000 CHAR) | 58 |
| T_INTERVIEW_HISTORY | ANSWER2 | VARCHAR2(3000 BYTE) | VARCHAR2(1000 CHAR) | 28 |
| T_INTERVIEW_HISTORY | ANSWER3 | VARCHAR2(3000 BYTE) | VARCHAR2(1000 CHAR) | 32 |
| T_INTERVIEW_HISTORY | ANSWER4 | VARCHAR2(3000 BYTE) | VARCHAR2(1000 CHAR) | 127 |
| T_INTERVIEW_HISTORY | ANSWER5 | VARCHAR2(3000 BYTE) | VARCHAR2(1000 CHAR) | 103 |
| T_INTERVIEW_RESULT | STRENGTH | VARCHAR2(500 BYTE) | VARCHAR2(250 CHAR) | 131 |
| T_INTERVIEW_RESULT | WEAKNESS | VARCHAR2(500 BYTE) | VARCHAR2(250 CHAR) | 127 |
| T_INTERVIEW_RESULT | PREVIOUS_COMPARISON | VARCHAR2(500 BYTE) | VARCHAR2(250 CHAR) | 11 |
| T_INTERVIEW_RESULT | IMPROVEMENT_POINT | VARCHAR2(500 BYTE) | VARCHAR2(250 CHAR) | 116 |

현재 데이터는 모두 새 제한 이내다. 다른 PC/운영 DB의 데이터는 다를 수 있으므로 마이그레이션에서 14개 컬럼을 다시 검사한다. 하나라도 새 문자 수 제한을 넘으면 어떤 ALTER도 실행하기 전에 오류로 중단한다. 문자열 절단·삭제·덮어쓰기는 하지 않는다.

## 실제 DB 적용 SQL

파일: `backend/src/main/resources/sql_query/normalize_interview_text_lengths.sql`

DB 백업과 면접 쓰기 중단 후 SQL Developer의 스크립트 실행(F5) 또는 SQL*Plus에서 실행한다. 파일의 사전 검사를 포함한 전체 스크립트를 사용한다. Oracle DDL은 자동 커밋이므로 일반 DML처럼 일괄 롤백할 수 없다.

사전 검사 통과 후 실행되는 ALTER는 아래와 같다.

```sql
ALTER TABLE T_INTERVIEW_HISTORY MODIFY (
    QUESTION1 VARCHAR2(120 CHAR), ANSWER1 VARCHAR2(1000 CHAR),
    QUESTION2 VARCHAR2(120 CHAR), ANSWER2 VARCHAR2(1000 CHAR),
    QUESTION3 VARCHAR2(120 CHAR), ANSWER3 VARCHAR2(1000 CHAR),
    QUESTION4 VARCHAR2(120 CHAR), ANSWER4 VARCHAR2(1000 CHAR),
    QUESTION5 VARCHAR2(120 CHAR), ANSWER5 VARCHAR2(1000 CHAR)
);

ALTER TABLE T_INTERVIEW_RESULT MODIFY (
    STRENGTH VARCHAR2(250 CHAR), WEAKNESS VARCHAR2(250 CHAR),
    PREVIOUS_COMPARISON VARCHAR2(250 CHAR), IMPROVEMENT_POINT VARCHAR2(250 CHAR)
);
```

스크립트는 재실행할 수 있으며, 끝에서 `CHAR_USED`, `CHAR_LENGTH`, `DATA_LENGTH`를 조회하여 반영 결과를 확인한다. 전역/세션 `NLS_LENGTH_SEMANTICS`, `MAX_STRING_SIZE` 설정을 변경하지 않는다.

실제 프로젝트 테이블에는 이 SQL을 아직 적용하지 않았다. 현재 DB에는 BYTE 정의가 남아 있으므로, DB에 SQL 적용 후 수정한 서버를 재배포해야 실제 서비스의 평가 저장 오류가 해결된다.

## Java·AI·프론트·Mapper

| 항목 | 프론트 | Service | AI 스키마/프롬프트 | DB |
| --- | --- | --- | --- | --- |
| 질문 | 생성 질문을 표시 | 120자 | 120자 | 120 CHAR |
| 답변 | maxLength=1000, .length 표시 | 1000자 | 사용자 답변을 전달 | 1000 CHAR |
| 평가 문구 4개 | 생성 결과를 표시 | 250자 | 250자 | 250 CHAR |

`InterviewTextLimits`에 질문·답변·평가 제한을 모아 Service와 AI JSON 스키마가 함께 사용한다. AI 출력 길이는 기존 250자를 유지했다. Service의 평가 최대 길이는 500→250자, 질문 최대 길이는 1000→120자로 맞췄다. 초과 AI 출력은 저장 전에 거절한다.

프론트 답변 제한과 프롬프트 문구는 이미 해당 값과 일치하여 수정하지 않았다. DTO는 기존 String, MyBatis는 기존 VARCHAR 문자열 바인딩을 유지한다. 데이터 타입이 VARCHAR2로 유지되므로 별도 CLOB TypeHandler나 DTO 변경은 필요 없다. 실제 Oracle 테스트에서도 현재 면접 Mapper의 SQL과 `DefaultParameterHandler`를 사용해 삽입했다.

## VARCHAR2 CHAR를 선택한 이유

면접 질문·답변·평가 문구는 현재 각각 120/1000/250자로 제한되어 있다. 최대 1,000자의 CHAR 정의는 AL32UTF8의 문자당 최대 4 BYTE를 고려해도 4,000 BYTE 이내이므로 이번 면접 필드에는 CLOB 전환이 필요하지 않다.

Oracle은 CHAR를 지정해도 `MAX_STRING_SIZE=STANDARD`의 VARCHAR2 바이트 상한 4,000을 적용한다. 이 사실을 고려하여 이번에 변경하는 면접 컬럼을 모두 상한 이내로 정했다. [Oracle 19c 데이터 타입 문서](https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html), [데이터 타입 크기 제한](https://docs.oracle.com/en/database/oracle/oracle-database/19/refrn/datatype-limits.html)

향후 면접 답변이나 상세 평가를 2,000자 이상으로 늘리는 별도 요구가 생기면 CLOB을 검토해야 한다. 현재 제한을 넘어서는 확장이나 다른 도메인의 컬럼 변경은 하지 않았다.

## 저장 실패와 평가 재사용

일시적인 저장 실패에서는 기존 평가와 마지막 답변을 보관하고 같은 답변의 저장만 재시도한다. 기존 동작과 AI 4회 호출 제한을 유지했다.

`DataIntegrityViolationException` 또는 중첩 SQLException의 Oracle 오류 코드 `12899`는 다시 저장해도 해결되지 않는 오류로 판단한다. 이 경우 세션을 실패 상태로 전환하고 관리자 문의 메시지를 반환한다. 이후 같은 세션에서 저장·평가 생성을 반복하지 않는다. DB 스키마를 고치는 대신 AI를 재호출하거나 응답을 자르는 방식은 사용하지 않았다.

## 테스트 결과

백엔드 79개, 프론트 면접 API·진행 상태·평가 점수 테스트 12개 모두 통과했다.

실제 Oracle에 임의 이름의 별도 테스트 테이블만 생성하고 현재 Mapper로 검증한 뒤 테스트 테이블을 삭제했다. 기존 T_INTERVIEW_* 테이블, 데이터, 시퀀스는 변경하지 않았다.

| 케이스 | 결과 |
| --- | --- |
| 변경 전 500 BYTE에 평가 문구 한글 250자 | ORA-12899 재현 |
| 변경 후 평가 문구 한글 250자 | 4개 컬럼 모두 저장·원문 조회 성공 |
| 변경 후 평가 문구 영문 250자 | 4개 컬럼 모두 성공 |
| 변경 후 평가 문구 한글/영문/숫자/기호 혼합 250자 | 4개 컬럼 모두 성공 |
| 일부 이모지를 포함한 UTF-16 길이 250의 평가 문구 | 4개 컬럼 모두 성공 |
| 질문 한글 120자 | 5개 컬럼 모두 성공 |
| 답변 한글 1000자 | 5개 컬럼 모두 성공 |
| 이모지 포함 혼합 답변 UTF-16 길이 1000 | 5개 컬럼 모두 성공 |
| Service에 질문 121자/답변 1001자/평가 251자 | 애플리케이션에서 차단 |
| 기존 영문 평가 251자가 있는 DB에 마이그레이션 실행 | ALTER 전에 중단, 14개 BYTE 정의와 데이터 보존 |
| 정상 기존 평가를 포함한 마이그레이션 | 기존 문자열 보존 |
| 마이그레이션 재실행 | 성공 |
| 영구 DB 저장 오류 후 같은 세션 재제출 | 저장 시도 1회, AI 평가 1회 유지 |
| 일시 저장 실패 후 재제출 | 캐시된 평가 재사용하여 정상 저장 |

Oracle 테스트는 기본 실행에서 건너뛰며 `-Dprovit.oracle.lengthTests=true`를 지정하면 실행한다. 이번 검증에서는 해당 옵션을 활성화하여 3개 Oracle 테스트까지 모두 실행했다.

## 수정 파일

| 파일 | 변경 |
| --- | --- |
| backend/src/main/resources/sql_query/schema.sql | 면접 두 테이블의 14개 VARCHAR2 컬럼만 CHAR 정의로 변경 |
| backend/src/main/resources/sql_query/normalize_interview_text_lengths.sql | 데이터 최대 길이 사전 검사, 면접 컬럼 ALTER, 반영 결과 조회 |
| backend/src/main/java/com/provit/service/interview/InterviewTextLimits.java | 면접 전용 길이 상수 |
| backend/src/main/java/com/provit/service/interview/impl/InterviewServiceImpl.java | AI와 같은 길이 검증, 영구 저장 오류의 반복 제출 방어 |
| backend/src/main/java/com/provit/service/interview/generator/OpenAiInterviewGenerator.java | Service와 같은 상수로 JSON 스키마 길이 지정 |
| backend/src/test/java/com/provit/service/interview/InterviewFourCallFlowTest.java | 최대 길이·초과 길이·영구 저장 오류 테스트 추가 |
| backend/src/test/java/com/provit/service/interview/OracleInterviewTextLengthTest.java | 실제 Oracle의 BYTE 오류 재현, 마이그레이션, 문자열 보존·저장 검증 |
| docs/interview-text-length-review.md | 변경 범위, 컬럼 정의, 적용 SQL, 검증 결과 정리 |

제목 한글 200자와 경력 주요 업무 한글 2,000자 등의 서류 영역은 사용자의 모의면접 범위 제한에 따라 원복했으며 이번 수정/테스트 대상에서 제외했다.
