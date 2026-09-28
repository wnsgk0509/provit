package com.provit.controller.document;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.provit.common.GlobalExceptionHandler;
import com.provit.dao.auth.UserDAO;
import com.provit.dao.document.DocumentDAO;
import com.provit.dto.auth.UserDTO;
import com.provit.dto.document.PortfolioDTO;
import com.provit.service.document.impl.DocumentServiceImpl;
import com.provit.service.document.storage.PortfolioFileStorage;
import com.provit.util.jwt.JwtProvider;

public class PortfolioSecurityTest {
    private final byte[] pdf = "%PDF-1.7\nsecurity fixture".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    private final AtomicInteger fileReads = new AtomicInteger();
    private final AtomicInteger portfolioReads = new AtomicInteger();
    private final AtomicInteger deletes = new AtomicInteger();
    private PortfolioDTO portfolio;
    private JwtProvider jwt;
    private MockMvc mvc;
    private Map<Long, UserDTO> users;

    @Before
    public void setUp() {
        UserDTO owner = user(7, "USER");
        UserDTO other = user(8, "ADMIN");
        users = Map.of(7L, owner, 8L, other);
        UserDAO usersDAO = (UserDAO) Proxy.newProxyInstance(UserDAO.class.getClassLoader(),
                new Class<?>[] { UserDAO.class }, (proxy, method, args) -> {
                    if (method.getName().equals("selectByUserNum")) return users.get(args[0]);
                    throw new AssertionError("Unexpected user DAO call: " + method.getName());
                });
        jwt = new JwtProvider(usersDAO);
        ReflectionTestUtils.setField(jwt, "secretKeyPlain", "portfolio-security-test-key-with-32-bytes-minimum");
        ReflectionTestUtils.setField(jwt, "expirationTime", 60000L);
        jwt.init();

        portfolio = new PortfolioDTO();
        portfolio.setPortfolioNum(15);
        portfolio.setUserNum(7);
        portfolio.setOriginalFileName("지원 포트폴리오.pdf");
        portfolio.setSavedFileName("d0fddf3e-8469-485e-a170-c1b280cab21d.pdf");
        portfolio.setFileUrl("D:/privateFileStorage_Provit/portfolio_uploadfile/" + portfolio.getSavedFileName());
        DocumentDAO dao = (DocumentDAO) Proxy.newProxyInstance(DocumentDAO.class.getClassLoader(),
                new Class<?>[] { DocumentDAO.class }, (proxy, method, args) -> {
                    if (method.getName().equals("selectPortfolioByPortfolioNum")) {
                        portfolioReads.incrementAndGet();
                        return args[0].equals(15) ? portfolio : null;
                    }
                    if (method.getName().equals("deletePortfolio")) {
                        deletes.incrementAndGet();
                        return 1;
                    }
                    throw new AssertionError("Unexpected document DAO call: " + method.getName());
                });
        PortfolioFileStorage storage = (PortfolioFileStorage) Proxy.newProxyInstance(
                PortfolioFileStorage.class.getClassLoader(), new Class<?>[] { PortfolioFileStorage.class },
                (proxy, method, args) -> {
                    assertEquals("loadAsResource", method.getName());
                    fileReads.incrementAndGet();
                    assertEquals(portfolio.getFileUrl(), args[0]);
                    return new ByteArrayResource(pdf);
                });
        var controller = new DocumentController(new DocumentServiceImpl(dao, storage), jwt);
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    public void missingInvalidAndExpiredJwtReturn401BeforeReadingPortfolio() throws Exception {
        assertEquals(401, mvc.perform(get("/api/documents/portfolios/15/file")).andReturn().getResponse().getStatus());
        assertEquals(401, mvc.perform(get("/api/documents/portfolios/15/file")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-jwt")).andReturn().getResponse().getStatus());
        ReflectionTestUtils.setField(jwt, "expirationTime", -1000L);
        assertEquals(401, mvc.perform(get("/api/documents/portfolios/15/file")
                .header(HttpHeaders.AUTHORIZATION, token(7))).andReturn().getResponse().getStatus());
        assertEquals(0, portfolioReads.get());
        assertEquals(0, fileReads.get());
    }

    @Test
    public void jwtExpiringBetweenValidationAndClaimExtractionStillReturns401() throws Exception {
        JwtProvider expiringJwt = new JwtProvider(null) {
            @Override public boolean validateToken(String token) { return true; }
            @Override public Long getUserNum(String token) {
                throw new io.jsonwebtoken.ExpiredJwtException(null, null, "Expired during claim extraction");
            }
        };
        var expirationMvc = MockMvcBuilders.standaloneSetup(new DocumentController(null, expiringJwt))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        assertEquals(401, expirationMvc.perform(get("/api/documents/portfolios/15/file")
                .accept(MediaType.APPLICATION_PDF).header(HttpHeaders.AUTHORIZATION, "Bearer fixture"))
                .andReturn().getResponse().getStatus());
    }

    @Test
    public void anotherUserCannotDownloadEvenWithAdminRoleOrSpoofedUserParameter() throws Exception {
        var response = mvc.perform(get("/api/documents/portfolios/15/file").param("userNum", "7")
                .accept(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.AUTHORIZATION, token(8))).andReturn().getResponse();
        assertEquals(403, response.getStatus());
        assertEquals(0, fileReads.get());
    }

    @Test
    public void ownerDownloadsPdfWithPrivateCacheHeadersAndOriginalFilename() throws Exception {
        var response = mvc.perform(get("/api/documents/portfolios/15/file").param("userNum", "8")
                .header(HttpHeaders.AUTHORIZATION, token(7))).andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertArrayEquals(pdf, response.getContentAsByteArray());
        assertEquals("application/pdf", response.getContentType());
        assertEquals("no-store", response.getHeader(HttpHeaders.CACHE_CONTROL));
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertEquals("지원 포트폴리오.pdf", org.springframework.http.ContentDisposition
                .parse(response.getHeader(HttpHeaders.CONTENT_DISPOSITION)).getFilename());
    }

    @Test
    public void legacyFileWithoutOriginalFilenameStillDownloads() throws Exception {
        portfolio.setOriginalFileName(null);
        var response = mvc.perform(get("/api/documents/portfolios/15/file")
                .header(HttpHeaders.AUTHORIZATION, token(7))).andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertEquals("15.pdf", org.springframework.http.ContentDisposition
                .parse(response.getHeader(HttpHeaders.CONTENT_DISPOSITION)).getFilename());
    }

    @Test
    public void metadataRequiresOwnerAndNeverRevealsPhysicalPathOrSavedFilename() throws Exception {
        assertEquals(401, mvc.perform(get("/api/documents/portfolios/15")).andReturn().getResponse().getStatus());
        assertEquals(403, mvc.perform(get("/api/documents/portfolios/15")
                .header(HttpHeaders.AUTHORIZATION, token(8))).andReturn().getResponse().getStatus());
        var response = mvc.perform(get("/api/documents/portfolios/15")
                .header(HttpHeaders.AUTHORIZATION, token(7))).andReturn().getResponse();
        assertEquals(200, response.getStatus());
        var data = new ObjectMapper().readTree(response.getContentAsByteArray()).path("data");
        assertEquals("지원 포트폴리오.pdf", data.path("originalFileName").asText());
        assertFalse(data.has("fileUrl"));
        assertFalse(data.has("savedFileName"));
        assertEquals(0, fileReads.get());
    }

    @Test
    public void missingPortfolioReturns404AndForeignDeleteReturns403() throws Exception {
        assertEquals(404, mvc.perform(get("/api/documents/portfolios/99/file")
                .accept(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.AUTHORIZATION, token(7))).andReturn().getResponse().getStatus());
        assertEquals(403, mvc.perform(delete("/api/documents/portfolios/15")
                .header(HttpHeaders.AUTHORIZATION, token(8))).andReturn().getResponse().getStatus());
        assertEquals(0, deletes.get());
        assertEquals(0, fileReads.get());
    }

    private String token(int userNum) { return "Bearer " + jwt.createToken(users.get((long) userNum)); }

    private UserDTO user(int number, String role) {
        UserDTO user = new UserDTO();
        user.setUserNum((long) number);
        user.setUserType(role);
        user.setUserIsDeleted(0);
        user.setUserTokenVersion(0);
        return user;
    }
}
