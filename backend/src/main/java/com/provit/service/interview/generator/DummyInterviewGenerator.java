package com.provit.service.interview.generator;

import java.util.List;

import com.provit.dto.interview.InterviewQuestionAnswerDTO;
import com.provit.dto.interview.InterviewQuestionDTO;
import com.provit.dto.interview.InterviewResultDTO;
import com.provit.dto.interview.LlmEvaluationRequestDTO;
import com.provit.dto.interview.LlmEvaluationResponseDTO;
import com.provit.dto.interview.LlmFollowUpRequestDTO;
import com.provit.dto.interview.LlmQuestionRequestDTO;
import com.provit.dto.interview.LlmQuestionResponseDTO;

public class DummyInterviewGenerator implements InterviewGenerator {

    @Override
    public LlmQuestionResponseDTO generateDocumentQuestions(LlmQuestionRequestDTO request) {
        LlmQuestionResponseDTO response = new LlmQuestionResponseDTO();
        response.setQuestions(List.of(
                documentQuestion(1, request),
                documentQuestion(2, request),
                documentQuestion(3, request)));
        return response;
    }

    private InterviewQuestionDTO documentQuestion(int questionOrder, LlmQuestionRequestDTO request) {
        String questionText;
        if (questionOrder == 1) {
            questionText = "자기소개서에서 지원 직무와 가장 관련 있는 경험과 본인의 역할을 설명해 주세요.";
        } else if (questionOrder == 2) {
            questionText = request.getContext().getPortfolio() == null
                    ? "자기소개서에 작성한 지원 동기를 본인의 강점과 연결해 설명해 주세요."
                    : "포트폴리오에서 가장 주도적으로 참여한 프로젝트와 본인이 담당한 역할을 설명해 주세요.";
        } else if (questionOrder == 3) {
            questionText = "지원 직무의 핵심 개념 하나를 선택해 그 원리를 설명해 주세요.";
        } else {
            throw new IllegalArgumentException("서류 질문 순서가 올바르지 않습니다.");
        }
        return createQuestion(questionOrder, "DOCUMENT", questionText);
    }

    @Override
    public InterviewQuestionDTO generateFollowUpQuestion(int questionOrder, LlmFollowUpRequestDTO request) {
        List<InterviewQuestionAnswerDTO> answers = request.getQuestionAnswers();
        String questionText;
        if (questionOrder == 4) {
            questionText = "지원 직무의 업무 결과가 목표에 미치지 못하는 상황이라면 원인을 어떻게 파악하시겠습니까?";
        } else if (questionOrder == 5) {
            var selected = answers.stream().filter(answer -> answer.getAnswer() != null
                    && !answer.getAnswer().isBlank()).findFirst();
            questionText = selected.map(answer -> answer.getQuestionOrder() + "번 답변에서 \""
                    + summarize(answer.getAnswer()) + "\"라고 설명한 근거는 무엇인가요?")
                    .orElse("앞선 문제 상황에서 원인을 파악하기 위해 가장 먼저 확인할 정보는 무엇인가요?");
        } else {
            throw new IllegalArgumentException("후속 질문 순서가 올바르지 않습니다.");
        }
        return createQuestion(questionOrder, "FOLLOW_UP", questionText);
    }

    @Override
    public LlmEvaluationResponseDTO evaluate(LlmEvaluationRequestDTO request) {
        LlmEvaluationResponseDTO response = new LlmEvaluationResponseDTO();
        response.setDocumentConsistencyScore(84);
        response.setProblemSolvingScore(79);
        response.setExpertiseScore(86);
        response.setLogicScore(80);
        response.setCommunicationScore(83);
        response.setTotalScore(82.4);
        response.setStrengths("프로젝트 경험을 구체적인 상황과 본인의 역할 중심으로 설명해 답변의 신뢰도가 높았습니다.");
        response.setWeaknesses("일부 답변에서 결론이 뒤에 제시되어 핵심 내용을 파악하는 데 시간이 걸렸습니다.");
        response.setComparison(createComparison(request.getPreviousResult()));
        response.setImprovements("답변을 결론, 근거, 실제 사례 순서로 구성하고 각 답변을 1분 30초 안에 마무리해 보세요.");
        return response;
    }

    private InterviewQuestionDTO createQuestion(int order, String type, String text) {
        InterviewQuestionDTO question = new InterviewQuestionDTO();
        question.setQuestionOrder(order);
        question.setQuestionType(type);
        question.setQuestionText(text);
        return question;
    }

    private String summarize(String answer) {
        if (answer == null || answer.isBlank()) {
            return "답변하지 못했다";
        }
        String normalized = answer.replaceAll("\\s+", " ").trim();
        return normalized.length() > 35 ? normalized.substring(0, 35) + "..." : normalized;
    }

    private String createComparison(InterviewResultDTO previousResult) {
        if (previousResult == null) {
            return "이전 면접 기록 없음";
        }
        double difference = 82.4 - previousResult.getTotalScore();
        if (difference > 0) {
            return String.format("이전 면접보다 총점이 %.1f점 상승했습니다.", difference);
        }
        if (difference < 0) {
            return String.format("이전 면접보다 총점이 %.1f점 하락했습니다.", Math.abs(difference));
        }
        return "이전 면접과 총점이 같습니다.";
    }
}
