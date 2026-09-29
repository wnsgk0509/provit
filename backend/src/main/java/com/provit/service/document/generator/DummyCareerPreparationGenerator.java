package com.provit.service.document.generator;

import java.io.IOException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.provit.dto.document.CareerPreparationDTO;
import com.provit.dto.document.DocumentReviewResultDTO.Document;

public class DummyCareerPreparationGenerator {
    private final ObjectMapper mapper = new ObjectMapper();

    public CareerPreparationDTO generate(List<Document> documents) {
        var result = new CareerPreparationDTO();
        var text = new StringBuilder();
        boolean portfolioSelected = false;
        boolean portfolioReadable = true;
        try {
            for (Document document : documents) {
                if (document.getDocumentTitle() != null) text.append('\n').append(document.getDocumentTitle());
                JsonNode snapshot = mapper.readTree(document.getSourceSnapshotJson());
                if ("resume".equals(document.getDocumentType())) {
                    JsonNode resume = snapshot.path("resume");
                    result.setOccupationCode(value(resume, "occupationCode"));
                    result.setOccupationName(value(resume, "occupationName"));
                    result.setJobCode(value(resume, "jobCode"));
                    result.setJobName(value(resume, "jobName"));
                    appendText(text, resume.path("motivation"));
                    appendText(text, snapshot.path("educationList"));
                    appendText(text, snapshot.path("careerList"));
                    appendText(text, snapshot.path("certificationList"));
                } else if ("cover-letter".equals(document.getDocumentType())) {
                    for (String field : List.of("growthProcess", "personalityStrengthsWeaknesses",
                            "problemSolvingExperience", "postJoiningAspiration")) {
                        appendText(text, snapshot.path(field));
                    }
                } else if ("portfolio".equals(document.getDocumentType())) {
                    portfolioSelected = true;
                    String pdfText = extractPdfText(document.getPdfSnapshot());
                    portfolioReadable = !pdfText.isBlank();
                    text.append('\n').append(pdfText);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("취업 준비 추천을 위한 서류 보관본을 읽지 못했습니다.", exception);
        }
        result.setCoverageNote(portfolioSelected
                ? portfolioReadable
                    ? "이력서·자기소개서와 PDF에서 추출한 텍스트의 키워드를 비교한 더미 추천입니다. 이미지 속 내용은 확인하지 않았습니다."
                    : "이력서·자기소개서의 키워드를 비교한 더미 추천입니다. 포트폴리오 텍스트를 읽지 못해 해당 PDF의 내용은 확인하지 않았습니다."
                : "이력서·자기소개서의 키워드를 비교한 더미 추천입니다.");
        if (result.getJobName() == null && result.getOccupationName() == null) {
            result.setSummary("이력서에 지원 직군·직무 정보가 없어 추천을 만들지 않았습니다. 이력서에서 지원 분야를 선택해 주세요.");
            return result;
        }
        String target = result.getJobName() == null ? result.getOccupationName() : result.getJobName();
        String normalizedText = normalize(text.toString());
        for (Candidate candidate : candidates(profile(result))) {
            if (candidate.keywords().stream().map(DummyCareerPreparationGenerator::normalize)
                    .anyMatch(normalizedText::contains)) continue;
            var recommendation = new CareerPreparationDTO.Recommendation();
            recommendation.setCategory(candidate.category());
            recommendation.setTitle(candidate.title());
            recommendation.setReason(target + " 준비에 활용할 수 있는 예시입니다. " + candidate.reason());
            recommendation.setAction(candidate.action());
            result.getRecommendations().add(recommendation);
        }
        result.setSummary(result.getRecommendations().isEmpty()
                ? target + "의 더미 추천 후보가 확인 가능한 서류 텍스트에 이미 언급되어 있어 추가 항목을 제시하지 않습니다."
                : target + "을 기준으로 확인 가능한 서류 텍스트에 언급되지 않은 준비 항목을 제안합니다. 실제 미보유 여부나 채용 필수 조건을 뜻하지 않습니다.");
        return result;
    }

    private String profile(CareerPreparationDTO result) {
        String job = normalize(result.getJobName() == null ? "" : result.getJobName());
        if (contains(job, "백엔드", "서버개발", "backend")) return "backend";
        if (contains(job, "프론트엔드", "frontend", "웹퍼블리셔")) return "frontend";
        if (contains(job, "데이터", "빅데이터", "data")) return "data";
        if (contains(job, "devops", "클라우드", "인프라")) return "infrastructure";
        return "general";
    }

    private List<Candidate> candidates(String profile) {
        Candidate qualification = new Candidate("qualification", "프로젝트 결과물과 기술 문서 공개",
                "직접 구현한 결과물과 판단 근거를 함께 정리하면 면접에서 경험을 설명하기 쉽습니다.",
                "직접 만든 결과물에 README, 실행 방법, 본인 역할과 기술 선택 이유를 작성해 공개 가능한 저장소나 포트폴리오로 정리해 보세요.",
                List.of("github", "gitlab", "readme", "기술 블로그", "결과물 공개"));
        return switch (profile) {
            case "backend" -> List.of(
                new Candidate("experience", "API 자동화 테스트 경험",
                    "정상·오류 상황을 검증하는 경험으로 API 안정성을 설명할 수 있습니다.",
                    "작은 API에 단위·통합 테스트를 작성하고 검증한 실패 사례와 개선 내용을 기록해 보세요.",
                    List.of("junit", "테스트 자동화", "자동화 테스트", "단위 테스트", "통합 테스트")),
                new Candidate("skill", "Docker 기반 실행 환경 구성",
                    "서버 실행 환경을 재현하는 과정으로 배포와 운영에 대한 이해를 보여줄 수 있습니다.",
                    "직접 구현한 서버를 Docker로 실행하고 환경 변수, 네트워크, 실행 절차를 문서화해 보세요.",
                    List.of("docker", "도커", "컨테이너")),
                new Candidate("certification", "SQLD 학습·자격 검토",
                    "데이터 모델과 SQL 기초 지식을 정리할 때 활용할 수 있는 선택적 자격입니다.",
                    "지원 공고의 우대 조건과 현재 학습 목표를 확인한 뒤 SQLD 학습·응시를 검토하세요. 응시 일정·조건은 공식 안내에서 확인하세요.",
                    List.of("sqld", "sql 개발자", "sql개발자")), qualification);
            case "frontend" -> List.of(
                new Candidate("experience", "웹 접근성 점검 경험",
                    "다양한 사용자를 고려한 UI 구현과 개선 경험을 설명할 수 있습니다.",
                    "직접 만든 화면의 키보드 탐색, 입력 레이블과 대비를 점검하고 수정 전후를 기록해 보세요.",
                    List.of("접근성", "accessibility", "wcag")),
                new Candidate("skill", "TypeScript 기반 UI 구현",
                    "데이터와 컴포넌트의 타입을 정리하는 과정으로 UI 코드 관리 역량을 보여줄 수 있습니다.",
                    "작은 화면을 TypeScript로 구현하고 API 응답과 컴포넌트 속성의 타입을 정의해 보세요.",
                    List.of("typescript", "타입스크립트")),
                new Candidate("certification", "정보처리기사 학습·자격 검토",
                    "소프트웨어 기초 지식을 정리하는 선택지이며 모든 프론트엔드 채용의 필수 조건은 아닙니다.",
                    "지원 공고의 우대 자격과 본인의 응시 자격을 공식 안내에서 확인한 뒤 실무 학습과 비교해 준비 여부를 결정하세요.",
                    List.of("정보처리기사")), qualification);
            case "data" -> List.of(
                new Candidate("experience", "데이터 품질 검증 프로젝트",
                    "결측·중복·이상값을 확인하고 처리 기준을 설명하는 경험을 만들 수 있습니다.",
                    "공개 데이터로 품질 검증 규칙과 처리 절차를 만들고 결과의 한계를 함께 기록해 보세요.",
                    List.of("데이터 품질", "data quality", "결측", "이상값")),
                new Candidate("skill", "SQL 기반 데이터 조회·집계",
                    "조회·조인·집계 과정을 직접 작성하면 데이터 처리의 기초 역량을 설명할 수 있습니다.",
                    "작은 데이터셋에서 JOIN과 집계 쿼리를 작성하고 결과 검증과 쿼리 설계 이유를 정리해 보세요.",
                    List.of("sql", "데이터 조회", "데이터 집계")),
                new Candidate("certification", "SQLD 학습·자격 검토",
                    "데이터 모델과 SQL 개념을 정리하는 선택적 학습 목표로 활용할 수 있습니다.",
                    "지원 공고와 학습 목표에 맞는지 확인하고 SQLD 학습·응시를 검토하세요. 응시 일정·조건은 공식 안내에서 확인하세요.",
                    List.of("sqld", "sql 개발자", "sql개발자")), qualification);
            case "infrastructure" -> List.of(
                new Candidate("experience", "CI/CD 배포 자동화 경험",
                    "변경 사항을 검사하고 배포하는 절차를 직접 구성한 경험을 보여줄 수 있습니다.",
                    "작은 서비스에 테스트·빌드·배포 파이프라인을 구성하고 실패 시 대응 절차를 기록해 보세요.",
                    List.of("ci/cd", "cicd", "배포 자동화", "파이프라인")),
                new Candidate("skill", "모니터링과 장애 대응 기초",
                    "서비스 상태를 관찰하고 문제를 확인하는 과정을 설명할 수 있습니다.",
                    "작은 서비스의 로그·지표·알림을 구성하고 가상의 장애 상황에서 확인한 대응 과정을 문서화해 보세요.",
                    List.of("모니터링", "monitoring", "prometheus", "grafana")),
                new Candidate("certification", "클라우드 관련 자격 검토",
                    "지원 분야에서 사용하는 클라우드의 기초 개념을 정리하는 선택지입니다.",
                    "지원 공고에서 사용하는 클라우드를 확인하고 해당 제공자의 공식 자격 안내와 학습 범위를 비교해 준비 여부를 결정하세요.",
                    List.of("클라우드 자격", "aws certified", "azure fundamentals", "cloud certification")), qualification);
            default -> List.of(
                new Candidate("experience", "지원 직무의 실무 과제 경험",
                    "지원 직무에서 다루는 문제를 직접 해결한 결과물로 준비 과정을 보여줄 수 있습니다.",
                    "지원 공고에서 반복되는 업무 하나를 골라 작은 실무 과제를 수행하고 본인 역할·과정·결과를 정리해 보세요.",
                    List.of("실무 과제", "실무 프로젝트")),
                new Candidate("skill", "지원 직무의 업무 도구 학습",
                    "실제 업무에 사용하는 도구를 익히고 활용 결과를 준비할 수 있습니다.",
                    "지원 공고에서 공통으로 요구하는 도구를 확인해 하나를 학습하고 직접 활용한 결과물을 만들어 보세요.",
                    List.of("업무 도구", "직무 도구")),
                new Candidate("certification", "공고의 우대 자격 검토",
                    "해당 직무의 실제 우대 조건을 확인한 뒤 필요한 자격을 선택할 수 있습니다.",
                    "관심 공고의 우대 자격을 비교하고 공식 응시 조건·학습 범위를 확인해 준비할 자격을 선택하세요.",
                    List.of("우대 자격", "직무 자격")),
                new Candidate("qualification", "직무 관련 결과물 정리",
                    "직무 준비 과정과 결과를 확인 가능한 자료로 제시할 수 있습니다.",
                    "실무 과제·교육에서 만든 자료를 본인 기여와 함께 정리하고 공개 가능한 범위의 결과물을 준비해 보세요.",
                    List.of("결과물 정리", "성과 자료")));
        };
    }

    private String extractPdfText(byte[] bytes) {
        if (bytes == null) return "";
        try (var pdf = Loader.loadPDF(bytes)) {
            if (pdf.isEncrypted()) return "";
            return new PDFTextStripper().getText(pdf);
        } catch (IOException exception) {
            return "";
        }
    }

    private void appendText(StringBuilder text, JsonNode node) {
        if (node.isTextual()) text.append('\n').append(node.asText());
        else if (node.isContainerNode()) node.forEach(child -> appendText(text, child));
    }

    private String value(JsonNode node, String name) {
        String value = node.path(name).asText("").strip();
        return value.isEmpty() ? null : value;
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}+#]", "");
    }

    private boolean contains(String text, String... keywords) {
        for (String keyword : keywords) if (text.contains(keyword)) return true;
        return false;
    }

    private record Candidate(String category, String title, String reason, String action, List<String> keywords) { }
}
