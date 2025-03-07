package ru.mtuci.demo.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mtuci.demo.auth.LoginRequest;
import ru.mtuci.demo.auth.LoginResponse;
import ru.mtuci.demo.configuration.JwtTokenProvider;
import ru.mtuci.demo.model.SessionStatus;

import ru.mtuci.demo.model.UserSession;
import ru.mtuci.demo.repo.UserSessionRepository;
import ru.mtuci.demo.services.TokenService;
import ru.mtuci.demo.services.impl.response.TokenResponse;

import java.util.HashSet;
import java.util.List;


@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final UserSessionRepository userSessionRepository;
    private final JwtTokenProvider jwtTokenProvider;
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Override
    public LoginResponse issueTokenPair(String version, Authentication authentication, LoginRequest request) {
        List<UserSession> existingSessions = userSessionRepository.findByEmail(request.getEmail());
        existingSessions.forEach(session -> {
            session.setStatus(SessionStatus.REVOKED);
        });
        userSessionRepository.saveAll(existingSessions);
        String accessToken = jwtTokenProvider.createAccessToken(request.getEmail(),
                new HashSet<>(authentication.getAuthorities()));

        String refreshToken = jwtTokenProvider.createRefreshToken(request.getEmail(), request.getDeviceId(),
                new HashSet<>(authentication.getAuthorities()));

        UserSession session = UserSession.builder()
                .email(request.getEmail())
                .deviceId(request.getDeviceId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiry(System.currentTimeMillis() + jwtExpiration)
                .refreshTokenExpiry(System.currentTimeMillis() + jwtExpiration * 2)
                .status(SessionStatus.ACTIVE)
                .version(version)
                .build();
        userSessionRepository.save(session);
        return new LoginResponse(request.getEmail(), accessToken, refreshToken);
    }

    @Override
    @Transactional
    public TokenResponse refreshTokenPair(String refreshToken) {
        UserSession session = userSessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (session.getStatus() == SessionStatus.USED || session.getRefreshTokenExpiry() < System.currentTimeMillis()) {
            blockAllSessionsForUser(session.getEmail());
            return null;
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(session.getEmail(),
                new HashSet<>(jwtTokenProvider.getAuthorities(refreshToken)));
        String newRefreshToken = jwtTokenProvider.createRefreshToken(session.getEmail(), session.getDeviceId(),
                new HashSet<>(jwtTokenProvider.getAuthorities(refreshToken)));

        session.setStatus(SessionStatus.USED);
        userSessionRepository.save(session);

        UserSession newSession = UserSession.builder()
                .email(session.getEmail())
                .deviceId(session.getDeviceId())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .accessTokenExpiry(System.currentTimeMillis() + jwtExpiration)
                .refreshTokenExpiry(System.currentTimeMillis() + jwtExpiration * 2)
                .status(SessionStatus.ACTIVE)
                .version(session.getVersion())
                .build();
        userSessionRepository.save(newSession);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }


    @Override
    @Transactional
    public void blockAllSessionsForUser(String email) {
        List<UserSession> sessions = userSessionRepository.findByEmail(email);
        for (UserSession session : sessions) {
            session.setStatus(SessionStatus.REVOKED);
        }
        userSessionRepository.saveAll(sessions);
    }
}
