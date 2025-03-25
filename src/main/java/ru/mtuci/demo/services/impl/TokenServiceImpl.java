package ru.mtuci.demo.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
import java.util.Set;


@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final UserSessionRepository userSessionRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public LoginResponse issueTokenPair(Authentication authentication, LoginRequest request) {

        String accessToken = jwtTokenProvider.createAccessToken(request.getEmail(),
                new HashSet<>(authentication.getAuthorities()));

        String refreshToken = jwtTokenProvider.createRefreshToken(request.getEmail(), request.getDeviceId());

        UserSession session = UserSession.builder()
                .email(request.getEmail())
                .deviceId(request.getDeviceId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiry(jwtTokenProvider.getTokenExpiration(accessToken))
                .refreshTokenExpiry(jwtTokenProvider.getTokenExpiration(refreshToken))
                .status(SessionStatus.ACTIVE)
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

        String oldAccessToken = session.getAccessToken();

        Set<GrantedAuthority> authorities = new HashSet<>(jwtTokenProvider.getAuthorities(oldAccessToken));

        String newAccessToken = jwtTokenProvider.createAccessToken(session.getEmail(), authorities);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(session.getEmail(), session.getDeviceId());

        session.setStatus(SessionStatus.USED);
        userSessionRepository.save(session);

        UserSession newSession = UserSession.builder()
                .email(session.getEmail())
                .deviceId(session.getDeviceId())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .accessTokenExpiry(jwtTokenProvider.getTokenExpiration(newAccessToken))
                .refreshTokenExpiry(jwtTokenProvider.getTokenExpiration(newRefreshToken))
                .status(SessionStatus.ACTIVE)
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
