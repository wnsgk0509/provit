package com.provit.service.auth;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.provit.dao.auth.UserDAO;
import com.provit.dto.auth.LoginRequestDTO;
import com.provit.dto.auth.SignupRequestDTO;
import com.provit.dto.auth.UserDTO;
import com.provit.dto.auth.UserResponseDTO;
import com.provit.service.auth.impl.AuthServiceImpl;
import com.provit.util.jwt.JwtProvider;

public class UserJobCodesTest {

    @Test
    public void existingSignupWithoutCodesAndBlankCodesBothSucceed() {
        for (String code : new String[] { null, "  " }) {
            Fixture fixture = new Fixture();
            var request = fixture.request();
            request.setOccupationCode(code);
            request.setJobCode(code);
            var response = fixture.service.signup(request);
            assertEquals(Long.valueOf(7), response.getUserNum());
            assertNull(response.getOccupationCode());
            assertNull(response.getJobCode());
            assertNull(fixture.saved.get().getOccupationCode());
            assertNull(fixture.saved.get().getJobCode());
        }
    }

    @Test
    public void codesReachInsertSignupLoginAndProfileResponses() throws Exception {
        Fixture fixture = new Fixture();
        var request = fixture.request();
        request.setOccupationCode(" 2 ");
        request.setJobCode(" 84 ");
        assertCodes(fixture.service.signup(request));
        assertEquals("2", fixture.saved.get().getOccupationCode());
        assertEquals("84", fixture.saved.get().getJobCode());
        assertFalse(request.getUserPw().equals(fixture.saved.get().getUserPw()));

        var login = new LoginRequestDTO();
        login.setUserEmail(request.getUserEmail());
        login.setUserPw(request.getUserPw());
        assertCodes(fixture.service.login(login).getUser());
        var profile = fixture.service.getUserProfile(7L);
        assertCodes(profile);
        var json = new ObjectMapper().valueToTree(profile);
        assertEquals("2", json.path("occupationCode").asText());
        assertEquals("84", json.path("jobCode").asText());
        assertFalse(json.has("userPw"));
    }

    @Test
    public void occupationWithoutJobRemainsOptional() {
        Fixture fixture = new Fixture();
        var request = fixture.request();
        request.setOccupationCode("2");
        var response = fixture.service.signup(request);
        assertEquals("2", response.getOccupationCode());
        assertNull(response.getJobCode());
    }

    @Test
    public void oversizedCodesAreRejectedBeforeInsert() {
        Fixture fixture = new Fixture();
        var request = fixture.request();
        request.setOccupationCode("2".repeat(21));
        assertThrows(IllegalArgumentException.class, () -> fixture.service.signup(request));
        request.setOccupationCode("2");
        request.setJobCode("8".repeat(21));
        assertThrows(IllegalArgumentException.class, () -> fixture.service.signup(request));
        assertNull(fixture.saved.get());
    }

    private void assertCodes(UserResponseDTO response) {
        assertEquals("2", response.getOccupationCode());
        assertEquals("84", response.getJobCode());
    }

    private static class Fixture {
        final AtomicReference<UserDTO> saved = new AtomicReference<>();
        final AtomicReference<String> verificationCode = new AtomicReference<>();
        final AuthServiceImpl service;

        Fixture() {
            UserDAO dao = (UserDAO) Proxy.newProxyInstance(UserDAO.class.getClassLoader(),
                    new Class<?>[] { UserDAO.class }, (proxy, method, args) -> switch (method.getName()) {
                        case "countByEmail", "countByNickname" -> 0;
                        case "insertUser" -> {
                            UserDTO user = (UserDTO) args[0];
                            user.setUserNum(7L);
                            user.setUserTokenVersion(0);
                            saved.set(user);
                            yield 1;
                        }
                        case "selectByEmail", "selectByUserNum" -> saved.get();
                        default -> throw new AssertionError("Unexpected DAO call: " + method.getName());
                    });
            JwtProvider jwt = new JwtProvider(dao) {
                @Override
                public String createToken(UserDTO user) { return "local-test-token"; }

                @Override
                public String createRefreshToken(UserDTO user) { return "local-test-refresh-token"; }
            };
            service = new AuthServiceImpl(dao, new BCryptPasswordEncoder(4), jwt,
                    (email, code) -> { verificationCode.set(code); return true; });
        }

        SignupRequestDTO request() {
            SignupRequestDTO request = new SignupRequestDTO();
            request.setUserName("테스터");
            request.setUserNickname("테스트회원");
            request.setUserEmail("local-test@example.com");
            request.setUserPw("Test-pass123!");
            request.setConfirmPw(request.getUserPw());
            service.sendVerificationEmail(request.getUserEmail());
            request.setVerificationToken(service.verifyEmailCode(request.getUserEmail(), verificationCode.get()));
            return request;
        }
    }
}
