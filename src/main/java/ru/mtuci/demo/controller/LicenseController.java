package ru.mtuci.demo.controller;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.mtuci.demo.configuration.JwtTokenProvider;
import ru.mtuci.demo.controller.requests.LicenseRequest;
import ru.mtuci.demo.controller.requests.UpdateLicenseRequest;
import ru.mtuci.demo.exception.UserNull;
import ru.mtuci.demo.model.*;
import ru.mtuci.demo.repo.DeviceLicenseRepository;
import ru.mtuci.demo.repo.UserRepository;
import ru.mtuci.demo.services.DeviceService;
import ru.mtuci.demo.services.LicenseHistoryService;
import ru.mtuci.demo.services.LicenseService;
import ru.mtuci.demo.services.UserService;
import ru.mtuci.demo.ticket.Ticket;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RequestMapping("/licenses")
@RestController
@PreAuthorize("hasAnyRole('ADMIN')")
public class LicenseController {
    private final LicenseService licenseService;
    private final UserRepository userRepository;

    @PostMapping("/add")
    public ResponseEntity<String> add(@RequestBody LicenseRequest licenseRequest) {
        try {

            License createdLicense = licenseService.createLicense(licenseRequest.getProductId(),
                    licenseRequest.getOwnerId(),
                    licenseRequest.getLicenseTypeId());


            return ResponseEntity.ok("Лицензия успешно создана с ID: " + createdLicense.getId());
        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest().body("Ошибка: " + e.getMessage());
        } catch (UserNull e) {

            return ResponseEntity.badRequest().body("Пользователь не найден");
        } catch (Exception e) {

            return ResponseEntity.status(500).body("Ошибка при создании лицензии: " + e.getMessage());
        }
    }

    //TODO: не совсем понял назначение метода. Нужно обсуждать
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping("/renew")
    public ResponseEntity<?> renewLicense(@RequestBody UpdateLicenseRequest updateLicenseRequest) {
        try {
            User authenticatedUser = getAuthenticatedUser();
            if (authenticatedUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Пользователь не авторизован");
            }

            Ticket ticket = licenseService.renewLicense(updateLicenseRequest, authenticatedUser);

            return ResponseEntity.ok(ticket);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
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




