package ru.mtuci.demo.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mtuci.demo.model.Signature;
import ru.mtuci.demo.model.StatusSignature;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SignatureRepository extends JpaRepository<Signature, UUID> {
    List<Signature> findByStatus(StatusSignature status);
    List<Signature> findByUpdatedAtAfter(LocalDateTime updatedAt);
}
