package com.provit.mapper;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.Test;

import com.provit.dto.auth.UserDTO;
import com.provit.dto.document.ResumeDTO;

public class ResumeJobMapperTest {
    @Test
    public void resumeQueriesBindCodesAndJoinNamesWhileUserQueriesDoNotReferenceCodes() throws Exception {
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
            assertFalse(userSql.contains("JOB_CODE"));
            assertFalse(userSql.contains("OCCUPATION_CODE"));
        }
    }

    private void load(Configuration configuration, String resource) throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream);
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }
}
