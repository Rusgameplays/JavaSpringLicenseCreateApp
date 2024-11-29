package ru.mtuci.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.mtuci.demo.controller.requests.LicenseActivationRequest;
import ru.mtuci.demo.model.Device;
import ru.mtuci.demo.model.License;
import ru.mtuci.demo.model.User;
import ru.mtuci.demo.repo.UserRepository;
import ru.mtuci.demo.services.DeviceLicenseService;
import ru.mtuci.demo.services.DeviceService;
import ru.mtuci.demo.services.LicenseHistoryService;
import ru.mtuci.demo.services.LicenseService;
import ru.mtuci.demo.ticket.Ticket;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class ActivationController {

    private final LicenseService licenseService;
    private final UserRepository userRepository;

    @PostMapping("/activate")
    public ResponseEntity<?> activateLicense(@RequestBody LicenseActivationRequest request) {
        try {
            User authenticatedUser = getAuthenticatedUser();
            Ticket fullTicket = licenseService.activateLicense(request, authenticatedUser);
            return ResponseEntity.ok(fullTicket);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка: " + ex.getMessage());
        }
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
