package com.provit.common.resolver;

import com.provit.common.annotation.LoginUser;
import com.provit.common.auth.AuthCookieService;
import com.provit.util.jwt.JwtProvider;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;

@Component
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtProvider jwtProvider;
    private final AuthCookieService authCookieService;

    public LoginUserArgumentResolver(JwtProvider jwtProvider, AuthCookieService authCookieService) {
        this.jwtProvider = jwtProvider;
        this.authCookieService = authCookieService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class) &&
               parameter.getParameterType().equals(Long.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {
        
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        String token = authCookieService.getAccessToken(request).orElse(null);
        if (token == null) return null;
        if (!jwtProvider.validateToken(token)) {
            return null;
        }
        
        return jwtProvider.getUserNum(token);
    }
}
