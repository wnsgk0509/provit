# 🚀 Provit (프로빗) 팀 프로젝트

> **Spring Framework (MVC) & React 19 기반 풀스택 팀 프로젝트**

---

## 🛠 기술 스택 (Tech Stack)

| 구분                | 기술 / 도구            | 상세 버전 및 스펙                             |
| :------------------ | :--------------------- | :-------------------------------------------- |
| **Backend**         | Spring Framework (MVC) | **5.3.39** (JDK 17 호환)                      |
| **WAS**             | Apache Tomcat          | **9.0.x** 권장                                |
| **Java**            | OpenJDK / Oracle JDK   | **JDK 17**                                    |
| **Database**        | Oracle Database        | **Oracle 19c** (ojdbc8, MyBatis, HikariCP)    |
| **Backend IDE**     | Eclipse / STS          | Dynamic Web Module 4.0, Maven                 |
| **Frontend**        | React 19 + Vite        | JavaScript, Axios, React Router, Lucide Icons |
| **Frontend IDE**    | Visual Studio Code     | ESLint, Prettier                              |
| **협업 & 형상관리** | Git / GitHub           | Git-Flow (PR & Code Review 기반)              |

---

## 📋 사전 필수 설치 환경

프로젝트를 시작하기 전에 아래 프로그램들이 설치되어 있어야 합니다:

1. **JDK 17** (환경 변수 `JAVA_HOME` 등록 확인)
2. **Node.js LTS (v20.x 이상 권장)**
3. **Oracle 19c** (DB 인스턴스 실행 및 계정 생성)
4. **Eclipse IDE for Enterprise Java and Web Developers** (또는 STS)
5. **Apache Tomcat 9.0.x**
6. **Visual Studio Code**

---

## 💻 초기 로컬 개발 환경 세팅 가이드

### 1. 저장소 Clone 및 브랜치 확인

```bash
git clone <저장소 URL>
cd provit
git checkout develop
```

---

### 2. 백엔드 (Backend - Eclipse) 세팅

> ⚠️ **주의**: `.project`, `.classpath`는 Git에 포함되지 않습니다. 반드시 **Maven Project**로 가져와야 라이브러리와 JDK 17이 자동 세팅됩니다.

1. **프로젝트 Import**:
   - 이클립스 실행 → `File` → `Import...` 클릭
   - `Maven` → `Existing Maven Projects` 선택 후 `Next`
   - `Root Directory`에서 `provit/backend` 폴더 선택 후 `Finish`
2. **DB 및 외부 API 설정 파일 생성 (필독)**:
   - 최상위에 있는 **`database 및 api properties.txt`** 파일을 열어 내용을 확인합니다.
   - `backend/src/main/resources/` 위치에 아래 2개 파일을 생성하고 본인의 정보를 입력합니다:
     - **`database.properties`**: Oracle 19c 계정 및 접속 URL
     - **`api.properties`**: OpenAI, 채용공고, 운세 등 외부 API 키
       _(두 파일 모두 `.gitignore`에 등록되어 있어 Git에 올라가지 않으므로 반드시 직접 생성해야 합니다)_
3. **톰캣 9 서버 연동 및 Context Path 설정 (매우 중요)**:
   - Eclipse 하단 `Servers` 탭 → `New` → `Server` → `Apache Tomcat v9.0 Server` 추가
   - 추가한 서버에 `backend` 프로젝트를 Add
   - 생성된 Tomcat 서버를 더블클릭하여 설정 창 열기:
     - 좌측 하단 **`Modules`** 탭 클릭
     - `backend` 모듈 선택 후 `Edit` 클릭
     - **Path를 `/backend`가 아니라 `/` (슬래시 하나)로 변경** 후 저장 (`Ctrl + S`)
       _(Path를 `/`로 설정해야 프론트엔드의 `/api` 요청을 정상 수신합니다)_
4. **서버 시작**:
   - Tomcat 서버 우클릭 → `Start`
   - 브라우저에서 `http://localhost:8080/api/health` 접속 시 JSON 응답(`"status": "UP"`)이 나오면 성공!

---

### 3. 프론트엔드 (Frontend - VSCode) 세팅

1. **VSCode로 열기**:
   - VSCode에서 `provit/frontend` 폴더를 엽니다.
   - 우측 하단에 권장 확장 프로그램(ESLint, Prettier) 알림이 뜨면 **[Install All]**을 클릭합니다.
2. **패키지 설치**:
   ```bash
   cd frontend
   npm install
   ```
3. **개발 서버 실행**:

   ```bash
   npm run dev
   ```

   - 브라우저에서 `http://localhost:5173` 접속
   - 화면 중앙의 **[서버 상태 확인 (/api/health)]** 버튼을 눌러 백엔드와의 통신을 테스트합니다.

---

## 🌿 Git 브랜치 전략 및 PR 협업 규칙

본 프로젝트는 **기술팀장의 코드 리뷰 및 승인(Approve)을 거치는 PR(Pull Request) 방식**으로 진행됩니다.

```
main (최종 배포용)
  ↑
develop (통합 개발 브랜치)
  ↑ (PR & 팀장 승인 후 Merge)
feature/be-xxx, feature/fe-xxx (개인 작업 브랜치)
```

### 1. 작업 브랜치 생성 규칙

- 항상 최신 `develop` 브랜치에서 분기합니다:
  ```bash
  git checkout develop
  git pull origin develop
  git checkout -b feature/be-login     # 백엔드 작업 시
  git checkout -b feature/fe-login     # 프론트엔드 작업 시
  ```
- **브랜치 네이밍 규칙**:
  - 백엔드 기능: `feature/be-기능명` (예: `feature/be-member-api`)
  - 프론트엔드 기능: `feature/fe-기능명` (예: `feature/fe-header`)
  - 버그 수정: `fix/be-기능명`, `fix/fe-기능명`

### 2. 커밋 메시지 컨벤션

```
feat: 새로운 기능 추가
fix: 버그 수정
refactor: 코드 리팩토링 (기능 변경 없음)
style: 코드 포맷팅, 세미콜론 누락 등
docs: 문서 수정 (README 등)
chore: 빌드 업무 수정, 패키지 매니저 수정
```

_(예: `feat: 회원가입 아이디 중복체크 API 구현`)_

### 3. PR(Pull Request) 생성 및 머지 절차

1. 본인 작업 브랜치로 커밋 후 원격 저장소에 push:
   ```bash
   git push origin feature/be-login
   ```
2. GitHub 저장소에서 **`develop` 브랜치를 대상(base)으로 PR 생성**
3. PR 템플릿 양식에 맞춰 작업 내용, 테스트 결과
4. **기술팀장이 코드 리뷰 후 승인(Approve) 및 Merge 진행**
5. 머지 완료 후 로컬 `develop` 브랜치 동기화:
   ```bash
   git checkout develop
   git pull origin develop
   ```

> ⚠️ **절대 `main`이나 `develop` 브랜치에 직접 push하지 마세요!**
