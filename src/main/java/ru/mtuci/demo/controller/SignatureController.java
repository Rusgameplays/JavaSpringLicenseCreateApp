package ru.mtuci.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import ru.mtuci.demo.model.Audit;
import ru.mtuci.demo.model.Signature;
import ru.mtuci.demo.model.StatusSignature;
import ru.mtuci.demo.model.User;
import ru.mtuci.demo.repo.AuditRepository;
import ru.mtuci.demo.repo.UserRepository;
import ru.mtuci.demo.services.SignatureService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/signatures")
public class SignatureController {
    @Autowired
    private SignatureService signatureService;
    private final UserRepository userRepository;
    private final AuditRepository auditRepository;

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String username = (String) authentication.getPrincipal();
            return userRepository.findByEmail(username).orElseThrow(() -> new IllegalArgumentException("User not found"));
        }
        return null;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<Signature> createSignature(@RequestBody Signature signatureEntity) {
        try {
            User authenticatedUser = getAuthenticatedUser();
            Signature createdSignature = signatureService.createSignature(signatureEntity, authenticatedUser.getEmail());
            return new ResponseEntity<>(createdSignature, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/update")
    public ResponseEntity<Signature> updateSignature(@RequestParam UUID id, @RequestBody Signature signatureEntity) {
        try {
            User authenticatedUser = getAuthenticatedUser();
            Signature updatedSignature = signatureService.updateSignature(id, signatureEntity, authenticatedUser.getEmail());
            return new ResponseEntity<>(updatedSignature, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<Signature>> getAllSignatures() {
        List<Signature> signatures = signatureService.getAllActiveSignatures();
        return ResponseEntity.ok(signatures);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/deleted")
    public ResponseEntity<List<Signature>> getDeletedSignatures() {
        List<Signature> signatures = signatureService.getSignaturesByStatus(StatusSignature.DELETED);
        return ResponseEntity.ok(signatures);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/corrupted")
    public ResponseEntity<List<Signature>> getCorruptedSignatures() {
        List<Signature> signatures = signatureService.getSignaturesByStatus(StatusSignature.CORRUPTED);
        return ResponseEntity.ok(signatures);
    }

    @GetMapping("/audit")
    public ResponseEntity<List<Audit>> getAllAuditRecords() {
        List<Audit> auditRecords = auditRepository.findAll();
        return ResponseEntity.ok(auditRecords);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/getByTime")
    public ResponseEntity<List<Signature>> getSignatures(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {

        List<Signature> signatures = (since == null)
                ? signatureService.getAllActiveSignatures()
                : signatureService.getSignaturesUpdatedSince(since);

        return ResponseEntity.ok(signatures);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/getByIds")
    public ResponseEntity<List<Signature>> getSignaturesByIds(@RequestBody List<UUID> ids) {
        List<Signature> signatures = signatureService.getSignaturesByIds(ids);
        return ResponseEntity.ok(signatures);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteSignature(@PathVariable UUID id) {
        User authenticatedUser = getAuthenticatedUser();
        signatureService.deleteSignature(id, authenticatedUser.getEmail());
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/export", produces = MediaType.MULTIPART_MIXED_VALUE)
    public ResponseEntity<MultiValueMap<String, Object>> exportSignatures() throws Exception {
        List<Signature> signatures = signatureService.getAllActiveSignatures();

        byte[] dataFile = signatureService.serializeSignaturesToBinary(signatures);
        byte[] manifestFile = signatureService.buildManifest(signatures);

        ByteArrayResource dataResource = new ByteArrayResource(dataFile) {
            @Override
            public String getFilename() {
                return "data.bin";
            }
        };

        ByteArrayResource manifestResource = new ByteArrayResource(manifestFile) {
            @Override
            public String getFilename() {
                return "manifest.bin";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(dataResource, createHeaders("data.bin", "application/octet-stream")));
        body.add("file", new HttpEntity<>(manifestResource, createHeaders("manifest.bin", "text/plain")));

        return ResponseEntity.ok()
                .contentType(MediaType.MULTIPART_MIXED)
                .body(body);
    }

    private HttpHeaders createHeaders(String filename, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDisposition(ContentDisposition.builder("attachment").filename(filename).build());
        return headers;
    }



}
