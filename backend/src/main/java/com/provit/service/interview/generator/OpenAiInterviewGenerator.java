package com.provit.service.interview.generator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.provit.dto.interview.*;

@Component
public class OpenAiInterviewGenerator implements InterviewGenerator {
    private static final int MAX_DOCUMENT_CHARACTERS = 9000;
    private final ObjectMapper mapper = new ObjectMapper();
    private final OpenAiInterviewClient client;
    private final String common = prompt("common");
    private final String document = prompt("document");
    private final String followUp4 = prompt("follow-up-4");
    private final String followUp5 = prompt("follow-up-5");
    private final String evaluation = prompt("evaluation");

    public OpenAiInterviewGenerator(OpenAiInterviewClient client) { this.client = client; }

    @Override
    public LlmQuestionResponseDTO generateDocumentQuestions(LlmQuestionRequestDTO request) {
        String text = request.getContext().getDocumentText();
        if (text == null || text.isBlank()) throw new IllegalArgumentException("면접에 사용할 서류 내용이 없습니다.");
        if (text.length() > MAX_DOCUMENT_CHARACTERS) {
            throw new IllegalArgumentException("면접 서류의 합계가 9,000자를 초과합니다. 서류 분량을 줄이거나 포트폴리오 선택을 해제해 주세요.");
        }
        ObjectNode input = settings(request.getContext(), request.getInterviewStyle(), request.getInterviewDifficulty());
        input.put("documentText", text);
        ObjectNode schema = object();
        ObjectNode questions = mapper.createObjectNode().put("type", "array").put("minItems", 3).put("maxItems", 3);
        questions.set("items", questionSchema(1, 3, "DOCUMENT"));
        property(schema, "questions", questions);
        return convert(client.generate("document", common + "\n" + document, input, schema, 900), LlmQuestionResponseDTO.class);
    }

    @Override
    public InterviewQuestionDTO generateFollowUpQuestion(int order, LlmFollowUpRequestDTO request) {
        if (order != 4 && order != 5) throw new IllegalArgumentException("후속 질문 순서가 올바르지 않습니다.");
        ObjectNode input = settings(request.getContext(), request.getInterviewStyle(), request.getInterviewDifficulty());
        input.set("questionAnswers", mapper.valueToTree(request.getQuestionAnswers()));
        return convert(client.generate("follow_up_" + order, common + "\n" + (order == 4 ? followUp4 : followUp5),
                input, questionSchema(order, order, "FOLLOW_UP"), 600), InterviewQuestionDTO.class);
    }

    @Override
    public LlmEvaluationResponseDTO evaluate(LlmEvaluationRequestDTO request) {
        ObjectNode input = settings(request.getContext(), request.getInterviewStyle(), request.getInterviewDifficulty());
        input.set("questionAnswers", mapper.valueToTree(request.getQuestionAnswers()));
        if (request.getPreviousResult() == null) {
            input.putNull("previousResult");
        } else {
            var previous = request.getPreviousResult();
            ObjectNode summary = input.putObject("previousResult");
            summary.put("confidenceScore", previous.getConfidenceScore());
            summary.put("persistenceScore", previous.getPersistenceScore());
            summary.put("expertiseScore", previous.getExpertiseScore());
            summary.put("logicScore", previous.getLogicScore());
            summary.put("deliveryScore", previous.getDeliveryScore());
            summary.put("totalScore", previous.getTotalScore());
            summary.put("strengths", previous.getStrengths());
            summary.put("weaknesses", previous.getWeaknesses());
            summary.put("improvements", previous.getImprovements());
            if (previous.getInterviewDate() != null) {
                summary.put("interviewDate", java.time.Instant.ofEpochMilli(previous.getInterviewDate().getTime()).toString());
            }
        }
        ObjectNode schema = object();
        for (String score : new String[] {"confidenceScore", "persistenceScore", "expertiseScore", "logicScore", "deliveryScore"}) {
            property(schema, score, mapper.createObjectNode().put("type", "number").put("minimum", 0).put("maximum", 100));
        }
        for (String feedback : new String[] {"strengths", "weaknesses", "improvements", "comparison"}) {
            property(schema, feedback, mapper.createObjectNode().put("type", "string").put("minLength", 1).put("maxLength", 250));
        }
        return convert(client.generate("evaluation", common + "\n" + evaluation, input, schema, 1400), LlmEvaluationResponseDTO.class);
    }

    private ObjectNode settings(LlmInterviewContextDTO context, String style, String difficulty) {
        ObjectNode input = mapper.createObjectNode();
        if (context.getJobPreference() != null) {
            input.put("occupation", context.getJobPreference().getOccupationName());
            input.put("job", context.getJobPreference().getJobName());
        }
        input.put("interviewStyle", style);
        input.put("interviewDifficulty", difficulty);
        return input;
    }

    private ObjectNode questionSchema(int minimum, int maximum, String type) {
        ObjectNode schema = object();
        property(schema, "questionOrder", mapper.createObjectNode().put("type", "integer")
                .put("minimum", minimum).put("maximum", maximum));
        ObjectNode questionType = mapper.createObjectNode().put("type", "string");
        questionType.putArray("enum").add(type);
        property(schema, "questionType", questionType);
        property(schema, "questionText", mapper.createObjectNode().put("type", "string")
                .put("minLength", 1).put("maxLength", 120));
        return schema;
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
