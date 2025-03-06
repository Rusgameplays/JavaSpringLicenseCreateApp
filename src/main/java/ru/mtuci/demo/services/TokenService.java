package ru.mtuci.demo.services;

import org.springframework.security.core.Authentication;
import ru.mtuci.demo.auth.LoginRequest;
import ru.mtuci.demo.auth.LoginResponse;
import ru.mtuci.demo.model.UserSession;

public interface TokenService {

    LoginResponse issueTokenPair(String version, Authentication authentication, LoginRequest request);

    void blockAllSessionsForUser(String email);
}
