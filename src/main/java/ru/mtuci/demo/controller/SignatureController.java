package ru.mtuci.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.mtuci.demo.model.Signature;
import ru.mtuci.demo.model.User;
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


    @GetMapping("/all")
    public ResponseEntity<List<Signature>> getAllSignatures() {
        List<Signature> signatures = signatureService.getAllActiveSignatures();
        return ResponseEntity.ok(signatures);
    }


    @GetMapping("/getByTime")
    public ResponseEntity<List<Signature>> getSignatures(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {

        List<Signature> signatures = (since == null)
                ? signatureService.getAllActiveSignatures()
                : signatureService.getSignaturesUpdatedSince(since);

        return ResponseEntity.ok(signatures);
    }


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

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String username = (String) authentication.getPrincipal();
            return userRepository.findByEmail(username).orElseThrow(() -> new IllegalArgumentException("User not found"));
        }
        return null;
    }
}
