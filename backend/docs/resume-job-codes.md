# 이력서별 지원 직군·직무

직군과 직무는 `T_USER`가 아닌 `T_RESUME`에서 관리한다. 회원가입 및 회원 응답 DTO에서 `occupationCode`, `jobCode`를 제거했다.

## DB 적용

- 신규 DB: `src/main/resources/sql_query/schema.sql` 사용.
- 기존 DB: 회원/이력서 쓰기를 중단하고 SQL*Plus 또는 SQLcl에서 `migrate_resume_job_codes.sql`을 한 번 실행한 뒤 새 애플리케이션 배포.
- 이관 스크립트는 원본 코드를 `T_USER_JOB_CODE_BACKUP`에 보존하고 회원의 모든 이력서에 복사한 후 검증 성공 시 회원 컬럼을 삭제한다. 직군·직무가 불일치한 기존 데이터는 직무의 소속 직군으로 정규화하며 원본은 백업에 남긴다.
- 이력서가 없는 회원은 백업에만 보존한다. 기존 값이 없는 이력서는 NULL을 허용하며 다음 작성/수정 시 두 값을 선택해야 한다.
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

면접 시작 요청은 기존대로 `resumeNum`을 전달한다. 서버가 해당 회원의 선택한 이력서를 조회하고 `T_OCCUPATION`, `T_JOB`을 JOIN하여 지원 분야를 결정한다. 별도 회원 직군 조회는 제거했다.

서류 질문, 4·5번 후속 질문, 최종 평가의 모든 AI 요청에 `occupationCode`, `occupation`, `jobCode`, `job`을 전달한다. `occupation`과 `job`은 DB에서 조회한 이름이다. 서류 텍스트와 공통 프롬프트도 선택한 이력서의 지원 분야를 반영한다. 기존 이력서에 코드가 없으면 서류에 명시된 사실만 사용하며 분야를 임의 추정하지 않는다.
