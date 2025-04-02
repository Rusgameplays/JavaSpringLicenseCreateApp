package ru.mtuci.demo.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mtuci.demo.model.Signature;
import ru.mtuci.demo.repo.SignatureRepository;
import ru.mtuci.demo.services.SignatureService;
import ru.mtuci.demo.model.StatusSignature;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Service
public class SignatureServiceImpl implements SignatureService {

    @Autowired
    private SignatureRepository signatureRepository;

    private static final String SECRET_KEY = "secret-key";  // потом

    @Override
    public Signature createSignature(Signature signatureEntity) {

        signatureEntity.setUpdatedAt(LocalDateTime.now());

        signatureEntity.setDigitalSignature(generateDigitalSignature(signatureEntity));

        return signatureRepository.save(signatureEntity);
    }

    @Override
    public Signature updateSignature(UUID id, Signature signatureEntity) {

        Signature existingSignature = signatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Сигнатура с данным ID не найдена"));

        existingSignature.setThreatName(signatureEntity.getThreatName());
        existingSignature.setFirstBytes(signatureEntity.getFirstBytes());
        existingSignature.setRemainderHash(signatureEntity.getRemainderHash());
        existingSignature.setRemainderLength(signatureEntity.getRemainderLength());
        existingSignature.setFileType(signatureEntity.getFileType());
        existingSignature.setOffsetStart(signatureEntity.getOffsetStart());
        existingSignature.setOffsetEnd(signatureEntity.getOffsetEnd());
        existingSignature.setStatus(signatureEntity.getStatus());
        existingSignature.setUpdatedAt(LocalDateTime.now());

        existingSignature.setDigitalSignature(generateDigitalSignature(existingSignature));

        return signatureRepository.save(existingSignature);
    }

    private byte[] generateDigitalSignature(Signature signatureEntity) {
        try {
            String data = serializeSignatureData(signatureEntity);

            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey);

            return mac.doFinal(data.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при создании цифровой подписи", e);
        }
    }

    private String serializeSignatureData(Signature signatureEntity) {
        return signatureEntity.getThreatName() +
                new String(signatureEntity.getFirstBytes()) +
                signatureEntity.getRemainderHash() +
                signatureEntity.getFileType() +
                signatureEntity.getOffsetStart() +
                signatureEntity.getOffsetEnd();
    }

    @Transactional
    @Override
    public List<Signature> getAllActiveSignatures() {
        return signatureRepository.findByStatus(StatusSignature.ACTUAL);
    }

    @Transactional
    @Override
    public List<Signature> getSignaturesUpdatedSince(LocalDateTime since) {
        return signatureRepository.findByUpdatedAtAfter(since);
    }

    @Override
    public List<Signature> getSignaturesByIds(List<UUID> ids) {
        return signatureRepository.findAllById(ids);
    }

    @Override
    public void deleteSignature(UUID id) {
        Signature signature = signatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Сигнатура с данным ID не найдена"));

        signature.setStatus(StatusSignature.DELETED);
        signature.setUpdatedAt(LocalDateTime.now());  // Обновляем время изменения

        signatureRepository.save(signature);
    }



}
