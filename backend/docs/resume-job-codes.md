# 이력서별 지원 직군·직무

`T_USER`에는 회원 기본 직군·직무를, `T_RESUME`에는 각 이력서의 지원 직군·직무를 관리한다. 두 테이블의 값은 서로 독립적이며 자동으로 동기화하지 않는다.
회원가입 요청의 `occupationCode`, `jobCode`는 선택값이며 누락·공백은 NULL로 저장한다. 회원 DTO, 회원 조회 mapper와 회원가입 INSERT에 두 코드를 포함하고 회원가입·로그인·프로필 조회 응답에도 반환한다. 현재 회원가입 화면은 코드를 보내지 않아도 그대로 가입할 수 있다. 이력서 작성·수정의 필수 코드 검증은 유지한다.

## DB 적용

- 신규 DB: `src/main/resources/sql_query/schema.sql` 사용.
- 회원·이력서에 두 코드 컬럼이 모두 있는 DB: 스키마 변경 없이 애플리케이션 배포.
- 과거 이관으로 회원 코드 컬럼이 삭제된 DB: SQL*Plus 또는 SQLcl에서 `restore_user_job_codes.sql`을 실행한 뒤 배포. 없는 컬럼만 추가하며 회원·이력서 데이터는 수정하지 않는다. 삭제됐던 회원 값은 자동 복원하지 않고 기존 `T_USER_JOB_CODE_BACKUP`에 보존한다.
- 이력서 코드 컬럼을 아직 추가하지 않은 기존 DB: 회원/이력서 쓰기를 중단하고 `migrate_resume_job_codes.sql`을 한 번 실행한 뒤 배포.
- 이관 스크립트는 원본 코드를 `T_USER_JOB_CODE_BACKUP`에 보존하고 회원의 모든 이력서에 복사한 후 검증한다. 회원 컬럼은 유지한다. 직군·직무가 불일치한 이력서 데이터는 직무의 소속 직군으로 정규화하며 회원 원본과 백업은 유지한다.
- 이력서가 없는 회원도 회원 코드와 백업을 보존한다. 기존 값이 없는 이력서는 NULL을 허용하며 다음 작성/수정 시 두 값을 선택해야 한다.
- Oracle DDL은 자동 커밋되므로 오류가 발생하면 완료된 단계를 확인하고 이어서 적용해야 한다. 전체 스크립트를 재실행하지 않는다. 백업은 자동 삭제하지 않는다.

## 작성·수정·조회 API

`GET /api/occupation`으로 1차 직군을 조회하고 `GET /api/occupation/{occupationCode}/jobs`로 해당 직군의 2차 직무를 조회한다. 직군을 변경하면 직무 선택을 초기화한다.

기존 이력서 생성/수정 요청의 `resume` 객체에 문자열 코드를 전달한다.

```json
{
  "resume": {
    "resumeTitle": "백엔드 지원 이력서",
    "occupationCode": "2",
    "jobCode": "84",
    "highestLevel": "대학교",
    "educationCode": 3
  },
  "educationList": [],
  "careerList": [],
  "certificationList": []
}
```

서버는 두 코드가 존재하며 직무가 해당 직군에 속하는지 확인한다. 코드 누락 또는 잘못된 조합은 400 응답으로 거절한다. `occupationName`, `jobName`은 요청에서 신뢰하지 않고 DB에서 조회하며 저장/조회 응답의 `resume`에 코드와 이름을 포함한다.

## AI 면접 요청

면접 시작 요청은 기존대로 `resumeNum`을 전달한다. 서버가 해당 회원의 선택한 이력서를 조회하고 `T_OCCUPATION`, `T_JOB`을 JOIN하여 지원 분야를 결정한다. 회원 직군·직무 조회용 `UserJobPreferenceDTO`, `user_job_mapper.xml`, `InterviewDAO.selectUserJobPreferenceByUserNum`은 복구했지만 현재 AI 면접에서는 호출하지 않는다. AI 입력은 이력서의 지원 분야와 선택한 채용 공고를 사용한다.

서류 질문, 4·5번 후속 질문, 최종 평가의 모든 AI 요청에 `occupationCode`, `occupation`, `jobCode`, `job`을 전달한다. `occupation`과 `job`은 DB에서 조회한 이름이다. 서류 텍스트와 공통 프롬프트도 선택한 이력서의 지원 분야를 반영한다. 기존 이력서에 코드가 없으면 서류에 명시된 사실만 사용하며 분야를 임의 추정하지 않는다.
