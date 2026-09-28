package com.provit.service.interview.generator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.provit.service.interview.InterviewTextLimits;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.provit.dto.interview.*;
import com.provit.service.interview.InterviewProcessingException;

@Component
public class OpenAiInterviewGenerator implements InterviewGenerator {
    private static final int MAX_DOCUMENT_CHARACTERS = 9000;
    private final ObjectMapper mapper = new ObjectMapper();
    private final OpenAiInterviewClient client;
    private final String common = prompt("common");
    private final String questionRules = prompt("question-rules");
    private final String answerFormat = prompt("answer-format");
    private final String document = prompt("document");
    private final String followUp4 = prompt("follow-up-4");
    private final String followUp5 = prompt("follow-up-5");
    private final String evaluation = prompt("evaluation");
    private final String easyDifficulty = prompt("difficulty-easy");
    private final String normalDifficulty = prompt("difficulty-normal");
    private final String hardDifficulty = prompt("difficulty-hard");

    public OpenAiInterviewGenerator(OpenAiInterviewClient client) { this.client = client; }

    @Override
    public LlmQuestionResponseDTO generateDocumentQuestions(LlmQuestionRequestDTO request) {
        String text = request.getContext().getDocumentText();
        if (text == null || text.isBlank()) throw new IllegalArgumentException("면접에 사용할 서류 내용이 없습니다.");
        if (text.length() > MAX_DOCUMENT_CHARACTERS) {
            throw new IllegalArgumentException("이력서·자기소개서와 서류 제목의 합계가 9,000자를 초과합니다. 서류 분량을 줄여 주세요.");
        }
        boolean hasPortfolio = request.getContext().getPortfolio() != null;
        byte[] portfolioPdf = request.getContext().getPortfolioPdf();
        if (hasPortfolio && (portfolioPdf == null || portfolioPdf.length == 0)) {
            throw new IllegalArgumentException("면접에 사용할 포트폴리오 PDF가 없습니다.");
        }
        ObjectNode input = questionSettings(request.getContext(), request.getInterviewDifficulty());
        input.put("documentText", text);
        input.put("hasPortfolio", hasPortfolio);
        ObjectNode schema = object();
        ObjectNode questions = mapper.createObjectNode().put("type", "array").put("minItems", 3).put("maxItems", 3);
        questions.set("items", questionTextSchema());
        property(schema, "questions", questions);
        JsonNode generated = client.generate("document", common + "\n" + questionRules + "\n" + document,
                input, schema, 900, hasPortfolio ? portfolioPdf : null);
        for (int index = 0; index < 3; index++) {
            boolean portfolioQuestion = generated.get("questions").get(index).asText().contains("포트폴리오");
            if (portfolioQuestion != (hasPortfolio && index == 1)) {
                throw new InterviewProcessingException(
                        "포트폴리오 관련 질문 구성이 올바르지 않습니다. 새 면접을 시작해 주세요.", true, false);
            }
        }
        List<InterviewQuestionDTO> result = new ArrayList<>();
        for (int index = 0; index < 3; index++) {
            result.add(question(index + 1, "DOCUMENT", generated.get("questions").get(index).asText()));
        }
        LlmQuestionResponseDTO response = new LlmQuestionResponseDTO();
        response.setQuestions(result);
        return response;
    }

    @Override
    public InterviewQuestionDTO generateFollowUpQuestion(int order, LlmFollowUpRequestDTO request) {
        if (order != 4 && order != 5) throw new IllegalArgumentException("후속 질문 순서가 올바르지 않습니다.");
        ObjectNode input = questionSettings(request.getContext(), request.getInterviewDifficulty());
        input.set("questionAnswers", answers(request.getQuestionAnswers()));
        ObjectNode schema = object();
        property(schema, "questionText", questionTextSchema());
        JsonNode generated = client.generate("follow_up_" + order,
                common + "\n" + questionRules + "\n" + answerFormat + "\n" + (order == 4 ? followUp4 : followUp5),
                input, schema, 600);
        return question(order, "FOLLOW_UP", generated.get("questionText").asText());
    }

