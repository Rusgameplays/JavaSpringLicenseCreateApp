package ru.mtuci.demo.services;

import org.springframework.security.core.Authentication;
import ru.mtuci.demo.auth.LoginRequest;
import ru.mtuci.demo.auth.LoginResponse;
import ru.mtuci.demo.services.impl.response.TokenResponse;


public interface TokenService {

    LoginResponse issueTokenPair(String version, Authentication authentication, LoginRequest request);
    TokenResponse refreshTokenPair(String refreshToken);
    void blockAllSessionsForUser(String email);
}
