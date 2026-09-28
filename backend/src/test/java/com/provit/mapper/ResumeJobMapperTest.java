package com.provit.mapper;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.junit.Test;

import com.provit.dto.auth.UserDTO;
import com.provit.dto.document.ResumeDTO;

public class ResumeJobMapperTest {
    @Test
    public void resumeAndUserQueriesBothBindTheirOwnJobCodes() throws Exception {
        Configuration configuration = new Configuration();
        load(configuration, "mappers/document/resume-mapper.xml");
        load(configuration, "mappers/user-mapper.xml");
        ResumeDTO resume = new ResumeDTO();
        resume.setOccupationCode("2");
        resume.setJobCode("84");
        String namespace = "com.provit.mapper.document.ResumeMapper.";
        for (String id : new String[] { "insertResume", "updateResume" }) {
            var sql = configuration.getMappedStatement(namespace + id).getBoundSql(resume);
            assertTrue(sql.getParameterMappings().stream().anyMatch(p -> p.getProperty().equals("occupationCode")));
            assertTrue(sql.getParameterMappings().stream().anyMatch(p -> p.getProperty().equals("jobCode")));
        }
        String select = configuration.getMappedStatement(namespace + "selectResume")
                .getBoundSql(Map.of("resumeNum", 1, "userNum", 7)).getSql();
        assertTrue(select.contains("LEFT JOIN T_JOB"));
        assertTrue(select.contains("LEFT JOIN T_OCCUPATION"));
        assertTrue(select.contains("J.JOB_NAME"));
        assertTrue(select.contains("O.OCCUPATION_NAME"));
        for (String id : new String[] { "selectByEmail", "selectByNickname", "selectByUserNum", "insertUser" }) {
            String userSql = configuration.getMappedStatement("com.provit.mapper.UserMapper." + id)
                    .getBoundSql(new UserDTO()).getSql();
            assertTrue(userSql.contains("JOB_CODE"));
            assertTrue(userSql.contains("OCCUPATION_CODE"));
        }
        var mappings = configuration.getResultMap("com.provit.mapper.UserMapper.UserResultMap").getResultMappings();
        assertTrue(mappings.stream().anyMatch(p -> p.getProperty().equals("occupationCode")
                && p.getColumn().equals("OCCUPATION_CODE")));
        assertTrue(mappings.stream().anyMatch(p -> p.getProperty().equals("jobCode")
                && p.getColumn().equals("JOB_CODE")));

        // 코드를 선택하지 않은 기존 화면도 Oracle에 타입을 지정한 NULL을 전달한다.
        var insert = configuration.getMappedStatement("com.provit.mapper.UserMapper.insertUser")
                .getBoundSql(new UserDTO());
        for (String property : new String[] { "occupationCode", "jobCode" }) {
            var parameter = insert.getParameterMappings().stream()
                    .filter(p -> p.getProperty().equals(property)).findFirst().orElseThrow();
            assertEquals(JdbcType.VARCHAR, parameter.getJdbcType());
        }
    }

    @Test
    public void nicknameOrPasswordUpdatesDoNotOverwriteUserJobCodes() throws Exception {
        Configuration configuration = new Configuration();
        load(configuration, "mappers/user-mapper.xml");
        UserDTO user = UserDTO.builder().userNum(7L).userNickname("새닉네임").userPw("encoded").build();
        String sql = configuration.getMappedStatement("com.provit.mapper.UserMapper.updateMyProfile")
                .getBoundSql(user).getSql();
        assertTrue(sql.contains("USER_NICKNAME"));
        assertTrue(sql.contains("USER_PW"));
        assertFalse(sql.contains("OCCUPATION_CODE"));
        assertFalse(sql.contains("JOB_CODE"));
    }

    @Test
    public void restoredUserPreferenceQueryLoadsWithItsDtoAndFiltersWithdrawnUsers() throws Exception {
        Configuration configuration = new Configuration();
        load(configuration, "mappers/interview/user_job_mapper.xml");
        var statement = configuration.getMappedStatement(
                "com.provit.mapper.interview.UserJobMapper.selectUserJobPreferenceByUserNum");
        assertEquals(com.provit.dto.user.UserJobPreferenceDTO.class, statement.getResultMaps().get(0).getType());
        String sql = statement.getBoundSql(7).getSql();
        assertTrue(sql.contains("FROM T_USER U"));
        assertTrue(sql.contains("U.USER_IS_DELETED = 0"));
        assertTrue(sql.contains("U.OCCUPATION_CODE"));
        assertTrue(sql.contains("U.JOB_CODE"));
        assertFalse(sql.contains("T_RESUME"));
    }

    private void load(Configuration configuration, String resource) throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream);
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }
}
