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
    private final DeviceService deviceService;
    private final LicenseHistoryService licenseHistoryService;
    private final UserRepository userRepository;
    private final DeviceLicenseService deviceLicenseService;


    @PostMapping("/activate")
    public ResponseEntity<?> activateLicense(@RequestBody LicenseActivationRequest request) {
        try {
            User authenticatedUser = getAuthenticatedUser();

            User user = userRepository.findById(request.getDeviceRequest().getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

            License license = licenseService.getByKey(request.getKey());
            if (license == null) {
                return ResponseEntity.badRequest().body("Лицензия не найдена");
            }

            if (license.getUser() != null && !license.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body("Невозможно активировать лицензию на другого пользователя");
            }

            long activeDeviceCount = licenseService.countActiveDevicesForLicense(license);
            if (activeDeviceCount >= license.getMaxDevices()) {
                return ResponseEntity.badRequest().body("Превышено максимальное количество устройств для данной лицензии");
            }
            Device device = deviceService.registerOrUpdateDevice(request.getDeviceRequest());

            deviceLicenseService.addDeviceToLicense(license, device);

            if (license.getUser() == null) {
                license.setUser(user);
            }
            Integer defaultDuration = license.getLicenseType().getDefaultDuration();

            LocalDateTime currentDateTime = LocalDateTime.now();
            LocalDate currentExpirationDate = currentDateTime.toLocalDate();

            LocalDate newExpirationDate = currentExpirationDate.plusMonths(defaultDuration);
            Date newExpiration = Date.from(newExpirationDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

            if (license.getExpirationDate() == null || newExpiration.after(license.getExpirationDate())) {
                license.setExpirationDate(newExpiration);
            }

            licenseService.updateLicense(license, authenticatedUser);

            licenseHistoryService.recordLicenseChange(license, authenticatedUser, "Activated", "Лицензия успешно активирована");

            Ticket fullTicket = licenseService.generateTicket(license, device);

            return ResponseEntity.ok(fullTicket);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка: " + e.getMessage());
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
