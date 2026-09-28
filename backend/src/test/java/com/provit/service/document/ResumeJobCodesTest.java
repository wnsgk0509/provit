package com.provit.service.document;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.provit.dao.document.DocumentDAO;
import com.provit.dto.document.ResumeDTO;
import com.provit.dto.document.ResumeDetailDTO;
import com.provit.service.document.impl.DocumentServiceImpl;

public class ResumeJobCodesTest {

    @Test
    public void createUsesValidatedCodesAndDatabaseNames() {
        AtomicInteger writes = new AtomicInteger();
        DocumentServiceImpl service = service(true, writes);
        ResumeDetailDTO request = request(" 2 ", " 84 ");
        request.getResume().setOccupationName("클라이언트 직군");
        request.getResume().setJobName("클라이언트 직무");

        ResumeDTO saved = service.createResume(7, request).getResume();

        assertEquals(7, saved.getUserNum());
        assertEquals("2", saved.getOccupationCode());
        assertEquals("84", saved.getJobCode());
        assertEquals("IT개발·데이터", saved.getOccupationName());
        assertEquals("백엔드/서버개발", saved.getJobName());
        assertEquals(1, writes.get());
    }

    @Test
    public void mismatchedOrUnknownCodesCannotBeCreatedOrUpdated() {
        AtomicInteger writes = new AtomicInteger();
        DocumentServiceImpl service = service(false, writes);
        assertThrows(IllegalArgumentException.class, () -> service.createResume(7, request("2", "310")));
        assertThrows(IllegalArgumentException.class, () -> service.updateResume(7, 1, request("2", "310")));
        assertEquals(0, writes.get());
    }

    @Test
    public void missingCodesAreRejectedBeforeDatabaseWrites() {
        AtomicInteger writes = new AtomicInteger();
        DocumentServiceImpl service = service(true, writes);
        assertThrows(IllegalArgumentException.class, () -> service.createResume(7, request(null, "84")));
        assertThrows(IllegalArgumentException.class, () -> service.createResume(7, request("2", " ")));
        assertThrows(IllegalArgumentException.class, () -> service.updateResume(7, 1, request("", "84")));
        assertEquals(0, writes.get());
    }

    @Test
    public void updateUsesValidatedCodesAndReturnsJoinedNames() {
        AtomicInteger writes = new AtomicInteger();
        ResumeDTO saved = service(true, writes).updateResume(7, 1, request("2", "84")).getResume();
        assertEquals("2", saved.getOccupationCode());
        assertEquals("84", saved.getJobCode());
        assertEquals("백엔드/서버개발", saved.getJobName());
        assertEquals(1, writes.get());
    }

    private ResumeDetailDTO request(String occupation, String job) {
        ResumeDTO resume = new ResumeDTO();
        resume.setResumeTitle("지원 이력서");
        resume.setHighestLevel("대학교");
        resume.setEducationCode(3);
        resume.setOccupationCode(occupation);
        resume.setJobCode(job);
        ResumeDetailDTO detail = new ResumeDetailDTO();
        detail.setResume(resume);
        return detail;
    }

    private DocumentServiceImpl service(boolean validPair, AtomicInteger writes) {
        ResumeDTO databaseResume = request("2", "84").getResume();
        databaseResume.setOccupationName("IT개발·데이터");
        databaseResume.setJobName("백엔드/서버개발");
        DocumentDAO dao = (DocumentDAO) Proxy.newProxyInstance(DocumentDAO.class.getClassLoader(),
                new Class<?>[] { DocumentDAO.class }, (proxy, method, args) -> switch (method.getName()) {
                    case "selectResumeJob" -> {
                        if (validPair) {
                            assertEquals("2", args[0]);
                            assertEquals("84", args[1]);
                        }
                        yield validPair ? databaseResume : null;
                    }
                    case "countEducationCode" -> 1;
                    case "selectResume" -> databaseResume;
                    case "insertResume", "updateResume" -> {
                        ResumeDTO resume = (ResumeDTO) args[0];
                        assertEquals("2", resume.getOccupationCode());
                        assertEquals("84", resume.getJobCode());
                        assertEquals("백엔드/서버개발", resume.getJobName());
                        writes.incrementAndGet();
                        yield 1;
                    }
                    case "deleteEducationList", "deleteCareerList", "deleteCertificationList" -> 0;
                    default -> null;
                });
        return new DocumentServiceImpl(dao, null);
    }
}
