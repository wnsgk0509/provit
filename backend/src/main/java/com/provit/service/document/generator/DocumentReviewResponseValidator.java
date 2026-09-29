package com.provit.service.document.generator;

import java.util.HashSet;
import java.util.Objects;
import com.fasterxml.jackson.databind.JsonNode;
import com.provit.service.document.DocumentReviewProcessingException;

final class DocumentReviewResponseValidator {
    void validate(JsonNode result, JsonNode schema, JsonNode input, int pages) {
        if (!matches(result, schema, schema)) reject();
        text(result, "summary", 500);
        strengths(result);
        var documents = input.path("documents");
        for (String key : new String[] { "resume", "coverLetter", "portfolio" }) {
            var feedback = result.path("documentReviews").path(key);
            if (feedback.isNull()) continue;
            text(feedback, "summary", 400);
            strengths(feedback);
            for (var improvement : feedback.path("improvements")) {
                text(improvement, "section", 500); text(improvement, "title", 500);
                text(improvement, "issue", 300); text(improvement, "original", 600);
                text(improvement, "suggestion", 800); text(improvement, "reason", 300);
                if (!"portfolio".equals(key) && !contains(documents.path(key), improvement.path("original").asText())) reject();
            }
        }
        for (var issue : result.path("consistencyIssues")) {
            text(issue, "title", 500); text(issue, "recommendation", 400);
            var types = new HashSet<String>();
            for (var source : issue.path("sources")) {
                text(source, "section", 500); text(source, "text", 600);
                String type = source.path("documentType").asText();
                types.add(type);
                var page = source.path("pageNumber");
                if ("portfolio".equals(type)) {
                    if (pages < 1 || (!page.isNull() && page.asInt() > pages)) reject();
                } else {
                    if (!page.isNull()) reject();
                    if (!contains(documents.path("cover-letter".equals(type) ? "coverLetter" : type), source.path("text").asText())) reject();
                }
            }
            if ("mismatch".equals(issue.path("type").asText()) && types.size() < 2) reject();
        }
        var preparation = result.path("careerPreparation");
        var resume = documents.path("resume");
        for (String key : new String[] { "occupationCode", "occupationName", "jobCode", "jobName" })
            if (!Objects.equals(resume.path(key), preparation.path(key))) reject();
        text(preparation, "summary", 400); text(preparation, "coverageNote", 300);
        if (resume.path("jobName").asText("").isBlank() && resume.path("occupationName").asText("").isBlank()
                && !preparation.path("recommendations").isEmpty()) reject();
        var titles = new HashSet<String>();
        for (var recommendation : preparation.path("recommendations")) {
            text(recommendation, "title", 150); text(recommendation, "reason", 300); text(recommendation, "action", 400);
            if (!titles.add(recommendation.path("title").asText().strip().toLowerCase(java.util.Locale.ROOT))) reject();
        }
    }

    private void strengths(JsonNode node) {
        for (var strength : node.path("strengths")) if (strength.asText().isBlank() || strength.asText().length() > 150) reject();
    }

    private void text(JsonNode node, String key, int max) {
        String text = node.path(key).asText();
        if (text.isBlank() || text.length() > max) reject();
    }

    private boolean contains(JsonNode node, String quote) {
        if (node.isTextual()) return node.asText().contains(quote);
        for (JsonNode child : node) if (contains(child, quote)) return true;
        return false;
    }

    private boolean matches(JsonNode value, JsonNode rule, JsonNode root) {
        if (rule.has("$ref")) return matches(value, root.at(rule.path("$ref").asText().substring(1)), root);
        if (rule.has("anyOf")) {
            for (var candidate : rule.path("anyOf")) if (matches(value, candidate, root)) return true;
            return false;
        }
        var types = rule.path("type");
        boolean correct = types.isTextual() ? type(value, types.asText()) : false;
        if (types.isArray()) for (var candidate : types) correct |= type(value, candidate.asText());
        if (!correct) return false;
        if (rule.has("enum")) {
            boolean found = false;
            for (var candidate : rule.path("enum")) found |= candidate.equals(value);
            if (!found) return false;
        }
        if (value.isNull()) return true;
        if (value.isIntegralNumber() && rule.has("minimum") && value.asLong() < rule.path("minimum").asLong()) return false;
        if (value.isArray()) {
            if (value.size() < rule.path("minItems").asInt(0) || value.size() > rule.path("maxItems").asInt(Integer.MAX_VALUE)) return false;
            for (var item : value) if (!matches(item, rule.path("items"), root)) return false;
        }
        if (value.isObject()) {
            for (var required : rule.path("required")) if (!value.has(required.asText())) return false;
            var fields = value.fieldNames();
            while (fields.hasNext()) {
                var name = fields.next();
                if (!rule.path("properties").has(name) || !matches(value.path(name), rule.path("properties").path(name), root)) return false;
            }
        }
        return true;
    }

    private boolean type(JsonNode value, String name) {
        switch (name) {
            case "object": return value.isObject();
            case "array": return value.isArray();
            case "string": return value.isTextual();
            case "integer": return value.isIntegralNumber() && value.canConvertToInt();
            case "null": return value.isNull();
            default: return false;
        }
    }

    private void reject() {
        throw new DocumentReviewProcessingException("AI 첨삭 결과의 형식 또는 원문 근거를 검증하지 못했습니다. 기록을 확인해 주세요.", 502);
    }
}
