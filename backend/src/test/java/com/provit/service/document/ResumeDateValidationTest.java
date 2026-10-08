package com.provit.service.document;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.Cookie;

import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.provit.common.GlobalExceptionHandler;
import com.provit.common.auth.AuthCookieService;
import com.provit.controller.document.DocumentController;
import com.provit.dao.document.DocumentDAO;
import com.provit.dto.document.CareerDTO;
import com.provit.dto.document.CertificationDTO;
import com.provit.dto.document.EducationDTO;
import com.provit.dto.document.ResumeDTO;
import com.provit.dto.document.ResumeDetailDTO;
import com.provit.service.document.impl.DocumentServiceImpl;
import com.provit.util.jwt.JwtProvider;

public class ResumeDateValidationTest {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Test
    public void futureDatesAreRejectedOnCreateAndUpdateBeforeAnyDatabaseCalls() {
        DocumentDAO dao = (DocumentDAO) Proxy.newProxyInstance(DocumentDAO.class.getClassLoader(),
                new Class<?>[] { DocumentDAO.class }, (proxy, method, args) -> {
                    throw new AssertionError("Unexpected database call: " + method.getName());
                });
        var service = new DocumentServiceImpl(dao, null);
        for (String field : List.of("admissionDate", "joinDate", "issueDate")) {
            var request = request();
            setDate(request, field, date(LocalDate.now(SEOUL).plusDays(1)));
            String expected = label(field) + "은 오늘 이후 날짜를 선택할 수 없습니다.";
            assertEquals(expected, assertThrows(IllegalArgumentException.class,
                    () -> service.createResume(7, request)).getMessage());
            assertEquals(expected, assertThrows(IllegalArgumentException.class,
                    () -> service.updateResume(7, 1, request)).getMessage());
        }
    }

    @Test
    public void todayPastAndMissingDatesAreAllowedOnCreateAndUpdate() {
        LocalDate today = LocalDate.now(SEOUL);
        // Compare calendar dates, including a time later than now on the same day.
        Date endOfToday = Date.from(today.atTime(LocalTime.MAX).atZone(SEOUL).toInstant());
        for (Date value : new Date[] { endOfToday, date(today.minusDays(1)), null }) {
            var request = request();
            for (String field : List.of("admissionDate", "joinDate", "issueDate")) {
                setDate(request, field, value);
            }
            var writes = new AtomicInteger();
            var service = service(request, writes);
            assertSame(request, service.createResume(7, request));
            assertEquals(4, writes.get());
            assertNotNull(service.updateResume(7, 1, request));
            assertEquals(11, writes.get());
        }
    }

    @Test
    public void futureGraduationAndResignationDatesRemainAllowed() {
        var request = request();
        Date today = date(LocalDate.now(SEOUL));
        request.getEducationList().get(0).setAdmissionDate(today);
        request.getCareerList().get(0).setJoinDate(today);
        Date future = date(LocalDate.now(SEOUL).plusDays(30));
        request.getEducationList().get(0).setGraduationDate(future);
        request.getCareerList().get(0).setResignDate(future);
        var writes = new AtomicInteger();
        var service = service(request, writes);
        service.createResume(7, request);
        service.updateResume(7, 1, request);
        assertEquals(11, writes.get());
    }

