# 대표이력서

## DB 구조

`T_USER.MAIN_RESUME_NUM`은 `T_RESUME.RESUME_NUM`을 참조하는 `NUMBER(18)` 컬럼이다. 사용자당 대표이력서는 최대 하나이며, `NULL`은 미지정을 의미한다. 기존 회원과 신규 회원 모두 자동으로 대표이력서를 지정하지 않는다.

외래키 `FK_USER_MAIN_RESUME`은 존재하지 않는 이력서 번호를 거절한다. 대표이력서를 삭제하면 `ON DELETE SET NULL`에 따라 사용자의 대표이력서 번호가 `NULL`로 변경된다. 외래키 조회를 위한 `IDX_USER_MAIN_RESUME_NUM` 인덱스를 함께 추가한다.

이 외래키는 이력서의 존재만 보장한다. 후속 지정 API에서는 로그인 사용자의 이력서인지 검증하고, 조회 JOIN에서도 `T_USER.USER_NUM = T_RESUME.USER_NUM` 조건을 적용해야 한다.

## 적용

- 신규 DB: [schema.sql](../src/main/resources/sql_query/schema.sql)을 사용한다. 사용자 테이블에 컬럼을 만들고, 이력서 테이블 생성 후 외래키와 인덱스를 추가한다.
- 기존 DB: SQL*Plus 또는 SQLcl에서 [migrate_user_main_resume.sql](../src/main/resources/sql_query/migrate_user_main_resume.sql)을 실행한다. 같은 스키마에 `T_USER`, `T_RESUME`이 먼저 존재해야 한다. 없는 컬럼·외래키·인덱스만 추가하므로 재실행할 수 있다.
- Oracle DDL은 자동 커밋된다. 오류가 발생하면 원인을 해결한 뒤 다시 실행한다.

이번 단계는 DB 구조와 적용 스크립트만 추가한다. 대표이력서 지정 API, 회원 DTO/Mapper 반영, 프론트 UI, 공고검색용 조회 Service는 후속 작업이다. 사용자 직군·직무 컬럼과 기존 데이터는 유지한다.
