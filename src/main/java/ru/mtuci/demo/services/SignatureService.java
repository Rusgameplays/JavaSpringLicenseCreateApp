package ru.mtuci.demo.services;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mtuci.demo.model.Signature;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/signatures")
public interface SignatureService {
    Signature createSignature(Signature signatureEntity);

    Signature updateSignature(UUID id, Signature signatureEntity);

    List<Signature> getAllActiveSignatures();

    @Transactional
    List<Signature> getSignaturesUpdatedSince(LocalDateTime since);

    List<Signature> getSignaturesByIds(List<UUID> ids);

    void deleteSignature(UUID id);
}