    @Test
    public void apiReturnsBadRequestForEachFutureDateOnCreateAndUpdate() throws Exception {
        var request = request();
        var writes = new AtomicInteger();
        var mvc = mvc(request, writes);
        var mapper = new ObjectMapper();
        for (String field : List.of("admissionDate", "joinDate", "issueDate")) {
            var invalid = request();
            setDate(invalid, field, date(LocalDate.now(SEOUL).plusDays(1)));
            String body = mapper.writeValueAsString(invalid);
            for (var call : List.of(post("/api/documents/resumes"), put("/api/documents/resumes/1"))) {
                var response = mvc.perform(call.cookie(new Cookie("provit_access", "owner"))
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse();
                assertEquals(400, response.getStatus());
                assertEquals(label(field) + "은 오늘 이후 날짜를 선택할 수 없습니다.",
                        mapper.readTree(response.getContentAsByteArray()).path("data").asText());
            }
        }
        assertEquals(0, writes.get());
    }

    @Test
    public void apiCreatesAndUpdatesResumeWithHighestLevelAndSchoolHistory() throws Exception {
        var request = request();
        var writes = new AtomicInteger();
        var mvc = mvc(request, writes);
        var mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(request);
        var calls = List.of(post("/api/documents/resumes"), put("/api/documents/resumes/1"));
        for (int index = 0; index < calls.size(); index++) {
            var response = mvc.perform(calls.get(index).cookie(new Cookie("provit_access", "owner"))
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse();
            assertEquals(index == 0 ? 201 : 200, response.getStatus());
            var detail = mapper.readTree(response.getContentAsByteArray()).path("data");
            assertEquals("대학교", detail.path("resume").path("highestLevel").asText());
            assertEquals("대학교", detail.path("educationList").get(0).path("schoolName").asText());
            assertEquals("재학", detail.path("educationList").get(0).path("educationStatus").asText());
        }
        assertEquals(11, writes.get());
    }

    private MockMvc mvc(ResumeDetailDTO request, AtomicInteger writes) {
        JwtProvider jwt = new JwtProvider(null) {
            @Override public boolean validateToken(String token) { return "owner".equals(token); }
            @Override public Long getUserNum(String token) { return 7L; }
        };
        var cookies = new AuthCookieService();
        ReflectionTestUtils.setField(cookies, "cookieName", "provit_access");
        return MockMvcBuilders.standaloneSetup(new DocumentController(service(request, writes), jwt, cookies))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private ResumeDetailDTO request() {
        var resume = new ResumeDTO();
        resume.setResumeTitle("지원 이력서");
        resume.setHighestLevel("대학교");
        resume.setOccupationCode("2");
        resume.setJobCode("84");
        var education = new EducationDTO();
        education.setSchoolName("대학교");
        education.setEducationStatus("재학");
        var career = new CareerDTO();
        career.setCompanyName("회사");
        var certification = new CertificationDTO();
        certification.setCertName("자격증");
        var request = new ResumeDetailDTO();
        request.setResume(resume);
        request.setEducationList(List.of(education));
        request.setCareerList(List.of(career));
        request.setCertificationList(List.of(certification));
        return request;
    }

    private DocumentServiceImpl service(ResumeDetailDTO request, AtomicInteger writes) {
        DocumentDAO dao = (DocumentDAO) Proxy.newProxyInstance(DocumentDAO.class.getClassLoader(),
                new Class<?>[] { DocumentDAO.class }, (proxy, method, args) -> switch (method.getName()) {
                    case "selectResumeJob", "selectResume" -> request.getResume();
                    case "selectEducationList" -> request.getEducationList();
                    case "selectCareerList" -> request.getCareerList();
                    case "selectCertificationList" -> request.getCertificationList();
                    case "insertResume", "updateResume", "insertEducation", "insertCareer",
                            "insertCertification", "deleteEducationList", "deleteCareerList",
                            "deleteCertificationList" -> { writes.incrementAndGet(); yield 1; }
                    default -> throw new AssertionError("Unexpected database call: " + method.getName());
                });
        return new DocumentServiceImpl(dao, null);
    }

    private void setDate(ResumeDetailDTO request, String field, Date value) {
        switch (field) {
            case "admissionDate" -> request.getEducationList().get(0).setAdmissionDate(value);
            case "joinDate" -> request.getCareerList().get(0).setJoinDate(value);
            case "issueDate" -> request.getCertificationList().get(0).setIssueDate(value);
            default -> throw new AssertionError(field);
        }
    }

    private String label(String field) {
        return switch (field) {
            case "admissionDate" -> "입학일";
            case "joinDate" -> "입사일";
            case "issueDate" -> "취득일";
            default -> throw new AssertionError(field);
        };
    }

    private Date date(LocalDate day) {
        return Date.from(day.atStartOfDay(SEOUL).toInstant());
    }
}