    @Override
    public LlmEvaluationResponseDTO evaluate(LlmEvaluationRequestDTO request) {
        String text = request.getContext().getDocumentText();
        if (text == null || text.isBlank() || text.length() > MAX_DOCUMENT_CHARACTERS) {
            throw new IllegalArgumentException("평가에 사용할 서류 내용이 올바르지 않습니다.");
        }
        boolean hasPortfolio = request.getContext().getPortfolio() != null;
        byte[] portfolioPdf = request.getContext().getPortfolioPdf();
        if (hasPortfolio && (portfolioPdf == null || portfolioPdf.length == 0)) {
            throw new IllegalArgumentException("평가에 사용할 포트폴리오 PDF가 없습니다.");
        }
        ObjectNode input = settings(request.getContext(), request.getInterviewDifficulty());
        input.put("documentText", text);
        input.put("hasPortfolio", hasPortfolio);
        input.set("questionAnswers", answers(request.getQuestionAnswers()));
        if (request.getPreviousResult() == null) {
            input.putNull("previousResult");
        } else {
            var previous = request.getPreviousResult();
            ObjectNode summary = input.putObject("previousResult");
            summary.put("documentConsistencyScore", previous.getDocumentConsistencyScore());
            summary.put("expertiseScore", previous.getExpertiseScore());
            summary.put("problemSolvingScore", previous.getProblemSolvingScore());
            summary.put("logicScore", previous.getLogicScore());
            summary.put("communicationScore", previous.getCommunicationScore());
            summary.put("totalScore", previous.getTotalScore());
            summary.put("strengths", previous.getStrengths());
            summary.put("weaknesses", previous.getWeaknesses());
            summary.put("improvements", previous.getImprovements());
            if (previous.getInterviewDate() != null) {
                summary.put("interviewDate", java.time.Instant.ofEpochMilli(previous.getInterviewDate().getTime()).toString());
            }
        }
        ObjectNode schema = object();
        for (String score : new String[] {"documentConsistencyScore", "expertiseScore", "problemSolvingScore", "logicScore", "communicationScore"}) {
            property(schema, score, mapper.createObjectNode().put("type", "number").put("minimum", 0).put("maximum", 100));
        }
        for (String feedback : new String[] {"strengths", "weaknesses", "improvements", "comparison"}) {
            property(schema, feedback, mapper.createObjectNode().put("type", "string").put("minLength", 1).put("maxLength", InterviewTextLimits.FEEDBACK));
        }
        return convert(client.generate("evaluation", common + "\n" + answerFormat + "\n" + evaluation,
                input, schema, 1400, hasPortfolio ? portfolioPdf : null), LlmEvaluationResponseDTO.class);
    }

    private ObjectNode settings(LlmInterviewContextDTO context, String difficulty) {
        ObjectNode input = mapper.createObjectNode();
        if (context.getResumeDetail() != null && context.getResumeDetail().getResume() != null) {
            var resume = context.getResumeDetail().getResume();
            input.put("occupationCode", resume.getOccupationCode());
            input.put("occupation", resume.getOccupationName());
            input.put("jobCode", resume.getJobCode());
            input.put("job", resume.getJobName());
        }
        input.put("interviewDifficulty", difficulty);
        if (context.getRecruitment() != null) {
            var recruitment = context.getRecruitment();
            ObjectNode target = input.putObject("recruitment");
            target.put("companyName", recruitment.getCompanyName());
            target.put("title", recruitment.getTitle());
            target.put("jobName", recruitment.getJobName());
            target.put("locationName", recruitment.getLocationName());
            target.put("experienceLevel", recruitment.getExperienceLevel());
        }
        return input;
    }

    private ObjectNode questionSettings(LlmInterviewContextDTO context, String difficulty) {
        ObjectNode input = settings(context, difficulty);
        input.put("difficultyGuide", switch (difficulty) {
            case "EASY" -> easyDifficulty;
            case "NORMAL" -> normalDifficulty;
            case "HARD" -> hardDifficulty;
            default -> throw new IllegalArgumentException("면접 난이도가 올바르지 않습니다.");
        });
        return input;
    }

    private com.fasterxml.jackson.databind.node.ArrayNode answers(List<InterviewQuestionAnswerDTO> answers) {
        var compact = mapper.createArrayNode();
        for (int index = 0; index < answers.size(); index++) {
            InterviewQuestionAnswerDTO answer = answers.get(index);
            if (answer.getQuestionOrder() != index + 1) {
                throw new IllegalArgumentException("질문·답변의 순서가 올바르지 않습니다.");
            }
            ObjectNode item = compact.addObject();
            item.put("question", answer.getQuestion());
            item.put("answer", answer.getAnswer());
            if (answer.isTimedOut()) item.put("timedOut", true);
        }
        return compact;
    }

    private ObjectNode questionTextSchema() {
        return mapper.createObjectNode().put("type", "string").put("minLength", 1).put("maxLength", InterviewTextLimits.QUESTION);
    }

    private InterviewQuestionDTO question(int order, String type, String text) {
        InterviewQuestionDTO question = new InterviewQuestionDTO();
        question.setQuestionOrder(order);
        question.setQuestionType(type);
        question.setQuestionText(text);
        return question;
    }

    private ObjectNode object() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object").put("additionalProperties", false);
        schema.putObject("properties");
        schema.putArray("required");
        return schema;
    }

    private void property(ObjectNode schema, String name, ObjectNode definition) {
        ((ObjectNode) schema.get("properties")).set(name, definition);
        ((com.fasterxml.jackson.databind.node.ArrayNode) schema.get("required")).add(name);
    }

    private <T> T convert(JsonNode value, Class<T> type) {
        try { return mapper.treeToValue(value, type); }
        catch (IOException exception) { throw new IllegalStateException("AI 면접 결과를 해석하지 못했습니다."); }
    }

    private String prompt(String name) {
        try (var stream = getClass().getResourceAsStream("/interview/prompts/" + name + ".txt")) {
            if (stream == null) throw new IllegalStateException("면접 프롬프트 파일이 없습니다: " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException exception) { throw new IllegalStateException("면접 프롬프트를 읽지 못했습니다."); }
    }
}
