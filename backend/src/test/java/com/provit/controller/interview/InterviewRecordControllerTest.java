package com.provit.controller.interview;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.springframework.http.HttpStatus;

import com.provit.dto.interview.InterviewHistoryDTO;
import com.provit.dto.interview.InterviewResultDTO;
import com.provit.service.interview.InterviewService;
import com.provit.common.GlobalExceptionHandler;
import com.provit.service.interview.InterviewMaintenanceException;

public class InterviewRecordControllerTest {
    @Test
    public void maintenanceResponseTellsTheBrowserToBlockAndDiscardProgress() {
        var response = new GlobalExceptionHandler().handleInterviewMaintenance(new InterviewMaintenanceException());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(true, response.getBody().getData().get("maintenance"));
        assertEquals(true, response.getBody().getData().get("restartRequired"));
        assertTrue(response.getBody().getData().get("message").toString().contains("23:55~00:00"));
    }

    @Test
    public void anonymousRequestsAreRejectedBeforeAccessingRecords() {
        InterviewService unusedService = fakeService((name, args) -> { throw new AssertionError("인증 전에 기록을 조회했습니다."); });
        InterviewController controller = new InterviewController(unusedService, null);
        assertEquals(HttpStatus.UNAUTHORIZED, controller.getResults(null).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, controller.getRecord(null, 17).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, controller.getSession(null, 17).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, controller.discardSession(null, 17).getStatusCode());
    }

    @Test
    public void recordListUsesAuthenticatedUser() {
        InterviewResultDTO result = new InterviewResultDTO();
        result.setHistoryNum(17);
        InterviewController controller = new InterviewController(fakeService((name, args) -> {
            assertEquals("getInterviewResultList", name);
            assertEquals(7, args[0]);
            return List.of(result);
        }), null);
        var response = controller.getResults(7L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(17, response.getBody().getData().get(0).getHistoryNum());
    }

    @Test
    public void detailsCombineQuestionsAnswersAndScoresForAuthenticatedOwner() {
        AtomicInteger calls = new AtomicInteger();
        InterviewHistoryDTO history = new InterviewHistoryDTO();
        history.setQuestion1("첫 질문");
        history.setAnswer1("첫 답변");
        InterviewResultDTO result = new InterviewResultDTO();
        result.setTotalScore(80);
        InterviewController controller = new InterviewController(fakeService((name, args) -> {
            assertEquals(17, args[0]);
            assertEquals(7, args[1]);
            calls.incrementAndGet();
            return name.equals("getInterviewHistory") ? history : result;
        }), null);
        var response = controller.getRecord(7L, 17);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("첫 질문", response.getBody().getData().getHistory().getQuestion1());
        assertEquals("첫 답변", response.getBody().getData().getHistory().getAnswer1());
        assertEquals(80, response.getBody().getData().getResult().getTotalScore(), 0.001);
        assertEquals(2, calls.get());
    }

    @Test
    public void missingOrOtherUsersRecordsReturnNotFoundWithoutData() {
        InterviewController controller = new InterviewController(fakeService((name, args) -> {
            assertEquals(8, args[1]);
            return null;
        }), null);
        var response = controller.getRecord(8L, 17);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody().getData());
    }

    @Test
    public void incompleteRecordDoesNotExposePartialData() {
        InterviewController controller = new InterviewController(fakeService((name, args) ->
                name.equals("getInterviewHistory") ? new InterviewHistoryDTO() : null), null);
        var response = controller.getRecord(7L, 17);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody().getData());
    }

    private InterviewService fakeService(ServiceCall call) {
        return (InterviewService) Proxy.newProxyInstance(InterviewService.class.getClassLoader(),
                new Class<?>[] { InterviewService.class }, (proxy, method, args) -> call.run(method.getName(), args));
    }

    private interface ServiceCall { Object run(String name, Object[] args); }
}
