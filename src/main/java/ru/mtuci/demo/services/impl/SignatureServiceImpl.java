package ru.mtuci.demo.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
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
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
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

    private static final Logger logger = LoggerFactory.getLogger(SignatureServiceImpl.class);
    private LocalDateTime lastCheckTime = LocalDateTime.now().minusDays(1);

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
        history.setStatus(signature.getStatus());
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

    @Scheduled(cron = "0 0 0 * * *")
    public void scheduledSignatureVerification() {
        logger.info("Запущена плановая проверка ЭЦП");

        List<Signature> updatedSignatures = getSignaturesUpdatedSince(lastCheckTime);
        LocalDateTime currentCheckTime = LocalDateTime.now();

        for (Signature signature : updatedSignatures) {
            try {
                byte[] actualSignature = generateDigitalSignature(signature);
                if (!Arrays.equals(signature.getDigitalSignature(), actualSignature)) {
                    logger.error("Несовпадение ЭЦП у сигнатуры с ID: {}", signature.getId());

                    saveSignatureHistory(signature);

                    signature.setStatus(StatusSignature.CORRUPTED);
                    signature.setUpdatedAt(currentCheckTime);
                    signatureRepository.save(signature);

                    saveAuditRecord(signature.getId(), "AutoCheck", "CORRUPTED", "{\"digitalSignature\": \"mismatch\"}");
                }
            } catch (Exception e) {
                logger.error("Ошибка при проверке ЭЦП сигнатуры с ID: {}", signature.getId(), e);
            }
        }

        lastCheckTime = currentCheckTime;
    }

    public byte[] serializeSignaturesToBinary(List<Signature> signatures) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (Signature sig : signatures) {

            ByteBuffer uuidBuffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
            uuidBuffer.putLong(sig.getId().getMostSignificantBits());
            uuidBuffer.putLong(sig.getId().getLeastSignificantBits());
            baos.write(uuidBuffer.array());

            byte[] threatNameBytes = sig.getThreatName().getBytes(StandardCharsets.UTF_8);
            ByteBuffer threatNameLength = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(threatNameBytes.length);
            baos.write(threatNameLength.array());
            baos.write(threatNameBytes);

            baos.write(sig.getFirstBytes());

            byte[] remainderHashBytes = sig.getRemainderHash().getBytes(StandardCharsets.UTF_8);
            ByteBuffer remainderHashLength = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(remainderHashBytes.length);
            baos.write(remainderHashLength.array());
            baos.write(remainderHashBytes);

            ByteBuffer remainderLength = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(sig.getRemainderLength());
            baos.write(remainderLength.array());

            byte[] fileTypeBytes = sig.getFileType().getBytes(StandardCharsets.UTF_8);
            ByteBuffer fileTypeLength = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(fileTypeBytes.length);
            baos.write(fileTypeLength.array());
            baos.write(fileTypeBytes);

            ByteBuffer offsetStart = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(sig.getOffsetStart());
            baos.write(offsetStart.array());

            ByteBuffer offsetEnd = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(sig.getOffsetEnd());
            baos.write(offsetEnd.array());
        }

        return baos.toByteArray();
    }


    public byte[] buildManifest(List<Signature> signatures) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        baos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(signatures.size()).array());

        for (Signature sig : signatures) {
            baos.write(ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
                    .putLong(sig.getId().getMostSignificantBits())
                    .putLong(sig.getId().getLeastSignificantBits())
                    .array());

            byte[] digitalSignature = sig.getDigitalSignature();
            baos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(digitalSignature.length).array());
            baos.write(digitalSignature);
        }

        byte[] manifestData = baos.toByteArray();
        byte[] manifestSignature = generateManifestSignature(manifestData);

        baos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(manifestSignature.length).array());
        baos.write(manifestSignature);

        return baos.toByteArray();
    }


//    private void writeIntLE(DataOutputStream dos, int value) throws IOException {
//        dos.writeByte(value & 0xFF);
//        dos.writeByte((value >> 8) & 0xFF);
//        dos.writeByte((value >> 16) & 0xFF);
//        dos.writeByte((value >> 24) & 0xFF);
//    }
//
//    private void writeLongLE(DataOutputStream dos, long value) throws IOException {
//        dos.writeByte((int)(value) & 0xFF);
//        dos.writeByte((int)(value >> 8) & 0xFF);
//        dos.writeByte((int)(value >> 16) & 0xFF);
//        dos.writeByte((int)(value >> 24) & 0xFF);
//        dos.writeByte((int)(value >> 32) & 0xFF);
//        dos.writeByte((int)(value >> 40) & 0xFF);
//        dos.writeByte((int)(value >> 48) & 0xFF);
//        dos.writeByte((int)(value >> 56) & 0xFF);
//    }
//
//    private void writeBytesWithLengthLE(DataOutputStream dos, byte[] bytes) throws IOException {
//        writeIntLE(dos, bytes.length);
//        dos.write(bytes);
//    }


    private byte[] generateManifestSignature(byte[] data) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKey);
        return mac.doFinal(data);
    }



}
