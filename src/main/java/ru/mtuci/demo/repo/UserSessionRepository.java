package ru.mtuci.demo.repo;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mtuci.demo.model.SessionStatus;
import ru.mtuci.demo.model.UserSession;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByAccessToken(String accessToken);

    Optional<UserSession> findByRefreshToken(String refreshToken);

    List<UserSession> findByEmail(String email);

    List<UserSession> findByEmailAndStatus(String email, SessionStatus status);

    boolean existsByDeviceIdAndEmail(String deviceId, String email);

    void deleteByAccessToken(String accessToken);
}
