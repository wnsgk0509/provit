package com.provit.service.interview;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.provit.dto.interview.InterviewResultDTO;
import com.provit.dto.interview.InterviewResultResponseDTO;
import com.provit.dto.interview.LlmEvaluationResponseDTO;

public class InterviewEvaluationMappingTest {
    private static final List<String> SCORE_FIELDS = List.of(
            "documentConsistencyScore", "expertiseScore", "problemSolvingScore",
            "logicScore", "communicationScore", "totalScore");
    private static final List<String> SCORE_COLUMNS = List.of(
            "DOCUMENT_CONSISTENCY_SCORE", "EXPERTISE_SCORE", "PROBLEM_SOLVING_SCORE",
            "LOGIC_SCORE", "COMMUNICATION_SCORE", "TOTAL_SCORE");
    private static final String NAMESPACE = "com.provit.mapper.interview.InterviewResultMapper.";

    @Test
    public void schemaAndEveryQueryUseTheSameScoreColumns() throws Exception {
        var configuration = mapperConfiguration();
        try (var stream = getClass().getResourceAsStream("/sql_query/schema.sql")) {
            assertNotNull(stream);
            String schema = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            String table = schema.substring(schema.indexOf("CREATE TABLE T_INTERVIEW_RESULT"));
            assertEquals(SCORE_COLUMNS, scoreColumns(table.substring(0, table.indexOf(";"))));
        }
        for (String statement : List.of("insertInterviewResult", "selectInterviewResultListByUserNum",
                "selectInterviewResult", "selectLatestInterviewResultByUserNum")) {
            String sql = configuration.getMappedStatement(NAMESPACE + statement)
                    .getBoundSql(new InterviewResultDTO()).getSql();
            assertEquals(statement, SCORE_COLUMNS, scoreColumns(sql));
        }
    }

    @Test
    public void insertParametersAndResultMapMatchTheDtoScoreProperties() throws Exception {
        var configuration = mapperConfiguration();
        var parameters = configuration.getMappedStatement(NAMESPACE + "insertInterviewResult")
                .getBoundSql(new InterviewResultDTO()).getParameterMappings();
        assertEquals(SCORE_FIELDS, parameters.stream().map(parameter -> parameter.getProperty())
                .filter(property -> property.endsWith("Score")).toList());
        var mappings = configuration.getResultMap(NAMESPACE + "interviewResultMap").getResultMappings();
        assertEquals(SCORE_FIELDS, mappings.stream().map(mapping -> mapping.getProperty())
                .filter(property -> property.endsWith("Score")).toList());
        for (int index = 0; index < SCORE_FIELDS.size(); index++) {
            String field = SCORE_FIELDS.get(index);
            var mapping = mappings.stream().filter(item -> item.getProperty().equals(field)).findFirst().orElseThrow();
            assertEquals(SCORE_COLUMNS.get(index), mapping.getColumn());
            assertTrue(configuration.getReflectorFactory().findForClass(InterviewResultDTO.class).hasSetter(field));
        }
    }

    @Test
    public void storedResultsApiResponsesAndAiResponsesExposeExactlyTheSameScores() {
        var mapper = new ObjectMapper();
        for (Object dto : List.of(new InterviewResultDTO(), new InterviewResultResponseDTO(),
                new LlmEvaluationResponseDTO())) {
            var fields = new ArrayList<String>();
            mapper.valueToTree(dto).fieldNames().forEachRemaining(name -> {
                if (name.endsWith("Score")) fields.add(name);
            });
            assertEquals(SCORE_FIELDS, fields);
        }
    }

    private Configuration mapperConfiguration() throws Exception {
        var configuration = new Configuration();
        String resource = "/mappers/interview/interview_result_mapper.xml";
        try (var stream = getClass().getResourceAsStream(resource)) {
            assertNotNull(stream);
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration;
    }

    private List<String> scoreColumns(String sql) {
        var columns = new ArrayList<String>();
        var matcher = Pattern.compile("\\b[A-Z_]+_SCORE\\b").matcher(sql);
        while (matcher.find()) columns.add(matcher.group());
        return columns;
    }
}
