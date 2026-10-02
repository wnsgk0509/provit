package com.provit.service.document;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.lang.reflect.Proxy;
import java.sql.SQLException;
import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.provit.common.GlobalExceptionHandler;
import com.provit.common.auth.AuthCookieService;
import com.provit.common.resolver.LoginUserArgumentResolver;
import com.provit.controller.document.MainResumeController;
import com.provit.dao.auth.UserDAO;
import com.provit.dao.document.MainResumeDAO;
import com.provit.dto.auth.UserDTO;
import com.provit.dto.document.MainResumeJobInfoDTO;
import com.provit.service.document.impl.MainResumeServiceImpl;
import com.provit.util.jwt.JwtProvider;

public class MainResumeControllerTest {
    private static final String URL = "/api/documents/main-resume";
    private final ObjectMapper json = new ObjectMapper();
    private MockMvc mvc;
    private Cookie cookie;
    private Long selected;
    private int writes;
    private boolean resumeDeletedDuringSet;

    @Before
    public void setup() {
        UserDTO user = UserDTO.builder().userNum(7L).userTokenVersion(0).userIsDeleted(0).build();
        UserDAO users = (UserDAO) Proxy.newProxyInstance(UserDAO.class.getClassLoader(),
                new Class<?>[] { UserDAO.class }, (proxy, method, args) -> user);
        JwtProvider jwt = new JwtProvider(users);
        ReflectionTestUtils.setField(jwt, "secretKeyPlain", "main-resume-test-secret-key-32-bytes-long");
        ReflectionTestUtils.setField(jwt, "expirationTime", 60_000L);
        jwt.init();
        AuthCookieService cookies = new AuthCookieService();
        ReflectionTestUtils.setField(cookies, "cookieName", "provit_access");
        cookie = new Cookie("provit_access", jwt.createToken(user));
        MainResumeDAO dao = new MainResumeDAO() {
            public int updateMainResume(long userNum, long resumeNum) {
                assertEquals(7L, userNum);
                if (resumeDeletedDuringSet) {
                    throw new DataIntegrityViolationException("이력서 삭제 경합",
                            new SQLException("parent key missing", "23000", 2291));
                }
                if (resumeNum != 11 && resumeNum != 12) return 0;
                writes++;
                selected = resumeNum;
                return 1;
            }
            public int clearMainResume(long userNum) {
                assertEquals(7L, userNum);
                writes++;
                selected = null;
                return 1;
            }
            public MainResumeJobInfoDTO selectMainResumeJobInfo(long userNum) {
                assertEquals(7L, userNum);
                if (selected == null) return null;
                var result = new MainResumeJobInfoDTO();
                result.setResumeNum(selected);
                result.setOccupationCode("2");
                result.setJobCode(selected == 11 ? "84" : "85");
                return result;
            }
        };
        mvc = MockMvcBuilders.standaloneSetup(new MainResumeController(new MainResumeServiceImpl(dao)))
                .setCustomArgumentResolvers(new LoginUserArgumentResolver(jwt, cookies))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    public void setChangeReadAndClearUseAuthenticatedUser() throws Exception {
        assertTrue(json.readTree(send(get(URL).cookie(cookie), 200).getContentAsString()).path("data").isNull());
        var response = send(put(URL).cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"resumeNum\":11,\"userNum\":999}"), 200);
        assertEquals(11, json.readTree(response.getContentAsString()).path("data").path("resumeNum").asLong());
        response = send(put(URL).cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"resumeNum\":12}"), 200);
        assertEquals("85", json.readTree(response.getContentAsString()).path("data").path("jobCode").asText());
        assertEquals(12, json.readTree(send(get(URL).cookie(cookie), 200).getContentAsString()).path("data").path("resumeNum").asLong());
        send(delete(URL).cookie(cookie), 200);
        send(delete(URL).cookie(cookie), 200);
        assertNull(selected);
        assertTrue(json.readTree(send(get(URL).cookie(cookie), 200).getContentAsString()).path("data").isNull());
    }

    @Test
    public void missingAndInvalidAuthenticationCannotReadOrWrite() throws Exception {
        for (Cookie auth : new Cookie[] { null, new Cookie("provit_access", "invalid-token") }) {
            for (var request : new MockHttpServletRequestBuilder[] { get(URL), delete(URL),
                    put(URL).contentType(MediaType.APPLICATION_JSON).content("{\"resumeNum\":11}") }) {
                if (auth != null) request.cookie(auth);
                send(request, 401);
            }
        }
        assertEquals(0, writes);
    }

    @Test
    public void invalidNumbersAndMalformedRequestsAreBadRequests() throws Exception {
        for (String body : new String[] { "{}", "{\"resumeNum\":null}", "{\"resumeNum\":0}",
                "{\"resumeNum\":-1}", "{\"resumeNum\":1000000000000000000}", "{\"resumeNum\":\"abc\"}",
                "{\"resumeNum\":11.2}", "{\"resumeNum\":\"11\"}", "{\"resumeNum\":true}",
                "{\"resumeNum\":9223372036854775808}", "{" }) {
            send(put(URL).cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(body), 400);
        }
        send(put(URL).cookie(cookie).contentType(MediaType.APPLICATION_JSON), 400);
        assertEquals(0, writes);
    }

    @Test
    public void inaccessibleResumeDoesNotChangeCurrentDesignation() throws Exception {
        send(put(URL).cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"resumeNum\":11}"), 200);
        send(put(URL).cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"resumeNum\":22}"), 404);
        assertEquals(Long.valueOf(11), selected);
    }

    @Test
    public void resumeDeletedDuringDesignationReturnsNotFound() throws Exception {
        send(put(URL).cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"resumeNum\":11}"), 200);
        resumeDeletedDuringSet = true;
        send(put(URL).cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"resumeNum\":12}"), 404);
        assertEquals(Long.valueOf(11), selected);
    }

    private MockHttpServletResponse send(MockHttpServletRequestBuilder request, int status) throws Exception {
        var response = mvc.perform(request).andReturn().getResponse();
        assertEquals(response.getContentAsString(), status, response.getStatus());
        return response;
    }
}
