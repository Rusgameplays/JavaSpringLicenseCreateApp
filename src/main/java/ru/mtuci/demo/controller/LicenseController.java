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
    private final DeviceService deviceService;
    private final LicenseHistoryService licenseHistoryService;
    private final DeviceLicenseRepository deviceLicenseRepository;
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

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping("/renew")
    public ResponseEntity<?> renewLicense(@RequestBody UpdateLicenseRequest updateLicenseRequest) {
        try {

            User authenticatedUser = getAuthenticatedUser();
            if (authenticatedUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Пользователь не авторизован");
            }

            boolean isAdmin = authenticatedUser.getRole() == ApplicationRole.ADMIN;

            Device device = deviceService.getByMac(updateLicenseRequest.getDeviceMac());
            if (device == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Устройство не найдено по указанному MAC адресу");
            }

            DeviceLicense deviceLicense = deviceLicenseRepository.findByDeviceId(device.getId());

            License oldLicense = deviceLicense.getLicense();

            User licenseOwner = oldLicense.getUser();
            if (!isAdmin && !licenseOwner.getEmail().equals(authenticatedUser.getEmail())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Вы не можете продлевать чужую лицензию");
            }

            License newLicense = licenseService.getByKey(updateLicenseRequest.getLicenseKey());
            if (newLicense == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Лицензия не найдена по указанному ключу");
            }

            if (newLicense.getActivationDate() != null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Лицензия уже активирована");
            }

            Integer oldMaxDevices = oldLicense.getLicenseType().getMaxDevices();
            Integer newMaxDevices = newLicense.getLicenseType().getMaxDevices();
            if (oldMaxDevices > newMaxDevices) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Новая лицензия не поддерживает текущее количество устройств. " +
                                "Максимум для старой лицензии: " + oldMaxDevices +
                                ", максимум для новой лицензии: " + newMaxDevices);
            }

            long activeDeviceCount = licenseService.countActiveDevicesForLicense(newLicense);
            if (activeDeviceCount >= newMaxDevices) {
                return ResponseEntity.badRequest().body("Превышено максимальное количество устройств для новой лицензии");
            }

            Integer defaultDuration = newLicense.getLicenseType().getDefaultDuration();
            LocalDate currentExpirationDate = LocalDate.now();
            LocalDate newExpirationDate = currentExpirationDate.plusMonths(defaultDuration);
            Date newExpiration = Date.from(newExpirationDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

            if (newExpiration.after(oldLicense.getExpirationDate())) {
                oldLicense.setExpirationDate(newExpiration);
            }

            oldLicense.setBlocked(true);
            oldLicense.setFlagForBlocked(true);
            licenseService.update(oldLicense);

            List<DeviceLicense> deviceLicenses = deviceLicenseRepository.findByLicenseId(oldLicense.getId());
            for (DeviceLicense dl : deviceLicenses) {
                dl.setLicense(newLicense);
                deviceLicenseRepository.save(dl);
            }

            newLicense.setUser(licenseOwner);
            newLicense.setBlocked(false);
            newLicense.setActivationDate(new Date());
            newLicense.setExpirationDate(newExpiration);
            licenseService.add(newLicense);

            licenseHistoryService.recordLicenseChange(
                    newLicense,
                    licenseOwner,
                    "Renewed",
                    "Лицензия была успешно продлена. Новая дата окончания: " + newExpiration
            );

            Ticket ticket = licenseService.generateTicket(newLicense, device);

            return ResponseEntity.ok(ticket);

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




