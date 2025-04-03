package ru.mtuci.demo.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mtuci.demo.model.Audit;
import ru.mtuci.demo.model.History;
import ru.mtuci.demo.model.Signature;
import ru.mtuci.demo.repo.AuditRepository;
import ru.mtuci.demo.repo.HistoryRepository;
import ru.mtuci.demo.repo.SignatureRepository;
import ru.mtuci.demo.services.SignatureService;
import ru.mtuci.demo.model.StatusSignature;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.*;


@Service
public class SignatureServiceImpl implements SignatureService {

    @Autowired
    private SignatureRepository signatureRepository;
    @Autowired
    private HistoryRepository historyRepository;
    @Autowired
    private AuditRepository auditRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String SECRET_KEY = "secret-key";  // потом

    @Override
    public Signature createSignature(Signature signatureEntity, String email) {

        signatureEntity.setUpdatedAt(LocalDateTime.now());

        signatureEntity.setDigitalSignature(generateDigitalSignature(signatureEntity));

        Signature savedSignature = signatureRepository.save(signatureEntity);

        saveAuditRecord(savedSignature.getId(), email, "CREATED", "");

        return savedSignature;
    }

    @Override
    public Signature updateSignature(UUID id, Signature signatureEntity, String email) {

        Signature existingSignature = signatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Сигнатура с данным ID не найдена"));

        saveSignatureHistory(existingSignature);

        saveAuditRecord(existingSignature.getId(), email, "UPDATED", getChangedFields(existingSignature, signatureEntity));

        if (signatureEntity.getThreatName() != null) {
            existingSignature.setThreatName(signatureEntity.getThreatName());
        }
        if (signatureEntity.getFirstBytes() != null) {
            existingSignature.setFirstBytes(signatureEntity.getFirstBytes());
        }
        if (signatureEntity.getRemainderHash() != null) {
            existingSignature.setRemainderHash(signatureEntity.getRemainderHash());
        }
        if (signatureEntity.getRemainderLength() != 0) {
            existingSignature.setRemainderLength(signatureEntity.getRemainderLength());
        }
        if (signatureEntity.getFileType() != null) {
            existingSignature.setFileType(signatureEntity.getFileType());
        }
        if (signatureEntity.getOffsetStart() != 0) {
            existingSignature.setOffsetStart(signatureEntity.getOffsetStart());
        }
        if (signatureEntity.getOffsetEnd() != 0) {
            existingSignature.setOffsetEnd(signatureEntity.getOffsetEnd());
        }
        if (signatureEntity.getStatus() != null) {
            existingSignature.setStatus(signatureEntity.getStatus());
        }

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
    public void deleteSignature(UUID id, String email) {
        Signature signature = signatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Сигнатура с данным ID не найдена"));

        saveSignatureHistory(signature);

        signature.setStatus(StatusSignature.DELETED);
        signature.setUpdatedAt(LocalDateTime.now());

        signatureRepository.save(signature);

        saveAuditRecord(signature.getId(), email, "DELETED", "{\"status\": \"DELETED\"}");
    }

    private void saveSignatureHistory(Signature signature) {
        History history = new History();
        history.setSignatureId(signature.getId());
        history.setVersionCreatedAt(LocalDateTime.now());
        history.setThreatName(signature.getThreatName());
        history.setFirstBytes(signature.getFirstBytes());
        history.setRemainderHash(signature.getRemainderHash());
        history.setRemainderLength(signature.getRemainderLength());
        history.setFileType(signature.getFileType());
        history.setOffsetStart(signature.getOffsetStart());
        history.setOffsetEnd(signature.getOffsetEnd());
        history.setDigitalSignature(signature.getDigitalSignature());
        history.setStatus(signature.getStatus()); // Сохраняем старый статус
        history.setUpdatedAt(signature.getUpdatedAt());

        historyRepository.save(history);
    }

    private void saveAuditRecord(UUID signatureId, String changedBy, String changeType, String fieldsChanged) {
        Audit audit = new Audit();
        audit.setSignatureId(signatureId);
        audit.setChangedBy(changedBy);
        audit.setChangeType(changeType);
        audit.setChangedAt(LocalDateTime.now());
        audit.setFieldsChanged(fieldsChanged);

        auditRepository.save(audit);
    }

    private String getChangedFields(Signature oldSignature, Signature newSignature) {
        Map<String, String> changes = new HashMap<>();

        if (!oldSignature.getThreatName().equals(newSignature.getThreatName())) {
            changes.put("threatName", "Old: " + oldSignature.getThreatName() + " | New: " + newSignature.getThreatName());
        }
        if (!Arrays.equals(oldSignature.getFirstBytes(), newSignature.getFirstBytes())) {
            changes.put("firstBytes", "Old: " + Arrays.toString(oldSignature.getFirstBytes()) + " | New: " + Arrays.toString(newSignature.getFirstBytes()));
        }
        if (!oldSignature.getRemainderHash().equals(newSignature.getRemainderHash())) {
            changes.put("remainderHash", "Old: " + oldSignature.getRemainderHash() + " | New: " + newSignature.getRemainderHash());
        }
        if (oldSignature.getRemainderLength() != newSignature.getRemainderLength()) {
            changes.put("remainderLength", "Old: " + oldSignature.getRemainderLength() + " | New: " + newSignature.getRemainderLength());
        }
        if (!oldSignature.getFileType().equals(newSignature.getFileType())) {
            changes.put("fileType", "Old: " + oldSignature.getFileType() + " | New: " + newSignature.getFileType());
        }
        if (oldSignature.getOffsetStart() != newSignature.getOffsetStart()) {
            changes.put("offsetStart", "Old: " + oldSignature.getOffsetStart() + " | New: " + newSignature.getOffsetStart());
        }
        if (oldSignature.getOffsetEnd() != newSignature.getOffsetEnd()) {
            changes.put("offsetEnd", "Old: " + oldSignature.getOffsetEnd() + " | New: " + newSignature.getOffsetEnd());
        }
        if (oldSignature.getStatus() != newSignature.getStatus()) {
            changes.put("status", "Old: " + oldSignature.getStatus() + " | New: " + newSignature.getStatus());
        }

        try {
            return objectMapper.writeValueAsString(changes);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка при сериализации изменений", e);
        }
    }



}
